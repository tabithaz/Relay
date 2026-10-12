# Relay

Relay is an evolving collaborative workspace for engineering teams. The current backend provides workspace management and incident coordination. The long-term goal is a general-purpose workspace for projects, tasks, collaboration, notifications, and team workflows.

## Implemented

- Java 21 / Spring Boot REST API backed by PostgreSQL
- Stateless OAuth2 JWT bearer authentication with issuer, audience, signature, and expiry validation
- Workspace membership with OWNER, ADMIN, EDITOR, and VIEWER roles; new workspace creators become owners atomically
- Workspace-scoped read/write authorization for workspaces, projects, and tasks; list results only include the caller's workspaces
- Owner-managed membership creation and paginated member listing
- Create, list, retrieve, and rename workspaces with immutable, unique URL-friendly slugs
- Paginated workspace listing (default 20, maximum 100) with deterministic ordering
- Workspace name and slug validation; HTTP 404 for missing workspaces and 409 for duplicate slugs
- Create, list, retrieve, and update projects within a workspace; project slugs are unique per workspace and immutable
- Create, list, retrieve, and update project tasks with priority, due dates, and status transitions
- Filter tasks by status with stable pagination and strict workspace/project-scoped lookups
- Project listing uses bounded, stable pagination and scoped project lookups
- Create and list incidents
- Change incident status (investigating, identified, monitoring, resolved)
- Persist an activity entry when an incident is created or its status changes
- Retrieve incident activity in chronological order
- Validate incident requests, including required status and database-compatible text lengths
- Transactional database writes, service tests, HTTP controller tests, and GitHub Actions checks

## Local development

Requires Java 21, Maven, Docker Compose, and an OpenID Connect-compatible identity provider that publishes signing keys (JWKS). Relay validates access tokens; it does not issue them.

Configure the issuer, signing-key endpoint, and expected API audience (replace the example values with your provider's real configuration):

```bash
export RELAY_JWT_ISSUER="https://identity.example.com/realms/relay"
export RELAY_JWT_JWKS_URI="https://identity.example.com/realms/relay/protocol/openid-connect/certs"
export RELAY_JWT_AUDIENCE="relay-api"
docker compose up -d
mvn spring-boot:run
```

Obtain an access token for this API from your identity provider, then set `RELAY_ACCESS_TOKEN` before running the requests below. The issuer and audience must match the token claims. Missing, expired, incorrectly signed, or otherwise invalid tokens return HTTP 401. Do not commit tokens or credentials. Use HTTPS for your identity provider outside local development.

The API runs at `http://localhost:8080`. The PostgreSQL configuration in `docker-compose.yml` is for local development only; use external secrets for non-local deployments.

### Workspace API

Create a workspace:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X POST http://localhost:8080/api/workspaces \
  -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","slug":"engineering"}'
```

List workspaces (newest first):

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" 'http://localhost:8080/api/workspaces?page=0&size=20'
```

Retrieve or rename a workspace (replace `1` with its ID):

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" http://localhost:8080/api/workspaces/1
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X PATCH http://localhost:8080/api/workspaces/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Platform Engineering"}'
```

Workspace slugs must be 3–63 characters, lowercase letters and numbers separated by single hyphens. Slugs are unique and cannot be changed after creation. Names may be changed, but must not be blank or exceed 120 characters. List responses use Spring Data's paginated JSON representation with `content`, `totalElements`, `totalPages`, `number`, and `size`.

### Membership and permissions

Creating a workspace makes the authenticated JWT subject its `OWNER` in the same database transaction. The workspace list shows only workspaces the caller belongs to. All workspace/project/task endpoints require membership; reads permit all roles, while writes require `OWNER`, `ADMIN`, or `EDITOR`. `VIEWER` is read-only. Unauthorized access returns HTTP 403.

Only an `OWNER` can add or list members. The `subject` must match the new member's stable OIDC `sub` claim from the configured identity provider. Adding another `OWNER` is not supported yet, and duplicate memberships return HTTP 409.

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X POST http://localhost:8080/api/workspaces/1/members \
  -H 'Content-Type: application/json' \
  -d '{"subject":"user-77","role":"EDITOR"}'
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" 'http://localhost:8080/api/workspaces/1/members?page=0&size=20'
```

### Project API

Projects belong to exactly one workspace. Create a project under workspace ID `1`:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X POST http://localhost:8080/api/workspaces/1/projects \
  -H 'Content-Type: application/json' \
  -d '{"name":"Platform","slug":"platform","description":"Internal services"}'
```

List, retrieve, or update projects:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" 'http://localhost:8080/api/workspaces/1/projects?page=0&size=20'
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" http://localhost:8080/api/workspaces/1/projects/7
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X PATCH http://localhost:8080/api/workspaces/1/projects/7 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Platform Services","description":"Service roadmap"}'
```

Project names are required (maximum 120 characters), slugs follow the workspace slug format and are unique **within each workspace**, and descriptions are optional (maximum 2,000 characters). Slugs and workspace ownership are immutable. PATCH replaces the project's name and description; omit the description to clear it. A missing workspace or a project outside the requested workspace returns HTTP 404. Duplicate slugs within the same workspace return HTTP 409.

### Task API

Tasks belong to exactly one project in one workspace. Create a task:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X POST http://localhost:8080/api/workspaces/1/projects/7/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Fix login timeout","description":"Investigate session expiry","priority":"HIGH","dueAt":"2026-11-01T12:00:00Z"}'
```

List tasks (newest first), optionally filtered by status, retrieve one, or replace editable details:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" 'http://localhost:8080/api/workspaces/1/projects/7/tasks?status=TODO&page=0&size=20'
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" http://localhost:8080/api/workspaces/1/projects/7/tasks/9
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X PATCH http://localhost:8080/api/workspaces/1/projects/7/tasks/9 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Fix session timeout","priority":"URGENT","description":null,"dueAt":null}'
```

Change task status separately:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X PATCH http://localhost:8080/api/workspaces/1/projects/7/tasks/9/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
```

Task status values: `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`. Priorities: `LOW`, `NORMAL`, `HIGH`, `URGENT`. New tasks start in `TODO`. Titles are required (up to 160 characters); descriptions are optional (up to 4,000). `dueAt` accepts an ISO-8601 instant or `null`. PATCH details replaces title, description, priority, and due date together; omitting description/due date clears them. Status changes use a separate endpoint; repeating the current status does not write again. Responses include creation/update timestamps and a JPA version field for future concurrency controls. Task reads and updates require the matching workspace and project in the URL; a mismatched task returns 404. This is scoping, **not authentication**.

### Incident API

Create an incident:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X POST http://localhost:8080/api/incidents \
  -H 'Content-Type: application/json' \
  -d '{"title":"API outage","description":"Requests are timing out"}'
```

Change its status (replace `1` with the returned ID):

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" -X PATCH http://localhost:8080/api/incidents/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IDENTIFIED"}'
```

View the timeline:

```bash
curl -H "Authorization: Bearer $RELAY_ACCESS_TOKEN" http://localhost:8080/api/incidents/1/activity
```

Timeline entries include `type` (`CREATED` or `STATUS_CHANGED`), `previousStatus`, `currentStatus`, and `occurredAt`. Repeating an unchanged status does not create a duplicate entry. Existing incidents created before the timeline feature have no retroactive creation entry.

Run tests:

```bash
mvn verify
```

## Direction

Workspaces, projects, and tasks are the foundation for upcoming organization membership and access controls. Planned work includes role-based access control, a React/TypeScript client, real-time collaboration, comments, notifications, search, file storage, workflow automation, analytics, and production deployment/observability.

**Current limitations:** Workspace, project, and task endpoints now enforce membership-based authorization, but the legacy incident endpoints remain global to all authenticated users and are **not tenant-isolated**. Member removal, role changes, owner transfers, and user invitations are not yet implemented. The schema currently uses Hibernate `ddl-auto: update` rather than versioned migrations. Existing workspaces created before membership support need an explicit owner backfill before they can be accessed. Do not expose this API publicly or use it with sensitive data until the remaining tenancy boundaries and migrations are implemented.

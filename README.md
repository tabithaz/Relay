# Relay

Relay is an evolving collaborative workspace for engineering teams. The current backend provides workspace management and incident coordination. The long-term goal is a general-purpose workspace for projects, tasks, collaboration, notifications, and team workflows.

## Implemented

- Java 21 / Spring Boot REST API backed by PostgreSQL
- Create, list, retrieve, and rename workspaces with immutable, unique URL-friendly slugs
- Paginated workspace listing (default 20, maximum 100) with deterministic ordering
- Workspace name and slug validation; HTTP 404 for missing workspaces and 409 for duplicate slugs
- Create and list incidents
- Change incident status (investigating, identified, monitoring, resolved)
- Persist an activity entry when an incident is created or its status changes
- Retrieve incident activity in chronological order
- Validate incident requests, including required status and database-compatible text lengths
- Transactional database writes, service tests, HTTP controller tests, and GitHub Actions checks

## Local development

Requires Java 21, Maven, and Docker Compose.

```bash
docker compose up -d
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. The PostgreSQL configuration in `docker-compose.yml` is for local development only; use external secrets for non-local deployments.

### Workspace API

Create a workspace:

```bash
curl -X POST http://localhost:8080/api/workspaces \
  -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","slug":"engineering"}'
```

List workspaces (newest first):

```bash
curl 'http://localhost:8080/api/workspaces?page=0&size=20'
```

Retrieve or rename a workspace (replace `1` with its ID):

```bash
curl http://localhost:8080/api/workspaces/1
curl -X PATCH http://localhost:8080/api/workspaces/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Platform Engineering"}'
```

Workspace slugs must be 3–63 characters, lowercase letters and numbers separated by single hyphens. Slugs are unique and cannot be changed after creation. Names may be changed, but must not be blank or exceed 120 characters. List responses use Spring Data's paginated JSON representation with `content`, `totalElements`, `totalPages`, `number`, and `size`.

### Incident API

Create an incident:

```bash
curl -X POST http://localhost:8080/api/incidents \
  -H 'Content-Type: application/json' \
  -d '{"title":"API outage","description":"Requests are timing out"}'
```

Change its status (replace `1` with the returned ID):

```bash
curl -X PATCH http://localhost:8080/api/incidents/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IDENTIFIED"}'
```

View the timeline:

```bash
curl http://localhost:8080/api/incidents/1/activity
```

Timeline entries include `type` (`CREATED` or `STATUS_CHANGED`), `previousStatus`, `currentStatus`, and `occurredAt`. Repeating an unchanged status does not create a duplicate entry. Existing incidents created before the timeline feature have no retroactive creation entry.

Run tests:

```bash
mvn verify
```

## Direction

Workspaces are the foundation for upcoming organization membership and project/task management. Planned work includes role-based access control, a React/TypeScript client, real-time collaboration, comments, notifications, search, file storage, workflow automation, analytics, and production deployment/observability.

**Current limitations:** Workspace records are not yet associated with users or incidents, and the API has no authentication or tenant isolation. Workspace endpoints are therefore **not access-controlled**. The schema currently uses Hibernate `ddl-auto: update` rather than versioned migrations. Do not expose this API publicly or use it with sensitive data until authorization, tenancy boundaries, and migrations are implemented.

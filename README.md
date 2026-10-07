# Relay

Relay is an evolving collaborative workspace for engineering teams. The first working module focuses on incident coordination: creating incidents, tracking their status, and preserving an ordered activity history. The long-term goal is a general-purpose workspace for projects, tasks, collaboration, notifications, and team workflows.

## Implemented

- Java 21 / Spring Boot REST API backed by PostgreSQL
- Create and list incidents
- Change incident status (investigating, identified, monitoring, resolved)
- Persist an activity entry when an incident is created or its status changes
- Retrieve incident activity in chronological order
- Validate requests, including required status and database-compatible text lengths
- Transactional incident changes and activity writes
- Unit and HTTP controller tests; GitHub Actions backend checks

## Local development

Requires Java 21, Maven, and Docker Compose.

```bash
docker compose up -d
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. The PostgreSQL configuration in `docker-compose.yml` is for local development only; use external secrets for non-local deployments.

### API examples

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

The incident module is the starting point, not the entire product. Planned work includes organizations and membership, project/task management, role-based access control, a React/TypeScript client, real-time collaboration, comments, notifications, search, file storage, workflow automation, analytics, and production deployment/observability.

Current limitations: the API does not yet have authentication, tenancy boundaries, or a production migration strategy. Do not expose it publicly until those controls are implemented.

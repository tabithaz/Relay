# Relay

Relay is a full-stack incident management platform for engineering teams. It is being built to make incident response easier to coordinate, from the first report through resolution and follow-up.

## Current backend

The first slice provides a Java 21 / Spring Boot API backed by PostgreSQL.

- Create incidents
- List incidents
- Move incidents through investigating, identified, monitoring, and resolved states
- Validate incoming incident data
- Run PostgreSQL locally with Docker Compose

## Stack

Java, Spring Boot, PostgreSQL, React, TypeScript, Redis, Docker, GitHub Actions

## Run locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the API:

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api/incidents`.

## Roadmap

Next iterations add incident timelines, services and responders, authentication and roles, WebSocket updates, Redis-backed event delivery, a React dashboard, tests, CI, and observability.

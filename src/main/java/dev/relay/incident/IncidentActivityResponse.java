package dev.relay.incident;

import java.time.Instant;

public record IncidentActivityResponse(
        Long id,
        IncidentActivityType type,
        IncidentStatus previousStatus,
        IncidentStatus currentStatus,
        Instant occurredAt
) {
    public static IncidentActivityResponse from(IncidentActivity activity) {
        return new IncidentActivityResponse(activity.getId(), activity.getType(),
                activity.getPreviousStatus(), activity.getCurrentStatus(), activity.getOccurredAt());
    }
}

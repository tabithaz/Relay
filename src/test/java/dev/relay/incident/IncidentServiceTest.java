package dev.relay.incident;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock IncidentRepository incidents;
    @Mock IncidentActivityRepository activities;
    @InjectMocks IncidentService service;

    @Test
    void creatingIncidentRecordsInitialActivity() {
        when(incidents.save(any(Incident.class))).thenAnswer(call -> call.getArgument(0));

        Incident created = service.create("  Database outage  ", "  Queries are failing  ");

        assertEquals("Database outage", created.getTitle());
        assertEquals("Queries are failing", created.getDescription());
        assertEquals(IncidentStatus.INVESTIGATING, created.getStatus());

        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(activities).save(captor.capture());
        assertEquals(IncidentActivityType.CREATED, captor.getValue().getType());
        assertNull(captor.getValue().getPreviousStatus());
        assertEquals(IncidentStatus.INVESTIGATING, captor.getValue().getCurrentStatus());
        assertNotNull(captor.getValue().getOccurredAt());
    }

    @Test
    void statusChangeRecordsPreviousAndNewStates() {
        Incident incident = new Incident("Outage", "Details");
        when(incidents.findById(7L)).thenReturn(Optional.of(incident));
        when(incidents.save(incident)).thenReturn(incident);

        Incident updated = service.updateStatus(7L, IncidentStatus.IDENTIFIED);

        assertEquals(IncidentStatus.IDENTIFIED, updated.getStatus());
        ArgumentCaptor<IncidentActivity> captor = ArgumentCaptor.forClass(IncidentActivity.class);
        verify(activities).save(captor.capture());
        assertEquals(IncidentActivityType.STATUS_CHANGED, captor.getValue().getType());
        assertEquals(IncidentStatus.INVESTIGATING, captor.getValue().getPreviousStatus());
        assertEquals(IncidentStatus.IDENTIFIED, captor.getValue().getCurrentStatus());
    }

    @Test
    void repeatedStatusUpdateDoesNotCreateDuplicateActivity() {
        Incident incident = new Incident("Outage", "Details");
        when(incidents.findById(7L)).thenReturn(Optional.of(incident));

        service.updateStatus(7L, IncidentStatus.INVESTIGATING);

        verify(incidents, never()).save(any());
        verifyNoInteractions(activities);
    }

    @Test
    void missingIncidentCannotBeUpdated() {
        when(incidents.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IncidentNotFoundException.class,
                () -> service.updateStatus(99L, IncidentStatus.RESOLVED));
        verifyNoInteractions(activities);
    }

    @Test
    void activityEndpointMapsHistoryWithoutExposingEntityRelationships() {
        Incident incident = new Incident("Outage", "Details");
        when(incidents.existsById(7L)).thenReturn(true);
        when(activities.findByIncidentIdOrderByOccurredAtAscIdAsc(7L))
                .thenReturn(List.of(IncidentActivity.created(incident)));

        List<IncidentActivityResponse> timeline = service.activityFor(7L);

        assertEquals(1, timeline.size());
        assertEquals(IncidentActivityType.CREATED, timeline.get(0).type());
        assertEquals(IncidentStatus.INVESTIGATING, timeline.get(0).currentStatus());
    }

    @Test
    void activityForUnknownIncidentReturnsNotFound() {
        when(incidents.existsById(99L)).thenReturn(false);

        assertThrows(IncidentNotFoundException.class, () -> service.activityFor(99L));
        verifyNoInteractions(activities);
    }
}

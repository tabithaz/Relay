package dev.relay.incident;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentActivityRepository extends JpaRepository<IncidentActivity, Long> {
    List<IncidentActivity> findByIncidentIdOrderByOccurredAtAscIdAsc(Long incidentId);
}

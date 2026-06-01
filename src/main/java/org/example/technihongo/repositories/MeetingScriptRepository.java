package org.example.technihongo.repositories;

import org.example.technihongo.entities.MeetingScript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingScriptRepository extends JpaRepository<MeetingScript, Integer> {
    List<MeetingScript> findByMeetingMeetingIdOrderByScriptOrder(Integer meetingId);
    Integer countByMeetingMeetingId(Integer meetingId);
    Page<MeetingScript> findByMeetingMeetingId(Pageable pageable, Integer meetingId);
}

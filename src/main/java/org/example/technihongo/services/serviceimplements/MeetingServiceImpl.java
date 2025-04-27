package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.MeetingDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.entities.Meeting;
import org.example.technihongo.entities.User;
import org.example.technihongo.repositories.MeetingRepository;
import org.example.technihongo.repositories.MeetingScriptRepository;
import org.example.technihongo.repositories.UserRepository;
import org.example.technihongo.services.interfaces.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Component
public class MeetingServiceImpl implements MeetingService {
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public PageResponseDTO<Meeting> getAllMeetings(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Meeting> meetingPage = meetingRepository.findAll(pageable);
        return getPageResponseDTO(meetingPage);
    }

    @Override
    public PageResponseDTO<Meeting> getAllActiveMeetings(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Meeting> meetingPage = meetingRepository.findActiveMeetings(pageable);
        return getPageResponseDTO(meetingPage);
    }

    @Override
    public Meeting createMeeting(Integer creatorId, MeetingDTO dto) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        Meeting meeting = new Meeting();
        meeting.setTitle(dto.getTitle());
        meeting.setDescription(dto.getDescription());
        meeting.setScriptsCount(0);
        meeting.setCreatorId(creator);
        meeting.setIsActive(false);
        meeting = meetingRepository.save(meeting);
        return meeting;
    }

    @Override
    public void updateMeeting(Integer meetingId, MeetingDTO dto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp"));

        meeting.setTitle(dto.getTitle());
        meeting.setDescription(dto.getDescription());
        meeting.setIsActive(dto.getIsActive());
        meeting.setVoiceName(dto.getVoiceName());
        meetingRepository.save(meeting);
    }

    @Override
    public Meeting getMeetingById(Integer meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp"));
    }

    @Override
    public Meeting getActiveMeetingById(Integer meetingId) {
        return meetingRepository.findActiveMeetingById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp đang hoạt động"));
    }

    private <T> PageResponseDTO<T> getPageResponseDTO(Page<T> page) {
        return PageResponseDTO.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

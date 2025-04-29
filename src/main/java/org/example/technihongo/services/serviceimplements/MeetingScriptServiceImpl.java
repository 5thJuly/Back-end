package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.MeetingScriptDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.ScriptOrderDTO;
import org.example.technihongo.dto.UpdateQuizQuestionOrderDTO;
import org.example.technihongo.entities.Meeting;
import org.example.technihongo.entities.MeetingScript;
import org.example.technihongo.entities.QuizQuestion;
import org.example.technihongo.repositories.MeetingRepository;
import org.example.technihongo.repositories.MeetingScriptRepository;
import org.example.technihongo.services.interfaces.MeetingScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Component
public class MeetingScriptServiceImpl implements MeetingScriptService {
    @Autowired
    private MeetingScriptRepository meetingScriptRepository;
    @Autowired
    private MeetingRepository meetingRepository;

    @Override
    public PageResponseDTO<MeetingScript> getListScriptsByMeetingId(Integer meetingId, int pageNo, int pageSize, String sortBy, String sortDir) {
        meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp"));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<MeetingScript> scriptPage = meetingScriptRepository.findByMeetingMeetingId(pageable, meetingId);
        return getPageResponseDTO(scriptPage);
    }

    @Override
    public MeetingScript getScriptById(Integer scriptId) {
        return meetingScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kịch bản"));
    }

    @Override
    @Transactional
    public MeetingScript createScript(MeetingScriptDTO dto) {
        Meeting meeting = meetingRepository.findById(dto.getMeetingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp"));

        Integer maxOrder = meetingScriptRepository.countByMeetingMeetingId(dto.getMeetingId());
        MeetingScript script = new MeetingScript();
        script.setMeeting(meeting);
        script.setQuestion(dto.getQuestion());
        script.setAnswer(dto.getAnswer());
        script.setExplanation(dto.getExplanation());
        script.setScriptOrder(maxOrder + 1);
        script = meetingScriptRepository.save(script);
        meeting.setScriptsCount(meeting.getScriptsCount() + 1);
        meetingRepository.save(meeting);
        return script;
    }

    @Override
    @Transactional
    public void updateScript(Integer scriptId, MeetingScriptDTO dto) {
        MeetingScript script = meetingScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kịch bản"));

//        Meeting meeting = script.getMeeting();
//        Integer oldOrder = script.getScriptOrder();
//        Integer newOrder = dto.getScriptOrder();
//        if (!oldOrder.equals(newOrder)) {
//            adjustScriptOrders(meeting.getMeetingId(), oldOrder, newOrder);
//        }
        script.setQuestion(dto.getQuestion());
        script.setAnswer(dto.getAnswer());
        script.setExplanation(dto.getExplanation());
//        script.setScriptOrder(newOrder);
        meetingScriptRepository.save(script);
    }

    @Override
    @Transactional
    public void deleteScript(Integer scriptId) {
        MeetingScript script = meetingScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kịch bản"));

        Meeting meeting = script.getMeeting();
        Integer deletedOrder = script.getScriptOrder();
        meetingScriptRepository.delete(script);
        meeting.setScriptsCount(meeting.getScriptsCount() - 1);
        meetingRepository.save(meeting);
        List<MeetingScript> scripts = meetingScriptRepository.findByMeetingMeetingIdOrderByScriptOrder(meeting.getMeetingId());
        for (int i = 0; i < scripts.size(); i++) {
            MeetingScript s = scripts.get(i);
            if (s.getScriptOrder() > deletedOrder) {
                s.setScriptOrder(i + 1);
                meetingScriptRepository.save(s);
            }
        }
    }

    @Override
    public void updateScriptOrder(Integer meetingId, ScriptOrderDTO scriptOrderDTO) {
        meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi họp"));

        List<MeetingScript> meetingScripts = meetingScriptRepository.findByMeetingMeetingIdOrderByScriptOrder(meetingId);
        List<Integer> newOrder = scriptOrderDTO.getNewScriptOrder();

        if (meetingScripts.size() != newOrder.size()) {
            throw new RuntimeException("Số lượng Script so với thứ tự không hợp lệ!");
        }

        for (int i = 0; i < meetingScripts.size(); i++) {
            meetingScripts.get(i).setScriptOrder(newOrder.get(i));
        }

        meetingScriptRepository.saveAll(meetingScripts);
    }

//    private void adjustScriptOrders(Integer meetingId, Integer oldOrder, Integer newOrder) {
//        List<MeetingScript> scripts = meetingScriptRepository.findByMeetingMeetingIdOrderByScriptOrder(meetingId);
//        if (newOrder > oldOrder) {
//            for (MeetingScript s : scripts) {
//                if (s.getScriptOrder() > oldOrder && s.getScriptOrder() <= newOrder) {
//                    s.setScriptOrder(s.getScriptOrder() - 1);
//                    meetingScriptRepository.save(s);
//                }
//            }
//        } else {
//            for (MeetingScript s : scripts) {
//                if (s.getScriptOrder() >= newOrder && s.getScriptOrder() < oldOrder) {
//                    s.setScriptOrder(s.getScriptOrder() + 1);
//                    meetingScriptRepository.save(s);
//                }
//            }
//        }
//    }

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

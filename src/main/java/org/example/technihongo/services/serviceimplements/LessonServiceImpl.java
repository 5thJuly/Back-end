package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.digester.Rule;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Component
public class LessonServiceImpl implements LessonService {
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    @Autowired
    private StudentLessonProgressRepository studentLessonProgressRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Override
    public Optional<Lesson> getLessonById(Integer lessonId) {
        return Optional.ofNullable(lessonRepository.findByLessonId(lessonId));
    }

    @Override
    public List<Lesson> getLessonListByStudyPlanId(Integer studyPlanId) {
        studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found!"));
        return lessonRepository.findByStudyPlan_StudyPlanIdOrderByLessonOrderAsc(studyPlanId);
    }

    @Override
    public Lesson createLesson(CreateLessonDTO createLessonDTO) {
        StudyPlan csp = studyPlanRepository.findByStudyPlanId(createLessonDTO.getStudyPlanId());
        if(csp == null){
            throw new RuntimeException("StudyPlan ID not found!");
        }

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .studyPlan(csp)
                .title(createLessonDTO.getTitle())
                .lessonOrder(lessonRepository.countLessonByStudyPlan(csp) + 1)
                .build());

        return lesson;
    }

    @Override
    public void updateLesson(Integer lessonId, UpdateLessonDTO updateLessonDTO) {
        if(lessonRepository.findByLessonId(lessonId) == null){
            throw new RuntimeException("Lesson ID not found!");
        }

        Lesson lesson = lessonRepository.findByLessonId(lessonId);
        lesson.setTitle(updateLessonDTO.getTitle());
        lesson.setUpdatedAt(LocalDateTime.now());

        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void updateLessonOrder(Integer studyPlanId, UpdateLessonOrderDTO updateLessonOrderDTO) {
        if(studyPlanRepository.findByStudyPlanId(studyPlanId) == null){
            throw new RuntimeException("StudyPlan ID not found!");
        }

        List<Lesson> lessons = lessonRepository.findByStudyPlan_StudyPlanIdOrderByLessonOrderAsc(studyPlanId);
        List<Integer> newOrder = updateLessonOrderDTO.getNewLessonOrder();

        if (lessons.size() != newOrder.size()) {
            throw new RuntimeException("Lesson count does not match newOrder!");
        }

        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).setLessonOrder(newOrder.get(i));
        }

        lessonRepository.saveAll(lessons);
    }

    @Override
    @Transactional
    public void deleteLesson(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson ID not found!"));

        StudyPlan studyPlan = studyPlanRepository.findById(lesson.getStudyPlan().getStudyPlanId())
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found!"));

        if(studyPlan.isActive()) {
            throw new RuntimeException("Cannot delete Lesson of an active StudyPlan.");
        }

        boolean existsInStudentLessonProgress = studentLessonProgressRepository.existsByLesson_LessonId(lessonId);
        if (existsInStudentLessonProgress) {
            throw new RuntimeException("Cannot delete Lesson because it is referenced in StudentLessonProgress.");
        }

        lessonResourceRepository.deleteByLesson_LessonId(lessonId);

        lessonRepository.deleteById(lessonId);
    }

    @Override
    public PageResponseDTO<Lesson> getLessonListByStudyPlanIdPaginated(Integer studyPlanId, int pageNo, int pageSize, String sortBy, String sortDir, String keyword) {
        if(studyPlanId == null){
            throw new RuntimeException("StudyPlan ID không thể null");
        }

        studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID StudyPlan!"));

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Lesson> lessons;
        if(keyword != null) {
            lessons = lessonRepository.findByStudyPlan_StudyPlanIdAndTitleContainingIgnoreCase(studyPlanId, keyword, pageable);
        }
        else {
            lessons = lessonRepository.findByStudyPlan_StudyPlanId(studyPlanId, pageable);
        }

        return getPageResponseDTO(lessons);
    }

    @Override
    public void checkLessonProgressPrerequisite(Integer studentId, Integer lessonId) {
        StudentLessonProgress progress = studentLessonProgressRepository
                .findByStudentStudentIdAndLessonLessonId(studentId, lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson progress not found for student ID: " + studentId));

        if (progress.getCompletionStatus().equals(CompletionStatus.NOT_STARTED)) {
            throw new RuntimeException("This lesson is not yet available to view!");
        }

        if (progress.getCompletionStatus().equals(CompletionStatus.PAUSED)) {
            throw new RuntimeException("This lesson is not available as it belongs to other StudyPlan!");
        }
    }

    @Override
    public Integer getCourseIdByLessonId(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found!"));

        StudyPlan studyPlan = studyPlanRepository.findById(lesson.getStudyPlan().getStudyPlanId())
                .orElseThrow(() -> new RuntimeException("StudyPlan not found!"));

        Course course = courseRepository.findById(studyPlan.getCourse().getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found!"));

        return course.getCourseId();
    }

    @Override
    public void setLessonOrder(Integer studyPlanId, Integer lessonId, Integer newOrder) {
        StudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found"));

        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson ID not found"));

        if (studyPlan.isActive()) {
            throw new RuntimeException("Cannot update lesson order because the StudyPlan is active.");
        }

        if (studentLessonProgressRepository.existsByLesson_LessonId(lessonId)) {
            throw new RuntimeException("Cannot update lesson order because it has associated StudentLessonProgress.");
        }

        List<Lesson> lessons = lessonRepository.findByStudyPlan_StudyPlanIdOrderByLessonOrderAsc(studyPlanId);
        Lesson target = lessons.stream()
                .filter(l -> l.getLessonId().equals(lessonId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Lesson not found in this StudyPlan!"));

        int currentOrder = target.getLessonOrder();
        if (newOrder < 1 || newOrder > lessons.size()) {
            throw new RuntimeException("Invalid order!");
        }

        if (newOrder < currentOrder) {
            lessons.stream()
                    .filter(l -> l.getLessonOrder() >= newOrder && l.getLessonOrder() < currentOrder)
                    .forEach(l -> l.setLessonOrder(l.getLessonOrder() + 1));
        } else if (newOrder > currentOrder) {
            lessons.stream()
                    .filter(l -> l.getLessonOrder() <= newOrder && l.getLessonOrder() > currentOrder)
                    .forEach(l -> l.setLessonOrder(l.getLessonOrder() - 1));
        }

        target.setLessonOrder(newOrder);

        lessonRepository.saveAll(lessons);
    }

    @Override
    public PageResponseDTO<LessonDTO> getLessonListByStudyPlanIdWithProgress(Integer studyPlanId, Integer studentId, int pageNo, int pageSize, String sortBy, String sortDir, String keyword) {
        if(studyPlanId == null){
            throw new RuntimeException("StudyPlan ID không thể null");
        }

        studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID StudyPlan!"));
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID Student!"));

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Lesson> lessonPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            lessonPage = lessonRepository.findByStudyPlan_StudyPlanIdAndTitleContainingIgnoreCase(studyPlanId, keyword, pageable);
        } else {
            lessonPage = lessonRepository.findByStudyPlan_StudyPlanId(studyPlanId, pageable);
        }

        List<StudentLessonProgress> progressList = studentLessonProgressRepository
                .findByStudentStudentIdAndLesson_StudyPlanStudyPlanId(studentId, studyPlanId);

        Map<Integer, BigDecimal> progressMap = new HashMap<>();
        for (StudentLessonProgress progress : progressList) {
            Integer lessonId = progress.getLesson().getLessonId();
            BigDecimal completionPercentage = progress.getCompletionPercentage();
            progressMap.put(lessonId, completionPercentage);
        }

        Page<LessonDTO> lessonDTOPage = lessonPage.map(lesson -> {
            BigDecimal studentProgress = progressMap.getOrDefault(lesson.getLessonId(), BigDecimal.ZERO);
            return new LessonDTO(
                    lesson.getLessonId(),
                    lesson.getStudyPlan(),
                    lesson.getTitle(),
                    lesson.getLessonOrder(),
                    studentProgress,
                    lesson.getCreatedAt(),
                    lesson.getUpdatedAt()
            );
        });

        return getPageResponseDTO2(lessonDTOPage);
    }

    private PageResponseDTO<Lesson> getPageResponseDTO(Page<Lesson> page) {
        return PageResponseDTO.<Lesson>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private PageResponseDTO<LessonDTO> getPageResponseDTO2(Page<LessonDTO> page) {
        return PageResponseDTO.<LessonDTO>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

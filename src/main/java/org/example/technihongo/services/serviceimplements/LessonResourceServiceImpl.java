package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.LessonResourceService;
import org.example.technihongo.services.interfaces.StudentFlashcardSetProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Component
public class LessonResourceServiceImpl implements LessonResourceService {
    @Autowired
    private LessonResourceRepository lessonResourceRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private LearningResourceRepository learningResourceRepository;
    @Autowired
    private SystemFlashcardSetRepository systemFlashcardSetRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentQuizAttemptRepository studentQuizAttemptRepository;
    @Autowired
    private StudentResourceProgressRepository studentResourceProgressRepository;
    @Autowired
    private StudentFlashcardSetProgressRepository studentFlashcardSetProgressRepository;

    @Override
    public List<LessonResource> getLessonResourceListByLessonId(Integer lessonId) {
        if(lessonId == null){
            throw new RuntimeException("Lesson ID không thể null");
        }

        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID Lesson"));

        return lessonResourceRepository.findByLesson_LessonIdOrderByTypeOrderAsc(lessonId);
    }

    @Override
    public List<LessonResourceDTO> getActiveLessonResourceListByLessonId(Integer studentId, Integer lessonId) {
        if(lessonId == null){
            throw new RuntimeException("Lesson ID không thể null");
        }

        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID Lesson"));

        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID Student"));

        List<LessonResource> resources = lessonResourceRepository
                .findByLesson_LessonIdOrderByTypeOrderAsc(lessonId)
                .stream()
                .filter(LessonResource::isActive)
                .toList();

        return resources.stream()
                .map(resource -> {
                    boolean isCompleted = false;

                    if (resource.getQuiz() != null) {
                        Quiz quiz = quizRepository.findById(resource.getQuiz().getQuizId())
                                .orElse(null);
                        if (quiz != null && quiz.isPublic()) {
                            isCompleted = studentQuizAttemptRepository
                                    .existsByStudentStudentIdAndQuizQuizIdAndIsPassedAndIsCompleted(
                                            studentId, quiz.getQuizId(), true, true);
                        }
                    } else if (resource.getSystemFlashCardSet() != null) {
                        SystemFlashcardSet set = systemFlashcardSetRepository
                                .findById(resource.getSystemFlashCardSet().getSystemSetId())
                                .orElse(null);
                        if (set != null && set.isPublic()) {
                            isCompleted = studentFlashcardSetProgressRepository
                                    .existsByStudentStudentIdAndSystemFlashcardSetSystemSetIdAndCompletionStatus(
                                            studentId, set.getSystemSetId(), CompletionStatus.COMPLETED);
                        }
                    } else if (resource.getLearningResource() != null) {
                        LearningResource lr = learningResourceRepository
                                .findById(resource.getLearningResource().getResourceId())
                                .orElse(null);
                        if (lr != null && lr.isPublic()) {
                            isCompleted = studentResourceProgressRepository
                                    .existsByStudentStudentIdAndLearningResourceResourceIdAndCompletionStatus(
                                            studentId, lr.getResourceId(), CompletionStatus.COMPLETED);
                        }
                    }

                    return LessonResourceDTO.builder()
                            .lessonResourceId(resource.getLessonResourceId())
                            .lesson(resource.getLesson())
                            .type(resource.getType())
                            .typeOrder(resource.getTypeOrder())
                            .quiz(resource.getQuiz() != null ? resource.getQuiz() : null)
                            .learningResource(resource.getLearningResource() != null ? resource.getLearningResource() : null)
                            .systemFlashCardSet(resource.getSystemFlashCardSet() != null ? resource.getSystemFlashCardSet() : null)
                            .isActive(resource.isActive())
                            .createdAt(resource.getCreatedAt())
                            .updatedAt(resource.getUpdatedAt())
                            .isProgressCompleted(isCompleted)
                            .build();
                })
                .toList();
    }

    @Override
    public LessonResource getLessonResourceById(Integer lessonResourceId) {
        return lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID LessonResource"));
    }

    @Override
    public LessonResource getActiveLessonResourceById(Integer lessonResourceId) {
        return lessonResourceRepository.findById(lessonResourceId)
                .filter(LessonResource::isActive)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID LessonResource"));
    }

    @Override
    public LessonResource createLessonResource(CreateLessonResourceDTO createLessonResourceDTO) {
        if(createLessonResourceDTO.getLessonId() == null){
            throw new RuntimeException("Lesson ID không thể null");
        }

        lessonRepository.findById(createLessonResourceDTO.getLessonId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID Lesson"));

        Integer l = createLessonResourceDTO.getResourceId();
        Integer s = createLessonResourceDTO.getSystemSetId();
        Integer q = createLessonResourceDTO.getQuizId();

        if(l == null && s == null && q == null){
            throw new RuntimeException("Không tìm thấy ID của loại Content (Quiz, LearningResource, SystemFlashcardSet)!");
        }

        if((l != null && s != null) || (l != null && q != null) || (q != null && s != null)){
            throw new RuntimeException("Chỉ có thể tạo 1 loại Content (Quiz, LearningResource, SystemFlashcardSet)!");
        }

        LearningResource learningResource = learningResourceRepository.findByResourceId(l);
        if(l != null && learningResource == null){
            throw new RuntimeException("Không tìm thấy ID LearningResource!");
        }

        SystemFlashcardSet systemFlashcardSet = systemFlashcardSetRepository.findBySystemSetId(s);
        if(s != null && systemFlashcardSet == null){
            throw new RuntimeException("Không tìm thấy ID SystemFlashcardSet!");
        }

        Quiz quiz = quizRepository.findByQuizId(q);
        if(q != null && quiz == null){
            throw new RuntimeException("Không tìm thấy ID Quiz!");
        }

        Lesson lesson = lessonRepository.findByLessonId(createLessonResourceDTO.getLessonId());
        if(l != null && lessonResourceRepository.existsByLesson_LessonIdAndLearningResource_ResourceId(
                lesson.getLessonId(), l)){
            throw new RuntimeException("LearningResource này đã tồn tại trong lesson!");
        }

        if(s != null && lessonResourceRepository.existsByLesson_LessonIdAndSystemFlashCardSet_SystemSetId(
                lesson.getLessonId(), s)){
            throw new RuntimeException("SystemFlashcardSet này đã tồn tại trong lesson!");
        }

        if(q != null && lessonResourceRepository.existsByLesson_LessonIdAndQuiz_QuizId(
                lesson.getLessonId(), q)){
            throw new RuntimeException("Quiz này đã tồn tại trong lesson!");
        }

        LessonResource lessonResource;
        if(learningResource != null){
            lessonResource = lessonResourceRepository.save(LessonResource.builder()
                    .lesson(lesson)
                    .type("LearningResource")
                    .typeOrder(lessonResourceRepository.countByLesson_LessonId(lesson.getLessonId()) + 1)
                    .learningResource(learningResource)
                    .isActive(createLessonResourceDTO.getIsActive())
                    .build());
        }
        else if(systemFlashcardSet != null){
            lessonResource = lessonResourceRepository.save(LessonResource.builder()
                    .lesson(lesson)
                    .type("FlashcardSet")
                    .typeOrder(lessonResourceRepository.countByLesson_LessonId(lesson.getLessonId()) + 1)
                    .systemFlashCardSet(systemFlashcardSet)
                    .isActive(createLessonResourceDTO.getIsActive())
                    .build());
        }
        else {
            lessonResource = lessonResourceRepository.save(LessonResource.builder()
                    .lesson(lesson)
                    .type("Quiz")
                    .typeOrder(lessonResourceRepository.countByLesson_LessonId(lesson.getLessonId()) + 1)
                    .quiz(quiz)
                    .isActive(createLessonResourceDTO.getIsActive())
                    .build());
        }

        return lessonResource;
    }

    @Override
    public void updateLessonResource(Integer lessonResourceId, UpdateLessonResourceDTO updateLessonResourceDTO) {
        lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new RuntimeException("LessonResource ID not found"));

        LessonResource lessonResource = lessonResourceRepository.findByLessonResourceId(lessonResourceId);
        lessonResource.setActive(updateLessonResourceDTO.getIsActive());
        lessonResourceRepository.save(lessonResource);
    }

    @Override
    public void updateLessonResourceOrder(Integer lessonId, UpdateLessonResourceOrderDTO updateLessonResourceOrderDTO) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson ID not found"));

        List<LessonResource> lessonResourceList = lessonResourceRepository.findByLesson_LessonIdOrderByTypeOrderAsc(lessonId);
        List<Integer> newOrder = updateLessonResourceOrderDTO.getNewLessonResourceOrder();

        if (lessonResourceList.size() != newOrder.size()) {
            throw new IllegalArgumentException("LessonResource count does not match newOrder!");
        }

        for (int i = 0; i < lessonResourceList.size(); i++) {
            lessonResourceList.get(i).setTypeOrder(newOrder.get(i));
        }

        lessonResourceRepository.saveAll(lessonResourceList);

    }

    @Override
    public void deleteLessonResource(Integer lessonResourceId) {
        LessonResource deletedLessonResource = lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new RuntimeException("LessonResource ID not found"));

        if(deletedLessonResource.isActive()){
            throw new RuntimeException("Cannot delete an active LessonResource!");
        }

        Integer lessonId = deletedLessonResource.getLesson().getLessonId();
        Integer deletedOrder = deletedLessonResource.getTypeOrder();

        lessonResourceRepository.delete(deletedLessonResource);

        List<LessonResource> lessonResources = lessonResourceRepository.findByLesson_LessonIdOrderByTypeOrderAsc(lessonId);
        for (LessonResource lessonResource : lessonResources) {
            if (lessonResource.getTypeOrder() > deletedOrder) {
                lessonResource.setTypeOrder(lessonResource.getTypeOrder() - 1);
            }
        }

        lessonResourceRepository.saveAll(lessonResources);
    }

    @Override
    public PageResponseDTO<LessonResource> getLessonResourcesByDefaultStudyPlanPaginated(Integer studyPlanId, String keyword, String type, int pageNo, int pageSize, String sortBy, String sortDir) {
        studyPlanRepository.findById(studyPlanId).filter(StudyPlan::isDefault)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found or is not default"));

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<LessonResource> lessonResources;

        if(keyword == null && type != null) {
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanIdAndType(studyPlanId, type, pageable);
        }
        else if(keyword != null && !keyword.isEmpty() && type.isEmpty()) {
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanIdAndLearningResource_TitleContainsIgnoreCaseOrSystemFlashCardSet_TitleContainsIgnoreCaseOrQuiz_TitleContainsIgnoreCase(studyPlanId, keyword, keyword, keyword, pageable);
        }
        else if(keyword != null && type.equalsIgnoreCase("LearningResource")){
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanIdAndLearningResource_TitleContainsIgnoreCase(studyPlanId, keyword, pageable);
        }
        else if(keyword != null && type.equalsIgnoreCase("FlashcardSet")){
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanIdAndSystemFlashCardSet_TitleContainsIgnoreCase(studyPlanId, keyword, pageable);
        }
        else if(keyword != null && type.equalsIgnoreCase("Quiz")){
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanIdAndQuiz_TitleContainsIgnoreCase(studyPlanId, keyword, pageable);
        }
        else{
            lessonResources = lessonResourceRepository.findByLesson_StudyPlan_StudyPlanId(studyPlanId, pageable);
        }

        return getPageResponseDTO(lessonResources);
    }

    @Override
    public void setLessonResourceOrder(Integer lessonId, Integer lessonResourceId, Integer newOrder) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson ID not found"));

        lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new RuntimeException("LessonResource ID not found"));

        List<LessonResource> lessonResources = lessonResourceRepository.findByLesson_LessonIdOrderByTypeOrderAsc(lessonId);
        LessonResource target = lessonResources.stream()
                .filter(lr -> lr.getLessonResourceId().equals(lessonResourceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("LessonResource not found!"));

        int currentOrder = target.getTypeOrder();
        if (newOrder < 1 || newOrder > lessonResources.size()) {
            throw new RuntimeException("Invalid order!");
        }

        if (newOrder < currentOrder) {
            lessonResources.stream()
                    .filter(lr -> lr.getTypeOrder() >= newOrder && lr.getTypeOrder() < currentOrder)
                    .forEach(lr -> lr.setTypeOrder(lr.getTypeOrder() + 1));
        } else if (newOrder > currentOrder) {
            lessonResources.stream()
                    .filter(lr -> lr.getTypeOrder() <= newOrder && lr.getTypeOrder() > currentOrder)
                    .forEach(lr -> lr.setTypeOrder(lr.getTypeOrder() - 1));
        }
        target.setTypeOrder(newOrder);

        lessonResourceRepository.saveAll(lessonResources);
    }

    private PageResponseDTO<LessonResource> getPageResponseDTO(Page<LessonResource> page) {
        return PageResponseDTO.<LessonResource>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

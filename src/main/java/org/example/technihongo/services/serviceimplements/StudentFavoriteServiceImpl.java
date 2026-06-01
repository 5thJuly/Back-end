package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.PageResponseDTO;

import org.example.technihongo.entities.LessonResource;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.StudentFavorite;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudentFavoriteService;
import org.example.technihongo.services.interfaces.AchievementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Component
public class StudentFavoriteServiceImpl implements StudentFavoriteService {
    private final StudentFavoriteRepository studentFavoriteRepository;
    private final StudentRepository studentRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final StudentSubscriptionRepository studentSubscriptionRepository;
    private final AchievementService achievementService;
    private final LessonResourceRepository lessonResourceRepository;


    @Override
    @Transactional
    public StudentFavorite saveLearningResource(Integer studentId, Integer lessonResourceId) {
        if(studentId == null){
            throw new RuntimeException("Student ID không thể null");
        }
        if(lessonResourceId == null){
            throw new RuntimeException("LessonResource ID không thể null");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Student với ID: " + studentId));

        LessonResource lessonResource = lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy LessonResource với ID: " + lessonResourceId));

        if(lessonResource.getLearningResource() == null){
            throw new RuntimeException("LessonResource không liên kết với LearningResource");
        }
        learningResourceRepository.findById(lessonResource.getLearningResource().getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Learning resource với ID: " + lessonResource.getLearningResource().getResourceId()));

        if (!lessonResource.getLearningResource().isPublic()) {
            throw new RuntimeException("Không thể yêu thích LearningResource chưa công khai!");
        }

        if (lessonResource.getLearningResource().isPremium()) {
            boolean hasActiveSubscription = studentSubscriptionRepository
                    .existsByStudent_StudentIdAndIsActive(studentId, true);
            if (!hasActiveSubscription) {
                throw new RuntimeException("Bạn phải mua gói để có thể yêu thích LearningResource cao cấp!");
            }
        }

        if (studentFavoriteRepository.existsByStudent_StudentIdAndLessonResource_LessonResourceId(studentId, lessonResourceId)) {
            throw new RuntimeException("LearningResource này đã có sẵn trong danh sách yêu thích của bạn!");
        }

        StudentFavorite favorite = StudentFavorite.builder()
                .student(student)
                .lessonResource(lessonResource)
                .build();

        StudentFavorite savedFavorite = studentFavoriteRepository.save(favorite);

        achievementService.checkAndAssignFirstFavoriteAchievement(studentId);

        return savedFavorite;
    }

    @Override
    public PageResponseDTO<LessonResource> getListFavoriteLearningResourcesByStudentId(Integer studentId, int pageNo, int pageSize, String sortBy, String sortDir) {
        try {
            if(studentId == null){
                throw new RuntimeException("Student ID không thể null");
            }

            studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Student với ID: " + studentId));

            boolean hasActiveSubscription = studentSubscriptionRepository
                    .existsByStudent_StudentIdAndIsActive(studentId, true);

            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

            Page<StudentFavorite> favorites;
            if (hasActiveSubscription) {
                favorites = studentFavoriteRepository.findByStudent_StudentIdAndLearningResource_IsPublic(
                        studentId, true, pageable);
            } else {
                favorites = studentFavoriteRepository.findByStudent_StudentIdAndLearningResource_IsPublicAndLearningResource_IsPremium(
                        studentId, true, false, pageable);
            }

            Page<LessonResource> lessonResources = favorites.map(StudentFavorite::getLessonResource);

            return getPageResponseDTO(lessonResources);
        }
        catch (Exception e){
            throw new RuntimeException();
        }
    }

    @Override
    @Transactional
    public void removeFavoriteLearningResource(Integer studentId, Integer lessonResourceId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        LessonResource lessonResource = lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy LessonResource với ID: " + lessonResourceId));

        if(lessonResource.getLearningResource() == null){
            throw new ResourceNotFoundException("LessonResource không liên kết với LearningResource");
        }
        learningResourceRepository.findById(lessonResource.getLearningResource().getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Learning resource với ID: " + lessonResource.getLearningResource().getResourceId()));

        StudentFavorite favorite = studentFavoriteRepository
                .findByStudent_StudentIdAndLessonResource_LessonResourceId(studentId, lessonResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa yêu thích LearningResource này"));

        studentFavoriteRepository.delete(favorite);
    }

    @Override
    public boolean checkLearningResourceFavorited(Integer studentId, Integer lessonResourceId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        LessonResource lessonResource = lessonResourceRepository.findById(lessonResourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy LessonResource với ID: " + lessonResourceId));

        if(lessonResource.getLearningResource() == null){
            throw new ResourceNotFoundException("LessonResource không liên kết với LearningResource");
        }
        learningResourceRepository.findById(lessonResource.getLearningResource().getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Learning resource với ID: " + lessonResource.getLearningResource().getResourceId()));

        return studentFavoriteRepository.existsByStudent_StudentIdAndLessonResource_LessonResourceId(studentId, lessonResourceId);
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

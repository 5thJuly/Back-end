package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.LearningResourceDTO;
import org.example.technihongo.dto.LearningResourceStatusDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.LearningResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Component
public class LearningResourceServiceImpl implements LearningResourceService {
    @Autowired
    private LearningResourceRepository learningResourceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentSubscriptionRepository studentSubscriptionRepository;
    @Autowired
    private StudentFavoriteRepository studentFavoriteRepository;
    @Autowired
    private StudentResourceProgressRepository studentResourceProgressRepository;
    @Autowired
    private StudentFlashcardSetRepository studentFlashcardSetRepository;
    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Override
    public List<LearningResource> getAllLearningResources() {
        return learningResourceRepository.findAll();
    }

    @Override
    public LearningResource getLearningResourceById(Integer learningResourceId) {
        if(learningResourceId == null){
            throw new RuntimeException("LearningResource ID không thể null");
        }
        return learningResourceRepository.findById(learningResourceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Learning resource ID!"));
    }

    @Override
    public LearningResource getPublicLearningResourceById(Integer userId, Integer learningResourceId) {
        LearningResource resource = learningResourceRepository.findByResourceId(learningResourceId);
        if(resource == null || !resource.isPublic()) {
            throw new RuntimeException("Không tìm thấy Learning resource ID!");
        }

        User user = userRepository.findByUserId(userId);
        if(user == null){
            throw new RuntimeException("Không tìm thất User ID!");
        }

        if(user.getRole().getRoleId() == 3){
            Student student = studentRepository.findByUser_UserId(user.getUserId());
            if(student == null){
                throw new RuntimeException("Không tìm thấy Student!");
            }

            if(!studentSubscriptionRepository.existsByStudent_StudentIdAndIsActive(student.getStudentId(), true)){
                throw new RuntimeException("Student không được phép xem Learning Resource này!");
            }
        }

        return resource;
    }

    @Override
    public LearningResource createLearningResource(Integer creatorId, LearningResourceDTO learningResourceDTO) {
        User user = userRepository.findByUserId(creatorId);
        if(user == null){
            throw new RuntimeException("User ID not found!");
        }

        LearningResource resource = learningResourceRepository.save(LearningResource.builder()
                .title(learningResourceDTO.getTitle())
                .description(learningResourceDTO.getDescription())
                .creator(user)
                .videoUrl(learningResourceDTO.getVideoUrl())
                .videoFilename(learningResourceDTO.getVideoFilename())
                .pdfUrl(learningResourceDTO.getPdfUrl())
                .pdfFilename(learningResourceDTO.getPdfFilename())
                .isPremium(learningResourceDTO.getIsPremium())
                .build());

        return resource;
    }

    @Override
    public void updateLearningResource(Integer learningResourceId, LearningResourceDTO learningResourceDTO) {
        LearningResource resource = learningResourceRepository.findByResourceId(learningResourceId);
        if(resource == null) {
            throw new RuntimeException("Learning resource ID not found!");
        }

        if(resource.isPublic()){
            throw new RuntimeException("Cannot update a public Learning resource!");
        }

        resource.setTitle(learningResourceDTO.getTitle());
        resource.setDescription(learningResourceDTO.getDescription());
        resource.setVideoUrl(learningResourceDTO.getVideoUrl());
        resource.setVideoFilename(learningResourceDTO.getVideoFilename());
        resource.setPdfUrl(learningResourceDTO.getPdfUrl());
        resource.setPdfFilename(learningResourceDTO.getPdfFilename());
        learningResourceRepository.save(resource);
    }

    @Override
    public void updateLearningResourceStatus(Integer learningResourceId, LearningResourceStatusDTO learningResourceStatusDTO) {
        LearningResource resource = learningResourceRepository.findByResourceId(learningResourceId);
        if(resource == null) {
            throw new RuntimeException("Learning resource ID not found!");
        }

        resource.setPublic(learningResourceStatusDTO.getIsPublic());
        learningResourceRepository.save(resource);

        List<LessonResource> lessonResources = lessonResourceRepository.findByLearningResource_ResourceId(learningResourceId);
        for (LessonResource lessonResource : lessonResources) {
            lessonResource.setActive(learningResourceStatusDTO.getIsPublic());
            lessonResourceRepository.save(lessonResource);
        }
    }

    @Override
    public void deleteLearningResource(Integer learningResourceId) {
        LearningResource resource = learningResourceRepository.findByResourceId(learningResourceId);
        if(resource == null) {
            throw new RuntimeException("Learning resource ID not found!");
        }

        if(resource.isPublic()){
            throw new RuntimeException("Cannot delete a public Learning resource!");
        }

        if (studentFlashcardSetRepository.existsByLearningResource_ResourceId(learningResourceId) ||
            lessonResourceRepository.existsByLearningResource_ResourceId(learningResourceId) ||
            studentResourceProgressRepository.existsByLearningResource_ResourceId(learningResourceId)){
                throw new RuntimeException("Cannot delete a referenced Learning resource!");
        }

        learningResourceRepository.delete(resource);
    }

    @Override
    public List<LearningResource> getListLearningResourcesByCreatorId(Integer creatorId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User ID not found."));
        return learningResourceRepository.findByCreator_UserId(creatorId);
    }
}

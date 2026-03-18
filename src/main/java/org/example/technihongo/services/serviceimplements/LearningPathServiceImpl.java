package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.CreateLearningPathDTO;
import org.example.technihongo.dto.UpdateLearningPathDTO;
import org.example.technihongo.entities.Domain;
import org.example.technihongo.entities.LearningPath;
import org.example.technihongo.entities.PathCourse;
import org.example.technihongo.repositories.DomainRepository;
import org.example.technihongo.repositories.LearningPathRepository;
import org.example.technihongo.repositories.PathCourseRepository;
import org.example.technihongo.repositories.UserRepository;
import org.example.technihongo.services.interfaces.LearningPathService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPathServiceImpl implements LearningPathService {
    private final LearningPathRepository learningPathRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final PathCourseRepository pathCourseRepository;

    @Override
    public List<LearningPath> getAllLearningPaths(String keyword, Integer domainId) {
        if (keyword != null && domainId == null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
        } else if (domainId != null && keyword == null) {
            return learningPathRepository.findByDomain_DomainIdOrderByCreatedAtDesc(domainId);
        } else if (keyword != null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseAndDomain_DomainIdOrderByCreatedAtDesc(keyword, domainId);
        } else {
            return learningPathRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }    }

    @Override
    public List<LearningPath> getPublicLearningPaths(String keyword, Integer domainId) {
        if (keyword != null && domainId == null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword).stream().filter(LearningPath::isPublic).toList();
        } else if (domainId != null && keyword == null) {
            return learningPathRepository.findByDomain_DomainIdOrderByCreatedAtDesc(domainId).stream().filter(LearningPath::isPublic).toList();
        } else if (keyword != null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseAndDomain_DomainIdOrderByCreatedAtDesc(keyword, domainId).stream().filter(LearningPath::isPublic).toList();
        } else {
            return learningPathRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().filter(LearningPath::isPublic).toList();
        }
    }

    @Override
    public List<LearningPath> getLearningPathsByTitle(String keyword) {
        return learningPathRepository.findByTitleContainsIgnoreCaseOrderByCreatedAtDesc(keyword).stream().toList();
    }

    @Override
    public List<LearningPath> getPublicLearningPathsByTitle(String keyword) {
        return learningPathRepository.findByTitleContainsIgnoreCaseOrderByCreatedAtDesc(keyword)
                .stream().filter(LearningPath::isPublic).toList();
    }

    @Override
    public LearningPath getLearningPathById(Integer pathId) {
        return learningPathRepository.findByPathId(pathId);
    }

    @Override
    public LearningPath getPublicLearningPathById(Integer pathId) {
        LearningPath learningPath = learningPathRepository.findByPathId(pathId);
        if(learningPath != null && learningPath.isPublic()) {
            return learningPath;
        }
        else return null;
    }

    @Override
    public LearningPath createLearningPath(Integer creatorId, CreateLearningPathDTO createLearningPathDTO) {
        if(userRepository.findByUserId(creatorId) == null){
            throw new RuntimeException("Creator ID not found!");
        }

        Domain domain = domainRepository.findByDomainId(createLearningPathDTO.getDomainId());
        if(domain == null){
            throw new RuntimeException("Domain ID not found!");
        }
        if(domain.getParentDomain() != null){
            throw new RuntimeException("Cannot assign sub domains!");
        }

        return learningPathRepository.save(LearningPath.builder()
                .title(createLearningPathDTO.getTitle())
                .description(createLearningPathDTO.getDescription())
                .domain(domainRepository.findByDomainId(createLearningPathDTO.getDomainId()))
                .creator(userRepository.findByUserId(creatorId))
                .totalCourses(0)
                .build());
    }

    @Override
    public void updateLearningPath(Integer pathId, UpdateLearningPathDTO updateLearningPathDTO) {
        if(learningPathRepository.findByPathId(pathId) == null){
            throw new RuntimeException("LearningPath ID not found!");
        }

        if(domainRepository.findByDomainId(updateLearningPathDTO.getDomainId()) == null){
            throw new RuntimeException("Domain ID not found!");
        }

        LearningPath learningPath = learningPathRepository.findByPathId(pathId);
        learningPath.setTitle(updateLearningPathDTO.getTitle());
        learningPath.setDescription(updateLearningPathDTO.getDescription());
        learningPath.setDomain(domainRepository.findByDomainId(updateLearningPathDTO.getDomainId()));
        learningPath.setPublic(updateLearningPathDTO.getIsPublic());

        learningPathRepository.save(learningPath);
    }

    @Override
    @Transactional
    public void deleteLearningPath(Integer pathId) {
        if(learningPathRepository.findByPathId(pathId) == null){
            throw new RuntimeException("LearningPath ID not found!");
        }

        LearningPath learningPath = learningPathRepository.findByPathId(pathId);
        if(learningPath.isPublic()){
            throw new RuntimeException("Cannot delete an active learning path!");
        }

        List<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathId(pathId);
        pathCourseRepository.deleteAll(pathCourses);
        learningPathRepository.delete(learningPath);
    }

    @Override
    public void updateTotalCourses(Integer pathId) {
        if(learningPathRepository.findByPathId(pathId) == null){
            throw new RuntimeException("LearningPath ID not found!");
        }

        LearningPath learningPath = learningPathRepository.findByPathId(pathId);
        learningPath.setTotalCourses(pathCourseRepository.countByLearningPath_PathId(pathId));
        learningPathRepository.save(learningPath);
    }

    @Override
    public List<LearningPath> getListLearningPathsByCreatorId(Integer creatorId, String keyword, Integer domainId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User ID not found."));

        if (keyword != null && domainId == null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseAndCreator_UserIdOrderByCreatedAtDesc(keyword, creatorId);
        } else if (domainId != null && keyword == null) {
            return learningPathRepository.findByDomain_DomainIdAndCreator_UserIdOrderByCreatedAtDesc(domainId, creatorId);
        } else if (keyword != null) {
            return learningPathRepository.findByTitleContainingIgnoreCaseAndDomain_DomainIdAndCreator_UserIdOrderByCreatedAtDesc(keyword, domainId, creatorId);
        } else {
            return learningPathRepository.findByCreator_UserIdOrderByCreatedAtDesc(creatorId);
        }
    }
}

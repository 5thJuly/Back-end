package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.CreatePathCourseDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.UpdatePathCourseOrderDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.repositories.CourseRepository;
import org.example.technihongo.repositories.DomainRepository;
import org.example.technihongo.repositories.LearningPathRepository;
import org.example.technihongo.repositories.PathCourseRepository;
import org.example.technihongo.services.interfaces.PathCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Component
public class PathCourseServiceImpl implements PathCourseService {
    @Autowired
    private PathCourseRepository pathCourseRepository;
    @Autowired
    private LearningPathRepository learningPathRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private DomainRepository domainRepository;

    @Override
    public PageResponseDTO<PathCourse> getPathCoursesByLearningPathId(Integer pathId, int pageNo, int pageSize, String sortBy, String sortDir) {
        if(pathId == null){
            throw new RuntimeException("LearningPath ID không thể null");
        }

        if(learningPathRepository.findByPathId(pathId) == null){
            throw new RuntimeException("Không tìm thấy ID LearningPath");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathId(pathId, pageable);
        return getPageResponseDTO(pathCourses);
    }

    @Override
    public PageResponseDTO<PathCourse> getPublicPathCourseListByLearningPathId(Integer pathId, int pageNo, int pageSize, String sortBy, String sortDir) {
        if(pathId == null){
            throw new RuntimeException("LearningPath ID không thể null");
        }

        if(learningPathRepository.findByPathId(pathId) == null){
            throw new RuntimeException("Không tìm thấy ID LearningPath");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathIdAndCourse_PublicStatus(pathId, true, pageable);
        return getPageResponseDTO(pathCourses);
    }

    @Override
    public PathCourse getPathCourseById(Integer pathCourseId) {
        return pathCourseRepository.findById(pathCourseId)
                .orElseThrow(() -> new RuntimeException("PathCourse ID not found"));
    }

    @Override
    @Transactional
    public PathCourse createPathCourse(CreatePathCourseDTO createPathCourseDTO) {
        LearningPath learningPath = learningPathRepository.findByPathId(createPathCourseDTO.getPathId());
        if (learningPath == null) {
            throw new RuntimeException("Không tìm thấy ID LearningPath");
        }

        Course course = courseRepository.findByCourseId(createPathCourseDTO.getCourseId());
        if (course == null) {
            throw new RuntimeException("Course ID not found!");
        }

        if(pathCourseRepository.findByLearningPath_PathIdAndCourse_CourseId(createPathCourseDTO.getPathId(), createPathCourseDTO.getCourseId()) != null){
            throw new RuntimeException("Course already exists in this learning path!");
        }

        Domain learningPathDomain = domainRepository.findById(learningPath.getDomain().getDomainId())
                .orElseThrow(() -> new RuntimeException("LearningPath domain not found!"));
        Domain courseDomain = domainRepository.findById(course.getDomain().getDomainId())
                .orElseThrow(() -> new RuntimeException("Course domain not found!"));

        if (courseDomain.getParentDomain() == null || !courseDomain.getParentDomain().getDomainId().equals(learningPathDomain.getDomainId())) {
            throw new RuntimeException("Course domain must be a direct subdomain of LearningPath domain!");
        }

        PathCourse pathCourse = pathCourseRepository.save(PathCourse.builder()
                .learningPath(learningPathRepository.findByPathId(createPathCourseDTO.getPathId()))
                .course(courseRepository.findByCourseId(createPathCourseDTO.getCourseId()))
                .courseOrder(pathCourseRepository.countByLearningPath_PathId(createPathCourseDTO.getPathId()) + 1)
                .build());

        return pathCourse;
    }

    @Override
    public void updatePathCourseOrder(Integer pathId, UpdatePathCourseOrderDTO updatePathCourseOrderDTO) {
        if(pathId == null){
            throw new RuntimeException("LearningPath ID không thể null");
        }

        if (learningPathRepository.findByPathId(pathId) == null) {
            throw new RuntimeException("Không tìm thấy ID LearningPath");
        }

        List<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathId(pathId);
        List<UpdatePathCourseOrderDTO.PathCourseOrderItem> newOrders = updatePathCourseOrderDTO.getNewPathCourseOrders();

        if (pathCourses.size() != newOrders.size()) {
            throw new RuntimeException("Số lượng PathCourse không đúng với thứ tự mới!");
        }

        Set<Integer> existingIds = pathCourses.stream()
                .map(PathCourse::getPathCourseId)
                .collect(Collectors.toSet());
        for (UpdatePathCourseOrderDTO.PathCourseOrderItem item : newOrders) {
            if (!existingIds.contains(item.getPathCourseId())) {
                throw new RuntimeException("PathCourseId không hợp lệ: " + item.getPathCourseId());
            }
        }

        for (UpdatePathCourseOrderDTO.PathCourseOrderItem item : newOrders) {
            PathCourse pathCourse = pathCourses.stream()
                    .filter(pc -> pc.getPathCourseId().equals(item.getPathCourseId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy PathCourse!"));
            pathCourse.setCourseOrder(item.getCourseOrder());
        }

        pathCourseRepository.saveAll(pathCourses);
    }

    @Override
    @Transactional
    public void deletePathCourse(Integer pathCourseId) {
        PathCourse deletedPathCourse = pathCourseRepository.findById(pathCourseId)
                .orElseThrow(() -> new RuntimeException("PathCourse ID not found"));

        Integer pathId = deletedPathCourse.getLearningPath().getPathId();
        Integer deletedOrder = deletedPathCourse.getCourseOrder();

        pathCourseRepository.delete(deletedPathCourse);

        List<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathIdOrderByCourseOrderAsc(pathId);
        for (PathCourse pathCourse : pathCourses) {
            if (pathCourse.getCourseOrder() > deletedOrder) {
                pathCourse.setCourseOrder(pathCourse.getCourseOrder() - 1);
            }
        }

        pathCourseRepository.saveAll(pathCourses);
    }

    @Override
    public void setPathCourseOrder(Integer pathId, Integer pathCourseId, Integer newOrder) {
        learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID LearningPath"));

        pathCourseRepository.findById(pathCourseId)
                .orElseThrow(() -> new RuntimeException("PathCourse ID not found"));

        List<PathCourse> pathCourses = pathCourseRepository.findByLearningPath_PathIdOrderByCourseOrderAsc(pathId);
        PathCourse target = pathCourses.stream()
                .filter(pc -> pc.getPathCourseId().equals(pathCourseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PathCourse not found!"));

        int currentOrder = target.getCourseOrder();
        if (newOrder < 1 || newOrder > pathCourses.size()) {
            throw new RuntimeException("Invalid order!");
        }

        if (newOrder < currentOrder) {
            pathCourses.stream()
                    .filter(pc -> pc.getCourseOrder() >= newOrder && pc.getCourseOrder() < currentOrder)
                    .forEach(pc -> pc.setCourseOrder(pc.getCourseOrder() + 1));
        } else if (newOrder > currentOrder) {
            pathCourses.stream()
                    .filter(pc -> pc.getCourseOrder() <= newOrder && pc.getCourseOrder() > currentOrder)
                    .forEach(pc -> pc.setCourseOrder(pc.getCourseOrder() - 1));
        }
        target.setCourseOrder(newOrder);

        pathCourseRepository.saveAll(pathCourses);
    }

    private PageResponseDTO<PathCourse> getPageResponseDTO(Page<PathCourse> page) {
        return PageResponseDTO.<PathCourse>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.*;
import org.example.technihongo.entities.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    List<Course> courseList();
    List<CoursePublicDTO> getPublicCourses();
    Optional<Course> getCourseById (Integer courseId);
    Optional<CoursePublicDTO> getPublicCourseById (Integer courseId);
    Course createCourse(Integer creatorId, CreateCourseDTO createCourseDTO);
    void updateCourse(Integer courseId, UpdateCourseDTO updateCourseDTO);
    List<Course> searchCourseByTitle(String keyword);
    List<Course> getListCoursesByCreatorId(Integer creatorId);
    void updateCourseStatus(Integer courseId, CourseStatusDTO courseStatusDTO);

    PageResponseDTO<Course> courseListPaginated(String keyword, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<Course> getPublicCoursesPaginated(String keyword, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<Course> getListCoursesByCreatorIdPaginated(String keyword, Integer creatorId, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<Course> searchCourseByTitlePaginated(String keyword, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<Course> getCourseListByParentDomainId(Integer parentDomainId, int pageNo, int pageSize, String sortBy, String sortDir);
}


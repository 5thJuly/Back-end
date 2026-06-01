package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.CreatePathCourseDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.UpdatePathCourseOrderDTO;
import org.example.technihongo.entities.PathCourse;


public interface PathCourseService {
    PageResponseDTO<PathCourse> getPathCoursesByLearningPathId(Integer pathId, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<PathCourse> getPublicPathCourseListByLearningPathId(Integer pathId, int pageNo, int pageSize, String sortBy, String sortDir);
    PathCourse getPathCourseById(Integer pathCourseId);
    PathCourse createPathCourse(CreatePathCourseDTO createPathCourseDTO);
    void updatePathCourseOrder(Integer pathId, UpdatePathCourseOrderDTO updatePathCourseOrderDTO);
    void deletePathCourse(Integer pathCourseId);
    void setPathCourseOrder(Integer pathId, Integer pathCourseId, Integer newOrder);
}

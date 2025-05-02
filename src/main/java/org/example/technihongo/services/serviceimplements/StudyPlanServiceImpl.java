package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.CreateStudyPlanDTO;
import org.example.technihongo.dto.StudyPlanStatusDTO;
import org.example.technihongo.dto.UpdateStudyPlanDTO;
import org.example.technihongo.entities.Course;
import org.example.technihongo.entities.Lesson;
import org.example.technihongo.entities.StudyPlan;
import org.example.technihongo.enums.StudyPlanStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudyPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Component
public class StudyPlanServiceImpl implements StudyPlanService {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    @Autowired
    private StudentStudyPlanRepository studentStudyPlanRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Override
    public List<StudyPlan> getStudyPlanListByCourseId(Integer courseId) {
        if(courseId == null){
            throw new RuntimeException("Course ID không thể null");
        }
        if(courseRepository.findByCourseId(courseId) == null){
            throw new RuntimeException("Không tim thấy Course ID");
        }

        return studyPlanRepository.findByCourse_CourseId(courseId);
    }

    @Override
    public List<StudyPlan> getActiveStudyPlanListByCourseId(Integer courseId) {
        if(courseId == null){
            throw new RuntimeException("Course ID không thể null");
        }
        if(courseRepository.findByCourseId(courseId) == null){
            throw new RuntimeException("Không tim thấy Course ID");
        }

        return studyPlanRepository.findByCourse_CourseId(courseId)
                .stream()
                .filter(StudyPlan::isActive)
                .toList();
    }

    @Override
    public StudyPlan getStudyPlanById(Integer studyPlanId) {
        return studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found"));
    }

    @Override
    public StudyPlan getActiveStudyPlanById(Integer studyPlanId) {
        return studyPlanRepository.findById(studyPlanId)
                .filter(StudyPlan::isActive)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found"));    }

    @Override
    public StudyPlan createStudyPlan(CreateStudyPlanDTO createStudyPlanDTO) {
        Course course = courseRepository.findByCourseId(createStudyPlanDTO.getCourseId());
        if(course == null){
            throw new RuntimeException("Course ID not found!");
        }

        StudyPlan studyPlan = StudyPlan.builder()
                .course(course)
                .title(createStudyPlanDTO.getTitle())
                .description(createStudyPlanDTO.getDescription())
                .hoursPerDay(createStudyPlanDTO.getHoursPerDay())
                .build();

        studyPlan.setDefault(studyPlanRepository.countByCourse_CourseId(course.getCourseId()) == 0);

        return studyPlanRepository.save(studyPlan);
    }

    @Override
    public void updateStudyPlan(Integer studyPlanId, UpdateStudyPlanDTO updateStudyPlanDTO) {
        StudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found"));

        if(studyPlan.isDefault() && !updateStudyPlanDTO.getIsDefault()){
            throw new RuntimeException("Cannot make default StudyPlan non-default.");
        }

        studyPlan.setTitle(updateStudyPlanDTO.getTitle());
        studyPlan.setDescription(updateStudyPlanDTO.getDescription());
        studyPlan.setHoursPerDay(updateStudyPlanDTO.getHoursPerDay());
//        studyPlan.setActive(updateStudyPlanDTO.getIsActive());

        if(updateStudyPlanDTO.getIsDefault()) {
            List<StudyPlan> studyPlanList = studyPlanRepository.findByCourse_CourseId(studyPlan.getCourse().getCourseId());
            for(StudyPlan s : studyPlanList) {
                s.setDefault(false);
            }
            studyPlanRepository.saveAll(studyPlanList);
            studyPlan.setDefault(true);
        }

        studyPlanRepository.save(studyPlan);
    }

    @Transactional
    @Override
    public void deleteStudyPlan(Integer studyPlanId) {
        StudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found!"));

        if(studyPlan.isActive()) {
            throw new RuntimeException("Cannot delete an active StudyPlan.");
        }

        if(studyPlan.isDefault()) {
            throw new RuntimeException("Cannot delete a default StudyPlan.");
        }

        boolean existsInStudentStudyPlan = studentStudyPlanRepository.existsByStudyPlan_StudyPlanId(studyPlanId);
        if (existsInStudentStudyPlan) {
            throw new IllegalStateException("Cannot delete StudyPlan because it is referenced in StudentStudyPlan.");
        }

        List<Lesson> lessons = lessonRepository.findByStudyPlan_StudyPlanId(studyPlanId);

        List<Integer> lessonIds = lessons.stream().map(Lesson::getLessonId).collect(Collectors.toList());
        if (!lessonIds.isEmpty()) {
            lessonResourceRepository.deleteByLesson_LessonIdIn(lessonIds);
        }

        lessonRepository.deleteByStudyPlan_StudyPlanId(studyPlanId);

        studyPlanRepository.deleteById(studyPlanId);
    }

    @Override
    public StudyPlan getDefaultStudyPlanByCourseId(Integer courseId) {
        return studyPlanRepository.findByCourse_CourseIdAndDefault(courseId, true)
                .orElseThrow(() -> new RuntimeException("StudyPlan default not found!"));
    }

    @Override
    public Course getCourseByStudyPlanId(Integer studyPlanId) {
        return studyPlanRepository.findByStudyPlanId(studyPlanId).getCourse();
    }

    @Override
    public void updateStudyPlanStatus(Integer studyPlanId, StudyPlanStatusDTO studyPlanStatusDTO) {
        StudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("StudyPlan ID not found"));

        boolean hasStudents = studentStudyPlanRepository.findAll().stream()
                .anyMatch(s -> s.getStudyPlan().getStudyPlanId().equals(studyPlanId)
                        && s.getStatus().equals(StudyPlanStatus.ACTIVE));

        if (Boolean.FALSE.equals(studyPlanStatusDTO.getIsActive()) && hasStudents) {
            throw new RuntimeException("Cannot deactivate StudyPlan because students are currently enrolled.");
        }

        studyPlan.setActive(studyPlanStatusDTO.getIsActive());
        studyPlanRepository.save(studyPlan);
    }
}

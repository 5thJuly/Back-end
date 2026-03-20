package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.Course;
import org.example.technihongo.entities.Domain;
import org.example.technihongo.enums.StudyPlanStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Component
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final DifficultyLevelRepository difficultyLevelRepository;
    private final StudentStudyPlanRepository studentStudyPlanRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final PathCourseRepository pathCourseRepository;

    @Override
    public List<Course> courseList() {
        return courseRepository.findAll();
    }

    @Override
    public List<CoursePublicDTO> getPublicCourses() {
        List<Course> courseList = courseRepository.findAll();

        return courseList.stream()
                .filter(Course::isPublicStatus)
                .map(course -> new CoursePublicDTO(course.getCourseId(), course.getTitle(),
                        course.getDescription(), course.getDomain(), course.getDifficultyLevel(),
                        course.getThumbnailUrl(), course.getEstimatedDuration()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Course> getCourseById(Integer courseId) {
        return Optional.ofNullable(courseRepository.findByCourseId(courseId));
    }

    @Override
    public Optional<CoursePublicDTO> getPublicCourseById(Integer courseId) {
        List<Course> courseList = courseRepository.findAll();

        return courseList.stream()
                .filter(course -> course.isPublicStatus() && course.getCourseId().equals(courseId))
                .map(course -> new CoursePublicDTO(course.getCourseId(), course.getTitle(),
                        course.getDescription(), course.getDomain(), course.getDifficultyLevel(),
                        course.getThumbnailUrl(), course.getEstimatedDuration()))
                .findFirst();
    }

    @Override
    public Course createCourse(Integer creatorId, CreateCourseDTO createCourseDTO) {
        if(userRepository.findByUserId(creatorId) == null){
            throw new RuntimeException("Không tìm thấy ID người tạo!");
        }

        if(domainRepository.findByDomainId(createCourseDTO.getDomainId()) == null){
            throw new RuntimeException("Không tìm thấy ID Domain!");
        }

        if(difficultyLevelRepository.findByLevelId(createCourseDTO.getDifficultyLevelId()) == null){
            throw new RuntimeException("Không tìm thấy ID Độ khó!");
        }

        return courseRepository.save(Course.builder()
                .title(createCourseDTO.getTitle())
                .description(createCourseDTO.getDescription())
                .creator(userRepository.findByUserId(creatorId))
                .domain(domainRepository.findByDomainId(createCourseDTO.getDomainId()))
                .difficultyLevel(difficultyLevelRepository.findByLevelId(createCourseDTO.getDifficultyLevelId()))
                .thumbnailUrl(createCourseDTO.getThumbnailUrl())
                .estimatedDuration(createCourseDTO.getEstimatedDuration())
                .isPremium(createCourseDTO.isPremium())
                .build());
    }

    @Override
    public void updateCourse(Integer courseId, UpdateCourseDTO updateCourseDTO) {
        if(courseRepository.findByCourseId(courseId) == null){
            throw new RuntimeException("Không tìm thấy ID khóa học!");
        }

        Domain domain = domainRepository.findByDomainId(updateCourseDTO.getDomainId());
        if(domain == null){
            throw new RuntimeException("Không tìm thấy ID Domain!");
        }
        if(domain.getParentDomain() == null){
            throw new RuntimeException("Không được gắn Domain cha!");
        }

        if(difficultyLevelRepository.findByLevelId(updateCourseDTO.getDifficultyLevelId()) == null){
            throw new RuntimeException("Không tìm thấy ID Độ khó!");
        }

        if(studyPlanRepository.findByCourse_CourseId(courseId).stream()
                .noneMatch(s -> s.isDefault() && s.isActive())){
            throw new RuntimeException("Không tìm thấy StudyPlan mặc định!");
        }

        boolean hasStudents = studentStudyPlanRepository.findAll().stream()
                .anyMatch(s -> s.getStudyPlan().getCourse().getCourseId().equals(courseId)
                            && s.getStatus().equals(StudyPlanStatus.ACTIVE));
//        Boolean.FALSE.equals(updateCourseDTO.getIsPublic()) &&
        if (hasStudents) {
            throw new RuntimeException("Không thể cập nhật khóa học đang có người học.");
        }

        boolean inLearningPath = pathCourseRepository.existsByCourse_CourseId(courseId);
        if (inLearningPath){
            throw new RuntimeException("Không thể cập nhật khóa học đã thêm vào LearningPath.");
        }

        Course course = courseRepository.findByCourseId(courseId);
        course.setTitle(updateCourseDTO.getTitle());
        course.setDescription(updateCourseDTO.getDescription());
        course.setDomain(domainRepository.findByDomainId(updateCourseDTO.getDomainId()));
        course.setDifficultyLevel(difficultyLevelRepository.findByLevelId(updateCourseDTO.getDifficultyLevelId()));
        course.setThumbnailUrl(updateCourseDTO.getThumbnailUrl());
        course.setEstimatedDuration(updateCourseDTO.getEstimatedDuration());
        course.setUpdateAt(LocalDateTime.now());

        courseRepository.save(course);
    }

    @Override
    public List<Course> searchCourseByTitle(String keyword) {
        return courseRepository.findByTitleContainingIgnoreCase(keyword)
                        .stream().filter(Course::isPublicStatus).toList();
    }

    @Override
    public List<Course> getListCoursesByCreatorId(Integer creatorId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User ID not found."));
        return courseRepository.findByCreator_UserId(creatorId);
    }

    @Override
    public void updateCourseStatus(Integer courseId, CourseStatusDTO courseStatusDTO) {
        if(courseRepository.findByCourseId(courseId) == null){
            throw new RuntimeException("Không tìm thấy ID khóa học!");
        }

        if(studyPlanRepository.findByCourse_CourseId(courseId).stream()
                .noneMatch(s -> s.isDefault() && s.isActive())){
            throw new RuntimeException("Không tìm thấy StudyPlan mặc định!");
        }

        boolean hasStudents = studentStudyPlanRepository.findAll().stream()
                .anyMatch(s -> s.getStudyPlan().getCourse().getCourseId().equals(courseId)
                        && s.getStatus().equals(StudyPlanStatus.ACTIVE));
        if (hasStudents) {
            throw new RuntimeException("Không thể cập nhật khóa học đang có người học.");
        }

        Course course = courseRepository.findByCourseId(courseId);
        course.setPublicStatus(courseStatusDTO.getIsPublic());
        course.setUpdateAt(LocalDateTime.now());

        courseRepository.save(course);
    }

    @Override
    public PageResponseDTO<Course> courseListPaginated(String keyword, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Pageable pageable = createPageable(pageNo, pageSize, sortBy, sortDir);
        Page<Course> courses = buildCourseFilter(keyword, domainId, difficultyLevelId, pageable, false, null);
        return getPageResponseDTO(courses);
    }

    @Override
    public PageResponseDTO<Course> getPublicCoursesPaginated(String keyword, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Pageable pageable = createPageable(pageNo, pageSize, sortBy, sortDir);
        Page<Course> courses = buildCourseFilter(keyword, domainId, difficultyLevelId, pageable, true, null);
        return getPageResponseDTO(courses);
    }

    @Override
    public PageResponseDTO<Course> getListCoursesByCreatorIdPaginated(String keyword, Integer creatorId, Integer domainId, Integer difficultyLevelId, int pageNo, int pageSize, String sortBy, String sortDir) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User ID not found."));

        Pageable pageable = createPageable(pageNo, pageSize, sortBy, sortDir);
        Page<Course> courses = buildCourseFilter(keyword, domainId, difficultyLevelId, pageable, false, creatorId);
        return getPageResponseDTO(courses);
    }

    @Override
    public PageResponseDTO<Course> searchCourseByTitlePaginated(String keyword, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Course> courses = courseRepository.findByTitleContainingIgnoreCaseAndPublicStatus(keyword, true, pageable);

        return getPageResponseDTO(courses);
    }

    @Override
    public PageResponseDTO<Course> getCourseListByParentDomainId(Integer parentDomainId, int pageNo, int pageSize, String sortBy, String sortDir) {
        domainRepository.findById(parentDomainId)
                .orElseThrow(() -> new RuntimeException("Parent Domain ID not found!"));

        List<Integer> subDomainIds = domainRepository.findByParentDomain_DomainId(parentDomainId)
                .stream()
                .map(Domain::getDomainId)
                .collect(Collectors.toList());

        if (subDomainIds.isEmpty()) {
            throw new RuntimeException("No subdomains found for this parent domain!");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Course> courses = courseRepository.findByDomain_DomainIdIn(subDomainIds, pageable);

        return getPageResponseDTO(courses);
    }

    private PageResponseDTO<Course> getPageResponseDTO(Page<Course> page) {
        return PageResponseDTO.<Course>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Pageable createPageable(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo, pageSize, sort);
    }

    private Page<Course> buildCourseFilter(String keyword, Integer domainId, Integer difficultyLevelId,
                                          Pageable pageable, boolean isPublic, Integer creatorId) {
        boolean hasKeyword = keyword != null;
        boolean hasDomain = domainId != null;
        boolean hasDifficulty = difficultyLevelId != null;
        boolean hasCreator = creatorId != null;

        // Nếu có creator, filter theo creator
        if (hasCreator) {
            if (hasKeyword && hasDomain && hasDifficulty) {
                return courseRepository.findByDomain_DomainIdAndCreator_UserIdAndTitleContainingIgnoreCaseAndDifficultyLevel_LevelId(domainId, creatorId, keyword, difficultyLevelId, pageable);
            } else if (hasKeyword && hasDomain) {
                return courseRepository.findByDomain_DomainIdAndCreator_UserIdAndTitleContainingIgnoreCase(domainId, creatorId, keyword, pageable);
            } else if (hasKeyword && hasDifficulty) {
                return courseRepository.findByTitleContainingIgnoreCaseAndCreator_UserIdAndDifficultyLevel_LevelId(keyword, creatorId, difficultyLevelId, pageable);
            } else if (hasDomain && hasDifficulty) {
                return courseRepository.findByDomain_DomainIdAndCreator_UserIdAndDifficultyLevel_LevelId(domainId, creatorId, difficultyLevelId, pageable);
            } else if (hasKeyword) {
                return courseRepository.findByTitleContainingIgnoreCaseAndCreator_UserId(keyword, creatorId, pageable);
            } else if (hasDomain) {
                return courseRepository.findByDomain_DomainIdAndCreator_UserId(domainId, creatorId, pageable);
            } else if (hasDifficulty) {
                return courseRepository.findByCreator_UserIdAndDifficultyLevel_LevelId(creatorId, difficultyLevelId, pageable);
            } else {
                return courseRepository.findByCreator_UserId(creatorId, pageable);
            }
        }

        // Filter không có creator - public courses
        if (isPublic) {
            if (hasKeyword && hasDomain && hasDifficulty) {
                return courseRepository.findByTitleContainingIgnoreCaseAndPublicStatusAndDomain_DomainIdAndDifficultyLevel_LevelId(keyword, true, domainId, difficultyLevelId, pageable);
            } else if (hasKeyword && hasDomain) {
                return courseRepository.findByTitleContainingIgnoreCaseAndPublicStatusAndDomain_DomainId(keyword, true, domainId, pageable);
            } else if (hasKeyword && hasDifficulty) {
                return courseRepository.findByTitleContainingIgnoreCaseAndPublicStatusAndDifficultyLevel_LevelId(keyword, true, difficultyLevelId, pageable);
            } else if (hasDomain && hasDifficulty) {
                return courseRepository.findByDomain_DomainIdAndPublicStatusAndDifficultyLevel_LevelId(domainId, true, difficultyLevelId, pageable);
            } else if (hasKeyword) {
                return courseRepository.findByTitleContainingIgnoreCaseAndPublicStatus(keyword, true, pageable);
            } else if (hasDomain) {
                return courseRepository.findByDomain_DomainIdAndPublicStatus(domainId, true, pageable);
            } else if (hasDifficulty) {
                return courseRepository.findByPublicStatusAndDifficultyLevel_LevelId(true, difficultyLevelId, pageable);
            } else {
                return courseRepository.findCoursesByPublicStatus(true, pageable);
            }
        }

        // Filter không có creator - all courses
        if (hasKeyword && hasDomain && hasDifficulty) {
            return courseRepository.findByTitleContainingIgnoreCaseAndDomain_DomainIdAndDifficultyLevel_LevelId(keyword, domainId, difficultyLevelId, pageable);
        } else if (hasKeyword && hasDomain) {
            return courseRepository.findByTitleContainingIgnoreCaseAndDomain_DomainId(keyword, domainId, pageable);
        } else if (hasKeyword && hasDifficulty) {
            return courseRepository.findByTitleContainingIgnoreCaseAndDifficultyLevel_LevelId(keyword, difficultyLevelId, pageable);
        } else if (hasDomain && hasDifficulty) {
            return courseRepository.findByDomain_DomainIdAndDifficultyLevel_LevelId(domainId, difficultyLevelId, pageable);
        } else if (hasKeyword) {
            return courseRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else if (hasDomain) {
            return courseRepository.findByDomain_DomainId(domainId, pageable);
        } else if (hasDifficulty) {
            return courseRepository.findByDifficultyLevel_LevelId(difficultyLevelId, pageable);
        } else {
            return courseRepository.findAll(pageable);
        }
    }
}

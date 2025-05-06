package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.StudentCourseRatingDTO;
import org.example.technihongo.dto.StudentCourseRatingRequest;
import org.example.technihongo.entities.Course;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.StudentCourseRating;
import org.example.technihongo.entities.User;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudentCourseRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentCourseRatingServiceImpl implements StudentCourseRatingService {
    @Autowired
    private StudentCourseRatingRepository studentCourseRatingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentCourseProgressRepository studentCourseProgressRepository;
    @Autowired
    private UserRepository userRepository;


    @Override
    public StudentCourseRatingDTO createRating(Integer studentId, StudentCourseRatingRequest request) {
        return createRatingWithStudentId(studentId, request);
    }

    @Override
    public StudentCourseRatingDTO getRatingById(Integer ratingId) {
        StudentCourseRating rating = studentCourseRatingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        if (rating.isDeleted()) {
            throw new RuntimeException("Đánh giá đã bị xóa.");
        }

        return mapToDTO(rating);
    }

    @Override
    public List<StudentCourseRatingDTO> getAllRatings() {
        List<StudentCourseRating> ratings = studentCourseRatingRepository.findAll();
        return ratings.stream()
                .filter(rating -> !rating.isDeleted())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StudentCourseRatingDTO updateRating(Integer ratingId, Integer studentId, StudentCourseRatingRequest request) {
        return updateRatingWithStudentId(ratingId, studentId, request);
    }

    @Override
    public void deleteRating(Integer ratingId) {
        StudentCourseRating rating = studentCourseRatingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with ID: " + ratingId));

        rating.setDeleted(true);
        studentCourseRatingRepository.save(rating);
//        studentCourseRatingRepository.delete(rating);
    }

    @Override
    public BigDecimal getAverageRatingForCourse(Integer courseId) {
        Course course = courseRepository.findByCourseId(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId);
        }
        List<StudentCourseRating> ratings = studentCourseRatingRepository.findByCourseCourseId(courseId).stream()
                .filter(rating -> !rating.isDeleted())
                .toList();

        if (ratings.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageRating = BigDecimal.valueOf(ratings.stream()
                        .mapToInt(StudentCourseRating::getRating)
                        .average()
                        .orElse(0.0))
                .setScale(2, RoundingMode.HALF_UP);
        return averageRating;
    }

    @Override
    public PageResponseDTO<StudentCourseRatingDTO> getAllRatingsForCourse(Integer courseId, int pageNo, int pageSize, String sortBy, String sortDir) {
        try {
            Course course = courseRepository.findByCourseId(courseId);
            if (course == null) {
                throw new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId);
            }

            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

            Page<StudentCourseRating> ratingsPage = studentCourseRatingRepository.findByCourse_CourseId(courseId, pageable);
            List<StudentCourseRatingDTO> ratingDTOs = ratingsPage.getContent().stream()
                    .filter(rating -> !rating.isDeleted())
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return PageResponseDTO.<StudentCourseRatingDTO>builder()
                    .content(ratingDTOs)
                    .pageNo(ratingsPage.getNumber())
                    .pageSize(ratingsPage.getSize())
                    .totalElements(ratingsPage.getTotalElements())
                    .totalPages(ratingsPage.getTotalPages())
                    .last(ratingsPage.isLast())
                    .build();
        }
        catch (Exception e) {
            throw new RuntimeException("Truy xuất danh sách đánh giá thất bại");

        }
    }

    @Override
    public PageResponseDTO<String> getAllReviewsForCourse(Integer courseId, int pageNo, int pageSize, String sortBy, String sortDir){
        Course course = courseRepository.findByCourseId(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with ID: " + courseId);
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<StudentCourseRating> ratingsPage = studentCourseRatingRepository.findByCourse_CourseId(courseId, pageable);
        List<String> reviews = ratingsPage.getContent().stream()
                .filter(rating -> !rating.isDeleted())
                .map(StudentCourseRating::getReview)
                .filter(review -> review != null && !review.isEmpty())
                .collect(Collectors.toList());

        return PageResponseDTO.<String>builder()
                .content(reviews)
                .pageNo(ratingsPage.getNumber())
                .pageSize(ratingsPage.getSize())
                .totalElements(ratingsPage.getTotalElements())
                .totalPages(ratingsPage.getTotalPages())
                .last(ratingsPage.isLast())
                .build();
    }

    @Override
    public StudentCourseRatingDTO getRatingByStudentAndCourse(Integer studentId, Integer courseId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        Course course = courseRepository.findByCourseId(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with ID: " + courseId);
        }
        StudentCourseRating rating = studentCourseRatingRepository.findByStudentStudentIdAndCourseCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalStateException("No rating found for student ID: " + studentId + " and course ID: " + courseId));
        if (rating.isDeleted()) {
            throw new ResourceNotFoundException("Đánh giá đã bị xóa.");
        }
        return mapToDTO(rating);
    }

    private StudentCourseRatingDTO createRatingWithStudentId(Integer studentId, StudentCourseRatingRequest request) {
        validateRequest(request);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Student với ID: " + studentId));
        Course course = courseRepository.findByCourseId(request.getCourseId());
        if (course == null) {
            throw new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + request.getCourseId());
        }

        boolean isEnrolled = studentCourseProgressRepository.existsByStudentStudentIdAndCourseCourseId(studentId, request.getCourseId());
        if (!isEnrolled) {
            throw new UnauthorizedAccessException("Bạn phải tham gia khóa học mới được đánh giá.");
        }

        if (studentCourseRatingRepository.existsByStudentStudentIdAndCourseCourseId(studentId, request.getCourseId())) {
            throw new IllegalStateException("Bạn đã có sắn 1 đánh giá.");
        }

        StudentCourseRating rating = StudentCourseRating.builder()
                .student(student)
                .course(course)
                .rating(request.getRating())
                .review(request.getReview())
                .isDeleted(false)
                .build();

        StudentCourseRating savedRating = studentCourseRatingRepository.save(rating);
        return mapToDTO(savedRating);
    }

    private StudentCourseRatingDTO updateRatingWithStudentId(Integer ratingId, Integer studentId, StudentCourseRatingRequest request) {
        validateRequest(request);

        StudentCourseRating rating = studentCourseRatingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with ID: " + ratingId));

        if (rating.isDeleted()) {
            throw new UnauthorizedAccessException("Đánh giá đã bị xóa.");
        }

        if (!rating.getStudent().getStudentId().equals(studentId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this rating.");
        }

        Course course = courseRepository.findByCourseId(request.getCourseId());
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with ID: " + request.getCourseId());
        }

        boolean isEnrolled = studentCourseProgressRepository.existsByStudentStudentIdAndCourseCourseId(studentId, request.getCourseId());
        if (!isEnrolled) {
            throw new UnauthorizedAccessException("Student must enroll in the course before updating the rating.");
        }
        rating.setCourse(course);

        if(request.getRating() != null) {
            rating.setRating(request.getRating());
        }

        if(request.getReview() != null) {
            rating.setReview(request.getReview());
        }
        StudentCourseRating updatedRating = studentCourseRatingRepository.save(rating);
        return mapToDTO(updatedRating);
    }

    private void validateRequest(StudentCourseRatingRequest request) {

        if (request.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID không thể null.");
        }
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5.");
        }
        if (request.getReview() != null && request.getReview().trim().isEmpty()) {
            throw new IllegalArgumentException("Review hoặc rating không thể null.");
        }
    }

    private StudentCourseRatingDTO mapToDTO(StudentCourseRating rating) {
        User user = userRepository.findByStudentStudentId(rating.getStudent().getStudentId());
        if(user == null){
            throw new ResourceNotFoundException("User not found!");
        }

        StudentCourseRatingDTO dto = new StudentCourseRatingDTO();
        dto.setRatingId(rating.getRatingId());
        dto.setStudentId(rating.getStudent().getStudentId());
        dto.setUserName(user.getUserName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setProfileImg(user.getProfileImg());
        dto.setCourseId(rating.getCourse().getCourseId());
        dto.setRating(rating.getRating());
        dto.setReview(rating.getReview());

        return dto;
    }
}

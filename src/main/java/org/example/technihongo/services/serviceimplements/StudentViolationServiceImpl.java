package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.mail.EmailService;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ViolationStatus;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.repositories.StudentCourseRatingRepository;
import org.example.technihongo.repositories.StudentFlashcardSetRepository;
import org.example.technihongo.repositories.StudentViolationRepository;
import org.example.technihongo.repositories.UserRepository;
import org.example.technihongo.services.interfaces.StudentViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudentViolationServiceImpl implements StudentViolationService {
    @Autowired
    private StudentViolationRepository studentViolationRepository;
    @Autowired
    private StudentFlashcardSetRepository studentFlashcardSetRepository;
    @Autowired
    private StudentCourseRatingRepository studentCourseRatingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;



    @Override
    public PageResponseDTO<StudentViolation> getAllStudentViolations(String classifyBy, String status, int pageNo, int pageSize, String sortBy, String sortDir) {
        if (!"FlashcardSet".equalsIgnoreCase(classifyBy) && !"Rating".equalsIgnoreCase(classifyBy)) {
            throw new RuntimeException("classifyBy phải là 'FlashcardSet' hoặc 'Rating'.");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        ViolationStatus violationStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                violationStatus = ViolationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status không hợp lệ, phải là PENDING, RESOLVED, hoặc DISMISSED.");
            }
        }

        Page<StudentViolation> violations = studentViolationRepository.findByClassifyByAndStatus(
                classifyBy.toLowerCase(),
                violationStatus,
                pageable);

        return getPageResponseDTO(violations);
    }

    @Override
    @Transactional
    public ReportViolationResponseDTO reportViolation(Integer reportedBy, ReportViolationRequestDTO request) {
        String classifyBy = request.getClassifyBy();
        if (!"FlashcardSet".equalsIgnoreCase(classifyBy) && !"Rating".equalsIgnoreCase(classifyBy)) {
            throw new RuntimeException("classifyBy phải là 'FlashcardSet' hoặc 'Rating'.");
        }

        Integer contentId = request.getContentId();
        if (contentId == null) {
            throw new RuntimeException("ContentId không thể null!");
        }
        if(request.getDescription().isEmpty()){
            throw new RuntimeException("Vui lòng điền mô tả vi phạm");
        }

        User user = userRepository.findById(reportedBy)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + reportedBy));

        StudentViolation violation = StudentViolation.builder()
                .description(request.getDescription())
                .reportedBy(user)
                .status(ViolationStatus.PENDING)
                .build();

        if ("FlashcardSet".equalsIgnoreCase(classifyBy)) {
            StudentFlashcardSet flashcardSet = studentFlashcardSetRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy StudentFlashcardSet với ID: " + contentId));
            violation.setStudentFlashcardSet(flashcardSet);
            violation.setStudentCourseRating(null);
        } else {
            StudentCourseRating rating = studentCourseRatingRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy StudentCourseRating với ID: " + contentId));
            violation.setStudentCourseRating(rating);
            violation.setStudentFlashcardSet(null);
        }

        studentViolationRepository.save(violation);

        ReportViolationResponseDTO response = new ReportViolationResponseDTO();
        response.setMessage("Thông báo đã được gửi tới Admin, cảm ơn bạn đã đóng góp cho app của chúng tôi");
        return response;
    }

    @Override
    @Transactional
    public HandleViolationResponseDTO handleViolation(Integer violationId, Integer handledBy, HandleViolationRequestDTO request) {
        if (violationId == null) {
            throw new ResourceNotFoundException("violationId không thể null");
        }
        StudentViolation violation = studentViolationRepository.findByViolationId(violationId);
        if (violation == null) {
            throw new ResourceNotFoundException("Không tìm thấy StudentViolation với ID: " + violationId);
        }

        if (!violation.getStatus().equals(ViolationStatus.PENDING)) {
            throw new IllegalStateException("Vi phạm đã được xử lý trước đó.");
        }

        String statusStr = request.getStatus();
        if (!"RESOLVED".equalsIgnoreCase(statusStr) && !"DISMISSED".equalsIgnoreCase(statusStr)) {
            throw new IllegalArgumentException("Trạng thái phải là 'RESOLVED' hoặc 'DISMISSED'.");
        }
        ViolationStatus status = ViolationStatus.valueOf(statusStr.toUpperCase());

        // Xác thực handledBy - đảm bảo là admin
        User admin = validateAdmin(handledBy);

        // Chuẩn bị response
        HandleViolationResponseDTO response = new HandleViolationResponseDTO();
        response.setViolationId(violationId);

        // Xử lý DISMISSED - trường hợp bác bỏ vi phạm
        if (status == ViolationStatus.DISMISSED) {
            updateViolationStatus(violation, admin, request.getActionTaken(), status);
            response.setMessage("Vi phạm đã bị bác bỏ.");
        }

        // Xử lý RESOLVED - trường hợp xác nhận vi phạm
        if (violation.getStudentFlashcardSet() != null && status == ViolationStatus.RESOLVED) {
            StudentFlashcardSet flashcardSet = violation.getStudentFlashcardSet();
            Student student = flashcardSet.getCreator();

            // Tăng violationCount cho student (xử lý ở mức student)
            int violationCount = incrementStudentViolationCount(student);

            // Xử lý flashcard set theo mức độ vi phạm
            FlashcardSetViolationResponseDTO flashcardResponse = processFlashcardSetViolation(
                    flashcardSet, student, violationCount, request.getActionTaken());
            response.setMessage(flashcardResponse.getMessage());

            // Đặt thời gian xử lý vi phạm nếu là lần đầu
            if (violationCount == 1) {
                violation.setViolationHandledAt(LocalDateTime.now());
            }
        } else if (violation.getStudentCourseRating() != null && status == ViolationStatus.RESOLVED) {
            // Xử lý cho Rating (có thể bổ sung code tương tự như trên)
            response.setMessage("Vi phạm cho Rating đã được xử lý.");
        }

        // Cập nhật trạng thái vi phạm chính
        updateViolationStatus(violation, admin, request.getActionTaken(), status);

        // Tìm và xử lý các vi phạm PENDING liên quan
        List<StudentViolation> relatedViolations = findRelatedPendingViolations(violation);
        for (StudentViolation relatedViolation : relatedViolations) {
            updateViolationStatus(
                    relatedViolation,
                    admin,
                    String.format("Tự động xử lý cùng với vi phạm có ID %d", violationId),
                    status
            );
        }

        studentViolationRepository.save(violation);
        if (!relatedViolations.isEmpty()) {
            studentViolationRepository.saveAll(relatedViolations);
        }

        return response;
    }

    private void updateViolationStatus(StudentViolation violation, User admin, String actionTaken, ViolationStatus status) {
        violation.setHandledBy(admin);
        violation.setActionTaken(actionTaken);
        violation.setStatus(status);
        violation.setResolvedAt(LocalDateTime.now());
    }

    private int incrementStudentViolationCount(Student student) {
        int violationCount = student.getViolationCount() != null ? student.getViolationCount() : 0;
        violationCount++;
        student.setViolationCount(violationCount);
        userRepository.save(student.getUser());
        return violationCount;
    }

    private User validateAdmin(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId không được để trống.");
        }
        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        if (!admin.getRole().getRoleId().equals(1)) {
            throw new UnauthorizedAccessException("Chỉ ADMIN mới được xử lý vi phạm");
        }
        return admin;
    }

    private FlashcardSetViolationResponseDTO processFlashcardSetViolation(
            StudentFlashcardSet flashcardSet, Student student, int violationCount, String actionTaken) {

        FlashcardSetViolationResponseDTO response = new FlashcardSetViolationResponseDTO();
        response.setStudentSetId(flashcardSet.getStudentSetId());

        // Đánh dấu là bị vi phạm
        flashcardSet.setViolated(true);

        // Xử lý theo số lần vi phạm
        if (violationCount == 1) {
            flashcardSet.setPublic(false);
            response.setMessage("Bộ flashcard của bạn bị report, vui lòng chỉnh sửa trong 24 giờ.");
        } else if (violationCount >= 2) {
            flashcardSet.setDeleted(true);
            response.setMessage("Bộ flashcard của bạn đã bị xóa do vi phạm lần thứ " + violationCount + ".");
        }

        // Gửi email thông báo
        emailService.sendViolationEmail(student, flashcardSet.getTitle(), actionTaken, violationCount);

        // Lưu thay đổi
        studentFlashcardSetRepository.save(flashcardSet);

        return response;
    }



    @Override
    @Transactional(readOnly = true)
    public ViolationSummaryDTO getViolationSummary(String classifyBy, String statusFilter, Integer entityId, int pageNo, int pageSize, String sortBy, String sortDir) {
        if (!"FlashcardSet".equalsIgnoreCase(classifyBy) && !"Rating".equalsIgnoreCase(classifyBy)) {
            throw new RuntimeException("classifyBy phải là 'FlashcardSet' hoặc 'Rating'.");
        }

        if (entityId == null) {
            throw new RuntimeException("entityId là bắt buộc.");
        }

        ViolationStatus violationStatus = null;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            try {
                violationStatus = ViolationStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Giá trị status không hợp lệ. Phải là PENDING, RESOLVED hoặc DISMISSED.");
            }
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<StudentViolation> violationPage;
        long totalViolations;
        if ("FlashcardSet".equalsIgnoreCase(classifyBy)) {
            violationPage = studentViolationRepository.findByStudentFlashcardSetId(entityId, violationStatus, pageable);
            totalViolations = studentViolationRepository.countByStudentFlashcardSetId(entityId);
        } else {
            violationPage = studentViolationRepository.findByStudentCourseRatingId(entityId, violationStatus, pageable);
            totalViolations = studentViolationRepository.countByStudentCourseRatingId(entityId);
        }

        List<ViolationSummaryDTO.ViolationDescriptionDTO> descriptions = violationPage.getContent().stream()
                .map(v -> {
                    ViolationSummaryDTO.ViolationDescriptionDTO descDTO = new ViolationSummaryDTO.ViolationDescriptionDTO();
                    descDTO.setViolationId(v.getViolationId());
                    descDTO.setDescription(v.getDescription());
                    descDTO.setStatus(v.getStatus().name());
                    descDTO.setCreatedAt(v.getCreatedAt());
                    return descDTO;
                })
                .collect(Collectors.toList());

        PageResponseDTO<ViolationSummaryDTO.ViolationDescriptionDTO> descriptionsPage = PageResponseDTO
                .<ViolationSummaryDTO.ViolationDescriptionDTO>builder()
                .content(descriptions)
                .pageNo(violationPage.getNumber())
                .pageSize(violationPage.getSize())
                .totalElements(violationPage.getTotalElements())
                .totalPages(violationPage.getTotalPages())
                .last(violationPage.isLast())
                .build();

        ViolationSummaryDTO dto = new ViolationSummaryDTO();
        if ("FlashcardSet".equalsIgnoreCase(classifyBy)) {
            dto.setStudentSetId(entityId);
        } else {
            dto.setRatingId(entityId);
        }
        dto.setTotalViolations(totalViolations);
        dto.setDescriptions(descriptionsPage);

        return dto;
    }

    private List<StudentViolation> findRelatedPendingViolations(StudentViolation violation) {
        // Tìm các vi phạm PENDING khác cho cùng student_set_id hoặc rating_id
        if (violation.getStudentFlashcardSet() != null) {
            return studentViolationRepository.findByStudentFlashcardSetStudentSetIdAndStatusAndViolationIdNot(
                    violation.getStudentFlashcardSet().getStudentSetId(),
                    ViolationStatus.PENDING,
                    violation.getViolationId()
            );
        } else if (violation.getStudentCourseRating() != null) {
            return studentViolationRepository.findByStudentCourseRatingRatingIdAndStatusAndViolationIdNot(
                    violation.getStudentCourseRating().getRatingId(),
                    ViolationStatus.PENDING,
                    violation.getViolationId()
            );
        }
        return List.of();
    }

    private PageResponseDTO<StudentViolation> getPageResponseDTO(Page<StudentViolation> page) {
        return PageResponseDTO.<StudentViolation>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

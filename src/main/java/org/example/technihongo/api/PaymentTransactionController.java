package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.PaymentTransaction;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.enums.TransactionStatus;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.PaymentTransactionService;
import org.example.technihongo.services.interfaces.StudentService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${spring.application.api-prefix}/payment")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentTransactionService;
    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionController.class);
    private final JwtUtil jwtUtil;
    private final StudentService studentService;
    private final UserActivityLogService userActivityLogService;


    @PostMapping("/initiateMomo")
    public ResponseEntity<ApiResponse> initiateMoMoPayment(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @RequestBody PaymentRequestDTO requestDTO) {
        try {
            if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);
                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        userId,
                        ActivityType.REGISTER_SUBSCRIPTION,
                        ContentType.PaymentTransaction,
                        null,
                        ipAddress,
                        userAgent
                );
                PaymentResponseDTO responseDTO = paymentTransactionService.initiateMoMoPayment(studentId, requestDTO);
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("MoMo payment initiated successfully!")
                        .data(responseDTO)
                        .build());
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.builder()
                        .success(false)
                        .message("Unauthorized access!")
                        .data(null)
                        .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Invalid request: " + e.getMessage())
                    .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Payment method unavailable: " + e.getMessage())
                    .build());
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to initiate MoMo payment: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Internal Server Error: " + e.getMessage())
                    .build());
        }
    }


    @GetMapping("/ipn-handler")
    public ResponseEntity<String> ipnHandler(@RequestParam Map<String, String> request) {
        try {
            MomoCallbackDTO callbackDTO = MomoCallbackDTO.builder()
                    .partnerCode(request.get("partnerCode"))
                    .orderId(request.get("orderId"))
                    .requestId(request.get("requestId"))
                    .amount(request.get("amount"))
                    .resultCode(request.get("resultCode"))
                    .message(request.get("message"))
                    .signature(request.get("signature"))
                    .orderInfo(request.get("orderInfo"))
                    .orderType(request.get("orderType"))
                    .payType(request.get("payType"))
                    .transId(request.get("transId"))
                    .responseTime(request.get("responseTime"))
                    .build();
            paymentTransactionService.handleMoMoCallback(callbackDTO, request);
            return ResponseEntity.ok("Callback processed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Callback failed: " + e.getMessage());
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse> handleMoMoReturn(@RequestParam Map<String, String> request) {
        try {
            log.info("Received MoMo callback: {}", request);

            MomoCallbackDTO callbackDTO = MomoCallbackDTO.builder()
                    .partnerCode(request.get("partnerCode"))
                    .orderId(request.get("orderId"))
                    .requestId(request.get("requestId"))
                    .amount(request.get("amount"))
                    .resultCode(request.get("resultCode"))
                    .message(request.get("message"))
                    .signature(request.get("signature"))
                    .orderInfo(request.get("orderInfo"))
                    .orderType(request.get("orderType"))
                    .payType(request.get("payType"))
                    .transId(request.get("transId"))
                    .responseTime(request.get("responseTime"))
                    .build();

            paymentTransactionService.handleMoMoCallback(callbackDTO, request);
            if ("0".equals(callbackDTO.getResultCode())) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Payment completed successfully!")
                        .data(Map.of("orderId", callbackDTO.getOrderId()))
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                        .success(false)
                        .message("Payment failed: " + callbackDTO.getMessage())
                        .data(Map.of("orderId", callbackDTO.getOrderId()))
                        .build());
            }
        } catch (Exception e) {
            log.error("Error processing MoMo callback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Error processing callback: " + e.getMessage())
                    .build());
        }
    }

//
    @GetMapping("/studentTransaction")
    public ResponseEntity<ApiResponse> getPaymentHistoryByStudentId(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String transactionStatus,
            HttpServletRequest httpRequest,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);
              
                PageResponseDTO<PaymentTransactionDTO> history = paymentTransactionService.getPaymentHistoryByStudentId(
                    studentId, pageNo, pageSize, sortBy, sortDir, transactionStatus);
              
                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        userId,
                        ActivityType.VIEW,
                        ContentType.PaymentTransaction,
                        null,
                        ipAddress,
                        userAgent
                );
                
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Payment history retrieved successfully!")
                        .data(history)
                        .build());
            }  else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Unauthorized")
                                .build());
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to retrieve payment history: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Internal Server Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllPaymentHistory(
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) String transactionStatus) {
        try {
            PaymentHistoryRequestDTO request = new PaymentHistoryRequestDTO();
            request.setStudentId(studentId);
            if (transactionStatus != null) {
                request.setTransactionStatus(TransactionStatus.valueOf(transactionStatus.toUpperCase()));
            }

            List<PaymentTransactionDTO> history = paymentTransactionService.getAllPaymentHistory(request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Payment history retrieved successfully!")
                    .data(history)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Invalid transaction status: " + e.getMessage())
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to retrieve payment history: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Internal Server Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse> getPaymentTransactionById(
            @RequestParam Integer transactionId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            PaymentTransaction transaction = paymentTransactionService.getPaymentTransactionById(studentId, transactionId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Payment transaction retrieved successfully!")
                    .data(transaction)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to retrieve payment transaction: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Internal Server Error: " + e.getMessage())
                    .build());
        }
    }

    private Integer extractStudentId(String authorizationHeader) throws Exception {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            Integer userId = jwtUtil.extractUserId(token);
            return studentService.getStudentIdByUserId(userId);
        }
        throw new Exception("Authorization failed!");
    }
}




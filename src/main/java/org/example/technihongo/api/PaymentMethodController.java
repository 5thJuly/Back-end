package org.example.technihongo.api;

import org.example.technihongo.dto.UpdatePaymentMethodRequestDTO;
import org.example.technihongo.entities.PaymentMethod;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.PaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-method")
public class PaymentMethodController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    @PatchMapping("/{methodId}/update")
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public ResponseEntity<ApiResponse> updatePaymentMethod(
            @PathVariable Integer methodId,
            @RequestBody UpdatePaymentMethodRequestDTO request) {
        try {
            PaymentMethod updatedPaymentMethod = paymentMethodService.updatePaymentMethod(methodId, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Payment method updated successfully")
                    .data(updatedPaymentMethod)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Payment method not found: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to update payment method: " + e.getMessage())
                            .build());
        }
    }
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllPaymentMethod() throws Exception {
        List<PaymentMethod> paymentMethodList = paymentMethodService.paymentMethodList();
        if(paymentMethodList.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.builder()
                            .success(false)
                            .message("List PaymentMethod is empty")
                            .build());
        }
        else {
            return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("All Payment Method : ")
                            .data(paymentMethodList)
                            .build());
        }
    }


}
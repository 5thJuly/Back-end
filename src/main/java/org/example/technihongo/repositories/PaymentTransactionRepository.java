package org.example.technihongo.repositories;

import org.example.technihongo.entities.PaymentTransaction;
import org.example.technihongo.entities.SubscriptionPlan;
import org.example.technihongo.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    Optional<PaymentTransaction> findByExternalOrderId(String externalOrderId);
    List<PaymentTransaction> findBySubscription_Student_StudentId(Integer studentId);
    List<PaymentTransaction> findByTransactionStatus(TransactionStatus status);
    List<PaymentTransaction> findBySubscription_Student_StudentIdAndTransactionStatus(Integer studentId, TransactionStatus status);
    PaymentTransaction findByTransactionId(Integer transactionId);
    List<PaymentTransaction> findAllBySubscription_SubscriptionId(Integer subscriptionId);
    Page<PaymentTransaction> findBySubscription_Student_StudentId(
            Integer studentId, Pageable pageable);
    Page<PaymentTransaction> findBySubscription_Student_StudentIdAndTransactionStatus(
            Integer studentId, TransactionStatus transactionStatus, Pageable pageable);
    List<PaymentTransaction> findAllBySubscription_SubscriptionIdAndTransactionStatusOrderByPaymentDateDesc(Integer subscriptionId, TransactionStatus transactionStatus);

    List<PaymentTransaction> findBySubscription_SubscriptionIdAndTransactionStatus(Integer subscriptionId, TransactionStatus transactionStatus);

    @Query("SELECT SUM(pt.transactionAmount) FROM PaymentTransaction pt JOIN StudentSubscription ss ON pt.subscription.subscriptionId = ss.subscriptionId " +
            "WHERE ss.student.studentId = :studentId and pt.transactionStatus = 'COMPLETED'")
    Double sumByStudentId(@Param("studentId") Integer studentId);

    @Query(value = "SELECT CAST(pt.payment_date AS DATE) AS paymentDay, SUM(pt.transaction_amount) AS total " +
            "FROM PaymentTransaction pt " +
            "WHERE pt.payment_date >= :startDate and pt.transaction_status = 'COMPLETED' " +
            "GROUP BY CAST(pt.payment_date AS DATE)", nativeQuery = true)
    List<Object[]> sumByDay(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT DATEADD(DAY, -DATEPART(WEEKDAY, pt.payment_date) + 1, CAST(pt.payment_date AS DATE)) AS weekStart, SUM(pt.transaction_amount) AS total " +
            "FROM PaymentTransaction pt " +
            "WHERE pt.payment_date >= :startDate and pt.transaction_status = 'COMPLETED' " +
            "GROUP BY DATEADD(DAY, -DATEPART(WEEKDAY, pt.payment_date) + 1, CAST(pt.payment_date AS DATE))", nativeQuery = true)
    List<Object[]> sumByWeek(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT DATEADD(MONTH, DATEDIFF(MONTH, 0, pt.payment_date), 0) AS monthStart, SUM(pt.transaction_amount) AS total " +
            "FROM PaymentTransaction pt " +
            "WHERE pt.payment_date >= :startDate and pt.transaction_status = 'COMPLETED' " +
            "GROUP BY DATEADD(MONTH, DATEDIFF(MONTH, 0, pt.payment_date), 0)", nativeQuery = true)
    List<Object[]> sumByMonth(@Param("startDate") LocalDateTime startDate);

    List<PaymentTransaction> findAllBySubscription_SubscriptionPlanAndTransactionStatus(SubscriptionPlan plan, TransactionStatus status);

    List<PaymentTransaction> findAllByTransactionStatus(TransactionStatus status);

    boolean existsByExternalOrderId(String orderId);
}






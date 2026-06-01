package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.dto.SubscriptionDTO;
import org.example.technihongo.dto.UpdateSubscriptionDTO;
import org.example.technihongo.entities.SubscriptionPlan;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.repositories.StudentSubscriptionRepository;
import org.example.technihongo.repositories.SubscriptionPlanRepository;
import org.example.technihongo.services.interfaces.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private StudentSubscriptionRepository studentSubscriptionRepository;

    @Override
    public SubscriptionPlan createSubscriptionPlan(SubscriptionDTO subscriptionDTO) {
        if(subscriptionDTO.getName() == null || subscriptionDTO.getPrice() == null || subscriptionDTO.getDurationDays() == null) {
            throw new IllegalArgumentException("Bạn cần điền tất cả thông tin!");

        }
        if (subscriptionPlanRepository.existsByName(subscriptionDTO.getName())) {
            throw new IllegalArgumentException("Subscription plan đã tồn tại với tên này!");
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(subscriptionDTO.getName())
                .price(subscriptionDTO.getPrice())
                .benefits(subscriptionDTO.getBenefits())
                .durationDays(subscriptionDTO.getDurationDays())
                .isActive(subscriptionDTO.isActive())
                .build();

        return subscriptionPlanRepository.save(plan);
    }

    @Override
    public SubscriptionPlan updateSubscriptionPlan(Integer id, UpdateSubscriptionDTO dto) {
        SubscriptionPlan existingPlan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with id: " + id));

        Optional.ofNullable(dto.getPrice()).ifPresent(existingPlan::setPrice);
        Optional.ofNullable(dto.getBenefits()).ifPresent(existingPlan::setBenefits);
        Optional.ofNullable(dto.getDurationDays()).ifPresent(existingPlan::setDurationDays);
        existingPlan.setActive(dto.isActive());
        return subscriptionPlanRepository.save(existingPlan);
    }

    @Override
    public void deleteSubscriptionPlan(Integer Id) {
        Optional<SubscriptionPlan> plan = subscriptionPlanRepository.findById(Id);
        if(plan.isEmpty()) {
            throw new RuntimeException("Subscription plan not found!");
        }
        if(checkIfPlanIsUsed(plan.get())) {
            throw new RuntimeException("Can't delete: SubscriptionPlan đang được sử dụng!");
        }
        subscriptionPlanRepository.deleteById(Id);

    }

    @Override
    public List<SubscriptionPlan> subscriptionList() {
        return subscriptionPlanRepository.findAll();
    }

    @Override
    public SubscriptionPlan getSubscriptionPlanById(Integer planId) {
        return subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription Plan not found!"));
    }

    private boolean checkIfPlanIsUsed(SubscriptionPlan plan) {
        return studentSubscriptionRepository.existsBySubscriptionPlan(plan);
    }
}

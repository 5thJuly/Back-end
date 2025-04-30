package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.dto.FlashcardResponseDTO;
import org.example.technihongo.dto.SystemFlashcardSetRequestDTO;
import org.example.technihongo.dto.SystemFlashcardSetResponseDTO;
import org.example.technihongo.dto.UpdateFlashcardOrderDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.DifficultyLevelEnum;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.SystemFlashcardSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SystemFlashcardSetServiceImpl implements SystemFlashcardSetService {
    @Autowired
    private SystemFlashcardSetRepository systemFlashcardSetRepository;
    @Autowired
    private FlashcardRepository flashcardRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DifficultyLevelRepository difficultyLevelRepository;
    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Override
    public SystemFlashcardSetResponseDTO create(Integer userId, SystemFlashcardSetRequestDTO requestDTO) {
        if (requestDTO.getTitle() == null) {
            throw new IllegalArgumentException("Vui lòng nhập Title nhé!");
        }

        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new ResourceNotFoundException("Content Manager not found with Id: " + userId);
        }

        SystemFlashcardSet flashcardSet = new SystemFlashcardSet();
        flashcardSet.setTitle(requestDTO.getTitle());
        flashcardSet.setDescription(requestDTO.getDescription());
        flashcardSet.setPublic(requestDTO.getIsPublic() != null ? requestDTO.getIsPublic() : false);
        flashcardSet.setPremium(requestDTO.getIsPremium() != null ? requestDTO.getIsPremium() : false);
        flashcardSet.setTotalCards(0);
        flashcardSet.setCreator(user);
        flashcardSet.setDeleted(false);

        if (requestDTO.getDifficultyLevel() != null) {
            DifficultyLevel difficultyLevel = difficultyLevelRepository.findByTag(requestDTO.getDifficultyLevel());
            if (difficultyLevel == null) {
                throw new ResourceNotFoundException("DifficultyLevel not found: " + requestDTO.getDifficultyLevel());
            }
            flashcardSet.setDifficultyLevel(difficultyLevel);
        }

        flashcardSet = systemFlashcardSetRepository.save(flashcardSet);
        return convertToSystemFlashcardSetResponseDTO(flashcardSet);
    }

    @Override
    public SystemFlashcardSetResponseDTO update(Integer userId, Integer flashcardSetId, SystemFlashcardSetRequestDTO requestDTO) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashcardSet not found with Id: " + flashcardSetId));

        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có thẩm quyền cập nhật FlashcardSet này.");
        }

        if (requestDTO.getIsPremium() != null && !requestDTO.getIsPremium().equals(flashcardSet.isPremium())) {
            throw new IllegalArgumentException("Premium status cannot be changed after creation");
        }

        if (requestDTO.getTitle() != null) {
            flashcardSet.setTitle(requestDTO.getTitle());
        }
        if (requestDTO.getDescription() != null) {
            flashcardSet.setDescription(requestDTO.getDescription());
        }
        Boolean newIsPublic = requestDTO.getIsPublic();
        if (newIsPublic != null) {
            flashcardSet.setPublic(newIsPublic);
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Flashcard> flashcardPage = flashcardRepository.findBySystemFlashCardSetSystemSetId(flashcardSetId, pageable);
        flashcardSet.setTotalCards((int) flashcardPage.getTotalElements());

        flashcardSet = systemFlashcardSetRepository.save(flashcardSet);

        if (newIsPublic != null) {
            List<LessonResource> lessonResources = lessonResourceRepository.findBySystemFlashCardSet_SystemSetId(flashcardSetId);
            for (LessonResource lessonResource : lessonResources) {
                lessonResource.setActive(newIsPublic);
                lessonResourceRepository.save(lessonResource);
            }
        }

        return convertToSystemFlashcardSetResponseDTO(flashcardSet);
    }

    @Override
    public void updateFlashcardOrder(Integer userId, Integer flashcardSetId, UpdateFlashcardOrderDTO updateFlashcardOrderDTO) {
        // Validate input
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (flashcardSetId == null) {
            throw new IllegalArgumentException("Flashcard set ID cannot be null");
        }
        if (updateFlashcardOrderDTO == null ||
                updateFlashcardOrderDTO.getNewFlashcardOrder() == null ||
                updateFlashcardOrderDTO.getNewFlashcardOrder().isEmpty()) {
            throw new IllegalArgumentException("New flashcard order không thể trống");
        }

        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("System Flashcard Set not found with ID: " + flashcardSetId));

        if (flashcardSet.isDeleted()) {
            throw new ResourceNotFoundException("SystemFlashcardSet đã bị xóa.");
        }
        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có thẩm quyền cập nhật FlashcardSet này.");
        }

        List<Flashcard> flashcards = flashcardRepository.findBySystemFlashCardSet_SystemSetId(flashcardSetId);

        Map<Integer, Flashcard> flashcardMap = flashcards.stream()
                .collect(Collectors.toMap(Flashcard::getCardOrder, Function.identity()));

        List<Integer> newOrder = updateFlashcardOrderDTO.getNewFlashcardOrder();
        for (int i = 0; i < newOrder.size(); i++) {
            Integer cardOrder = newOrder.get(i);
            Flashcard flashcard = flashcardMap.get(cardOrder);
            if (flashcard != null) {
                flashcard.setCardOrder(i + 1);
            } else {
                throw new ResourceNotFoundException("Flashcard with order " + cardOrder + " not found in the specified system flashcard set");
            }
        }

        flashcardRepository.saveAll(flashcards);
    }

    @Override
    public void deleteSystemFlashcardSet(Integer userId, Integer flashcardSetId) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashcardSet not found with Id: " + flashcardSetId));

        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có thẩm quyền xóa FlashcardSet này.");
        }

        flashcardSet.setDeleted(true);
        systemFlashcardSetRepository.save(flashcardSet);
    }

    @Override
    public SystemFlashcardSetResponseDTO getSystemFlashcardSetById(Integer flashcardSetId) {
        SystemFlashcardSet flashcardSet = getActiveFlashcardSet(flashcardSetId);
        return convertToSystemFlashcardSetResponseDTO(flashcardSet);
    }

    @Override
    public SystemFlashcardSetResponseDTO updateSystemFlashcardSetVisibility(
            Integer userId, Integer flashcardSetId, Boolean isPublic) {
        SystemFlashcardSet flashcardSet = getActiveFlashcardSet(flashcardSetId);

        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to update visibility of this flashcard set.");
        }
        flashcardSet.setPublic(isPublic);
        flashcardSet = systemFlashcardSetRepository.save(flashcardSet);

        List<LessonResource> lessonResources = lessonResourceRepository.findBySystemFlashCardSet_SystemSetId(flashcardSetId);
        for (LessonResource lessonResource : lessonResources) {
            lessonResource.setActive(isPublic);
            lessonResourceRepository.save(lessonResource);
        }

        return convertToSystemFlashcardSetResponseDTO(flashcardSet);
    }

    @Override
    public SystemFlashcardSetResponseDTO getAllFlashcardsInSet(Integer userId, Integer systemFlashcardSetId) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findBySystemSetId(systemFlashcardSetId);
        if (flashcardSet == null) {
            throw new ResourceNotFoundException("FlashcardSet not found with Id: " + systemFlashcardSetId);
        }

        if (flashcardSet.isDeleted()) {
            throw new ResourceNotFoundException("FlashcardSet đã bị xóa và không thể truy cập.");
        }



        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("cardOrder").ascending());
        Page<Flashcard> flashcardPage = flashcardRepository.findBySystemFlashCardSetSystemSetId(systemFlashcardSetId, pageable);

        flashcardSet.setTotalCards((int) flashcardPage.getTotalElements());
        systemFlashcardSetRepository.save(flashcardSet);

        SystemFlashcardSetResponseDTO responseDTO = new SystemFlashcardSetResponseDTO();
        responseDTO.setSystemSetId(flashcardSet.getSystemSetId());
        responseDTO.setContentManagerId(flashcardSet.getCreator().getUserId());
        responseDTO.setTitle(flashcardSet.getTitle());
        responseDTO.setDescription(flashcardSet.getDescription());
        responseDTO.setIsPublic(flashcardSet.isPublic());
        responseDTO.setIsPremium(flashcardSet.isPremium());
        responseDTO.setDifficultyLevel(flashcardSet.getDifficultyLevel() != null ? flashcardSet.getDifficultyLevel().getTag() : null);
        responseDTO.setFlashcards(flashcardPage.getContent().stream()
                .map(this::convertToFlashcardResponseDTO)
                .collect(Collectors.toList()));

        return responseDTO;
    }

    @Override
    public List<SystemFlashcardSetResponseDTO> systemFlashcardList(Integer userId) {
        List<SystemFlashcardSet> flashcardSets = systemFlashcardSetRepository.findByCreatorUserId(userId);
        return flashcardSets.stream()
                .filter(set -> !set.isDeleted())
                .map(this::convertToSystemFlashcardSetResponseDTO)
                .collect(Collectors.toList());
    }

    private SystemFlashcardSet getActiveFlashcardSet(Integer flashcardSetId) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashcardSet not found with Id: " + flashcardSetId));
        if (flashcardSet.isDeleted()) {
            throw new ResourceNotFoundException("lashcardSet đã bị xóa và không thể truy cập.");
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Flashcard> flashcardPage = flashcardRepository.findBySystemFlashCardSetSystemSetId(flashcardSetId, pageable);
        flashcardSet.setTotalCards((int) flashcardPage.getTotalElements());
        systemFlashcardSetRepository.save(flashcardSet);

        return flashcardSet;
    }

    private SystemFlashcardSetResponseDTO convertToSystemFlashcardSetResponseDTO(SystemFlashcardSet flashcardSet) {
        SystemFlashcardSetResponseDTO response = new SystemFlashcardSetResponseDTO();
        response.setSystemSetId(flashcardSet.getSystemSetId());
        response.setContentManagerId(flashcardSet.getCreator().getUserId());
        response.setTitle(flashcardSet.getTitle());
        response.setDescription(flashcardSet.getDescription());
        response.setIsPublic(flashcardSet.isPublic());
        response.setIsPremium(flashcardSet.isPremium());
        response.setDifficultyLevel(flashcardSet.getDifficultyLevel() != null
                ? DifficultyLevelEnum.valueOf(String.valueOf(flashcardSet.getDifficultyLevel().getTag()))
                : null);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("cardOrder").ascending());
        Page<Flashcard> flashcardPage = flashcardRepository.findBySystemFlashCardSetSystemSetId(
                flashcardSet.getSystemSetId(), pageable);
        response.setFlashcards(flashcardPage.getContent().stream()
                .map(this::convertToFlashcardResponseDTO)
                .collect(Collectors.toList()));

        return response;
    }

    private FlashcardResponseDTO convertToFlashcardResponseDTO(Flashcard flashcard) {
        FlashcardResponseDTO response = new FlashcardResponseDTO();
        response.setFlashcardId(flashcard.getFlashCardId());
        response.setJapaneseDefinition(flashcard.getDefinition());
        response.setVietEngTranslation(flashcard.getTranslation());
        response.setCardOrder(flashcard.getCardOrder());
        response.setImageUrl(flashcard.getImgUrl());
        return response;
    }
}
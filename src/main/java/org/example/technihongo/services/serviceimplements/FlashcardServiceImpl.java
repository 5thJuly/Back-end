package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.FlashcardRequestDTO;
import org.example.technihongo.dto.FlashcardResponseDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.entities.Flashcard;
import org.example.technihongo.entities.StudentFlashcardSet;
import org.example.technihongo.entities.SystemFlashcardSet;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.repositories.FlashcardRepository;
import org.example.technihongo.repositories.StudentFlashcardSetRepository;
import org.example.technihongo.repositories.SystemFlashcardSetRepository;
import org.example.technihongo.services.interfaces.FlashcardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {
    private final FlashcardRepository flashcardRepository;
    private final StudentFlashcardSetRepository studentFlashcardSetRepository;
    private final SystemFlashcardSetRepository systemFlashcardSetRepository;

    @Override
    public PageResponseDTO<FlashcardResponseDTO> getStudentFlashcards(Integer studentId, Integer flashcardSetId, int pageNo, int pageSize, String sortBy, String sortDir) {

        StudentFlashcardSet flashcardSet = studentFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard Set not found with id: " + flashcardSetId));

        if (!flashcardSet.getCreator().getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You don't have permission to view this flashcard set");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Flashcard> flashcardPage = flashcardRepository.findByStudentFlashCardSetStudentSetId(flashcardSetId, pageable);

        return getPageResponseDTO(flashcardPage);
    }

    @Override
    public PageResponseDTO<FlashcardResponseDTO> getSystemFlashcards(Integer userId, Integer flashcardSetId, int pageNo, int pageSize, String sortBy, String sortDir) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard Set not found with id: " + flashcardSetId));

        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to view this flashcard set");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Flashcard> flashcardPage = flashcardRepository.findBySystemFlashCardSetSystemSetId(flashcardSetId, pageable);

        return getPageResponseDTO(flashcardPage);
    }

    @Override
    public List<FlashcardResponseDTO> createStudentFlashcards(Integer studentId, Integer flashcardSetId, List<FlashcardRequestDTO> requests) {
        StudentFlashcardSet flashcardSet = studentFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard Set not found with id: " + flashcardSetId));

        if (!flashcardSet.getCreator().getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You don't have permission to add flashcards to this set");
        }

        Integer maxVocabOrder = flashcardRepository.findMaxVocabOrderByStudentFlashCardSet(flashcardSet);
        if (maxVocabOrder == null) {
            maxVocabOrder = 0;
        }

        List<Flashcard> flashcards = new ArrayList<>();
        int currentOrder = maxVocabOrder + 1;
        for (FlashcardRequestDTO request : requests) {
            Flashcard flashcard = new Flashcard();
            flashcard.setDefinition(request.getJapaneseDefinition());
            flashcard.setTranslation(request.getVietEngTranslation());
            flashcard.setImgUrl(request.getImageUrl());
            flashcard.setCardOrder(currentOrder);
            flashcard.setStudentFlashCardSet(flashcardSet);
            flashcards.add(flashcard);
            currentOrder++;
        }

        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(flashcards);
        return savedFlashcards.stream()
                .map(this::convertToFlashcardResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlashcardResponseDTO> createSystemFlashcards(Integer userId, Integer flashcardSetId, List<FlashcardRequestDTO> requests) {
        SystemFlashcardSet flashcardSet = systemFlashcardSetRepository.findById(flashcardSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard Set not found with id: " + flashcardSetId));
        if (!flashcardSet.getCreator().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to add flashcards to this set");
        }
        Integer maxVocabOrder = flashcardRepository.findMaxVocabOrderBySystemFlashCardSet(flashcardSet);
        if (maxVocabOrder == null) {
            maxVocabOrder = 0;
        }
        List<Flashcard> flashcards = new ArrayList<>();
        int currentOrder = maxVocabOrder + 1;
        for (FlashcardRequestDTO request : requests) {
            Flashcard flashcard = new Flashcard();
            flashcard.setDefinition(request.getJapaneseDefinition());
            flashcard.setTranslation(request.getVietEngTranslation());
            flashcard.setImgUrl(request.getImageUrl());
            flashcard.setCardOrder(currentOrder);
            flashcard.setSystemFlashCardSet(flashcardSet);
            flashcards.add(flashcard);
            currentOrder++;
        }

        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(flashcards);
        return savedFlashcards.stream()
                .map(this::convertToFlashcardResponseDTO)
                .collect(Collectors.toList());

    }

    @Override
    public FlashcardResponseDTO updateFlashcard(Integer userId, Integer studentId , Integer flashcardId, FlashcardRequestDTO request) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with Id: " + flashcardId));

        boolean hasPermission = false;
        if (flashcard.getStudentFlashCardSet() != null) {
            hasPermission = flashcard.getStudentFlashCardSet().getCreator().getStudentId().equals(studentId);
        } else if (flashcard.getSystemFlashCardSet() != null) {
            hasPermission = flashcard.getSystemFlashCardSet().getCreator().getUserId().equals(userId);
        }
        if (!hasPermission) {
            throw new RuntimeException("You don't have permission to update this flashcard");
        }

        flashcard.setDefinition(request.getJapaneseDefinition());
        flashcard.setTranslation(request.getVietEngTranslation());
        flashcard.setImgUrl(request.getImageUrl());
        flashcard.setCardOrder(request.getVocabOrder());
        flashcard = flashcardRepository.save(flashcard);
        return convertToFlashcardResponseDTO(flashcard);
    }

    @Override
    public void deleteFlashcard(Integer userId, Integer studentId , Integer flashcardId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + flashcardId));

        boolean hasPermission = false;
        if (flashcard.getStudentFlashCardSet() != null) {
            hasPermission = flashcard.getStudentFlashCardSet().getCreator().getStudentId().equals(studentId);
        } else if (flashcard.getSystemFlashCardSet() != null) {
            hasPermission = flashcard.getSystemFlashCardSet().getCreator().getUserId().equals(userId);
        }

        if (!hasPermission) {
            throw new RuntimeException("You don't have permission to delete this flashcard");
        }

        try {
            flashcardRepository.deleteByFlashcardIdNative(flashcardId);
            System.out.println("Deleted flashcard with ID: " + flashcardId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete flashcard: " + e.getMessage());
        }
    }


    private PageResponseDTO<FlashcardResponseDTO> getPageResponseDTO(Page<Flashcard> flashcardPage) {
        List<FlashcardResponseDTO> content = flashcardPage.getContent().stream()
                .map(this::convertToFlashcardResponseDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<FlashcardResponseDTO>builder()
                .content(content)
                .pageNo(flashcardPage.getNumber())
                .pageSize(flashcardPage.getSize())
                .totalElements(flashcardPage.getTotalElements())
                .totalPages(flashcardPage.getTotalPages())
                .last(flashcardPage.isLast())
                .build();
    }

    @Override
    public FlashcardResponseDTO getFlashcardById(Integer flashcardId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + flashcardId));
        return convertToFlashcardResponseDTO(flashcard);
    }
    private FlashcardResponseDTO convertToFlashcardResponseDTO(Flashcard flashcard) {
        FlashcardResponseDTO response = new FlashcardResponseDTO();
        response.setFlashcardId(flashcard.getFlashCardId());
        response.setJapaneseDefinition(flashcard.getDefinition());
        response.setVietEngTranslation(flashcard.getTranslation());
        response.setImageUrl(flashcard.getImgUrl());
        response.setCardOrder(flashcard.getCardOrder());
        return response;
    }
}

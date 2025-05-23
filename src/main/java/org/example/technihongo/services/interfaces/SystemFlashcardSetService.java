package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.SystemFlashcardSetRequestDTO;
import org.example.technihongo.dto.SystemFlashcardSetResponseDTO;
import org.example.technihongo.dto.UpdateFlashcardOrderDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SystemFlashcardSetService {
    SystemFlashcardSetResponseDTO create(Integer userId, SystemFlashcardSetRequestDTO requestDTO);

    SystemFlashcardSetResponseDTO update(Integer userId, Integer flashcardSetId, SystemFlashcardSetRequestDTO requestDTO);

    void updateFlashcardOrder(Integer userId,Integer flashcardSetId, UpdateFlashcardOrderDTO updateFlashcardOrderDTO);

    void deleteSystemFlashcardSet(Integer userId, Integer flashcardSetId);

    SystemFlashcardSetResponseDTO getSystemFlashcardSetById(Integer flashcardSetId);
    SystemFlashcardSetResponseDTO updateSystemFlashcardSetVisibility(Integer userId,Integer flashcardSetId,Boolean isPublic);
    SystemFlashcardSetResponseDTO getAllFlashcardsInSet(Integer userId ,Integer systemFlashcardSetId);

    List<SystemFlashcardSetResponseDTO> systemFlashcardList(Integer userId);



}

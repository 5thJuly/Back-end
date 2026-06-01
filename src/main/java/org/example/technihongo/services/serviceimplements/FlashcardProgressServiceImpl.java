package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.FlashcardProgressDTO;
import org.example.technihongo.entities.Flashcard;
import org.example.technihongo.entities.StudentFlashcardProgress;
import org.example.technihongo.repositories.FlashcardRepository;
import org.example.technihongo.repositories.StudentFlashcardProgressRepository;
import org.example.technihongo.repositories.StudentRepository;
import org.example.technihongo.services.interfaces.FlashcardProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FlashcardProgressServiceImpl implements FlashcardProgressService {

    private final StudentFlashcardProgressRepository studentFlashcardProgressRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<FlashcardProgressDTO> getStarredFlashcards(Integer studentId, Integer setId, boolean isSystemSet) {
        List<StudentFlashcardProgress> progresses = isSystemSet
                ? studentFlashcardProgressRepository.findStarredByStudentAndSystemSet(studentId, setId, true)
                : studentFlashcardProgressRepository.findStarredByStudentAndStudentSet(studentId, setId, true);

        return progresses.stream().map(progress -> {
            Flashcard flashcard = progress.getFlashcard();
            return FlashcardProgressDTO.builder()
                    .flashcardId(flashcard.getFlashCardId())
                    .japaneseDefinition(flashcard.getDefinition())
                    .vietEngTranslation(flashcard.getTranslation())
                    .isLearned(progress.isLearned())
                    .lastStudied(progress.getLastStudied())
                    .setType(isSystemSet ? "SYSTEM" : "STUDENT")
                    .starred(progress.getStarred())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void updateFlashcardProgress(Integer studentId, Integer flashcardId, Boolean starred) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found");
        }
        StudentFlashcardProgress progress = studentFlashcardProgressRepository
                .findByStudentStudentIdAndFlashcardFlashCardId(studentId, flashcardId)
                .orElseGet(() -> StudentFlashcardProgress.builder()
                        .student(studentRepository.findByUser_UserId(studentId))
                        .flashcard(flashcardRepository.findById(flashcardId).orElseThrow(() -> new RuntimeException("Flashcard not found")))
                        .createdAt(LocalDateTime.now())
                        .build());
        if(starred != null) {
            progress.setStarred(starred);
        }
        progress.setLearned(true);
        progress.setLastStudied(LocalDateTime.now());
        studentFlashcardProgressRepository.save(progress);
    }
}

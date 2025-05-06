package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.dto.FlashcardSetProgressDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.AchievementService;
import org.example.technihongo.services.interfaces.StudentFlashcardSetProgressService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentFlashcardSetProgressServiceImpl implements StudentFlashcardSetProgressService {
    @Autowired
    private StudentFlashcardSetProgressRepository setProgressRepo;
    @Autowired
    private StudentFlashcardSetRepository studentSetRepo;
    @Autowired
    private StudentFlashcardProgressRepository flashcardProgressRepo;
    @Autowired
    private SystemFlashcardSetRepository systemSetRepo;
    @Autowired
    private StudentRepository studentRepo;
    @Autowired
    private FlashcardRepository flashcardRepo;
    @Autowired
    private StudentDailyLearningLogRepository dailyLogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserActivityLogService userActivityLogService;
    @Autowired
    private AchievementService achievementService;

    @Override
    public List<FlashcardSetProgressDTO> getAllStudentAndSystemSetProgress(Integer studentId) {
        List<StudentFlashcardSetProgress> progresses = setProgressRepo.findByStudentStudentId(studentId);
        return progresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FlashcardSetProgressDTO getStudentOrSystemSetProgress(Integer studentId, Integer setId, boolean isSystemSet) {
        if (!isSystemSet) {
            StudentFlashcardSetProgress progress = setProgressRepo
                    .findByStudentStudentIdAndStudentFlashcardSet_StudentSetId(studentId, setId)
                    .orElseThrow(() -> new RuntimeException("Student set progress not found for student ID " + studentId + " and set ID " + setId));
            return mapToDTO(progress);
        } else {
            StudentFlashcardSetProgress progress = setProgressRepo
                    .findByStudentStudentIdAndSystemFlashcardSet_SystemSetId(studentId, setId)
                    .orElseThrow(() -> new RuntimeException("System set progress not found for student ID " + studentId + " and set ID " + setId));
            return mapToDTO(progress);
        }
    }

    @Override
    public void trackFlashcardSetProgress(Integer studentId, Integer setId, boolean isSystemSet, Integer currentFlashcardId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID " + studentId));

        Optional<StudentFlashcardSetProgress> existingProgressOpt = isSystemSet
                ? setProgressRepo.findByStudentStudentIdAndSystemFlashcardSet_SystemSetId(studentId, setId)
                : setProgressRepo.findByStudentStudentIdAndStudentFlashcardSet_StudentSetId(studentId, setId);

        StudentFlashcardSetProgress progress;
        boolean isNewProgress = false;
        if (existingProgressOpt.isEmpty()) {
            progress = new StudentFlashcardSetProgress();
            progress.setStudent(student);
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            progress.setCardStudied(0);
            progress.setStudyCount(1);
            progress.setLastStudied(LocalDateTime.now());
            isNewProgress = true;

            if (isSystemSet) {
                SystemFlashcardSet systemSet = systemSetRepo.findById(setId)
                        .orElseThrow(() -> new RuntimeException("SystemFlashcardSet not found with ID " + setId));
                progress.setSystemFlashcardSet(systemSet);
            } else {
                StudentFlashcardSet studentSet = studentSetRepo.findById(setId)
                        .orElseThrow(() -> new RuntimeException("StudentFlashcardSet not found with ID " + setId));
                progress.setStudentFlashcardSet(studentSet);
            }

            if (currentFlashcardId != null) {
                Flashcard currentCard = flashcardRepo.findById(currentFlashcardId)
                        .orElseThrow(() -> new RuntimeException("Flashcard not found with ID " + currentFlashcardId));
                if (isSystemSet && currentCard.getSystemFlashCardSet() != null && currentCard.getSystemFlashCardSet().getSystemSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                } else if (!isSystemSet && currentCard.getStudentFlashCardSet() != null && currentCard.getStudentFlashCardSet().getStudentSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                } else {
                    progress.setCurrentFlashCardId(null);
                }
            } else {
                List<Flashcard> firstCardOpt = isSystemSet
                        ? flashcardRepo.findTopBySystemFlashCardSet_SystemSetIdOrderByCardOrderAsc(setId)
                        : flashcardRepo.findTopByStudentFlashCardSet_StudentSetIdOrderByCardOrderAsc(setId);
                if (firstCardOpt.isEmpty()) {
                    progress.setCurrentFlashCardId(firstCardOpt.get(0));
                } else {
                    progress.setCurrentFlashCardId(null);
                }
            }
        } else {
            progress = existingProgressOpt.get();
        }

        long cardStudied = isSystemSet
                ? flashcardProgressRepo.findByStudentStudentIdAndFlashcard_SystemFlashCardSet_SystemSetId(studentId, setId)
                .stream().filter(StudentFlashcardProgress::isLearned).count()
                : flashcardProgressRepo.findByStudentStudentIdAndFlashcard_StudentFlashCardSet_StudentSetId(studentId, setId)
                .stream().filter(StudentFlashcardProgress::isLearned).count();

        boolean wasNotStudiedBefore = progress.getCardStudied() == 0 && cardStudied > 0;
        progress.setCardStudied((int) cardStudied);
        progress.setLastStudied(LocalDateTime.now());
        if (!isNewProgress) {
            progress.setStudyCount(progress.getStudyCount() + 1);
        }

        int totalCards = isSystemSet
                ? progress.getSystemFlashcardSet().getTotalCards()
                : progress.getStudentFlashcardSet().getTotalCards();

        if (cardStudied >= totalCards && progress.getCompletionStatus() != CompletionStatus.COMPLETED) {
            progress.setCompletionStatus(CompletionStatus.COMPLETED);
            Optional<StudentDailyLearningLog> dailyLogOpt = dailyLogRepository.findByStudentStudentIdAndLogDate(studentId, LocalDate.now());
            if (dailyLogOpt.isPresent()) {
                StudentDailyLearningLog dailyLog = dailyLogOpt.get();
                dailyLog.setCompletedFlashcardSets(dailyLog.getCompletedFlashcardSets() + 1);
                dailyLogRepository.save(dailyLog);
            }

            if (isSystemSet) {
                userActivityLogService.trackUserActivityLog(
                        userRepository.findByStudentStudentId(studentId).getUserId(),
                        ActivityType.COMPLETE,
                        ContentType.SystemFlashcardSet,
                        setId,
                        null,
                        null);
            } else {
                userActivityLogService.trackUserActivityLog(
                        userRepository.findByStudentStudentId(studentId).getUserId(),
                        ActivityType.COMPLETE,
                        ContentType.StudentFlashcardSet,
                        setId,
                        null,
                        null);
                achievementService.checkAndAssignFlashcardAchievements(studentId);
            }
        }

        if (currentFlashcardId != null) {
            Flashcard currentCard = flashcardRepo.findById(currentFlashcardId)
                    .orElseThrow(() -> new RuntimeException("Flashcard not found with ID " + currentFlashcardId));
            if (isSystemSet && currentCard.getSystemFlashCardSet() != null && currentCard.getSystemFlashCardSet().getSystemSetId().equals(setId)) {
                progress.setCurrentFlashCardId(currentCard);
            } else if (!isSystemSet && currentCard.getStudentFlashCardSet() != null && currentCard.getStudentFlashCardSet().getStudentSetId().equals(setId)) {
                progress.setCurrentFlashCardId(currentCard);
            }
        } else if (progress.getCurrentFlashCardId() == null) {
            List<Flashcard> firstCardOpt = isSystemSet
                    ? flashcardRepo.findTopBySystemFlashCardSet_SystemSetIdOrderByCardOrderAsc(setId)
                    : flashcardRepo.findTopByStudentFlashCardSet_StudentSetIdOrderByCardOrderAsc(setId);
            if (!firstCardOpt.isEmpty()) {
                progress.setCurrentFlashCardId(firstCardOpt.get(0));
            }
        }

        setProgressRepo.save(progress);

        if (!isSystemSet && wasNotStudiedBefore) {
            achievementService.checkAndAssignFirstFlashcardAchievement(studentId, setId);
        }
    }

    @Override
    public void completeFlashcardSetProgress(Integer studentId, Integer setId, boolean isSystemSet, Integer currentFlashcardId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID " + studentId));

        Optional<StudentFlashcardSetProgress> existingProgressOpt = isSystemSet
                ? setProgressRepo.findByStudentStudentIdAndSystemFlashcardSet_SystemSetId(studentId, setId)
                : setProgressRepo.findByStudentStudentIdAndStudentFlashcardSet_StudentSetId(studentId, setId);

        StudentFlashcardSetProgress progress;
        if (existingProgressOpt.isEmpty()) {
            progress = new StudentFlashcardSetProgress();
            progress.setStudent(student);
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            progress.setCardStudied(0);
            progress.setStudyCount(1);
            progress.setLastStudied(LocalDateTime.now());

            if (isSystemSet) {
                SystemFlashcardSet systemSet = systemSetRepo.findById(setId)
                        .orElseThrow(() -> new RuntimeException("SystemFlashcardSet not found with ID " + setId));
                progress.setSystemFlashcardSet(systemSet);
            } else {
                StudentFlashcardSet studentSet = studentSetRepo.findById(setId)
                        .orElseThrow(() -> new RuntimeException("StudentFlashcardSet not found with ID " + setId));
                progress.setStudentFlashcardSet(studentSet);
            }

            if (currentFlashcardId != null) {
                Flashcard currentCard = flashcardRepo.findById(currentFlashcardId)
                        .orElseThrow(() -> new RuntimeException("Flashcard not found with ID " + currentFlashcardId));
                if (isSystemSet && currentCard.getSystemFlashCardSet() != null && currentCard.getSystemFlashCardSet().getSystemSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                } else if (!isSystemSet && currentCard.getStudentFlashCardSet() != null && currentCard.getStudentFlashCardSet().getStudentSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                } else {
                    progress.setCurrentFlashCardId(null);
                }
            } else {
                List<Flashcard> firstCardOpt = isSystemSet
                        ? flashcardRepo.findTopBySystemFlashCardSet_SystemSetIdOrderByCardOrderAsc(setId)
                        : flashcardRepo.findTopByStudentFlashCardSet_StudentSetIdOrderByCardOrderAsc(setId);
                if (firstCardOpt.isEmpty()) {
                    progress.setCurrentFlashCardId(firstCardOpt.get(0));
                } else {
                    progress.setCurrentFlashCardId(null);
                }
            }
        } else {
            progress = existingProgressOpt.get();

            progress.setLastStudied(LocalDateTime.now());
            progress.setStudyCount(progress.getStudyCount() + 1);

            if (progress.getCompletionStatus() != CompletionStatus.COMPLETED) {
                progress.setCompletionStatus(CompletionStatus.COMPLETED);
                StudentDailyLearningLog dailyLog = dailyLogRepository.findByStudentStudentIdAndLogDate(studentId, LocalDate.now()).get();
                dailyLog.setCompletedFlashcardSets(dailyLog.getCompletedFlashcardSets() + 1);
                dailyLogRepository.save(dailyLog);

                if(isSystemSet) {
                    userActivityLogService.trackUserActivityLog(userRepository.findByStudentStudentId(studentId).getUserId(), ActivityType.COMPLETE, ContentType.SystemFlashcardSet, setId, null, null);
                }
                else {
                    userActivityLogService.trackUserActivityLog(userRepository.findByStudentStudentId(studentId).getUserId(), ActivityType.COMPLETE, ContentType.StudentFlashcardSet, setId, null, null);
                }
            }

            if (currentFlashcardId != null) {
                Flashcard currentCard = flashcardRepo.findById(currentFlashcardId)
                        .orElseThrow(() -> new RuntimeException("Flashcard not found with ID " + currentFlashcardId));
                if (isSystemSet && currentCard.getSystemFlashCardSet() != null && currentCard.getSystemFlashCardSet().getSystemSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                } else if (!isSystemSet && currentCard.getStudentFlashCardSet() != null && currentCard.getStudentFlashCardSet().getStudentSetId().equals(setId)) {
                    progress.setCurrentFlashCardId(currentCard);
                }
            } else if (progress.getCurrentFlashCardId() == null) {
                List<Flashcard> firstCardOpt = isSystemSet
                        ? flashcardRepo.findTopBySystemFlashCardSet_SystemSetIdOrderByCardOrderAsc(setId)
                        : flashcardRepo.findTopByStudentFlashCardSet_StudentSetIdOrderByCardOrderAsc(setId);
                if (!firstCardOpt.isEmpty()) {
                    progress.setCurrentFlashCardId(firstCardOpt.get(0));
                }
            }
        }

        setProgressRepo.save(progress);
    }

    public void markFlashcardAsLearned(Integer studentId, Integer flashcardId) {
        StudentFlashcardProgress progress = flashcardProgressRepo
                .findByStudentStudentIdAndFlashcardFlashCardId(studentId, flashcardId)
                .orElseGet(() -> StudentFlashcardProgress.builder()
                        .student(Student.builder().studentId(studentId).build())
                        .flashcard(Flashcard.builder().flashCardId(flashcardId).build())
                        .build());

        progress.setLearned(true);
        progress.setLastStudied(LocalDateTime.now());
        flashcardProgressRepo.save(progress);
    }

    private FlashcardSetProgressDTO mapToDTO(StudentFlashcardSetProgress progress) {
        if (progress.getStudentFlashcardSet() != null) {
            StudentFlashcardSet set = progress.getStudentFlashcardSet();
            long cardStudied = flashcardProgressRepo
                    .findByStudentStudentIdAndFlashcard_StudentFlashCardSet_StudentSetId(
                            progress.getStudent().getStudentId(), set.getStudentSetId())
                    .stream()
                    .filter(StudentFlashcardProgress::isLearned)
                    .count();

            return FlashcardSetProgressDTO.builder()
                    .progressId(progress.getProgressId())
                    .setId(set.getStudentSetId())
                    .title(set.getTitle())
                    .totalCards(set.getTotalCards())
                    .cardStudied((int) cardStudied)
                    .lastStudied(progress.getLastStudied())
                    .studyCount(progress.getStudyCount())
                    .completionStatus(CompletionStatus.valueOf(progress.getCompletionStatus().toString()))
                    .isSystemSet(false)
                    .build();
        } else if (progress.getSystemFlashcardSet() != null) {
            SystemFlashcardSet set = progress.getSystemFlashcardSet();
            long cardStudied = flashcardProgressRepo
                    .findByStudentStudentIdAndFlashcard_SystemFlashCardSet_SystemSetId(
                            progress.getStudent().getStudentId(), set.getSystemSetId())
                    .stream()
                    .filter(StudentFlashcardProgress::isLearned)
                    .count();

            return FlashcardSetProgressDTO.builder()
                    .progressId(progress.getProgressId())
                    .setId(set.getSystemSetId())
                    .title(set.getTitle())
                    .totalCards(set.getTotalCards())
                    .cardStudied((int) cardStudied)
                    .lastStudied(progress.getLastStudied())
                    .studyCount(progress.getStudyCount())
                    .completionStatus(CompletionStatus.valueOf(progress.getCompletionStatus().toString()))
                    .isSystemSet(true)
                    .build();
        } else {
            throw new RuntimeException("Progress is not associated with any FlashcardSet");
        }
    }
}

package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.enums.QuestionType;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudentQuizAttemptService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentQuizAttemptServiceImpl implements StudentQuizAttemptService {
    private final QuizRepository quizRepository;
    private final StudentQuizAttemptRepository studentQuizAttemptRepository;                     
    private final QuizAnswerResponseRepository quizAnswerResponseRepository;
    private final QuestionAnswerOptionRepository questionAnswerOptionRepository;
    private final StudentDailyLearningLogRepository dailyLogRepository;
    private final StudentLearningStatisticsRepository statisticsRepository;
    private final UserRepository userRepository;
    private final UserActivityLogService userActivityLogService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final StudentRepository studentRepository;
    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionServiceImpl.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long WAIT_TIME_MINUTES = 30;
    private static final long MAX_QUIZ_DURATION_MINUTES = 120;

    @Override
    public StartQuizResponseDTO startQuiz(Integer studentId, Integer quizId) {
        validateInput(studentId, quizId);
        Quiz quiz = getValidQuiz(quizId);

        // Kiểm tra trạng thái chu kỳ
        AttemptStatusDTO attemptStatus = getAttemptStatus(studentId, quizId);
        if (attemptStatus.getRemainingAttempts() <= 0 && attemptStatus.getRemainingWaitTime() > 0) {
            throw new IllegalStateException("Bạn đã dùng hết số lần thử. Vui lòng chờ " +
                    attemptStatus.getRemainingWaitTime() + " phút nữa.");
        }

        // Kiểm tra các lần thử đang tiến hành
        Optional<StudentQuizAttempt> inProgressAttempt = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizIdAndIsCompletedFalse(studentId, quizId);

        if (inProgressAttempt.isPresent()) {
            StudentQuizAttempt attempt = inProgressAttempt.get();
            if (isAttemptExpired(attempt)) {
                attempt.setIsCompleted(true);
                attempt.setIsPassed(false);
                studentQuizAttemptRepository.save(attempt);
            } else {
                return buildStartQuizResponse(attempt, quiz, true);
            }
        }

        // Chỉ cho phép tạo attemptNumber=0 nếu chu kỳ mới hoặc sau cool down
        List<StudentQuizAttempt> completedAttempts = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizIdAndIsCompletedTrueAndAttemptNumberGreaterThan(studentId, quizId, 0);
        if (!completedAttempts.isEmpty() && completedAttempts.size() < MAX_ATTEMPTS && attemptStatus.getRemainingWaitTime() == 0) {
            throw new IllegalStateException("Bạn vẫn còn " + attemptStatus.getRemainingAttempts() +
                    " lần thử hiện tại.");
        }

        // Tạo lần thử khởi tạo (attemptNumber=0)
        StudentQuizAttempt newAttempt = StudentQuizAttempt.builder()
                .quiz(quiz)
                .student(Student.builder().studentId(studentId).build())
                .score(BigDecimal.ZERO)
                .isPassed(false)
                .isCompleted(false)
                .timeTaken(LocalTime.of(0, 0, 0))
                .attemptNumber(0)
                .dateTaken(LocalDateTime.now())
                .build();
        newAttempt = studentQuizAttemptRepository.save(newAttempt);

        return buildStartQuizResponse(newAttempt, quiz, false);
    }

    @Override
    public QuizAttemptResponseDTO attemptQuiz(Integer studentId, QuizAttemptRequestDTO request) {
        validateInput(studentId, request.getQuizId());
        Quiz quiz = getValidQuiz(request.getQuizId());

        // Kiểm tra trạng thái để ngăn attemptQuiz nếu đã hết lượt và đang trong thời gian cool down
        AttemptStatusDTO attemptStatus = getAttemptStatus(studentId, request.getQuizId());
        if (attemptStatus.getRemainingAttempts() <= 0 && attemptStatus.getRemainingWaitTime() > 0) {
            throw new IllegalStateException("Bạn đã dùng hết số lần thử. Vui lòng chờ " +
                    attemptStatus.getRemainingWaitTime() + " phút nữa.");
        }

        // Kiểm tra lần thử khởi tạo
        StudentQuizAttempt startAttempt = studentQuizAttemptRepository.findByAttemptId(request.getAttemptId())
                .orElseThrow(() -> new IllegalArgumentException("No active quiz attempt found. Please start the quiz first."));

        validateAttemptOwnership(studentId, startAttempt);
        if (isAttemptExpired(startAttempt)) {
            startAttempt.setIsCompleted(true);
            startAttempt.setIsPassed(false);
            studentQuizAttemptRepository.save(startAttempt);
            throw new IllegalArgumentException("This quiz attempt has expired. Please start a new attempt.");
        }

        // Xác định attemptNumber
        List<StudentQuizAttempt> completedAttempts = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizIdAndIsCompletedTrueAndAttemptNumberGreaterThan(studentId, request.getQuizId(), 0);

        int attemptNumber = completedAttempts.size() + 1;
        if (shouldResetAttemptCounter(completedAttempts)) {
            attemptNumber = 1;
        }

        // Kiểm tra số lần thử tối đa
        if (attemptNumber > MAX_ATTEMPTS && attemptStatus.getRemainingWaitTime() > 0) {
            throw new IllegalStateException("Bạn đã đạt số lần thử tối đa (" + MAX_ATTEMPTS + "). Vui lòng chờ " +
                    attemptStatus.getRemainingWaitTime() + " phút nữa.");
        }

        // Tạo lần thử mới
        StudentQuizAttempt newAttempt = createNewAttempt(studentId, quiz, attemptNumber);
        newAttempt = studentQuizAttemptRepository.save(newAttempt);

        // Kiểm tra xem đã pass trước đó chưa để thêm thông báo
        boolean hasPassedBefore = studentQuizAttemptRepository
                .existsByStudentStudentIdAndQuizQuizIdAndIsPassedTrueAndIsCompletedTrue(studentId, request.getQuizId());

        // Xử lý câu trả lời và tính điểm
        QuizAttemptResponseDTO response = processQuizAttempt(newAttempt, quiz, request);

        // Cập nhật remainingAttempts
        int newRemainingAttempts = Math.max(0, attemptStatus.getRemainingAttempts() - 1);
        response.setRemainingAttempts(newRemainingAttempts);
        response.setRemainingWaitTime(attemptStatus.getRemainingWaitTime());

        // Thêm thông báo nếu đã pass trước đó
        if (hasPassedBefore) {
            response.setMessage("Mặc dù bạn đã pass, nhưng hãy cố hoàn thành bài học nhé!");
        } else {
            response.setMessage("Quiz attempted successfully");
        }

        return response;
    }


    // lấy ra các lần làm quiz (nỗ lực làm bài kiểm tra) của một học sinh
    //Lần làm bài có điểm cao nhất (top attempt)
    //3 lần làm bài gần đây nhất (recent attempts)
    @Override
    public List<StudentQuizAttempt> getTopAndRecentQuizAttempts(Integer studentId, Integer quizId) {
        validateInput(studentId, quizId);
        getValidQuiz(quizId);

        List<StudentQuizAttempt> allAttempts = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizId(studentId, quizId);

        if (allAttempts.isEmpty()) {
            throw new RuntimeException("No quiz attempts found for student ID: " + studentId + " and quiz ID: " + quizId);
        }

        StudentQuizAttempt topAttempt = allAttempts.stream()
                .max(Comparator.comparing(StudentQuizAttempt::getScore))
                .orElse(null);

        List<StudentQuizAttempt> recentAttempts = allAttempts.stream()
                .sorted(Comparator.comparing(StudentQuizAttempt::getDateTaken).reversed())
                .limit(3)
                .filter(attempt -> !attempt.equals(topAttempt))
                .toList();

        List<StudentQuizAttempt> selectedAttempts = new ArrayList<>();
        selectedAttempts.add(topAttempt);
        selectedAttempts.addAll(recentAttempts);
        selectedAttempts = selectedAttempts.stream().distinct().limit(4).collect(Collectors.toList());
        return selectedAttempts;
    }

    @Override
    public ReviewQuizAttemptDTO reviewQuizAttempt(Integer studentId, Integer attemptId) {
        if (studentId == null || attemptId == null) {
            throw new RuntimeException("Student ID và Attempt ID không thể null");
        }

        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Student với ID: " + studentId));

        StudentQuizAttempt attempt = studentQuizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Không tim thấy Quiz attempt với ID: " + attemptId));

        if (!attempt.getStudent().getStudentId().equals(studentId)) {
            throw new RuntimeException("Lần quiz attempt này không phải của bạn.");
        }

        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByQuiz_QuizId(attempt.getQuiz().getQuizId());
        int totalQuestions = quizQuestions.size();

        List<QuizAnswerResponse> responses = quizAnswerResponseRepository.findByStudentQuizAttempt_AttemptId(attemptId);

        log.info("Quiz ID: {}, Questions: {}, Responses: {}",
                attempt.getQuiz().getQuizId(),
                quizQuestions.stream().map(q -> q.getQuestion().getQuestionId()).collect(Collectors.toList()),
                responses.stream().map(r -> r.getSelectedOption().getOptionId()).collect(Collectors.toList()));

        Map<Integer, List<QuizAnswerResponse>> responseMap = responses.stream()
                .collect(Collectors.groupingBy(r -> r.getSelectedOption().getQuestion().getQuestionId()));

        List<AnswerReviewDTO> answerReviews = quizQuestions.stream()
                .map(quizQuestion -> {
                    Question question = quizQuestion.getQuestion();
                    List<QuizAnswerResponse> questionResponses = responseMap.getOrDefault(question.getQuestionId(), Collections.emptyList());

                    List<QuestionAnswerOptionDTO2> selectedOptionDTOs = questionResponses.stream()
                            .map(response -> QuestionAnswerOptionDTO2.builder()
                                    .optionId(response.getSelectedOption().getOptionId())
                                    .optionText(response.getSelectedOption().getOptionText())
                                    .isCorrect(response.isCorrect())
                                    .build())
                            .collect(Collectors.toList());

                    boolean isCorrect;
                    if (questionResponses.isEmpty()) {
                        isCorrect = false;
                    } else if (question.getQuestionType().equals(QuestionType.Single_choice)) {
                        isCorrect = questionResponses.size() == 1 && questionResponses.get(0).isCorrect();
                    } else {
                        List<QuestionAnswerOption> allOptions = questionAnswerOptionRepository.findByQuestion_QuestionId(question.getQuestionId());
                        Set<Integer> correctOptionIds = allOptions.stream()
                                .filter(QuestionAnswerOption::isCorrect)
                                .map(QuestionAnswerOption::getOptionId)
                                .collect(Collectors.toSet());
                        Set<Integer> selectedOptionIds = questionResponses.stream()
                                .map(r -> r.getSelectedOption().getOptionId())
                                .collect(Collectors.toSet());
                        isCorrect = correctOptionIds.equals(selectedOptionIds);
                    }

                    return AnswerReviewDTO.builder()
                            .questionId(question.getQuestionId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType().name())
                            .selectedOptions(selectedOptionDTOs)
                            .isCorrect(isCorrect)
                            .explanation(question.getExplanation())
                            .build();
                })
                .collect(Collectors.toList());

        int correctAnswers = (int) answerReviews.stream().filter(AnswerReviewDTO::getIsCorrect).count();
        int unansweredQuestions = (int) answerReviews.stream().filter(r -> r.getSelectedOptions().isEmpty()).count();
        int incorrectAnswers = totalQuestions - correctAnswers - unansweredQuestions;

        return ReviewQuizAttemptDTO.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(attempt.getQuiz().getQuizId())
                .quizTitle(attempt.getQuiz().getTitle())
                .score(attempt.getScore())
                .isPassed(attempt.getIsPassed())
                .timeTaken(attempt.getTimeTaken())
                .isCompleted(attempt.getIsCompleted())
                .attemptNumber(attempt.getAttemptNumber())
                .dateTaken(attempt.getDateTaken())
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .incorrectAnswers(incorrectAnswers)
                .unansweredQuestions(unansweredQuestions)
                .answers(answerReviews)
                .build();
    }

    public AttemptStatusDTO getAttemptStatus(Integer studentId, Integer quizId) {
        List<StudentQuizAttempt> attempts = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizIdOrderByDateTakenDesc(studentId, quizId);

        if (attempts.isEmpty()) {
            return new AttemptStatusDTO(0, 0, MAX_ATTEMPTS);
        }

        int completedAttempts = 0;
        LocalDateTime now = LocalDateTime.now();
        boolean withinCycle = false;

        // Đếm các lần thử hoàn thành trong chu kỳ hiện tại (trong 30 phút)
        for (StudentQuizAttempt attempt : attempts) {
            if (attempt.getIsCompleted() && attempt.getAttemptNumber() > 0) {
                long minutesSinceAttempt = Duration.between(attempt.getDateTaken(), now).toMinutes();
                if (minutesSinceAttempt <= WAIT_TIME_MINUTES) {
                    completedAttempts++;
                    withinCycle = true;
                } else {
                    break; // Thoát nếu gặp lần thử ngoài chu kỳ
                }
            }
        }

        int remainingAttempts = MAX_ATTEMPTS - completedAttempts;
        long remainingWaitTime = 0;

        // Nếu không còn lần thử nào trong chu kỳ hiện tại, reset remainingAttempts
        if (!withinCycle && completedAttempts == 0) {
            remainingAttempts = MAX_ATTEMPTS;
        } else if (remainingAttempts <= 0) {
            // Tính thời gian chờ nếu đã hết lần thử
            LocalDateTime lastAttemptTime = attempts.stream()
                    .filter(a -> a.getIsCompleted() && a.getAttemptNumber() > 0)
                    .map(StudentQuizAttempt::getDateTaken)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            long minutesPassed = Duration.between(lastAttemptTime, now).toMinutes();
            remainingWaitTime = Math.max(0, WAIT_TIME_MINUTES - minutesPassed);
        }

        return new AttemptStatusDTO(completedAttempts, remainingWaitTime, remainingAttempts);
    }

    @Override
    public QuizPerformanceReportDTO generatePerformanceReport(Integer studentId, Integer quizId) {
        validateInput(studentId, quizId);
        Quiz quiz = getValidQuiz(quizId);

        // Chỉ lấy các lần thử với attemptNumber > 0
        List<StudentQuizAttempt> attempts = studentQuizAttemptRepository
                .findByStudentStudentIdAndQuizQuizIdAndAttemptNumberGreaterThan(studentId, quizId, 0);

        List<AttemptSummaryDTO> summary = attempts.stream().map(attempt -> AttemptSummaryDTO.builder()
                .attemptNumber(attempt.getAttemptNumber())
                .score(attempt.getScore())
                .isPassed(attempt.getIsPassed())
                .dateTaken(attempt.getDateTaken())
                .isCompleted(attempt.getIsCompleted())
                .build()).collect(Collectors.toList());

        BigDecimal averageScore = attempts.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(attempts.stream().mapToDouble(a -> a.getScore().doubleValue()).average().orElse(0));
        int passedAttempts = (int) attempts.stream().filter(StudentQuizAttempt::getIsPassed).count();

        return QuizPerformanceReportDTO.builder()
                .quizId(quizId)
                .quizTitle(quiz.getTitle())
                .attempts(summary)
                .averageScore(averageScore)
                .totalAttempts(attempts.size())
                .passedAttempts(passedAttempts)
                .build();
    }


    private Quiz getValidQuiz(Integer quizId) {
        Quiz quiz = quizRepository.findByQuizId(quizId);
        if (quiz == null || quiz.isDeleted()) {
            throw new IllegalArgumentException("Quiz not found or has been deleted.");
        }
        return quiz;
    }

    private void validateInput(Integer studentId, Integer quizId) {
        if (studentId == null || quizId == null) {
            throw new IllegalArgumentException("Student ID and Quiz ID must not be null.");
        }
    }

    private void validateAttemptOwnership(Integer studentId, StudentQuizAttempt attempt) {
        if (!attempt.getStudent().getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("This quiz attempt does not belong to the current student.");
        }
    }

    private StudentQuizAttempt createNewAttempt(Integer studentId, Quiz quiz, int attemptNumber) {
        return StudentQuizAttempt.builder()
                .quiz(quiz)
                .student(Student.builder().studentId(studentId).build())
                .score(BigDecimal.ZERO)
                .isPassed(false)
                .isCompleted(false)
                .timeTaken(LocalTime.of(0, 0, 0))
                .attemptNumber(attemptNumber)
                .dateTaken(LocalDateTime.now())
                .build();
    }

    // Tách logic xây dựng StartQuizResponseDTO
    private StartQuizResponseDTO buildStartQuizResponse(StudentQuizAttempt attempt, Quiz quiz, boolean resuming) {
        return StartQuizResponseDTO.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .totalQuestions(quiz.getTotalQuestions())
                .attemptNumber(attempt.getAttemptNumber())
                .startTime(attempt.getDateTaken())
                .resuming(resuming)
                .remainingTimeInSeconds(resuming ? calculateRemainingTime(attempt) : MAX_QUIZ_DURATION_MINUTES * 60)
                .build();
    }

    // private void validateAttempt(Integer studentId, List<StudentQuizAttempt> attempts, int attemptNumber) {
    //     if (attemptNumber > MAX_ATTEMPTS) {
    //         StudentQuizAttempt lastAttempt = attempts.stream()
    //                 .max(Comparator.comparing(StudentQuizAttempt::getDateTaken))
    //                 .orElseThrow(() -> new IllegalStateException("No previous attempts found."));
    //         LocalDateTime lastAttemptTime = lastAttempt.getDateTaken();
    //         LocalDateTime now = LocalDateTime.now();
    //         long minutesSinceLastAttempt = lastAttemptTime.until(now, java.time.temporal.ChronoUnit.MINUTES);

    //         if (minutesSinceLastAttempt < WAIT_TIME_MINUTES) {
    //             long minutesToWait = WAIT_TIME_MINUTES - minutesSinceLastAttempt;
    //             throw new IllegalStateException("You have reached the maximum number of attempts (" + MAX_ATTEMPTS + "). Please wait " + minutesToWait + " minutes before retrying.");
    //         }
    //     }
    // }

    private boolean shouldResetAttemptCounter(List<StudentQuizAttempt> attempts) {
        if (attempts.isEmpty()) return true;

        // Chỉ reset nếu đã hoàn thành MAX_ATTEMPTS lần thử và qua thời gian cool down
        if (attempts.size() < MAX_ATTEMPTS) return false;

        StudentQuizAttempt lastAttempt = attempts.stream()
                .max(Comparator.comparing(StudentQuizAttempt::getDateTaken))
                .orElse(null);

        if (lastAttempt == null) return true;

        long minutesSinceLastAttempt = Duration.between(
                lastAttempt.getDateTaken(),
                LocalDateTime.now()
        ).toMinutes();

        return minutesSinceLastAttempt >= WAIT_TIME_MINUTES;
    }

    private QuizAttemptResponseDTO processQuizAttempt(StudentQuizAttempt attempt, Quiz quiz, QuizAttemptRequestDTO request) {
        Map<Integer, List<QuizAnswerDTO>> answersByQuestion = request.getAnswers().stream()
                .collect(Collectors.groupingBy(QuizAnswerDTO::getQuestionId));

        int correctAnswers = 0;
        int totalQuestions = quiz.getTotalQuestions();

        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByQuiz_QuizId(quiz.getQuizId());
        Set<Integer> validQuestionIds = quizQuestions.stream()
                .map(q -> q.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        // Kiểm tra tính hợp lệ của questionId
        for (Integer questionId : answersByQuestion.keySet()) {
            if (!validQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException("Question ID " + questionId + " does not belong to quiz ID " + quiz.getQuizId());
            }
        }

        // Xử lý đáp án từ request
        for (Map.Entry<Integer, List<QuizAnswerDTO>> entry : answersByQuestion.entrySet()) {
            Integer questionId = entry.getKey();
            List<QuizAnswerDTO> questionAnswers = entry.getValue();

            QuizQuestion quizQuestion = quizQuestions.stream()
                    .filter(q -> q.getQuestion().getQuestionId().equals(questionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Question ID " + questionId + " not found."));

            Question question = quizQuestion.getQuestion();
            List<QuestionAnswerOption> allOptions = questionAnswerOptionRepository.findByQuestion_QuestionId(questionId);
            Set<Integer> correctOptionIds = allOptions.stream()
                    .filter(QuestionAnswerOption::isCorrect)
                    .map(QuestionAnswerOption::getOptionId)
                    .collect(Collectors.toSet());
            Set<Integer> selectedOptionIds = questionAnswers.stream()
                    .flatMap(a -> a.getSelectedOptionIds().stream())
                    .collect(Collectors.toSet());

            // Kiểm tra tính hợp lệ của optionId
            Set<Integer> availableOptionIds = allOptions.stream()
                    .map(QuestionAnswerOption::getOptionId)
                    .collect(Collectors.toSet());
            for (Integer optionId : selectedOptionIds) {
                if (!availableOptionIds.contains(optionId)) {
                    throw new IllegalArgumentException("Option ID " + optionId + " does not exist for question ID " + questionId);
                }
            }

            boolean isQuestionCorrect;
            if (questionAnswers.isEmpty()) {
                isQuestionCorrect = false;
            } else if (question.getQuestionType().equals(QuestionType.Single_choice)) {
                isQuestionCorrect = selectedOptionIds.size() == 1 && correctOptionIds.containsAll(selectedOptionIds);
            } else {
                isQuestionCorrect = correctOptionIds.equals(selectedOptionIds);
            }

            if (isQuestionCorrect) {
                correctAnswers++;
            }

            for (QuizAnswerDTO answerDTO : questionAnswers) {
                for (Integer optionId : answerDTO.getSelectedOptionIds()) {
                    QuestionAnswerOption selectedOption = questionAnswerOptionRepository.findById(optionId)
                            .orElseThrow(() -> new IllegalArgumentException("Option ID " + optionId + " not found."));
                    QuizAnswerResponse response = QuizAnswerResponse.builder()
                            .studentQuizAttempt(attempt)
                            .selectedOption(selectedOption)
                            .isCorrect(selectedOption.isCorrect())
                            .build();
                    quizAnswerResponseRepository.save(response);
                }
            }
        }

        BigDecimal score = BigDecimal.valueOf(correctAnswers)
                .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);
        boolean isPassed = score.compareTo(quiz.getPassingScore()) >= 0;
        LocalTime timeTaken = calculateTimeTaken(attempt);

        attempt.setScore(score.multiply(BigDecimal.valueOf(10)));
        attempt.setIsPassed(isPassed);
        attempt.setTimeTaken(timeTaken);
        attempt.setIsCompleted(true);
        attempt = studentQuizAttemptRepository.save(attempt);

        // Chỉ cập nhật log và thống kê cho lần pass đầu tiên
        boolean isFirstPass = isPassed && studentQuizAttemptRepository
                .existsByStudentStudentIdAndQuizQuizIdAndIsPassedTrueAndIsCompletedTrue(
                        attempt.getStudent().getStudentId(), quiz.getQuizId());

        if (isFirstPass) {
            Optional<StudentDailyLearningLog> dailyLogOpt = dailyLogRepository
                    .findByStudentStudentIdAndLogDate(attempt.getStudent().getStudentId(), LocalDate.now());
            if (dailyLogOpt.isPresent()) {
                StudentDailyLearningLog dailyLog = dailyLogOpt.get();
                dailyLog.setCompletedQuizzes(dailyLog.getCompletedQuizzes() + 1);
                dailyLogRepository.save(dailyLog);
            }

            Optional<StudentLearningStatistics> statsOpt = statisticsRepository
                    .findByStudentStudentId(attempt.getStudent().getStudentId());
            if (statsOpt.isPresent()) {
                StudentLearningStatistics statistics = statsOpt.get();
                statistics.setTotalCompletedQuizzes(statistics.getTotalCompletedQuizzes() + 1);
                statisticsRepository.save(statistics);
            }
        }

        userActivityLogService.trackUserActivityLog(
                userRepository.findByStudentStudentId(attempt.getStudent().getStudentId()).getUserId(),
                ActivityType.COMPLETE, ContentType.Quiz, quiz.getQuizId(), null, null);

        return mapToResponseDTO(attempt);
    }

    private boolean isAttemptExpired(StudentQuizAttempt attempt) {
        LocalDateTime startTime = attempt.getDateTaken();
        LocalDateTime now = LocalDateTime.now();
        long minutes = Duration.between(startTime, now).toMinutes();
        return minutes > MAX_QUIZ_DURATION_MINUTES;
    }

    private long calculateRemainingTime(StudentQuizAttempt attempt) {
        LocalDateTime startTime = attempt.getDateTaken();
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = Duration.between(startTime, now).getSeconds();
        long maxSeconds = MAX_QUIZ_DURATION_MINUTES * 60;
        return Math.max(0, maxSeconds - elapsedSeconds);
    }

    private LocalTime calculateTimeTaken(StudentQuizAttempt attempt) {
        LocalDateTime startTime = attempt.getDateTaken();
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();
        return LocalTime.of((int) (seconds / 3600), (int) ((seconds % 3600) / 60), (int) (seconds % 60));
    }

    private QuizAttemptResponseDTO mapToResponseDTO(StudentQuizAttempt attempt) {
        BigDecimal multipliedScore = attempt.getScore()
                .setScale(2, RoundingMode.HALF_UP);

        return QuizAttemptResponseDTO.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(attempt.getQuiz().getQuizId())
                .score(multipliedScore)
                .isPassed(attempt.getIsPassed())
                .timeTaken(LocalTime.ofSecondOfDay(attempt.getTimeTaken().toSecondOfDay()))
                .isCompleted(attempt.getIsCompleted())
                .attemptNumber(attempt.getAttemptNumber())
                .dateTaken(attempt.getDateTaken())
                .remainingAttempts(0)
                .remainingWaitTime(0L)
                .build();
    }
}
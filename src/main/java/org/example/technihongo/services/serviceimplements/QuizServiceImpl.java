package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.CreateQuizDTO;
import org.example.technihongo.dto.UpdateQuizDTO;
import org.example.technihongo.dto.UpdateQuizStatusDTO;
import org.example.technihongo.entities.DifficultyLevel;
import org.example.technihongo.entities.Quiz;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.User;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Component
@Transactional
public class QuizServiceImpl implements QuizService {
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DifficultyLevelRepository difficultyLevelRepository;
    @Autowired
    private QuizQuestionRepository quizQuestionRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentSubscriptionRepository studentSubscriptionRepository;
    @Autowired
    private StudentQuizAttemptRepository studentQuizAttemptRepository;
    @Autowired
    private QuizAnswerResponseRepository quizAnswerResponseRepository;
    @Autowired
    private QuestionAnswerOptionRepository questionAnswerOptionRepository;

    private static final int MAX_ATTEMPTS = 3;
    private static final long WAIT_TIME_MINUTES = 30;


    @Override
    public List<Quiz> getQuizList() {
        return quizRepository.findAll().stream().filter(quiz -> !quiz.isDeleted()).toList();
    }

    @Override
    public List<Quiz> getPublicQuizList() {
        return quizRepository.findAll().stream().filter(quiz -> !quiz.isDeleted() && quiz.isPublic()).toList();
    }

    @Override
    public Quiz getQuizById(Integer quizId) {
        Quiz quiz = quizRepository.findByQuizId(quizId);
        if(quiz == null || quiz.isDeleted()){
            throw new RuntimeException("Quiz ID not found!");
        }
        return quiz;
    }

    @Override
    public Quiz getPublicQuizById(Integer userId, Integer quizId) {
        Quiz quiz = quizRepository.findByQuizId(quizId);
        if(quiz == null || quiz.isDeleted() || !quiz.isPublic()){
            throw new RuntimeException("Quiz ID not found!");
        }

        User user = userRepository.findByUserId(userId);
        if(user == null){
            throw new RuntimeException("User ID not found!");
        }

        if(user.getRole().getRoleId() == 3){
            Student student = studentRepository.findByUser_UserId(user.getUserId());
            if(student == null){
                throw new RuntimeException("Student not found for this user!");
            }

            if(!studentSubscriptionRepository.existsByStudent_StudentIdAndIsActive(student.getStudentId(), true)){
                throw new RuntimeException("Student not allowed to view this quiz!");
            }
        }
        return quiz;
    }

    @Override
    public Quiz createQuiz(Integer creatorId, CreateQuizDTO createQuizDTO) {
        User user = userRepository.findById(creatorId).orElseThrow(()
                -> new RuntimeException("User ID not found!"));


        DifficultyLevel difficultyLevel = difficultyLevelRepository.findById(createQuizDTO.getDifficultyLevelId())
                .orElseThrow(() -> new RuntimeException("DifficultyLevel ID not found!"));

        if(createQuizDTO.getPassingScore().compareTo(BigDecimal.valueOf(1)) > 0
                || createQuizDTO.getPassingScore().compareTo(BigDecimal.valueOf(0)) <= 0){
            throw new RuntimeException("PassingScore must between 0 and 1!");
        }

        Quiz quiz = quizRepository.save(Quiz.builder()
                        .title(createQuizDTO.getTitle())
                        .description(createQuizDTO.getDescription())
                        .creator(user)
                        .difficultyLevel(difficultyLevel)
                        .totalQuestions(0)
                        .passingScore(createQuizDTO.getPassingScore())
                .build());

        return quiz;
    }

    @Override
    public void updateQuiz(Integer quizId, UpdateQuizDTO updateQuizDTO) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz ID not found!"));

        if(quiz.isPublic()){
            throw new RuntimeException("Cannot update a public quiz");
        }

//        Domain domain = domainRepository.findById(updateQuizDTO.getDomainId())
//                .orElseThrow(() -> new RuntimeException("Domain ID not found!"));
//        if(domain.getParentDomain() == null){
//            throw new RuntimeException("Cannot assign parent domain!");
//        }

        DifficultyLevel difficultyLevel = difficultyLevelRepository.findById(updateQuizDTO.getDifficultyLevelId())
                .orElseThrow(() -> new RuntimeException("DifficultyLevel ID not found!"));

        if(updateQuizDTO.getPassingScore().compareTo(BigDecimal.valueOf(1)) > 0
                || updateQuizDTO.getPassingScore().compareTo(BigDecimal.valueOf(0)) <= 0){
            throw new RuntimeException("PassingScore must between 0 and 1!");
        }

        quiz.setTitle(updateQuizDTO.getTitle());
        quiz.setDescription(updateQuizDTO.getDescription());
        quiz.setDifficultyLevel(difficultyLevel);
        quiz.setPassingScore(updateQuizDTO.getPassingScore());

        quizRepository.save(quiz);
    }

    @Override
    public void updateQuizStatus(Integer quizId, UpdateQuizStatusDTO updateQuizStatusDTO) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz ID not found!"));

        if(updateQuizStatusDTO.getIsPublic() && updateQuizStatusDTO.getIsDeleted()){
            throw new RuntimeException("Cannot delete a public quiz");
        }

        quiz.setPublic(updateQuizStatusDTO.getIsPublic());
        quiz.setDeleted(updateQuizStatusDTO.getIsDeleted());

        quizRepository.save(quiz);
    }

    @Override
    public void updateTotalQuestions(Integer quizId) {
        if(quizRepository.findByQuizId(quizId) == null){
            throw new RuntimeException("Quiz ID not found!");
        }

        Quiz quiz = quizRepository.findByQuizId(quizId);
        quiz.setTotalQuestions(quizQuestionRepository.countByQuiz_QuizId(quizId));
        quizRepository.save(quiz);
    }

    @Override
    public List<Quiz> getListQuizzesByCreatorId(Integer creatorId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User ID not found."));
        return quizRepository.findByCreator_UserId(creatorId);
    }


}

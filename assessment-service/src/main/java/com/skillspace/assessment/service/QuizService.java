package com.skillspace.assessment.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.skillspace.assessment.exceptions.QuizNotFoundException;
import com.skillspace.assessment.model.*;
import com.skillspace.assessment.repositories.BadgeRepository;
import com.skillspace.assessment.repositories.QuizAttemptRepository;
import com.skillspace.assessment.repositories.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final BadgeRepository badgeRepository;

    @Autowired
    Cloudinary cloudinary;

    public Quiz createQuiz(Quiz quiz, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.emptyMap());
            quiz.setImageUrl(uploadResult.get("url").toString());
        }

        quiz.setId(UUID.randomUUID());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUpdatedAt(LocalDateTime.now());
        quiz.setTotalPoints(calculateTotalPoints(quiz));

        log.info("Creating quiz with title: {}", quiz.getTitle());
        return quizRepository.save(quiz);
    }

    public Quiz getQuizById(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found with ID: " + id));
    }

    public Quiz updateQuiz(UUID id, Quiz quizDetails) {
        Quiz existingQuiz = getQuizById(id);
        existingQuiz.setTitle(quizDetails.getTitle());
        existingQuiz.setQuestions(quizDetails.getQuestions());
        existingQuiz.setTimeLimit(quizDetails.getTimeLimit());
        existingQuiz.setPassingScore(quizDetails.getPassingScore());
        existingQuiz.setUpdatedAt(LocalDateTime.now());

        log.info("Updating quiz with ID: {}", id);
        return quizRepository.save(existingQuiz);
    }

    public void deleteQuiz(UUID id) {
        log.info("Deleting quiz with ID: {}", id);
        quizRepository.deleteById(id);
    }

    public int calculateScore(UUID quizId, List<String> userAnswers) {
        Quiz quiz = getQuizById(quizId);
        int score = 0;
        List<Question> questions = quiz.getQuestions();

        if (userAnswers == null || userAnswers.size() < questions.size()) {
            throw new IllegalArgumentException("User answers are not complete");
        }

        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getCorrect_answer().equals(userAnswers.get(i))) {
                score += questions.get(i).getPoints();
            }
        }

        return score;
    }

    public QuizAttemptResult saveQuizAttempt(UUID userId, UUID quizId, List<String> userAnswers) {
        Quiz quiz = getQuizById(quizId);
        int score = calculateScore(quizId, userAnswers);
        QuizAttempt attempt = new QuizAttempt();
        attempt.setAttemptId(UUID.randomUUID().toString());
        attempt.setUserId(userId.toString());
        attempt.setQuizId(quizId.toString());
        attempt.setUserAnswers(userAnswers);
        attempt.setScore(score);
        attempt.setTimestamp(System.currentTimeMillis());

        quizAttemptRepository.save(attempt);

        QuizAttemptResult result = new QuizAttemptResult();
        result.setScore(score);
        result.setTotalScore(quiz.getTotalPoints());
        result.setPassed(score >= quiz.getPassingScore());

        if (score >= quiz.getPassingScore()) {
            Badge badge = new Badge(UUID.randomUUID(), quiz.getTitle());
            badgeRepository.save(badge);
            result.setBadge(badge.getName());
        }

        List<QuestionResult> questionResults = new ArrayList<>();
        List<Question> questions = quiz.getQuestions();

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            QuestionResult questionResult = new QuestionResult();
            questionResult.setQuestion_text(question.getQuestion_text());
            questionResult.setCorrect_answer(question.getCorrect_answer());
            questionResult.setSelectedAnswer(userAnswers.get(i));
            questionResult.setSelectedAnswer(userAnswers.get(i));
            questionResult.setCorrect(question.getCorrect_answer().equals(userAnswers.get(i)));

            questionResult.setImageUrl(question.getImageUrl());

            questionResults.add(questionResult);
        }

        result.setQuestionResults(questionResults);
        return result;
    }

    public List<Quiz> filterQuizzes(String companyId, String title, String programId) {
        return quizRepository.findAll();
    }

    public boolean evaluateQuizCompletion(UUID quizId, int score) {
        Quiz quiz = getQuizById(quizId);
        return score >= quiz.getPassingScore();
    }

    private int calculateTotalPoints(Quiz quiz) {
        return quiz.getQuestions().stream().mapToInt(Question::getPoints).sum();
    }

    public boolean canRetryQuiz(UUID quizId) {
        Quiz quiz = getQuizById(quizId);
        LocalDateTime lastAttempt = quiz.getUpdatedAt();
        return LocalDateTime.now().isAfter(lastAttempt.plusDays(7));
    }
}

package ru.yandex.market.pers.qa.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.model.DateFilter;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.State;

public class AnswerServiceTest extends PersQATest {
    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionService questionService;

    @Test
    public void testLockCheck() {
        SecurityData securityData = SecurityData.generate("source", "reason");
        Question question = Question.buildModelQuestion(234L, "test", 432L);
        long questionId = questionService.createQuestionGetId(question, securityData);
        Answer answer = Answer.buildBasicAnswer(123L, "test", questionId);

        Assertions.assertFalse(answerService.isLockExist(answer));

        answerService.createAnswer(answer, securityData);

        Assertions.assertTrue(answerService.isLockExist(answer));
    }

    @Test
    public void testDeleteHardAnswersByFilter() {
        SecurityData securityData = SecurityData.generate("source", "reason");
        Question question = Question.buildModelQuestion(234L, "test", 432L);
        long questionId = questionService.createQuestionGetId(question, securityData);
        Answer answer = Answer.buildBasicAnswer(123L, "test", questionId);
        answerService.createAnswer(answer, securityData);
        AnswerFilter filter = new AnswerFilter()
                .authorUid(123L)
                .fromReplica()
                .allowsNonPublic();
        AnswerFilter filterWithDate = filter.dateFilter(new DateFilter(null, getDateTo()));

        answerService.deleteHardAnswersByFilter(filterWithDate);
        List<Answer> answers = answerService.getAnswers(filter);
        Assertions.assertEquals(1, answers.size());

        answerService.deleteAnswersByFilter(filter);
        List<Answer> deletedAnswers = answerService.getAnswers(filter);
        Assertions.assertEquals(1, deletedAnswers.size());
        Assertions.assertEquals(State.DELETED, deletedAnswers.get(0).getState());

        answerService.deleteHardAnswersByFilter(filterWithDate);
        List<Answer> deletedHardAnswers = answerService.getAnswers(filterWithDate);
        Assertions.assertEquals(0, deletedHardAnswers.size());
    }

    private Date getDateTo() {
        return Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    }
}

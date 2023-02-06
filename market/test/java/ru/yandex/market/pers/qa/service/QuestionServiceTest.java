package ru.yandex.market.pers.qa.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.model.DateFilter;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.model.State;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionServiceTest extends PersQATest {

    @Autowired
    private QuestionService questionService;

    @Test
    public void testDeleteHardQuestionsByFilter() {
        SecurityData securityData = SecurityData.generate("source", "reason");
        Question question = Question.buildModelQuestion(234L, "test", 432L);
        questionService.createQuestion(question, securityData);
        QuestionFilter filter = new QuestionFilter()
                .allowAllEntities()
                .authorUid(234L)
                .fromReplica()
                .allowsNonPublic();
        QuestionFilter filterWithDate = filter.dateFilter(new DateFilter(null, getDateTo()));

        questionService.deleteHardQuestionsByFilter(filterWithDate);
        List<Question> questions = questionService.getQuestions(filter);
        assertEquals(1, questions.size());

        questionService.deleteQuestionsByFilter(filter);
        List<Question> deletedQuestions = questionService.getQuestions(filter);
        assertEquals(1, deletedQuestions.size());
        assertEquals(State.DELETED, deletedQuestions.get(0).getState());

        questionService.deleteHardQuestionsByFilter(filterWithDate);
        List<Question> deletedHardQuestions = questionService.getQuestions(filterWithDate);
        assertEquals(0, deletedHardQuestions.size());
    }

    private Date getDateTo() {
        return Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
    }
}

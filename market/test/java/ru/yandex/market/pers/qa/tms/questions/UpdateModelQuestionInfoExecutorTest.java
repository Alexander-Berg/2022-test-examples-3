package ru.yandex.market.pers.qa.tms.questions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.report.ReportService;

import java.util.UUID;

/**
 * @author varvara
 * 21.08.2018
 */
abstract class UpdateModelQuestionInfoExecutorTest extends PersQaTmsTest {

    private static final long USER_ID = 325581000;

    @Autowired
    protected UpdateModelQuestionInfoExecutor executor;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;
    @Autowired
    private QuestionService questionService;
    @Autowired
    protected ReportService reportService;

    protected Long createQuestion(long modelId) {
        final Question question = Question.buildModelQuestion(USER_ID, UUID.randomUUID().toString(), modelId);
        return questionService.createQuestion(question, null).getId();
    }

}

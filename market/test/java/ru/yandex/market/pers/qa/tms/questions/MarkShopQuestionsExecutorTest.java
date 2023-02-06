package ru.yandex.market.pers.qa.tms.questions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.common.util.csv.CSVProcessor;
import ru.yandex.common.util.csv.CSVRowMapper;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.suggest.ShopQuestionRegExpMatcherService;
import ru.yandex.market.util.db.ConfigurationService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author varvara
 * 31.10.2018
 */
public class MarkShopQuestionsExecutorTest extends PersQaTmsTest {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Autowired
    private MarkShopQuestionsExecutor markShopQuestionsExecutor;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private QuestionService questionService;

    @Test
    public void testShopQuestionRegexpSmallBatch() {
        qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", 1, ".*?[^а-яА-Я]цен.*");
        qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", 2, "^цен.*");

        qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", 3, ".*?оплат.*?");

        qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", 4, "^срок[^а-яА-Я].*");
        qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", 5, "^.*[^а-яА-Я]срок");

        Question question1 = createConfirmedQuestion("что с ценой?");
        Question question2 = createConfirmedQuestion("цена?");
        Question question3 = createConfirmedQuestion("ЦЕНА");
        Question question4 = createConfirmedQuestion("это хороший аппарат?");

        Question question5 = createConfirmedQuestion("как ОпЛаТиТь?");

        Question question6 = createConfirmedQuestion("срок доставки");
        Question question7 = createConfirmedQuestion("какой срок доставки");
        Question question8 = createConfirmedQuestion("срок ДоСтавки");

        markShopQuestionsExecutor.processQuestionsByRegExp();

        checkQuestionShopFlag(1, question1);
        checkQuestionShopFlag(1, question2);
        checkQuestionShopFlag(1, question3);
        checkQuestionShopFlag(0, question4);

        checkQuestionShopFlag(1, question5);

        checkQuestionShopFlag(1, question6);
        checkQuestionShopFlag(1, question7);
        checkQuestionShopFlag(1, question8);

        assertEquals(question8.getId(), getLastProcessedId());
    }

    private void checkQuestionShopFlag(int flag, Question question) {
        assertEquals(flag, (long) getQuestionShopFlag(question), question.getText());
    }

    @Test
    public void testShopQuestionRegexpBigBatch() throws IOException {
        Map<Question, Long> questions = new HashMap<>();
        createQuestions(questions, "/data/bad_questions_for_regexp.txt", 1L);
        createQuestions(questions, "/data/good_questions_for_regexp.txt", 0L);
        List<String> regexps = getShopRegexp("/changesets/table/qa/SHOP_QUESTION_REGEXP.csv");

        int i = 1;
        for (String regexp : regexps) {
            qaJdbcTemplate.update("insert into qa.SHOP_QUESTION_REGEXP(id, regexp) values(?, ?)", i, regexp);
            i++;
        }

        configurationService.mergeValue("shopQuestionRegexpMatchBatchSize", questions.size() + 1L);

        markShopQuestionsExecutor.processQuestionsByRegExp();

        questions.forEach(
            (question, result) -> assertEquals(result, getQuestionShopFlag(question), question.getText())
        );

        Optional<Long> maxQuestionId = questions.keySet().stream().map(Question::getId).max(Long::compareTo);
        assertTrue(maxQuestionId.isPresent());
        assertEquals(maxQuestionId.get(), getLastProcessedId());
    }

    private Long getQuestionShopFlag(Question question) {
        final List<Long> shopFlags = qaJdbcTemplate.queryForList(
            "select shop_rx_fl from qa.question_info where question_id = ?", Long.class, question.getId());
        assertNotNull(shopFlags);
        assertEquals(1, shopFlags.size());
        return shopFlags.get(0);
    }


    private Question createConfirmedQuestion(String text) {
        Question question = questionService.createModelQuestion(1, text, 1);
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        return question;
    }

    private void createQuestions(Map<Question, Long> questionResult, String filename, Long isShopQuestion) {
        InputStream inputStream = this.getClass().getResourceAsStream(filename);
        Scanner s = new Scanner(inputStream);

        while(s.hasNextLine()) {
            String questionText = s.nextLine();
            Question question = createConfirmedQuestion(questionText);
            questionResult.putIfAbsent(question, isShopQuestion);
        }
        s.close();
    }

    private List<String> getShopRegexp(String filename) throws IOException {
        CSVProcessor csvProcessor = new CSVProcessor(new ClassPathResource(filename).getInputStream());
        List<String> regExps = csvProcessor.process(new CSVRowMapper<String>() {
            @Override
            public void onHeaders(Set<String> headers) {
            }

            @Override
            public String mapRow(Map<String, String> fieldsByNames) {
                return fieldsByNames.get("REGEXP");
            }
        });
        return regExps;
    }

    private Long getLastProcessedId() {
        return configurationService.getValueAsLong(
            ShopQuestionRegExpMatcherService.SHOP_QUESTION_REGEXP_MATCH_LAST_PROCESSED_ID);
    }

}



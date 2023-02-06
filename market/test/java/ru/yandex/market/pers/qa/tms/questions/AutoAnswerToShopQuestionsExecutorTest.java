package ru.yandex.market.pers.qa.tms.questions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.ModerationLogService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.suggest.ShopQuestionRegExpMatcherService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoAnswerToShopQuestionsExecutorTest extends PersQaTmsTest {
    public static final String SINGLE_REGEXP_AUTO_ANSWER = "Добрый день!\n" +
        "Меня зовут Бендер. Я робот Яндекс.Маркета. Моя цель — нести добро и принести немного пользы. Яндекс.Маркет - это сервис для поиска и подбора товаров и магазинов.\n" +
        "Перейдя на страницу товара, вы увидите предложения разных магазинов, если товар доступен для покупки. Тут будут цены, отзывы, сроки и условия доставки. Для более подробных деталей (например, условия и способы оплаты) нужно перейти по кнопке \"в магазин\".\n" +
        "Отличного вам дня!";
    public static final String MULTIPLE_REGEXP_AUTO_ANSWER = "Добрый день!\n" +
        "Меня зовут Бендер. Я робот Яндекс.Маркета. Моя цель — нести добро и принести немного пользы. Яндекс.Маркет - это сервис для поиска и подбора товаров.\n" +
        "Насколько я понял, вас интересует сертификация товара. На странице товара вы увидите характеристики, но информации о сертификации там пока нет: у каждого магазина свои условия продажи. Пока Вы можете выбрать понравившуюся стоимость, перейти в конкретный магазин и узнать детали о товаре в нём.\n" +
        "Отличного вам дня!";

    private static final Long MODEL_ID = 123L;
    private static final Long USER_ID = 123L;
    private static final Long BANNED_VENDOR_MODEL_ID = 1234321L;
    private static final Long NEW_MODEL_ID = 234L;
    private static final Long MODEL_ID_WITHOUT_VENDOR = 345L;
    private static final Long BANNED_VENDOR_ID = 111L;
    @Autowired
    AutoAnswerToShopQuestionsExecutor autoAnswerToShopQuestionsExecutor;
    @Autowired
    QuestionService questionService;
    @Autowired
    AnswerService answerService;
    @Qualifier("pgJdbcTemplate")
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ShopQuestionRegExpMatcherService shopQuestionRegExpMatcherService;
    @Autowired
    ModerationLogService moderationLogService;

    private void prepareAutoAnswers() {
        jdbcTemplate.update("INSERT INTO qa.auto_answer (id, answer_text) VALUES (4, ?)", SINGLE_REGEXP_AUTO_ANSWER);
        jdbcTemplate.update("INSERT INTO qa.auto_answer (id, answer_text) VALUES (5, ?)", MULTIPLE_REGEXP_AUTO_ANSWER);
        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (1, '.*?[^а-яА-Я]оригинал.*', 5)");
        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (2, '^оригинал.*', 5)");
        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (3, '.*?[^а-яА-Я]подделк.*', 5)");
        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (4, '^подделк.*', 5)");
        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (5, '.*?оплат.*?', 4)");
        jdbcTemplate.update("INSERT INTO qa.model_vendor (model_id, vendor_id) VALUES (?, 1)", MODEL_ID);
    }

    @Test
    void testPerformAutoAnswerToShopQuestions() {
        prepareAutoAnswers();

        Question questionWithMultipleRegexps = questionService.createModelQuestion(USER_ID, "подделка оригинал", MODEL_ID);

        Question questionSingleRegexp = questionService.createModelQuestion(USER_ID, "оплата", MODEL_ID);
        Question questionSingleRegexpWithAnswer = questionService.createModelQuestion(USER_ID, "ещё одна оплата и почек больше нет", MODEL_ID);
        answerService.createAnswer(USER_ID, "Random answer", questionSingleRegexpWithAnswer.getId());

        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (6, '.*?фейк.*?', null)");
        Question questionWithoutAutoAnswer = questionService.createModelQuestion(USER_ID, "фейк", MODEL_ID);

        jdbcTemplate.update("INSERT INTO qa.shop_question_regexp (id, regexp, auto_answer_id) VALUES (7, '.*?candy.*?', null)");
        jdbcTemplate.update("INSERT INTO qa.model_vendor (model_id, vendor_id) VALUES (?, ?)", BANNED_VENDOR_MODEL_ID, BANNED_VENDOR_ID);
        jdbcTemplate.update("INSERT INTO qa.auto_answer_vendor_ban (id, name) VALUES (?, 'Candy')", BANNED_VENDOR_ID);
        Question questionForBannedVendor = questionService.createModelQuestion(USER_ID, "вопрос про candy", BANNED_VENDOR_MODEL_ID);

        //model id not in model_vendor table
        Question questionWithFreshModelId = questionService.createModelQuestion(USER_ID, "вопрос про очень новую модель", NEW_MODEL_ID);
        //model id is in model_vendor table, but vendor_id is null for now
        jdbcTemplate.update("INSERT INTO qa.model_vendor (model_id, vendor_id) VALUES (?, null)", MODEL_ID_WITHOUT_VENDOR);
        Question questionWithNotUpdatedVendorId = questionService.createModelQuestion(USER_ID, "вопрос про очень новую модель", MODEL_ID_WITHOUT_VENDOR);

        jdbcTemplate.update("update qa.question_info set shop_rx_fl = ?", 1);
        jdbcTemplate.update("update qa.question set mod_state = ?", ModState.CONFIRMED.getValue());

        autoAnswerToShopQuestionsExecutor.performAutoAnswerToShopQuestions();

        List<Answer> list = getAnswersByQuestionId(questionWithMultipleRegexps.getId());
        assertEquals(1, list.size());
        Answer answer = list.get(0);
        assertEquals(UserType.UID, answer.getUserType());
        assertEquals(String.valueOf(AutoAnswerToShopQuestionsExecutor.FAKE_USER), answer.getUserId());
        assertEquals(MULTIPLE_REGEXP_AUTO_ANSWER, answer.getText());
        assertEquals(ModState.CONFIRMED, answer.getModState());
        assertEquals(Long.valueOf(1), jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionWithMultipleRegexps.getId()));
        assertEquals(1, moderationLogService.getModerationLogRecordsCount(QaEntityType.ANSWER, answer.getId(), ModState.CONFIRMED));

        list = getAnswersByQuestionId(questionSingleRegexp.getId());
        assertEquals(1, list.size());
        answer = list.get(0);
        assertEquals(UserType.UID, answer.getUserType());
        assertEquals(String.valueOf(AutoAnswerToShopQuestionsExecutor.FAKE_USER), answer.getUserId());
        assertEquals(SINGLE_REGEXP_AUTO_ANSWER, answer.getText());
        assertEquals(ModState.CONFIRMED, answer.getModState());
        assertEquals(Long.valueOf(1), jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionSingleRegexp.getId()));
        assertEquals(1, moderationLogService.getModerationLogRecordsCount(QaEntityType.ANSWER, answer.getId(), ModState.CONFIRMED));

        list = getAnswersByQuestionId(questionSingleRegexpWithAnswer.getId());
        assertEquals(1, list.size());
        answer = list.get(0);
        assertEquals(UserType.UID, answer.getUserType());
        assertEquals(USER_ID.toString(), answer.getUserId());
        assertEquals("Random answer", answer.getText());

        list = getAnswersByQuestionId(questionWithoutAutoAnswer.getId());
        assertTrue(list.isEmpty());
        assertEquals(Long.valueOf(0), jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionWithoutAutoAnswer.getId()));

        list = getAnswersByQuestionId(questionForBannedVendor.getId());
        assertTrue(list.isEmpty());
        assertEquals(Long.valueOf(0), jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionForBannedVendor.getId()));

        list = getAnswersByQuestionId(questionWithFreshModelId.getId());
        assertTrue(list.isEmpty());
        assertNull(jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionWithFreshModelId.getId()));

        list = getAnswersByQuestionId(questionWithNotUpdatedVendorId.getId());
        assertTrue(list.isEmpty());
        assertNull(jdbcTemplate.queryForObject("select shop_auto_answer from qa.question_info where question_id = ?", Long.class, questionWithNotUpdatedVendorId.getId()));
    }

    @Test
    void testAutoAnswersEraseAfterRealAnswer() {
        prepareAutoAnswers();

        Question question = questionService.createModelQuestion(USER_ID, "оплата", MODEL_ID);

        jdbcTemplate.update("update qa.question_info set shop_rx_fl = ?", 1);
        jdbcTemplate.update("update qa.question set mod_state = ?", ModState.CONFIRMED.getValue());

        autoAnswerToShopQuestionsExecutor.performAutoAnswerToShopQuestions();
        autoAnswerToShopQuestionsExecutor.removeRedundantAutoAnswers();

        List<Answer> list = getAnswersByQuestionId(question.getId());
        assertEquals(1, list.size());
        Answer answer = list.get(0);
        assertEquals(UserType.UID, answer.getUserType());
        assertEquals(String.valueOf(AutoAnswerToShopQuestionsExecutor.FAKE_USER), answer.getUserId());
        assertEquals(SINGLE_REGEXP_AUTO_ANSWER, answer.getText());

        answerService.createAnswer(USER_ID, "Some answer", question.getId());

        autoAnswerToShopQuestionsExecutor.removeRedundantAutoAnswers();

        list = getAnswersByQuestionId(question.getId());
        assertEquals(1, list.size());
        answer = list.get(0);
        assertEquals(UserType.UID, answer.getUserType());
        assertEquals(USER_ID.toString(), answer.getUserId());
        assertEquals("Some answer", answer.getText());
    }

    private List<Answer> getAnswersByQuestionId(Long questionWithNotUpdatedVendorId) {
        return answerService.getAnswers(new AnswerFilter().questionId(questionWithNotUpdatedVendorId));
    }
}

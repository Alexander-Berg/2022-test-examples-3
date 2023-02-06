package ru.yandex.market.pers.qa.tms.telegram;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.Objects;

import com.google.common.html.HtmlEscapers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.QuestionAgitationInfo;
import ru.yandex.market.pers.qa.model.QuestionFilter;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.telegram.TelegramBotClient;
import ru.yandex.market.telegram.TelegramResponse;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.09.2018
 */
public class ExportNewQuestionsToTelegramExecutorTest extends PersQaTmsTest {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate qaJdbcTemplate;

    @Autowired
    private ExportNewQuestionsToTelegramExecutor executor;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TelegramBotClient telegramBotClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected void resetMocks() {
        super.resetMocks();

        // mark all previous questions as sent
        final QuestionFilter filter = new QuestionFilter()
            .allowAllEntities()
            .exportedToTelegram(false);

        questionService.getQuestions(filter).forEach(
            question -> questionService.setSentToTelegram(question.getId())
        );
    }

    @Test
    void testMessageGeneration() {
        final long userId = 1;
        final long modelId = 2;
        final String text = "Test question<&>";

        // check nothing exported yet
        executor.sendNewQuestions();
        verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());

        // try to export single question
        final QuestionAgitationInfo agitationInfo = createQuestionForTelegram(userId, modelId, text, null);
        final Question question = agitationInfo.getQuestion();

        final String checkAnswer = String.format(
                "Вопрос #%d от %s\n" +
                "\n" +
                "<i>%s</i>\n" +
                "\n" +
                "Ответить на вопрос можно по ссылке:\n" +
                "localhost/product/%d/question/%d",
            question.getId(),
            DATE_FORMAT.format(question.getTimestamp().toEpochMilli()),
            HtmlEscapers.htmlEscaper().escape(text),
            modelId,
            question.getId());

        // check still nothing done since question is not in final state
        executor.sendNewQuestions();
        verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());

        // force update of state and try again
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        executor.sendNewQuestions();

        // check only one message is sent and it is the expected one
        verify(telegramBotClient, times(1)).sendBotMessage(anyString(), anyString());
        verify(telegramBotClient).sendBotMessage("none", checkAnswer);

        // try again, nothing should change
        executor.sendNewQuestions();
        verify(telegramBotClient, times(1)).sendBotMessage(anyString(), anyString());
    }

    @Test
    void testGeneration() {
        final long userId = 1;
        final long modelId = 2;
        final String modelName = "Model XXX<&>";
        final String text = "Test question";

        final String source = "some source";
        final int regionId = 113857;

        final Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);

        final SecurityData sec = new SecurityData();
        sec.setRegionId(regionId);
        sec.setSource(source);
        sec.setIp("some ip");
        sec.setUserAgent("some user agent");

        // create question
        final QuestionAgitationInfo agitationInfo = createQuestionForTelegram(userId, modelId, text, sec);
        final Question question = agitationInfo.getQuestion();

        final String message = executor.buildMessageText(agitationInfo, Collections.singletonMap(modelId, model));
        final String expectedMessage = String.format(
            "Вопрос #%d от %s\n" +
                "<b>%s</b>\n" +
                "\n" +
                "<i>%s</i>\n" +
                "\n" +
                "<b>Регион:</b> %s\n" +
                "<b>Источник:</b> %s\n" +
                "\n" +
                "Ответить на вопрос можно по ссылке:\n" +
                "localhost/product/%s/question/%s",
            question.getId(),
            DATE_FORMAT.format(question.getTimestamp().toEpochMilli()),
            HtmlEscapers.htmlEscaper().escape(modelName),
            HtmlEscapers.htmlEscaper().escape(text),
            "Инверкаргилл",
            source,
            modelId,
            question.getId());

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    void testGenerationWithShop() {
        final long userId = 1;
        final long modelId = 2;
        final String modelName = "Model XXX<&>";
        final String text = "Test question";

        final String source = "some source";
        final int regionId = 113857;

        final Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);

        final SecurityData sec = new SecurityData();
        sec.setRegionId(regionId);
        sec.setSource(source);
        sec.setIp("some ip");
        sec.setUserAgent("some user agent");

        // create question
        final QuestionAgitationInfo agitationInfo = createShopQuestionForTelegram(userId, modelId, text, sec);
        final Question question = agitationInfo.getQuestion();

        final String message = executor.buildMessageText(agitationInfo, Collections.singletonMap(modelId, model));
        final String expectedMessage = String.format(
            "Вопрос #%d от %s\n" +
                "<b>%s</b>\n" +
                "\n" +
                "<i>%s</i>\n" +
                "\n" +
                "<b>Регион:</b> %s\n" +
                "<b>Источник:</b> %s\n" +
                "<b>Признаки:</b> магазинный\n" +
                "\n" +
                "Ответить на вопрос можно по ссылке:\n" +
                "localhost/product/%s/question/%s",
            question.getId(),
            DATE_FORMAT.format(question.getTimestamp().toEpochMilli()),
            HtmlEscapers.htmlEscaper().escape(modelName),
            HtmlEscapers.htmlEscaper().escape(text),
            "Инверкаргилл",
            source,
            modelId,
            question.getId());

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    void testGenerationWithShopNoRegion() {
        final long userId = 1;
        final long modelId = 2;
        final String modelName = "Model XXX<&>";
        final String text = "Test question";

        final String source = "some source";

        final Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);

        final SecurityData sec = new SecurityData();
        sec.setSource(source);
        sec.setIp("some ip");
        sec.setUserAgent("some user agent");

        // create question
        final QuestionAgitationInfo agitationInfo = createShopQuestionForTelegram(userId, modelId, text, sec);
        final Question question = agitationInfo.getQuestion();

        final String message = executor.buildMessageText(agitationInfo, Collections.singletonMap(modelId, model));
        final String expectedMessage = String.format(
            "Вопрос #%d от %s\n" +
                "<b>%s</b>\n" +
                "\n" +
                "<i>%s</i>\n" +
                "\n" +
                "<b>Источник:</b> %s\n" +
                "<b>Признаки:</b> магазинный\n" +
                "\n" +
                "Ответить на вопрос можно по ссылке:\n" +
                "localhost/product/%s/question/%s",
            question.getId(),
            DATE_FORMAT.format(question.getTimestamp().toEpochMilli()),
            HtmlEscapers.htmlEscaper().escape(modelName),
            HtmlEscapers.htmlEscaper().escape(text),
            source,
            modelId,
            question.getId());

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    void testGenerationWithShopNoSource() {
        final long userId = 1;
        final long modelId = 2;
        final String modelName = "Model XXX<&>";
        final String text = "Test question";

        final int regionId = 113857;

        final Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);

        final SecurityData sec = new SecurityData();
        sec.setRegionId(regionId);
        sec.setIp("some ip");
        sec.setUserAgent("some user agent");

        // create question
        final QuestionAgitationInfo agitationInfo = createShopQuestionForTelegram(userId, modelId, text, sec);
        final Question question = agitationInfo.getQuestion();

        final String message = executor.buildMessageText(agitationInfo, Collections.singletonMap(modelId, model));
        final String expectedMessage = String.format(
            "Вопрос #%d от %s\n" +
                "<b>%s</b>\n" +
                "\n" +
                "<i>%s</i>\n" +
                "\n" +
                "<b>Регион:</b> %s\n" +
                "<b>Признаки:</b> магазинный\n" +
                "\n" +
                "Ответить на вопрос можно по ссылке:\n" +
                "localhost/product/%s/question/%s",
            question.getId(),
            DATE_FORMAT.format(question.getTimestamp().toEpochMilli()),
            HtmlEscapers.htmlEscaper().escape(modelName),
            HtmlEscapers.htmlEscaper().escape(text),
            "Инверкаргилл",
            modelId,
            question.getId());

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    void testMonitoring() throws Exception {
        String configKey = AbstractExportToTelegramExecutor.LAST_SUCCESS_PREF + "none";
        TelegramResponse telegramResponse = new TelegramResponse();
        TemporalAmount offset = ExportNewQuestionsToTelegramExecutor.UNSUCCESSFUL_PERIOD;
        LocalDateTime someOldDate = LocalDateTime.now().minus(offset).minusDays(1L);

        // happy path
        QuestionAgitationInfo agitationInfo = createQuestionForTelegram(1, 2, "text 1", null);
        Question question = agitationInfo.getQuestion();
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        configurationService.mergeValue(configKey, someOldDate);
        telegramResponse.setOk(true);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);
        executor.sendNewQuestions();

        // assert that date has been changed
//        Assertions.assertEquals(configurationService.getValueAsLocalDateTime(configKey), someOldDate);
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isAfter(someOldDate));

        // Telegram does not returns OK and last success date is long ago enough to throw exception
        agitationInfo = createQuestionForTelegram(1, 2, "text 2", null);
        question = agitationInfo.getQuestion();
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        configurationService.mergeValue(configKey, someOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Should throw exception because last success delivery was too long ago
        Assertions.assertThrows(RuntimeException.class, () -> executor.sendNewQuestions());

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isBefore(someOldDate));

        // Telegram does not returns OK and last success date is NOT long ago enough to throw exception
        agitationInfo = createQuestionForTelegram(1, 2, "text 3", null);
        question = agitationInfo.getQuestion();
        questionService.forceUpdateModState(question.getId(), ModState.CONFIRMED);
        LocalDateTime shiftedOldDate = someOldDate.plusDays(2L);
        configurationService.mergeValue(configKey, shiftedOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Shouldn't throw exception
        executor.sendNewQuestions();

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isBefore(shiftedOldDate));
    }

    private Question getQuestionSimple(long userId, long modelId, String text, SecurityData sec) {
        return questionService.createQuestion(
            Question.buildModelQuestion(userId, text, modelId), sec
        );
    }

    private QuestionAgitationInfo createQuestionForTelegram(long userId, long modelId, String text, SecurityData sec) {
        Question question = getQuestionSimple(userId, modelId, text, sec);
        return makeShop(question, false);
    }

    private QuestionAgitationInfo createShopQuestionForTelegram(long userId,
                                                                long modelId,
                                                                String text,
                                                                SecurityData sec) {
        Question question = getQuestionSimple(userId, modelId, text, sec);
        return makeShop(question, true);
    }

    private QuestionAgitationInfo makeShop(Question question, boolean shopFlag) {
        int shopFlagValue = shopFlag ? 1 : 0;
        qaJdbcTemplate.update("update qa.question_info\n" +
                "set shop_rx_fl = ?\n" +
                "where question_id = ?",
            shopFlagValue,
            question.getId());

        return new QuestionAgitationInfo(question, shopFlagValue);
    }
}

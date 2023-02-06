package ru.yandex.market.pers.qa.tms.telegram;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.shopinfo.ShopInfoService;
import ru.yandex.market.telegram.TelegramBotClient;
import ru.yandex.market.telegram.TelegramResponse;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportNewAnswersToTelegramExecutorTest extends PersQaTmsTest {

    private static final long FAKE_USER = 1234;

    @Autowired
    private ExportNewAnswersToTelegramExecutor executor;

    @Autowired
    private TelegramBotClient telegramBotClient;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private BlackBoxService blackBoxService;

    @Autowired
    private ShopInfoService shopInfoService;

    @Autowired
    private CatalogerClient catalogerClient;

    @Autowired
    private ConfigurationService configurationService;

    @Value("${pers.qa.telegram.export.answers.all.group}")
    private String chatIdAll;

    @Value("${pers.qa.telegram.export.answers.shop.group}")
    private String chatIdShop;

    @Test
    void testSendNewArticleComments() {
        int shopId = 6;
        int modelId = 1;

        executor.getLastProcessed();

        Question[] questions = {
            questionService.createModelQuestion((int) FAKE_USER, "question 1", modelId),
            questionService.createModelQuestion((int) FAKE_USER + 1, "question 2", modelId)
        };

        Answer[] answers = {
            answerService.createAnswer((int) FAKE_USER, "answer 1", questions[0].getId()),
            answerService.createShopAnswer((int) FAKE_USER + 3, "answer 2 shop", questions[0].getId(), shopId),
            answerService.createAnswer((int) FAKE_USER, "answer 3", questions[0].getId()),
            answerService.createAnswer((int) FAKE_USER, "answer 4", questions[1].getId()),
        };

        executor.sendNewAnswers();

        verify(telegramBotClient, times(2)).sendBotMessage(eq(chatIdAll), anyString());
        verify(telegramBotClient, times(1)).sendBotMessage(eq(chatIdShop), anyString());
    }

    @Test
    void testMessageText() {
        int shopId = 6;
        int shopIdOther = 7;
        int brandId = 45;
        int brandIdOther = 46;
        int modelId = 1;

        executor.getLastProcessed();

        Question[] questions = {
            questionService.createModelQuestion((int) FAKE_USER, "question 1", modelId),
        };

        Answer[] answers = {
            answerService.createAnswer((int) FAKE_USER, "answer 1", questions[0].getId()),
            answerService.createShopAnswer((int) FAKE_USER + 3, "answer 2 shop", questions[0].getId(), shopId),
            answerService.createAnswer((int) FAKE_USER + 1, "answer 3", questions[0].getId()),
            answerService.createShopAnswer((int) FAKE_USER + 5, "answer 4 shop", questions[0].getId(), shopIdOther),
            answerService.createVendorAnswer((int) FAKE_USER + 7, "answer 5 vendor", questions[0].getId(), brandId),
            answerService.createVendorAnswer((int) FAKE_USER + 8,
                "answer 6 vendor",
                questions[0].getId(),
                brandIdOther),
        };

        // mock blackbox
        BlackBoxUserInfo mockedInfo = new BlackBoxUserInfo(FAKE_USER);
        mockedInfo.addField(UserInfoField.LOGIN, "somelogin");
        when(blackBoxService.getUserInfo(FAKE_USER, UserInfoField.LOGIN)).thenReturn(mockedInfo);

        // mock shop info
        when(shopInfoService.getShopName(shopId)).thenReturn(Optional.of("some_test_shop"));

        // mock cataloger
        when(catalogerClient.getBrandName(brandId)).thenReturn(Optional.of("somevendor"));

        executor.sendNewAnswers();

        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient).sendBotMessage(eq(chatIdAll), arg.capture());

        assertEquals(String.format(
            "Новые ответы на вопрос #%s\n" +
                "\n" +
                "<i>question 1</i>\n" +
                "___________________________\n" +
                "<b>Ответы:</b>\n" +
                "- Пользователь 1234 <b>(somelogin)</b>\n" +
                "<i>answer 1</i>\n" +
                "\n" +
                "- Магазин 6 <b>(some_test_shop)</b>\n" +
                "<i>answer 2 shop</i>\n" +
                "\n" +
                "- Пользователь 1235\n" +
                "<i>answer 3</i>\n" +
                "\n" +
                "- Магазин 7\n" +
                "<i>answer 4 shop</i>\n" +
                "\n" +
                "- Вендор 45 <b>(somevendor)</b>\n" +
                "<i>answer 5 vendor</i>\n" +
                "\n" +
                "- Вендор 46\n" +
                "<i>answer 6 vendor</i>\n" +
                "\n" +
                "___________________________\n" +
                "Все ответы по ссылке:\n" +
                "localhost/product/1/question/%s",
            questions[0].getId(),
            questions[0].getId()
        ), arg.getValue());

        verify(telegramBotClient).sendBotMessage(eq(chatIdShop), arg.capture());

        assertEquals(String.format(
            "Новые ответы на вопрос #%s\n" +
                "\n" +
                "<i>question 1</i>\n" +
                "___________________________\n" +
                "<b>Ответы:</b>\n" +
                "- Магазин 6 <b>(some_test_shop)</b>\n" +
                "<i>answer 2 shop</i>\n" +
                "\n" +
                "- Магазин 7\n" +
                "<i>answer 4 shop</i>\n" +
                "\n" +
                "___________________________\n" +
                "Все ответы по ссылке:\n" +
                "localhost/product/1/question/%s",
            questions[0].getId(),
            questions[0].getId()
        ), arg.getValue());
    }

    @Test
    void testMonitoring() throws Exception {
        int shopId = 6;
        int modelId = 1;

        executor.getLastProcessed();

        Question question = questionService.createModelQuestion((int) FAKE_USER, "question 1", modelId);
        String configKeyAll = AbstractExportToTelegramExecutor.LAST_SUCCESS_PREF + chatIdAll;
        String configKeyShop = AbstractExportToTelegramExecutor.LAST_SUCCESS_PREF + chatIdShop;
        TelegramResponse telegramResponse = new TelegramResponse();
        TemporalAmount offset = ExportNewAnswersToTelegramExecutor.UNSUCCESSFUL_PERIOD;
        LocalDateTime someOldDate = LocalDateTime.now().minus(offset).minusDays(1L);

        // happy path
        answerService.createAnswer((int) FAKE_USER, "answer 1", question.getId());
        answerService.createShopAnswer((int) FAKE_USER + 1, "answer 1 shop", question.getId(), shopId);
        configurationService.mergeValue(configKeyAll, someOldDate);
        configurationService.mergeValue(configKeyShop, someOldDate);
        telegramResponse.setOk(true);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);
        executor.sendNewAnswers();

        // assert that date has been changed
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyAll))
            .isAfter(someOldDate));
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyShop))
            .isAfter(someOldDate));

        // Telegram does not returns OK and last success date is long ago enough to throw exception
        answerService.createAnswer((int) FAKE_USER, "answer 2", question.getId());
        answerService.createShopAnswer((int) FAKE_USER + 1, "answer 2 shop", question.getId(), shopId);
        configurationService.mergeValue(configKeyAll, someOldDate);
        configurationService.mergeValue(configKeyShop, someOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Should throw exception because last success delivery was too long ago
        Assertions.assertThrows(RuntimeException.class, () -> executor.sendNewAnswers());

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyAll))
            .isBefore(someOldDate));
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyShop))
            .isBefore(someOldDate));

        // Telegram does not returns OK and last success date is NOT long ago enough to throw exception
        answerService.createAnswer((int) FAKE_USER, "answer 3", question.getId());
        answerService.createShopAnswer((int) FAKE_USER + 1, "answer 3 shop", question.getId(), shopId);
        LocalDateTime shiftedOldDate = someOldDate.plusDays(2L);
        configurationService.mergeValue(configKeyAll, shiftedOldDate);
        configurationService.mergeValue(configKeyShop, shiftedOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Shouldn't throw exception
        executor.sendNewAnswers();

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyAll))
            .isBefore(shiftedOldDate));
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKeyShop))
            .isBefore(shiftedOldDate));
    }
}

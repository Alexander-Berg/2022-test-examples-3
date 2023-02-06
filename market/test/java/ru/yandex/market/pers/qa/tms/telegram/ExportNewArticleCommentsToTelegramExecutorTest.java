package ru.yandex.market.pers.qa.tms.telegram;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mbo.cms.ArticleInfo;
import ru.yandex.market.mbo.cms.ArticleInfoWrapper;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.telegram.TelegramBotClient;
import ru.yandex.market.telegram.TelegramResponse;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.qa.tms.telegram.ExportNewArticleCommentsToTelegramExecutor.THRESHOLD;

class ExportNewArticleCommentsToTelegramExecutorTest extends PersQaTmsTest {

    private static final long FAKE_USER = 1234;

    @Autowired
    private ExportNewArticleCommentsToTelegramExecutor executor;

    @Autowired
    private TelegramBotClient telegramBotClient;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("mboCmsRestTemplate")
    RestTemplate restTemplate;

    @Value("${pers.qa.telegram.export.comments.article.group}")
    private String chatId;

    private void mockMboCmsOnGetInfoMethod() {
        Mockito.when(restTemplate.getForEntity(anyString(), any())).thenAnswer(invocation -> {
            List<NameValuePair> queryParams = new URIBuilder((String) invocation.getArgument(0)).getQueryParams();
            long pageId = queryParams.stream()
                    .filter(nameValuePair -> "page_id".equals(nameValuePair.getName()))
                    .map(NameValuePair::getValue)
                    .map(Long::valueOf)
                    .findAny()
                    .orElseThrow(IllegalStateException::new);
            if (pageId == 3) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(createArticleInfoWrapper(pageId));
            }
        });
    }

    private ArticleInfoWrapper createArticleInfoWrapper(long pageId) {
        ArticleInfo articleInfo = new ArticleInfo(pageId, "Cтатья", "type", "semanticId", Collections.emptyList());
        return new ArticleInfoWrapper(Collections.singletonList(articleInfo));
    }

    @Test
    void testSendNewArticleComments() {
        CommentProject project = CommentProject.ARTICLE;
        commentService.createComment(project, FAKE_USER, "text", 1);
        commentService.createComment(project, FAKE_USER, "text2", 1);
        commentService.createComment(project, FAKE_USER, "question?", 1);
        commentService.createComment(project, FAKE_USER, "new_text", 2);
        commentService.createComment(project, FAKE_USER, "comment_3", 3);
        mockMboCmsOnGetInfoMethod();
        executor.sendNewArticleComments();
        verify(telegramBotClient, times(2)).sendBotMessage(eq(chatId), anyString());
    }

    @Test
    void testMessageText() {
        CommentProject project = CommentProject.ARTICLE;
        commentService.createComment(project, FAKE_USER, "text1\ntext1", 1);
        commentService.createComment(project, FAKE_USER, "question1?", 1);
        mockMboCmsOnGetInfoMethod();
        executor.sendNewArticleComments();
        String EXPECTED_MESSAGE =
            "<b>Новые комментарии к статье:</b>\n" +
            "Cтатья\n" +
            "\n" +
            "<b>Комментарии с вопросами:</b>\n" +
            "question1?\n" +
            "___________________________\n" +
            "\n" +
            "<b>Прочие комментарии:</b>\n" +
            "text1\n" +
            "text1\n" +
            "___________________________\n" +
            "\n" +
            "<b>Просмотреть комментарии можно по ссылке:</b>\n" +
            "localhost/journal/type/semanticId";
        verify(telegramBotClient).sendBotMessage(eq(chatId), eq(EXPECTED_MESSAGE));
    }

    @Test
    void testThreeCommentsTextsAreExceedThreshold() {
        CommentProject project = CommentProject.ARTICLE;
        String comment = new Random().ints(THRESHOLD, 'a', 'z')
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        commentService.createComment(project, FAKE_USER, comment, 1);
        commentService.createComment(project, FAKE_USER, "This comment will be sent", 1);
        commentService.createComment(project, FAKE_USER, "And this will be sent too", 1);
        mockMboCmsOnGetInfoMethod();
        executor.sendNewArticleComments();
        verify(telegramBotClient).sendBotMessage(eq(chatId), anyString());
    }

    @Test
    void testCommentTextIsExceedThreshold() {
        CommentProject project = CommentProject.ARTICLE;
        String comment = new Random().ints(THRESHOLD + 1, 'a', 'z')
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        commentService.createComment(project, FAKE_USER, comment, 1);
        mockMboCmsOnGetInfoMethod();
        Assertions.assertThrows(RuntimeException.class, () -> executor.sendNewArticleComments());
    }

    @Test
    void testMonitoring() throws Exception {
        CommentProject project = CommentProject.ARTICLE;
        mockMboCmsOnGetInfoMethod();
        String configKey = AbstractExportToTelegramExecutor.LAST_SUCCESS_PREF + chatId;
        TelegramResponse telegramResponse = new TelegramResponse();
        TemporalAmount offset = ExportNewArticleCommentsToTelegramExecutor.UNSUCCESSFUL_PERIOD;
        LocalDateTime someOldDate = LocalDateTime.now().minus(offset).minusDays(1L);

        // happy path
        commentService.createComment(project, FAKE_USER, "text1", 1);
        configurationService.mergeValue(configKey, someOldDate);
        telegramResponse.setOk(true);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);
        executor.sendNewArticleComments();

        // assert that date has been changed
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isAfter(someOldDate));

        // Telegram does not returns OK and last success date is long ago enough to throw exception
        commentService.createComment(project, FAKE_USER, "text2", 1);
        configurationService.mergeValue(configKey, someOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Should throw exception because last success delivery was too long ago
        Assertions.assertThrows(RuntimeException.class, () -> executor.sendNewArticleComments());

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isBefore(someOldDate));

        // Telegram does not returns OK and last success date is NOT long ago enough to throw exception
        commentService.createComment(project, FAKE_USER, "text2", 1);
        LocalDateTime shiftedOldDate = someOldDate.plusDays(2L);
        configurationService.mergeValue(configKey, shiftedOldDate);
        telegramResponse.setOk(false);
        when(telegramBotClient.sendBotMessage(any(), any())).thenReturn(telegramResponse);

        // Shouldn't throw exception
        executor.sendNewArticleComments();

        // assert that date has not been changed.
        // .isBefore() because save and read from ConfigService trims nanoseconds, dunno why
        Assertions.assertTrue(Objects.requireNonNull(configurationService.getValueAsLocalDateTime(configKey))
            .isBefore(shiftedOldDate));
    }
}

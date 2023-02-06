package ru.yandex.market.acw.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.acw.api.Automation;
import ru.yandex.market.acw.api.CwTextCheckType;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.api.Text.TextVerdict;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.internal.CleanWebTextResponseProcessor;
import ru.yandex.market.acw.jooq.enums.Status;
import ru.yandex.market.acw.jooq.tables.pojos.TextCache;
import ru.yandex.market.acw.jooq.tables.pojos.TextQueue;
import ru.yandex.market.acw.json.CWDocumentRequest;
import ru.yandex.market.acw.json.CWDocumentResponse;
import ru.yandex.market.acw.json.CWRawResult;
import ru.yandex.market.acw.json.CWRawVerdict;
import ru.yandex.market.acw.json.CWTextParams;
import ru.yandex.market.acw.internal.CleanWebService;
import ru.yandex.market.acw.utils.ProtoUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendTextRequestTaskTest extends Base {

    private static final long ASYNC_VERDICT_REQUEST_ID = 1L;
    private static final long AUTO_VERDICT_REQUEST_ID = 3L;

    private CleanWebService cleanWebService;
    private SendTextRequestTask sendTextRequestTask;
    private CleanWebTextResponseProcessor responseProcessor;

    @BeforeEach
    void setup() {
        cleanWebService = mock(CleanWebService.class);
        responseProcessor = new CleanWebTextResponseProcessor(textCacheDao, textQueueDao);
        var docResponse1 = new CWDocumentResponse(ASYNC_VERDICT_REQUEST_ID);
        var docResponse2 = new CWDocumentResponse(2L);
        var docResponse3 = new CWDocumentResponse(3L);
        docResponse1.setResult(new CWRawResult(List.of(
                new CWRawVerdict(String.valueOf(ASYNC_VERDICT_REQUEST_ID), "need_async", "true", null, null,  null)), null, null));
        docResponse2.setResult(new CWRawResult(List.of(
                new CWRawVerdict("2", "need_async", "true", null, null,  null)), null, null));
        docResponse3.setResult(new CWRawResult(List.of(
                new CWRawVerdict(String.valueOf(AUTO_VERDICT_REQUEST_ID), "text_auto_good", "true", null, null,  null),
                new CWRawVerdict(String.valueOf(AUTO_VERDICT_REQUEST_ID), "clean_web_moderation_end", "true", null, null,  null)), null, null));

        when(cleanWebService.check(argThat(new AsyncVerdictResponseList()))).thenReturn(List.of(docResponse1, docResponse2));
        when(cleanWebService.check(argThat(new AutoVerdictResponseList()))).thenReturn(List.of(docResponse3));
        sendTextRequestTask = new SendTextRequestTask(cleanWebService, responseProcessor, textCacheDao, textQueueDao, 60);
    }

    @Test
    @DisplayName("send text request to CW")
    void sendTextRequestToCW() {
        textQueueDao.insert(List.of(
                new TextQueue(1L, UUID.randomUUID(), LocalDateTime.now(), Status.NEW, RequestMode.DEFAULT.name(), "text1"),
                new TextQueue(2L, UUID.randomUUID(), LocalDateTime.now(), Status.NEW, RequestMode.DEFAULT.name(), "text2")
        ));

        sendTextRequestTask.execute(null);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);

        var textRequest1 = (CWDocumentRequest<CWTextParams>) captor.getValue().stream()
                .filter(image -> ((CWDocumentRequest<CWTextParams>) image).getId() == 1L)
                .findFirst()
                .get();

        assertThat(textRequest1.getCwParams().getBody().getCheckList()).containsExactlyInAnyOrder(
                getFormattedChecks(RequestMode.DEFAULT, Set.of()));

        var textQueueItems = textQueueDao.fetchById(1L, 2L);
        assertThat(textQueueItems.size()).isEqualTo(2);
        assertThat(textQueueItems.stream().allMatch(item -> item.getStatus() == Status.WAITING_RESPONSE)).isTrue();
    }

    @Test
    @DisplayName("send text request if intim to CW")
    void sendRequestIfIntim() {
        textQueueDao.insert(List.of(
                new TextQueue(1L, UUID.randomUUID(), LocalDateTime.now(), Status.NEW, RequestMode.INTIM.name(), "text1"),
                new TextQueue(2L, UUID.randomUUID(), LocalDateTime.now(), Status.NEW, RequestMode.INTIM.name(), "text2")
        ));

        textCacheDao.insert(List.of(
                new TextCache(3L, UUID.randomUUID(), LocalDateTime.now(),
                        Text.TextVerdictResult.newBuilder()
                                .setAutomationLevel(Automation.AutomationLevel.AUTO_ONLY)
                                .build()),
                new TextCache(4L, UUID.randomUUID(), LocalDateTime.now(),
                        Text.TextVerdictResult.newBuilder()
                                .setAutomationLevel(Automation.AutomationLevel.AUTO_ONLY)
                                .build())));

        sendTextRequestTask.execute(null);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);
        assertThat(captor.getValue().stream()
                .allMatch(text -> ((CWDocumentRequest<CWTextParams>) text).getCwParams().getBody().getAutomationLevels()
                        .values().stream().allMatch(level -> level.equals("human_only"))));
    }

    @Test
    @DisplayName("update existing cache if autoverdict")
    void updateExistingCache() {
        var autoVerdictHash = UUID.randomUUID();
        textQueueDao.insert(List.of(
                new TextQueue(AUTO_VERDICT_REQUEST_ID, autoVerdictHash, LocalDateTime.now(), Status.NEW, RequestMode.INTIM.name(), "text3")
        ));

        textCacheDao.insert(List.of(
                new TextCache(AUTO_VERDICT_REQUEST_ID, autoVerdictHash, LocalDateTime.now(),
                        Text.TextVerdictResult.newBuilder()
                                .setAutomationLevel(Automation.AutomationLevel.AUTO_ONLY)
                                .addAllVerdicts(List.of(TextVerdict.TEXT_TOLOKA_SPAM))
                                .build())));

        sendTextRequestTask.execute(null);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isOne();
        assertThat(((CWDocumentRequest<CWTextParams>) captor.getValue().get(0)).getId()).isEqualTo(AUTO_VERDICT_REQUEST_ID);

        assertThat(textQueueDao.existsById(AUTO_VERDICT_REQUEST_ID)).isFalse();

        var cachedText = textCacheDao.fetchById(AUTO_VERDICT_REQUEST_ID);
        assertThat(cachedText.size()).isOne();
        assertThat(cachedText.get(0).getData()).isNotNull();
        assertThat(cachedText.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(
                TextVerdict.TEXT_TOLOKA_SPAM,
                TextVerdict.TEXT_AUTO_GOOD,
                TextVerdict.CLEAN_WEB_MODERATION_END);
    }

    private String[] getFormattedChecks(RequestMode mode, Set<CwTextCheckType> excludedChecks) {
        return Iterables.toArray(ProtoUtils.REQUEST_MODE_TEXT_MAP.get(mode).stream()
                .filter(check -> !excludedChecks.contains(check))
                .map(check -> check.name().toLowerCase())
                .toList(), String.class);
    }

    class AutoVerdictResponseList implements ArgumentMatcher<List<CWDocumentRequest<CWTextParams>>> {
        public boolean matches(List<CWDocumentRequest<CWTextParams>> list) {
            return list != null && ((List) list).stream()
                    .anyMatch(item -> ((CWDocumentRequest<CWTextParams>) item).getId() == AUTO_VERDICT_REQUEST_ID);
        }
    }

    class AsyncVerdictResponseList implements ArgumentMatcher<List<CWDocumentRequest<CWTextParams>>> {
        public boolean matches(List<CWDocumentRequest<CWTextParams>> list) {
            return list != null && ((List) list).stream()
                    .anyMatch(item -> ((CWDocumentRequest<CWTextParams>) item).getId() == ASYNC_VERDICT_REQUEST_ID);
        }
    }
}

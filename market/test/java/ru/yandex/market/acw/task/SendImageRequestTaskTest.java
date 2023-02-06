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

import ru.yandex.market.acw.api.CheckImagePayload;
import ru.yandex.market.acw.api.CwImageCheckType;
import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.internal.CleanWebImageResponseProcessor;
import ru.yandex.market.acw.jooq.enums.Status;
import ru.yandex.market.acw.jooq.tables.pojos.ImageCache;
import ru.yandex.market.acw.jooq.tables.pojos.ImageQueue;
import ru.yandex.market.acw.json.CWDocumentRequest;
import ru.yandex.market.acw.json.CWDocumentResponse;
import ru.yandex.market.acw.json.CWImageParams;
import ru.yandex.market.acw.json.CWRawResult;
import ru.yandex.market.acw.json.CWRawVerdict;
import ru.yandex.market.acw.internal.CleanWebService;
import ru.yandex.market.acw.service.TaskPropertiesService;
import ru.yandex.market.acw.utils.ProtoUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.acw.api.Image.ImageVerdict.CLEAN_WEB_MODERATION_END;
import static ru.yandex.market.acw.api.Image.ImageVerdict.COMMON_IMAGE_TOLOKA_AUTO_GOOD;
import static ru.yandex.market.acw.api.Image.ImageVerdict.NO_WATERMARKS;
import static ru.yandex.market.acw.api.Image.ImageVerdict.WATERMARK_CLEAN;
import static ru.yandex.market.acw.task.SendImageRequestTask.TURN_OFF_IMAGE_SENDING_PROPERTY;

public class SendImageRequestTaskTest extends Base {

    private static final long ASYNC_VERDICT_REQUEST_ID = 1L;
    private static final long AUTO_VERDICT_REQUEST_ID = 3L;

    private SendImageRequestTask sendImageRequestTask;
    private CleanWebService cleanWebService;
    private CleanWebImageResponseProcessor responseProcessor;
    private TaskPropertiesService taskPropertiesService;

    @BeforeEach
    void setup() {
        cleanWebService = mock(CleanWebService.class);
        responseProcessor = new CleanWebImageResponseProcessor(imageCacheDao, imageQueueDao);
        taskPropertiesService = mock(TaskPropertiesService.class);
        when(taskPropertiesService.getProperty(TURN_OFF_IMAGE_SENDING_PROPERTY, Boolean.class)).thenReturn(false);
        var docResponse1 = new CWDocumentResponse(ASYNC_VERDICT_REQUEST_ID);
        var docResponse2 = new CWDocumentResponse(2L);
        var docResponse3 = new CWDocumentResponse(AUTO_VERDICT_REQUEST_ID);
        docResponse1.setResult(new CWRawResult(List.of(
                new CWRawVerdict(String.valueOf(ASYNC_VERDICT_REQUEST_ID), "need_async", "true", null, null,  null)), null, null));
        docResponse2.setResult(new CWRawResult(List.of(
                new CWRawVerdict("2", "need_async", "true", null, null,  null)), null, null));
        docResponse3.setResult(new CWRawResult(List.of(
                new CWRawVerdict(String.valueOf(AUTO_VERDICT_REQUEST_ID), "common_image_toloka_auto_good", "true", null, null, null),
                new CWRawVerdict(String.valueOf(AUTO_VERDICT_REQUEST_ID), "clean_web_moderation_end", "true", null, null, null)), null, null
        ));

        when(cleanWebService.check(argThat(new AsyncVerdictResponseList()))).thenReturn(List.of(docResponse1, docResponse2));
        when(cleanWebService.check(argThat(new AutoVerdictResponseList()))).thenReturn(List.of(docResponse3));
        sendImageRequestTask = new SendImageRequestTask(cleanWebService, responseProcessor, imageCacheDao,
                imageQueueDao, taskPropertiesService,60);
    }

    @Test
    @DisplayName("send image request to CW")
    void sendImageRequestToCW() {
        imageQueueDao.insert(List.of(
                new ImageQueue(1L, "//idx_url1", "mbo_url1", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build()),
                new ImageQueue(2L, "idx_url2", "mbo_url2", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title2").build())));

        sendImageRequestTask.execute(null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);
        var imageRequest1 = (CWDocumentRequest<CWImageParams>) captor.getValue().stream()
                .filter(image -> ((CWDocumentRequest<CWImageParams>) image).getId() == 1L)
                .findFirst()
                .get();

        assertThat(imageRequest1.getCwParams().getBody().getCheckList()).containsExactlyInAnyOrder(
                getFormattedChecks(RequestMode.DEFAULT, Set.of()));
        assertThat(imageRequest1.getCwParams().getBody().getImageUrl()).isEqualTo("http://idx_url1");

        List<ImageQueue> imageQueueItems = imageQueueDao.fetchById(1L, 2L);
        assertThat(imageQueueItems.size()).isEqualTo(2);
        assertThat(imageQueueItems.stream().allMatch(item -> item.getStatus() == Status.WAITING_RESPONSE)).isTrue();
    }

    @Test
    @DisplayName("send filtered checks")
    void sendFilteredChecks() {
        imageQueueDao.insert(List.of(
                new ImageQueue(1L, "idx_url1", "mbo_url1", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build()),
                new ImageQueue(2L, "idx_url2", "mbo_url2", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title2").build())));

        imageCacheDao.insert(List.of(
                new ImageCache(1L, "idx_url1", "mbo_url1", null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_CLEAN))
                                .build()),
                new ImageCache(2L, "idx_url2", "mbo_url2", null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(NO_WATERMARKS))
                                .build())));

        sendImageRequestTask.execute(null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);
        var imageRequest1 = (CWDocumentRequest<CWImageParams>) captor.getValue().stream()
                .filter(image -> ((CWDocumentRequest<CWImageParams>) image).getId() == 1L)
                .findFirst()
                .get();
        var imageRequest2 = (CWDocumentRequest<CWImageParams>) captor.getValue().stream()
                .filter(image -> ((CWDocumentRequest<CWImageParams>) image).getId() == 2L)
                .findFirst()
                .get();

        assertThat(imageRequest1.getCwParams().getBody().getCheckList()).containsExactlyInAnyOrder(
                getFormattedChecks(RequestMode.DEFAULT, Set.of(CwImageCheckType.WATERMARK_IMAGE_TOLOKA)));

        assertThat(imageRequest2.getCwParams().getBody().getCheckList()).containsExactlyInAnyOrder(
                getFormattedChecks(RequestMode.DEFAULT, Set.of(CwImageCheckType.WATERMARK_IMAGE_TOLOKA)));

        List<ImageQueue> imageQueueItems = imageQueueDao.fetchById(1L, 2L);
        assertThat(imageQueueItems.size()).isEqualTo(2);
        assertThat(imageQueueItems.stream().allMatch(item -> item.getStatus() == Status.WAITING_RESPONSE)).isTrue();
    }

    @Test
    @DisplayName("update existing cache if autoverdict")
    void updateExistingCache() {
        imageQueueDao.insert(List.of(
                new ImageQueue(AUTO_VERDICT_REQUEST_ID, "idx_url1", "mbo_url1", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build())));

        imageCacheDao.insert(List.of(
                new ImageCache(AUTO_VERDICT_REQUEST_ID, "idx_url1", "mbo_url1", null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_CLEAN))
                                .build())));

        sendImageRequestTask.execute(null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(1);
        assertThat(((CWDocumentRequest<CWImageParams>) captor.getValue().get(0)).getId()).isEqualTo(AUTO_VERDICT_REQUEST_ID);

        assertThat(imageQueueDao.existsById(AUTO_VERDICT_REQUEST_ID)).isFalse();

        var cachedImage = imageCacheDao.fetchById(AUTO_VERDICT_REQUEST_ID);
        assertThat(cachedImage.size()).isOne();
        assertThat(cachedImage.get(0).getData()).isNotNull();
        assertThat(cachedImage.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(
                WATERMARK_CLEAN,
                COMMON_IMAGE_TOLOKA_AUTO_GOOD,
                CLEAN_WEB_MODERATION_END);
    }

    @Test
    @DisplayName("update null cache fields if new request has some")
    void updateNullCacheFields() {
        var newUUID = UUID.randomUUID();
        imageQueueDao.insert(List.of(
                new ImageQueue(AUTO_VERDICT_REQUEST_ID, "idx_url1", "mbo_url1", newUUID, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build())));

        imageCacheDao.insert(List.of(
                new ImageCache(AUTO_VERDICT_REQUEST_ID, "idx_url1", null, null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_CLEAN))
                                .build())));

        var cachedImage = imageCacheDao.fetchById(AUTO_VERDICT_REQUEST_ID);
        assertThat(cachedImage.get(0).getData()).isNotNull();
        assertThat(cachedImage.get(0).getIdxUrl()).isEqualTo("idx_url1");
        assertThat(cachedImage.get(0).getMboUrl()).isNull();
        assertThat(cachedImage.get(0).getHash()).isNull();

        sendImageRequestTask.execute(null);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cleanWebService).check(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(1);
        assertThat(((CWDocumentRequest<CWImageParams>) captor.getValue().get(0)).getId()).isEqualTo(AUTO_VERDICT_REQUEST_ID);

        var updatedCachedImage = imageCacheDao.fetchById(AUTO_VERDICT_REQUEST_ID);
        assertThat(updatedCachedImage.get(0).getData()).isNotNull();
        assertThat(updatedCachedImage.get(0).getIdxUrl()).isEqualTo("idx_url1");
        assertThat(updatedCachedImage.get(0).getMboUrl()).isEqualTo("mbo_url1");
        assertThat(updatedCachedImage.get(0).getHash()).isEqualTo(newUUID);
    }

    private String[] getFormattedChecks(RequestMode mode, Set<CwImageCheckType> excludedChecks) {
        return Iterables.toArray(ProtoUtils.REQUEST_MODE_IMAGE_MAP.get(mode).stream()
                .filter(check -> !excludedChecks.contains(check))
                .map(check -> check.name().toLowerCase())
                .toList(), String.class);
    }

    class AutoVerdictResponseList implements ArgumentMatcher<List<CWDocumentRequest<CWImageParams>>> {
        public boolean matches(List<CWDocumentRequest<CWImageParams>> list) {
            return list != null && ((List) list).stream()
                    .anyMatch(item -> ((CWDocumentRequest<CWImageParams>) item).getId() == AUTO_VERDICT_REQUEST_ID);
        }
    }

    class AsyncVerdictResponseList implements ArgumentMatcher<List<CWDocumentRequest<CWImageParams>>> {
        public boolean matches(List<CWDocumentRequest<CWImageParams>> list) {
            return list != null && ((List) list).stream()
                    .anyMatch(item -> ((CWDocumentRequest<CWImageParams>) item).getId() == ASYNC_VERDICT_REQUEST_ID);
        }
    }
}

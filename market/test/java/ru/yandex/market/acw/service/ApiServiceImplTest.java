package ru.yandex.market.acw.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.acw.api.CheckImageParameters;
import ru.yandex.market.acw.api.CheckImagesRequest;
import ru.yandex.market.acw.api.CheckMode;
import ru.yandex.market.acw.api.CheckTextParameters;
import ru.yandex.market.acw.api.CheckTextsRequest;
import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.Image.ImageVerdictResult;
import ru.yandex.market.acw.api.OverrideImageVerdictsRequest;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.db.daos.ExtendedImageCacheDao;
import ru.yandex.market.acw.db.daos.ExtendedImageQueueDao;
import ru.yandex.market.acw.db.daos.ExtendedTextCacheDao;
import ru.yandex.market.acw.db.daos.ExtendedTextQueueDao;
import ru.yandex.market.acw.jooq.tables.pojos.ImageCache;
import ru.yandex.market.acw.jooq.tables.pojos.TextCache;
import ru.yandex.market.acw.utils.ProtoUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.acw.api.OverrideImageVerdictsResponse.Verdict.Status.IMAGE_NOT_FOUND;
import static ru.yandex.market.acw.api.OverrideImageVerdictsResponse.Verdict.Status.OK;
import static ru.yandex.market.acw.api.Text.TextVerdict.*;

class ApiServiceImplTest {


    private static final String URL_1 = "idx_url1";
    private static final String MBO_URL_1 = "mbo_url1";
    private static final String URL_2 = "url2";
    private static final String URL_3 = "url3";
    private static final String URL_5 = "idx_url5";
    private static final UUID IMAGE_HASH_1 = UUID.randomUUID();
    private static final LocalDateTime LOCAL_DATE_TIME_1 = LocalDateTime.now();
    private static final long BUSINESS_ID_1 = 1L;
    private static final long BUSINESS_ID_2 = 2L;
    private static final long BUSINESS_ID_3 = 3L;
    private static final String OFFER_ID_1 = "offer1";
    private static final String OFFER_ID_2 = "offer2";
    private static final String OFFER_ID_3 = "offer3";

    private static final CheckImageParameters IMAGE_PARAM_1 = CheckImageParameters.newBuilder()
            .setIdxUrl(URL_1)
            .setMboUrl(MBO_URL_1)
            .setRequestMode(RequestMode.DEFAULT)
            .setMboUrlHash("hash1")
            .build();

    private static final CheckImageParameters IMAGE_PARAM_2 = CheckImageParameters.newBuilder()
            .setIdxUrl(URL_2)
            .setMboUrl(URL_2)
            .setRequestMode(RequestMode.DEFAULT)
            .setMboUrlHash("hash22")
            .build();

    private static final CheckImageParameters IMAGE_PARAM_3 = CheckImageParameters.newBuilder()
            .setIdxUrl(URL_3)
            .setMboUrl(URL_3)
            .setRequestMode(RequestMode.DEFAULT)
            .setMboUrlHash("hash3")
            .build();

    private static final CheckImageParameters IMAGE_PARAM_4 = CheckImageParameters.newBuilder()
            .setIdxUrl(URL_1)
            .setMboUrl(MBO_URL_1)
            .setRequestMode(RequestMode.DEFAULT)
            .setMboUrlHash("")
            .build();

    private static final CheckImageParameters IMAGE_PARAM_5 = CheckImageParameters.newBuilder()
            .setIdxUrl(URL_5)
            .setMboUrl(MBO_URL_1)
            .setRequestMode(RequestMode.DEFAULT)
            .setMboUrlHash("hash1")
            .build();

    private static final CheckTextParameters TEXT_PARAM_1 = CheckTextParameters.newBuilder()
            .setBusinessId(BUSINESS_ID_1)
            .setOfferId(OFFER_ID_1)
            .setText("Some random text")
            .build();

    private static final CheckTextParameters TEXT_PARAM_2 = CheckTextParameters.newBuilder()
            .setBusinessId(BUSINESS_ID_2)
            .setOfferId(OFFER_ID_2)
            .setText("Some random text")
            .build();

    private static final CheckTextParameters TEXT_PARAM_3 = CheckTextParameters.newBuilder()
            .setBusinessId(BUSINESS_ID_3)
            .setOfferId(OFFER_ID_3)
            .setText("Some random text_3")
            .build();


    private ApiServiceImpl apiService;
    private ExtendedImageQueueDao imageQueueDao;
    private ExtendedTextQueueDao textQueueDao;
    private ExtendedImageCacheDao imageCacheDao;

    @BeforeEach
    void setUp() {
        var textCacheDao = mock(ExtendedTextCacheDao.class);
        imageCacheDao = mock(ExtendedImageCacheDao.class);
        imageQueueDao = mock(ExtendedImageQueueDao.class);
        textQueueDao = mock(ExtendedTextQueueDao.class);

        var imageResult = ImageVerdictResult.newBuilder()
                .addAllVerdicts(ProtoUtils.getImageVerdictResult(RequestMode.DEFAULT, ProtoUtils.GOOD_VERDICTS))
                .build();
        var textResult = Text.TextVerdictResult.newBuilder()
                .addAllVerdicts(List.of(TEXT_AUTO_GOOD))
                .build();

        when(imageCacheDao.getImageVerdicts(IMAGE_PARAM_1)).thenReturn(new ArrayList<>());
        when(imageCacheDao.getImageVerdicts(IMAGE_PARAM_2)).thenReturn(List.of(
                new ImageCache(2L, URL_2, URL_2, UUID.randomUUID(), LocalDateTime.now(), imageResult)));
        when(imageCacheDao.getImageVerdicts(IMAGE_PARAM_3)).thenReturn(new ArrayList<>());
        when(imageCacheDao.fetchByIdxUrl(any())).thenReturn(List.of(
                new ImageCacheTest(1L, URL_2, URL_2, IMAGE_HASH_1, LOCAL_DATE_TIME_1, imageResult)));

        when(imageQueueDao.exists(IMAGE_PARAM_3)).thenReturn(true);
        when(textQueueDao.exists(TEXT_PARAM_3)).thenReturn(true);

        when(textCacheDao.getTextVerdicts(TEXT_PARAM_1)).thenReturn(Optional.empty());
        when(textCacheDao.getTextVerdicts(TEXT_PARAM_2)).thenReturn(Optional.of(
                new TextCache(1L, UUID.randomUUID(), LocalDateTime.now(), textResult)));
        when(textCacheDao.getTextVerdicts(TEXT_PARAM_3)).thenReturn(Optional.empty());

        apiService = new ApiServiceImpl(imageCacheDao, imageQueueDao, textCacheDao, textQueueDao);
    }

    @Nested
    @DisplayName("check image")
    class CheckImage {

        @Test
        @DisplayName("check image without adding to the queue")
        void checkOnly() {
            var request = CheckImagesRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_ONLY)
                    .addAllImages(List.of(IMAGE_PARAM_1, IMAGE_PARAM_2))
                    .build();

            var response = apiService.checkImage(request);

            assertThat(response.getImageVerdictsCount()).isEqualTo(2);
            assertThat(response.getImageVerdictsList()
                    .stream()
                    .filter(image -> image.getMboUrl().equals(URL_2))
                    .findFirst().get()
                    .getVerdictsList())
                    .containsExactly(Iterables.toArray(ProtoUtils
                            .getImageVerdictResult(RequestMode.DEFAULT, ProtoUtils.GOOD_VERDICTS),
                            Image.ImageVerdict.class));

            assertThat(response.getImageVerdictsList()
                    .stream()
                    .filter(image -> image.getMboUrl().equals(MBO_URL_1))
                    .findFirst().get()
                    .getVerdictsList())
                    .isEmpty();
            verify(imageQueueDao, times(0)).insert(anyList());
        }

        @Test
        @DisplayName("check image and add to the queue")
        void checkAndSend() {
            var request = CheckImagesRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_AND_SEND)
                    .addAllImages(List.of(IMAGE_PARAM_1, IMAGE_PARAM_2))
                    .build();

            var response = apiService.checkImage(request);

            assertThat(response.getImageVerdictsCount()).isEqualTo(2);
            verify(imageQueueDao, times(1)).insert(anyList());
        }

        @Test
        @DisplayName("check image when it's already been added to the queue")
        void checkAlreadyInQueue() {
            var request = CheckImagesRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_AND_SEND)
                    .addAllImages(List.of(IMAGE_PARAM_2, IMAGE_PARAM_3))
                    .build();

            var response = apiService.checkImage(request);

            assertThat(response.getImageVerdictsCount()).isEqualTo(2);
            verify(imageQueueDao, times(0)).insert(anyList());
        }

        @Test
        @DisplayName("check image and don't add duplicates to the queue")
        void checkAndSendNoDuplicates() {
            var request = CheckImagesRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_AND_SEND)
                    .addAllImages(List.of(IMAGE_PARAM_1, IMAGE_PARAM_4, IMAGE_PARAM_5))
                    .build();

            var response = apiService.checkImage(request);

            assertThat(response.getImageVerdictsCount()).isEqualTo(3);
            ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
            verify(imageQueueDao).insert(captor.capture());
            assertThat(captor.getValue().size()).isOne();
        }
    }

    @Nested
    @DisplayName("check text")
    class CheckText {

        @Test
        @DisplayName("check text without adding to the queue")
        void checkOnly() {
            var request = CheckTextsRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_ONLY)
                    .addAllTexts(List.of(TEXT_PARAM_1, TEXT_PARAM_2))
                    .build();

            var response = apiService.checkText(request);

            assertThat(response.getTextVerdictsCount()).isEqualTo(2);
            assertThat(response.getTextVerdictsList()
                    .stream()
                    .filter(text -> text.getBusinessId() == BUSINESS_ID_2 && text.getOfferId().equals(OFFER_ID_2))
                    .findFirst().get()
                    .getVerdictsList()).containsExactly(TEXT_AUTO_GOOD);
            assertThat(response.getTextVerdictsList()
                    .stream()
                    .filter(text -> text.getBusinessId() == BUSINESS_ID_1 && text.getOfferId().equals(OFFER_ID_1))
                    .findFirst().get()
                    .getVerdictsList()).isEmpty();

            verify(textQueueDao, times(0)).insert(anyList());
        }

        @Test
        @DisplayName("check text and add to the queue")
        void checkAndSend() {
            var request = CheckTextsRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_AND_SEND)
                    .addAllTexts(List.of(TEXT_PARAM_1, TEXT_PARAM_2))
                    .build();

            var response = apiService.checkText(request);

            assertThat(response.getTextVerdictsCount()).isEqualTo(2);
            verify(textQueueDao, times(1)).insert(anyList());
        }

        @Test
        @DisplayName("check text when it's already been added to the queue")
        void checkAlreadyInQueue() {
            var request = CheckTextsRequest.newBuilder()
                    .setCheckMode(CheckMode.CHECK_AND_SEND)
                    .addAllTexts(List.of(TEXT_PARAM_2, TEXT_PARAM_3))
                    .build();

            var response = apiService.checkText(request);

            assertThat(response.getTextVerdictsCount()).isEqualTo(2);
            verify(textQueueDao, times(0)).insert(anyList());
        }
    }

    @Nested
    @DisplayName("override image")
    class OverrideImage {

        @Test
        @DisplayName("successful image verdicts override")
        void overrideImageVerdicts() {
            var request = OverrideImageVerdictsRequest.newBuilder()
                    .setVerdictRequest(OverrideImageVerdictsRequest.VerdictRequest.newBuilder()
                            .setRequestMode(RequestMode.DEFAULT)
                            .addAllUrl(List.of(URL_1, URL_2))
                            .build())
                    .build();

            var imageCache = new ImageCacheTest(1L, URL_2, URL_2, IMAGE_HASH_1, LOCAL_DATE_TIME_1,
                    ImageVerdictResult.newBuilder().addAllVerdicts(
                            ProtoUtils.getImageVerdictResult(RequestMode.DEFAULT, ProtoUtils.GOOD_VERDICTS))
                            .build());

            var response = apiService.overrideImageVerdicts(request);
            assertThat(response.getVerdictResponseCount()).isEqualTo(2);
            assertThat(response.getVerdictResponseList()
                    .stream()
                    .filter(image -> image.getUrl().equals(URL_1))
                    .findFirst().get()
                    .getStatus()).isEqualTo(IMAGE_NOT_FOUND);
            assertThat(response.getVerdictResponseList()
                    .stream()
                    .filter(image -> image.getUrl().equals(URL_2))
                    .findFirst().get()
                    .getStatus()).isEqualTo(OK);

            verify(imageCacheDao, times(1)).update(List.of(imageCache));
        }
    }

    class ImageCacheTest extends ImageCache {

        ImageCacheTest(long id, String idxUrl, String mboUrl, UUID hash, LocalDateTime insertDate,
                              ImageVerdictResult data) {
            super(id, idxUrl, mboUrl, hash, insertDate, data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ImageCache that = (ImageCache) o;
            return Objects.equals(getId(), that.getId()) && Objects.equals(getIdxUrl(), that.getIdxUrl()) && Objects.equals(getMboUrl(), that.getMboUrl()) && Objects.equals(getHash(), that.getHash()) && Objects.equals(getInsertDate(), that.getInsertDate()) && Objects.equals(getData(), that.getData());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getIdxUrl(), getMboUrl(), getHash(), getInsertDate(), getData());
        }
    }
}

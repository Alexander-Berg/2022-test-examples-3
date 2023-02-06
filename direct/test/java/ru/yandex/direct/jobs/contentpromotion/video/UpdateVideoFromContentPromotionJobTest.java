package ru.yandex.direct.jobs.contentpromotion.video;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.type.contentpromo.BannerWithContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.type.ContentPromotionCoreTypeSupportFacade;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.repository.TestContentPromotionRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.libs.video.VideoClient;
import ru.yandex.direct.libs.video.model.VideoBanner;
import ru.yandex.direct.test.utils.differ.LocalDateSecondsPrecisionDiffer;
import ru.yandex.direct.utils.HashingUtils;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.fromVideoBannerAsContent;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.realLifeVideoBanner;

/**
 * Тесты на джобу UpdateVideoContentNewJob
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateVideoFromContentPromotionJobTest {
    // MySQL возвращает JSON с пропертями не в том порядке, в каком они задавались, так что сравнивать неясно как,
    // для дат используем примерный матчер (до секунды), так как наносекунды в MySQL округляются
    private static final CompareStrategy COMPARE_STRATEGY = allFieldsExcept(newPath("metadata"))
            .forFields(
                    newPath("metadataModifyTime"),
                    newPath("metadataCreateTime"),
                    newPath("metadataRefreshTime")).useDiffer(new LocalDateSecondsPrecisionDiffer());

    @Autowired
    private Steps steps;
    @Autowired
    private BannerWithContentPromotionRepository bannerContentPromotionRepository;

    @Autowired
    private ContentPromotionRepository contentRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private ContentPromotionCoreTypeSupportFacade contentPromotionCoreTypeSupportFacade;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private TestContentPromotionRepository testContentPromotionRepository;

    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    private VideoClient videoClient;

    private UpdateVideoFromContentPromotionJob job;

    private ClientId clientId;
    private int shard;
    private ContentPromotionAdGroupInfo adGroupInfo;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);

        videoClient = mock(VideoClient.class);

        job = new UpdateVideoFromContentPromotionJob(clientInfo.getShard(), bannerContentPromotionRepository,
                contentRepository, bannerCommonRepository,
                contentPromotionCoreTypeSupportFacade, dslContextProvider, videoClient);
    }

    @AfterEach
    void after() {
        testContentPromotionRepository.removeAllContentPromotions(shard);
    }

    @Test
    void noVideosToUpdate_ClientNoCalls() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId);
        contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        verify(videoClient, never()).getMeta(anyList(), anyString(), anyString());
    }

    @Test
    void oneVideoToUpdate_NoChangeFromClient_NothingIsUpdatedButRefreshTime()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now());
        var actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);

        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneVideoToUpdate_PreviewChangeFromClient_EverythingIsUpdated()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        banner.setThmbHref("//newsite.ru");
        banner.setTitle("New title");

        when(videoClient.getMeta(anyList(), anyString(), anyString())).thenReturn(List.of(banner));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withMetadata(JsonUtils.toJson(banner))
                .withPreviewUrl("https://newsite.ru")
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(JsonUtils.toJson(banner)));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneVideoToUpdate_VideoIsNotReturnedFromClient_IsInaccessibleIsTrue()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString())).thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneVideoToUpdate_VideoIsReturnedFromClient_IsInaccessibleIsFalse()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long id = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now())
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(id)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleVideoToUpdate_VideoIsReturnedFromClient_IsInaccessibleIsFalse()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleVideoToUpdate_WrongFormat_ExceptionThrown() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenThrow(new IllegalStateException());

        assertThrows(IllegalStateException.class, () -> job.execute());
    }

    @Test
    void oneAccessibleVideoToUpdate_WrongFormat_VideoNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenThrow(new IllegalStateException());

        try {
            job.execute();
        } catch (IllegalStateException e) {
        }

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleVideoToUpdate_VideoIsNotReturnedFromClient_IsInaccessibleIsTrue()
            throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        video.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(video).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void previewChanges_StatusBsSyncedIsUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        banner.setThmbHref("https://newsite.ru");
        banner.setTitle("New title");

        when(videoClient.getMeta(anyList(), anyString(), anyString())).thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void accessibleVideoNowInaccessible_StatusBsSyncedIsUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void inaccessibleVideoNowAccessible_StatusBsSyncedIsUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void previewChanges_StatusModeratedIsNotChanged() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        banner.setThmbHref("https://newsite.ru");
        banner.setTitle("New title");

        when(videoClient.getMeta(anyList(), anyString(), anyString())).thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void metadataChanges_StatusModeratedIsNotChanged() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        banner.setVhd("12345");

        when(videoClient.getMeta(anyList(), anyString(), anyString())).thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void accessibleVideoNowInaccessible_StatusModeratedIsNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void inaccessibleVideoNowAccessible_StatusModeratedIsNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void accessibleVideoNowInaccessible_StatusPostModerateIsNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusPostModerate(), equalTo(OldBannerStatusPostModerate.YES));
    }

    @Test
    void accessibleVideoNowInaccessible_DraftBanner_StatusModerateIsNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        var bannerInfo = steps.contentPromotionBannerSteps().createBanner(
                adGroupInfo,
                video,
                testContentPromotionBanners.fullContentPromoBanner().withStatusModerate(BannerStatusModerate.NEW));

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.NEW));
    }

    @Test
    void inaccessibleVideoNowAccessible_StatusPostModerateIsNotUpdated() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(List.of(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusPostModerate(), equalTo(OldBannerStatusPostModerate.YES));
    }

    @Test
    void accessibleVideoNowInaccessibleAutoModerationIsTrue_StatusModeratedIsNotUpdated()
            throws IOException {

        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}));

        ppcPropertiesSupport.set(PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION.getName(), "1");
        job.execute();
        ppcPropertiesSupport.remove(PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION.getName());

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void execute_AccessibleVideoBuFirstRequestFailed_VideoRemainsAccessible() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, video);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerVideoType(contentId);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}), asList(banner));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void execute_InaccessibleVideo_RetryDone() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{null}), asList(new VideoBanner[]{null}));

        job.execute();

        verify(videoClient, times(2)).getMeta(anyList(), anyString(), anyString());
    }

    @Test
    void execute_AccessibleVideo_NoRetry() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        contentRepository.insertContentPromotion(clientId, video);

        when(videoClient.getMeta(anyList(), anyString(), anyString()))
                .thenReturn(asList(new VideoBanner[]{banner}));

        job.execute();

        verify(videoClient, times(1)).getMeta(anyList(), anyString(), anyString());
    }

    private ContentPromotionBannerInfo createActiveContentPromotionBannerVideoType(Long contentId) {
        var content = contentRepository
                .getContentPromotion(adGroupInfo.getClientId(), singletonList(contentId)).get(0);
        return steps.contentPromotionBannerSteps().createDefaultBanner(adGroupInfo, content);
    }
}

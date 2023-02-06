package ru.yandex.direct.jobs.contentpromotion.collection.updatecontent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.asynchttpclient.netty.NettyResponse;
import org.asynchttpclient.netty.NettyResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.asynchttp.ErrorResponseWrapperException;
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
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.libs.collections.CollectionsClient;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpDataResult;
import ru.yandex.direct.test.utils.differ.LocalDateSecondsPrecisionDiffer;
import ru.yandex.direct.utils.HashingUtils;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB;
import static ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionUtils.convertToPreviewUrl;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;

/**
 * Тесты на джобу UpdateContentCollectionsJob
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateContentCollectionsJobTest {
    private static final EnvironmentType UNIT_TEST_ENVIRONMENT_TYPE = EnvironmentType.DEVELOPMENT;
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
    private ContentPromotionRepository contentRepository;

    @Autowired
    private BannerWithContentPromotionRepository bannerContentPromotionRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private ContentPromotionCoreTypeSupportFacade contentPromotionCoreTypeSupportFacade;

    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    private CollectionsClient collectionsClient;

    private UpdateContentCollectionsJob job;

    private ClientId clientId;
    private ContentPromotionAdGroupInfo adGroupInfo;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);

        collectionsClient = mock(CollectionsClient.class);

        job = new UpdateContentCollectionsJob(clientInfo.getShard(), bannerContentPromotionRepository,
                contentRepository, bannerCommonRepository,
                contentPromotionCoreTypeSupportFacade, dslContextProvider, ppcPropertiesSupport, collectionsClient,
                UNIT_TEST_ENVIRONMENT_TYPE);
        ppcPropertiesSupport.set(ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB.getName(), "1");
    }

    @Test
    void noCollectionsToUpdate_ClientNoCalls() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        verify(collectionsClient, never()).getCollectionSerpData(anyString());
    }

    @Test
    void oneCollectionToUpdate_NoChangesFromClient_NothingIsUpdatedButRefreshTime()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now());
        var actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);

        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_PreviewChangeFromClient_EverythingIsUpdated()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_CollectionIsNotReturnedFromClient_IsInaccessibleIsTrue()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_CollectionIsReturnedFromClient_IsInaccessibleIsFalse()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now())
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionToUpdate_CollectionIsReturnedFromClient_IsInaccessibleIsFalse()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionToUpdate_CollectionIsNotReturnedFromClient_IsInaccessibleIsTrue()
            throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        collection.withMetadataRefreshTime(LocalDateTime.now())
                .withMetadataModifyTime(LocalDateTime.now())
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void previewChanges_StatusBsSyncedIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void accessibleCollectionNowInaccessible_StatusBsSyncedIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void inaccessibleCollectionNowAccessible_StatusBsSyncedIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(new CollectionSerpDataResult(serpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    void previewChanges_StatusModeratedIsNotChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void metadataChanges_StatusModeratedIsNotChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("name", "прекрасный текстик"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void accessibleCollectionNowInaccessible_StatusModeratedIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void inaccessibleCollectionNowAccessible_StatusModeratedIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void accessibleCollectionNowInaccessible_StatusPostModerateIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusPostModerate(), equalTo(OldBannerStatusPostModerate.YES));
    }

    @Test
    void accessibleCollectionNowInaccessible_DraftBanner_StatusModerateIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(adGroupInfo.getClientInfo(), ContentPromotionAdgroupType.COLLECTION);

        var bannerInfo = steps.contentPromotionBannerSteps()
                .createBanner(adGroupInfo,
                        collection,
                        testContentPromotionBanners.fullContentPromoCollectionBanner(null, null)
                                .withStatusModerate(BannerStatusModerate.NEW));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.NEW));
    }

    @Test
    void inaccessibleCollectionNowAccessible_StatusPostModerateIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.execute();

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusPostModerate(), equalTo(OldBannerStatusPostModerate.YES));
    }

    @Test
    void accessibleCollectionNowInaccessibleAutoModerationIsTrue_StatusModeratedIsNotUpdated()
            throws IOException {

        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        ppcPropertiesSupport.set(PpcPropertyNames.CONTENT_PROMOTION_COLLECTION_AUTO_MODERATION.getName(), "1");
        job.execute();
        ppcPropertiesSupport.remove(PpcPropertyNames.CONTENT_PROMOTION_COLLECTION_AUTO_MODERATION.getName());

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), equalTo(OldBannerStatusModerate.YES));
    }

    @Test
    void accessibleCollection_RequestFailed_CollectionNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createForbiddenResponse());

        job.execute();

        var actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);

        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void inaccessibleCollection_RequestFailed_CollectionNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createForbiddenResponse());

        job.execute();

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    private ContentPromotionBannerInfo createActiveContentPromotionBannerCollectionType(Long contentId) {
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(adGroupInfo.getClientInfo(), ContentPromotionAdgroupType.COLLECTION);
        var content = contentRepository
                .getContentPromotion(adGroupInfo.getClientId(), singletonList(contentId)).get(0);
        return steps.contentPromotionBannerSteps()
                .createDefaultBanner(adGroupInfo, content);
    }

    private CollectionSerpDataResult createSuccessResponse(CollectionSerpData collectionSerpData) {
        return new CollectionSerpDataResult(collectionSerpData);
    }

    private CollectionSerpDataResult createNotFoundResponse() {
        return createBadResponse(NOT_FOUND);
    }

    private CollectionSerpDataResult createForbiddenResponse() {
        return createBadResponse(FORBIDDEN);
    }

    private CollectionSerpDataResult createBadResponse(HttpResponseStatus responseStatus) {
        return new CollectionSerpDataResult(null, singletonList(new ErrorResponseWrapperException(null,
                new NettyResponse(
                        new NettyResponseStatus(null, new DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus),
                                null),
                        null, null),
                null)));
    }
}

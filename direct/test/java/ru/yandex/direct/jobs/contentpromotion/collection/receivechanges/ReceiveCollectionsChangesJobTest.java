package ru.yandex.direct.jobs.contentpromotion.collection.receivechanges;

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
import ru.yandex.direct.config.DirectConfig;
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
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.contentpromotion.collection.receivechanges.model.CollectionChangeInfo;
import ru.yandex.direct.jobs.contentpromotion.collection.receivechanges.model.CollectionChangeStatus;
import ru.yandex.direct.libs.collections.CollectionsClient;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpDataResult;
import ru.yandex.direct.test.utils.differ.LocalDateSecondsPrecisionDiffer;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.utils.HashingUtils;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB;
import static ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionUtils.convertToPreviewUrl;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;

@JobsTest
@ExtendWith(SpringExtension.class)
class ReceiveCollectionsChangesJobTest {

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
    private DirectConfig directConfig;

    @Autowired
    private TvmIntegration tvmIntegration;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private BannerWithContentPromotionRepository bannerContentPromotionRepository;

    @Autowired
    private ContentPromotionRepository contentRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    private CollectionsClient collectionsClient;

    private ReceiveCollectionsChangesJob job;

    private ClientId clientId;
    private ClientId anotherClientId;
    private ClientId anotherShardClientId;
    private ContentPromotionAdGroupInfo adGroupInfo;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        anotherClientId = anotherClientInfo.getClientId();

        ClientInfo anotherShardClientInfo = steps.clientSteps().createDefaultClientAnotherShard();
        anotherShardClientId = anotherShardClientInfo.getClientId();

        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo);

        collectionsClient = mock(CollectionsClient.class);

        job = new ReceiveCollectionsChangesJob(directConfig, tvmIntegration,
                new CollectionsChangesService(UNIT_TEST_ENVIRONMENT_TYPE, shardHelper,
                        bannerContentPromotionRepository, contentRepository,
                        collectionsClient, dslContextProvider, bannerCommonRepository),
                ppcPropertiesSupport, UNIT_TEST_ENVIRONMENT_TYPE);
        ppcPropertiesSupport.set(ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB.getName(), "0");
    }

    @Test
    void oneCollectionToUpdate_JobIsOff_NothingUpdated() throws IOException {
        ppcPropertiesSupport.set(ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB.getName(), "1");

        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_AllJobsAreOff_NothingUpdated() throws IOException {
        ppcPropertiesSupport.remove(ENABLE_CONTENT_PROMOTION_COLLECTIONS_JOB.getName());

        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_NoChangesFromClient_NothingIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        var actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);

        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_CollectionNotFound_NothingChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_ForbiddenError_NothingChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createForbiddenResponse());

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_PreviewChanges_ContentIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_PreviewChanges_StatusBsSyncedIsUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    void oneCollectionsToUpdate_DraftBanner_StatusBsSyncedIsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);

        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(adGroupInfo.getClientInfo(), ContentPromotionAdgroupType.COLLECTION);


        ContentPromotionBannerInfo bannerInfo = steps.contentPromotionBannerSteps().createBanner(
                adGroupInfo,
                collection,
                testContentPromotionBanners.fullContentPromoCollectionBanner(null, null)
                        .withStatusModerate(BannerStatusModerate.NEW));

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    void oneCollectionToUpdate_PreviewChanges_ModerateStatusesNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banners.get(0).getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    void oneCollectionToUpdate_PreviewChanges_TimeFieldsNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusDays(1))
                .withMetadataModifyTime(LocalDateTime.now().minusDays(1))
                .withMetadataCreateTime(LocalDateTime.now().minusDays(1));
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_OneRequest() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        verify(collectionsClient, times(1)).getCollectionSerpDataResult(anyString());
    }

    @Test
    void oneCollectionToUpdate_MultipleContentsInOneShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        ContentPromotionContent anotherCollection = fromSerpData(serpData, anotherClientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherContentId = contentRepository.insertContentPromotion(anotherClientId, anotherCollection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));
        anotherCollection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherActual =
                contentRepository.getContentPromotion(anotherClientId, List.of(anotherContentId)).get(0);

        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherActual, beanDiffer(anotherCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionToUpdate_MultipleContentsInDifferentShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        ContentPromotionContent anotherCollection = fromSerpData(serpData, anotherShardClientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherShardContentId = contentRepository.insertContentPromotion(anotherShardClientId, anotherCollection);

        CollectionSerpData newSerpData = realLifeCollection(Map.of("thumb_id", "thumb12345"));

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(newSerpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_UPDATED)));

        collection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));
        anotherCollection
                .withMetadata(newSerpData.getNormalizedJson())
                .withPreviewUrl(convertToPreviewUrl(newSerpData.getThumbId(), UNIT_TEST_ENVIRONMENT_TYPE))
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(newSerpData.getNormalizedJson()));

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherShardActual =
                contentRepository.getContentPromotion(anotherShardClientId, List.of(anotherShardContentId)).get(0);

        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherShardActual, beanDiffer(anotherCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionOpened_IsInaccessibleIsFalse() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        collection
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionOpened_IsInaccessibleIsFalse() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        collection
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionOpened_TimeFieldsNotChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        collection
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionOpened_StatusBsSyncedNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    void oneInaccessibleCollectionOpened_ModerateStatusesNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banners.get(0).getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    void oneInaccessibleCollectionOpened_NoRequests() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        verify(collectionsClient, never()).getCollectionSerpDataResult(anyString());
    }

    @Test
    void oneCollectionOpened_MultipleContentsInOneShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        ContentPromotionContent anotherCollection = fromSerpData(serpData, anotherClientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherContentId = contentRepository.insertContentPromotion(anotherClientId, anotherCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        collection
                .withIsInaccessible(false);
        anotherCollection
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherActual = contentRepository.getContentPromotion(anotherClientId,
                List.of(anotherContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherActual, beanDiffer(anotherCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionOpened_MultipleContentsInDifferentShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        ContentPromotionContent anotherShardCollection = fromSerpData(serpData, anotherShardClientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherShardContentId = contentRepository.insertContentPromotion(anotherShardClientId,
                anotherShardCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_OPENED)));

        collection
                .withIsInaccessible(false);
        anotherShardCollection
                .withIsInaccessible(false);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherShardActual = contentRepository.getContentPromotion(anotherShardClientId,
                List.of(anotherShardContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherShardActual, beanDiffer(anotherShardCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionDeleted_IsInaccessibleIsTrue() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionDeleted_IsInaccessibleIsTrue() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionDeleted_TimeFieldsNotChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionDeleted_StatusBsSyncedNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    void oneAccessibleCollectionDeleted_ModerateStatusesNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banners.get(0).getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    void oneAccessibleCollectionDeleted_NoRequests() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        verify(collectionsClient, never()).getCollectionSerpDataResult(anyString());
    }

    @Test
    void oneCollectionDeleted_MultipleContentsInOneShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        ContentPromotionContent anotherCollection = fromSerpData(serpData, anotherClientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherContentId = contentRepository.insertContentPromotion(anotherClientId, anotherCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        collection
                .withIsInaccessible(true);
        anotherCollection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherActual = contentRepository.getContentPromotion(anotherClientId,
                List.of(anotherContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherActual, beanDiffer(anotherCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionDeleted_MultipleContentsInDifferentShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        ContentPromotionContent anotherShardCollection = fromSerpData(serpData, anotherShardClientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherShardContentId = contentRepository.insertContentPromotion(anotherShardClientId,
                anotherShardCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_DELETED)));

        collection
                .withIsInaccessible(true);
        anotherShardCollection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherShardActual = contentRepository.getContentPromotion(anotherShardClientId,
                List.of(anotherShardContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherShardActual, beanDiffer(anotherShardCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionClosed_IsInaccessibleIsTrue() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneInaccessibleCollectionClosed_IsInaccessibleIsTrue() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(true);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionClosed_TimeFieldsNotChanged() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withMetadataRefreshTime(LocalDateTime.now().minusHours(24))
                .withMetadataModifyTime(LocalDateTime.now().minusHours(24))
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createNotFoundResponse());

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        collection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneAccessibleCollectionClosed_StatusBsSyncedNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    void oneAccessibleCollectionClosed_ModerateStatusesNotUpdated() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);

        ContentPromotionBannerInfo bannerInfo = createActiveContentPromotionBannerCollectionType(contentId);

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        List<OldBanner> banners = bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId()));
        assertThat(banners.get(0).getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banners.get(0).getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    void oneAccessibleCollectionClosed_NoRequests() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        contentRepository.insertContentPromotion(clientId, collection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        verify(collectionsClient, never()).getCollectionSerpDataResult(anyString());
    }

    @Test
    void oneCollectionClosed_MultipleContentsInOneShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        ContentPromotionContent anotherCollection = fromSerpData(serpData, anotherClientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherContentId = contentRepository.insertContentPromotion(anotherClientId, anotherCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        collection
                .withIsInaccessible(true);
        anotherCollection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherActual = contentRepository.getContentPromotion(anotherClientId,
                List.of(anotherContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherActual, beanDiffer(anotherCollection).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    void oneCollectionClosed_MultipleContentsInDifferentShard() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientId)
                .withIsInaccessible(false);
        ContentPromotionContent anotherShardCollection = fromSerpData(serpData, anotherShardClientId)
                .withIsInaccessible(false);
        Long contentId = contentRepository.insertContentPromotion(clientId, collection);
        Long anotherShardContentId = contentRepository.insertContentPromotion(anotherShardClientId,
                anotherShardCollection);

        when(collectionsClient.getCollectionSerpDataResult(anyString())).thenReturn(createSuccessResponse(serpData));

        job.processEvents(singletonList(new CollectionChangeInfo()
                .withBoardId(serpData.getId())
                .withCollectionChangeStatus(CollectionChangeStatus.COLLECTION_CLOSED)));

        collection
                .withIsInaccessible(true);
        anotherShardCollection
                .withIsInaccessible(true);

        ContentPromotionContent actual = contentRepository.getContentPromotion(clientId, List.of(contentId)).get(0);
        ContentPromotionContent anotherShardActual = contentRepository.getContentPromotion(anotherShardClientId,
                List.of(anotherShardContentId)).get(0);
        assertThat(actual, beanDiffer(collection).useCompareStrategy(COMPARE_STRATEGY));
        assertThat(anotherShardActual, beanDiffer(anotherShardCollection).useCompareStrategy(COMPARE_STRATEGY));
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

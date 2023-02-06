package ru.yandex.direct.core.entity.contentpromotion;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import junitparams.converters.Nullable;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.type.ContentPromotionCoreTypeSupportFacade;
import ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.eda.ContentPromotionEdaCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.service.ContentPromotionServiceCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.video.ContentPromotionVideoCoreTypeSupport;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.libs.collections.CollectionsClient;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.libs.video.VideoClient;
import ru.yandex.direct.libs.video.model.VideoBanner;
import ru.yandex.direct.utils.HashingUtils;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.contentpromotion.validation.defects.ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_INACCESSIBLE;
import static ru.yandex.direct.core.entity.contentpromotion.validation.defects.ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_NOT_FOUND;
import static ru.yandex.direct.core.entity.contentpromotion.validation.defects.ContentPromotionDefectIds.NumberDefectIds.NOT_ENOUGH_CARDS_IN_NEW_COLLECTION;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.DB_META_VIDEO_JSON_WITH_PREVIEW_TITLE_AND_PASSAGE;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.fromVideoBannerAsContent;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.realLifeVideoBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ContentPromotionMultipleObjectsTest {
    private static final String VIDEO_URL_FIRST = "https://videourlfst";
    private static final String VIDEO_URL_SECOND = "https://videourlsnd";
    private static final String COLLECTION_URL_FIRST = "https://collectionurlfst";
    private static final String COLLECTION_EXTERNAL_ID_FIRST = "extidcolfst";
    private static final String DB_META_VIDEO_JSON = DB_META_VIDEO_JSON_WITH_PREVIEW_TITLE_AND_PASSAGE;
    private static final String COLLECTION_URL_SECOND = "https://collectionurlsnd";
    private static final String COLLECTION_EXTERNAL_ID_SECOND = "collectionurlsnd";

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private Steps steps;

    private ContentPromotionService service;
    private VideoClient videoClient;
    private CollectionsClient collectionsClient;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        videoClient = mock(VideoClient.class);
        collectionsClient = mock(CollectionsClient.class);
        ContentPromotionVideoCoreTypeSupport contentPromotionVideoCoreTypeSupport =
                new ContentPromotionVideoCoreTypeSupport(videoClient);
        ContentPromotionCollectionCoreTypeSupport collectionCoreTypeSupport =
                new ContentPromotionCollectionCoreTypeSupport(EnvironmentType.DEVELOPMENT, collectionsClient);
        ContentPromotionServiceCoreTypeSupport contentPromotionServiceCoreTypeSupport =
                new ContentPromotionServiceCoreTypeSupport();
        ContentPromotionEdaCoreTypeSupport contentPromotionEdaCoreTypeSupport =
                new ContentPromotionEdaCoreTypeSupport();
        ContentPromotionCoreTypeSupportFacade contentPromotionCoreTypeSupportFacade =
                new ContentPromotionCoreTypeSupportFacade(collectionCoreTypeSupport,
                        contentPromotionVideoCoreTypeSupport, contentPromotionServiceCoreTypeSupport,
                        contentPromotionEdaCoreTypeSupport);
        service = new ContentPromotionService(contentPromotionRepository, shardHelper,
                contentPromotionCoreTypeSupportFacade, collectionsClient, EnvironmentType.DEVELOPMENT);
    }

    @Test
    public void oneNewValidVideo_Success() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validVideoExternalRequest(VIDEO_URL_FIRST));
        List<String> existingExternalIds = emptyList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0,
                ContentPromotionContentType.VIDEO);
        int expectedErrorSize = 0;
        int expectedWarningSize = 0;
        List<Matcher> matchers = emptyList();
        performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
    }

    @Test
    public void oneNewValidCollection_Success() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validCollectionExternalRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST));
        List<String> existingExternalIds = emptyList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0,
                ContentPromotionContentType.COLLECTION);
        int expectedErrorSize = 0;
        int expectedWarningSize = 0;
        List<Matcher> matchers = emptyList();
        performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
    }

    @Test
    public void oneNewValidVideo_OneNewValidCollection_Success() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validCollectionExternalRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                () -> validVideoExternalRequest(VIDEO_URL_FIRST));
        List<String> existingExternalIds = emptyList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0,
                ContentPromotionContentType.COLLECTION, 1, ContentPromotionContentType.VIDEO);
        int expectedErrorSize = 0;
        int expectedWarningSize = 0;
        List<Matcher> matchers = emptyList();
        performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
    }

    @Test
    public void oneValidVideoInDb_OneNewValidCollection_OneInvalidVideo() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validVideoDBRequest(VIDEO_URL_FIRST),
                () -> validCollectionExternalRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                () -> invalidVideoRequest(VIDEO_URL_SECOND));
        List<String> existingExternalIds = StreamEx.of(String.valueOf(HashingUtils.getMd5HalfHashUtf8(VIDEO_URL_FIRST)),
                null, null).toList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0, ContentPromotionContentType.VIDEO,
                1, ContentPromotionContentType.COLLECTION);
        int expectedErrorSize = 1;
        int expectedWarningSize = 0;
        List<Matcher> matchers = List.of(
                hasDefectDefinitionWith(validationError(path(index(2)), CONTENT_PROMOTION_NOT_FOUND)));
        performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
    }

    @Test
    public void oneNewValidVideo_oneInaccessibleCollection_OneNewInvalidCollection_OneNewInvalidVideo() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validVideoDBRequest(VIDEO_URL_FIRST),
                () -> inaccessibleCollectionRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                () -> fewCardsCollectionExternalRequest(COLLECTION_URL_SECOND, COLLECTION_EXTERNAL_ID_SECOND),
                () -> invalidVideoRequest(VIDEO_URL_SECOND));
        List<String> existingExternalIds = emptyList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0, ContentPromotionContentType.VIDEO,
                1, ContentPromotionContentType.COLLECTION);
        int expectedErrorSize = 2;
        int expectedWarningSize = 1;
        List<Matcher> matchers = List.of(
                hasDefectDefinitionWith(validationError(path(index(1)), CONTENT_PROMOTION_INACCESSIBLE)),
                hasDefectDefinitionWith(validationError(path(index(2)), NOT_ENOUGH_CARDS_IN_NEW_COLLECTION)),
                hasDefectDefinitionWith(validationError(path(index(3)), CONTENT_PROMOTION_NOT_FOUND)));
        performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
    }

    @Test
    public void twoNewCollectionsSameExternalId_OneExistingValidVideo() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> validCollectionExternalRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                () -> validVideoDBRequest(VIDEO_URL_FIRST),
                () -> validCollectionExternalRequest(COLLECTION_URL_SECOND, COLLECTION_EXTERNAL_ID_FIRST));
        List<String> existingExternalIds = StreamEx.of(null,
                String.valueOf(HashingUtils.getMd5HalfHashUtf8(VIDEO_URL_FIRST)), null).toList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0,
                ContentPromotionContentType.COLLECTION, 1, ContentPromotionContentType.VIDEO,
                2, ContentPromotionContentType.COLLECTION);
        int expectedErrorSize = 0;
        int expectedWarningSize = 0;
        List<Matcher> matchers = emptyList();
        var vr = performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
        assertThat("First and second collections have same id", vr.getValue().get(0), is(vr.getValue().get(2)));
    }

    @Test
    public void twoExistingInaccessibleCollectionsSameExternalId_OneNewValidVideo() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> inaccessibleCollectionRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                //здесь функция для новой, чтобы не добавлять в базу дважды
                () -> validCollectionExternalRequest(COLLECTION_URL_SECOND, COLLECTION_EXTERNAL_ID_FIRST),
                () -> validVideoExternalRequest(VIDEO_URL_FIRST));
        List<String> existingExternalIds = StreamEx.of(COLLECTION_EXTERNAL_ID_FIRST, COLLECTION_EXTERNAL_ID_FIRST, null)
                .toList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0,
                ContentPromotionContentType.COLLECTION, 1, ContentPromotionContentType.COLLECTION,
                2, ContentPromotionContentType.VIDEO);
        int expectedErrorSize = 0;
        int expectedWarningSize = 2;
        List<Matcher> matchers = List.of(
                hasDefectDefinitionWith(validationError(path(index(0)), CONTENT_PROMOTION_INACCESSIBLE)),
                hasDefectDefinitionWith(validationError(path(index(1)), CONTENT_PROMOTION_INACCESSIBLE)));
        var vr = performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
        assertThat("First and second collections have same id", vr.getValue().get(0), is(vr.getValue().get(1)));
    }

    @Test
    public void twoExistingValidCollectionsSameExternalId_OneInaccessibleVideo() {
        List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers = List.of(
                () -> inaccessibleVideoRequest(VIDEO_URL_FIRST),
                () -> existingCollectionRequest(COLLECTION_URL_FIRST, COLLECTION_EXTERNAL_ID_FIRST),
                //здесь функция для новой, чтобы не добавлять в базу дважды
                () -> validCollectionExternalRequest(COLLECTION_URL_SECOND, COLLECTION_EXTERNAL_ID_FIRST));
        List<String> existingExternalIds = StreamEx.of(String.valueOf(HashingUtils.getMd5HalfHashUtf8(VIDEO_URL_FIRST)),
                COLLECTION_EXTERNAL_ID_FIRST, COLLECTION_EXTERNAL_ID_FIRST).toList();
        Map<Integer, ContentPromotionContentType> expectedContentTypes = Map.of(0, ContentPromotionContentType.VIDEO,
                1, ContentPromotionContentType.COLLECTION, 2, ContentPromotionContentType.COLLECTION);
        int expectedErrorSize = 0;
        int expectedWarningSize = 1;
        List<Matcher> matchers = List.of(
                hasDefectDefinitionWith(validationError(path(index(0)), CONTENT_PROMOTION_INACCESSIBLE)));
        var vr = performCompleteCheck(requestSuppliers, existingExternalIds, expectedContentTypes, expectedErrorSize,
                expectedWarningSize, matchers);
        assertThat("First and second collections have same id", vr.getValue().get(1), is(vr.getValue().get(2)));
    }

    private ContentPromotionSingleObjectRequest validVideoDBRequest(String videoUrl) {
        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                fromVideoBannerAsContent(JsonUtils.fromJson(DB_META_VIDEO_JSON, VideoBanner.class),
                        clientInfo.getClientId(), videoUrl)
                        .withPreviewUrl("somepreview")
                        .withMetadataHash(BigInteger.ONE));
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(videoUrl);
    }

    private ContentPromotionSingleObjectRequest validVideoExternalRequest(String videoUrl) {
        try {
            VideoBanner videoBanner = realLifeVideoBanner().setUrl(videoUrl);
            when(videoClient.getMeta(eq(List.of(videoUrl)), isNull(), isNull(), isNull(), anyString()))
                    .thenReturn(singletonList(videoBanner));
        } catch (IOException e) {
        }
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(videoUrl);
    }

    private ContentPromotionSingleObjectRequest invalidVideoRequest(String videoUrl) {
        when(videoClient.getMeta(eq(List.of(videoUrl)), isNull(), isNull(), isNull(), anyString()))
                .thenReturn(emptyList());
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(videoUrl);
    }

    private ContentPromotionSingleObjectRequest validCollectionExternalRequest(String collectionUrl,
                                                                               String externalId) {
        try {
            CollectionSerpData serpData = realLifeCollection(Map.of("url", collectionUrl));
            when(collectionsClient.getCollectionId(eq(collectionUrl))).thenReturn(externalId);
            when(collectionsClient.getCollectionSerpData(eq(externalId))).thenReturn(serpData);
        } catch (IOException e) {
        }
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.COLLECTION)
                .withUrl(collectionUrl);
    }

    private ContentPromotionSingleObjectRequest inaccessibleCollectionRequest(String collectionUrl,
                                                                              String externalId) {
        try {
            CollectionSerpData serpData = realLifeCollection(Map.of("url", collectionUrl));
            contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                    fromSerpData(serpData, clientInfo.getClientId())
                            .withMetadataHash(BigInteger.ONE)
                            .withExternalId(externalId)
                            .withPreviewUrl("somepreview")
                            .withIsInaccessible(true));
            when(collectionsClient.getCollectionId(eq(collectionUrl))).thenReturn(externalId);
        } catch (IOException e) {
        }
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.COLLECTION)
                .withUrl(collectionUrl);
    }

    private ContentPromotionSingleObjectRequest inaccessibleVideoRequest(String videoUrl) {
        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                fromVideoBannerAsContent(JsonUtils.fromJson(DB_META_VIDEO_JSON, VideoBanner.class),
                        clientInfo.getClientId(), videoUrl)
                        .withPreviewUrl("somepreview")
                        .withMetadataHash(BigInteger.ONE)
                        .withIsInaccessible(true));
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(videoUrl);
    }

    private ContentPromotionSingleObjectRequest existingCollectionRequest(String collectionUrl,
                                                                          String externalId) {
        try {
            CollectionSerpData serpData = realLifeCollection(Map.of("url", collectionUrl));
            contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                    fromSerpData(serpData, clientInfo.getClientId())
                            .withMetadataHash(BigInteger.ONE)
                            .withExternalId(externalId)
                            .withPreviewUrl("somepreview"));
            when(collectionsClient.getCollectionId(eq(collectionUrl))).thenReturn(externalId);
        } catch (IOException e) {
        }
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.COLLECTION)
                .withUrl(collectionUrl);
    }

    private ContentPromotionSingleObjectRequest fewCardsCollectionExternalRequest(String collectionUrl,
                                                                                  String externalId) {
        try {
            CollectionSerpData serpData = realLifeCollection(Map.of("url", collectionUrl, "cards_count", 6));
            when(collectionsClient.getCollectionId(eq(collectionUrl))).thenReturn(externalId);
            when(collectionsClient.getCollectionSerpData(eq(externalId))).thenReturn(serpData);
        } catch (IOException e) {
        }
        return new ContentPromotionSingleObjectRequest()
                .withClientLogin(clientInfo.getLogin())
                .withContentType(ContentPromotionContentType.COLLECTION)
                .withUrl(collectionUrl);
    }

    private ValidationResult<List<Long>, Defect> performCompleteCheck(
            List<Supplier<ContentPromotionSingleObjectRequest>> requestSuppliers,
            List<String> existingExternalIds,
            Map<Integer, ContentPromotionContentType> expectedContentTypes,
            Integer expectedErrorSize,
            Integer expectedWarningSize,
            List<Matcher> matchers) {
        List<ContentPromotionSingleObjectRequest> requests = mapList(requestSuppliers, Supplier::get);
        Map<String, Long> existingContentIdsByExternalIds = EntryStream.of(contentPromotionRepository
                .getContentPromotionByExternalIds(clientInfo.getClientId(), existingExternalIds))
                .mapValues(ContentPromotionContent::getId)
                .toMap();
        Map<Integer, Long> existingIds = EntryStream.of(existingExternalIds)
                .nonNullValues()
                .mapValues(existingContentIdsByExternalIds::get)
                .toMap();
        var vr = service.addContentPromotion(clientInfo.getClientId(), requests);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(vr.flattenErrors())
                    .describedAs("error sizes must match")
                    .matches(t -> hasSize(expectedErrorSize).matches(t));
            softly.assertThat(vr.flattenWarnings())
                    .describedAs("warning sizes must match")
                    .matches(t -> hasSize(expectedWarningSize).matches(t));
            matchers.forEach(m -> softly.assertThat(m.matches(vr)).isTrue());
            existingIds.forEach((index, id) -> softly.assertThat(vr.getValue().get(index))
                    .describedAs("id has to match existing")
                    .isEqualTo(id));
            checkContentTypesInDb(softly, expectedContentTypes, vr);
        });
        return vr;
    }

    private void checkContentTypesInDb(SoftAssertions softly,
                                       Map<Integer, ContentPromotionContentType> expectedContentTypes,
                                       ValidationResult<List<Long>, Defect> vr) {
        EntryStream.of(vr.getValue()).forKeyValue((index, id) ->
                checkSingleObjectType(softly, id, expectedContentTypes.get(index)));
    }

    private void checkSingleObjectType(SoftAssertions softly, @Nullable Long id,
                                       @Nullable ContentPromotionContentType expected) {
        if (id == null) {
            softly.assertThat(expected).describedAs("expected type has to be null").isNull();
        } else {
            softly.assertThat(contentPromotionRepository.getContentPromotion(
                    clientInfo.getClientId(), List.of(id)).get(0).getType())
                    .describedAs("db content type has to equal to expected").isEqualTo(expected);
        }
    }
}

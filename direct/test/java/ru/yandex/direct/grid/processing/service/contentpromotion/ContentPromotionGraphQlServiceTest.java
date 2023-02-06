package ru.yandex.direct.grid.processing.service.contentpromotion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.contentpromotion.GdContentPromotionMeta;
import ru.yandex.direct.grid.processing.model.contentpromotion.GdGetContentPromotionMetaInput;
import ru.yandex.direct.grid.processing.model.contentpromotion.GdGetContentPromotionMetaPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.libs.collections.CollectionsClient;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.libs.video.VideoClient;
import ru.yandex.direct.libs.video.model.VideoBanner;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionUtils.convertToPreviewUrl;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.fromVideoBannerAsContent;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.realLifeVideoBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.contentpromotion.GdContentPromotionType.COLLECTION;
import static ru.yandex.direct.grid.processing.model.contentpromotion.GdContentPromotionType.VIDEO;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ContentPromotionGraphQlServiceTest {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoClient videoClient;

    @Autowired
    private CollectionsClient collectionsClient;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  reqId: getReqId\n"
            + "  getContentPromotionMeta(input: %s) {\n"
            + "    contentPromotionMeta {\n"
            + "      contentId\n"
            + "      url\n"
            + "      previewUrl\n"
            + "      title\n"
            + "      description\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String QUERY_NAME = "getContentPromotionMeta";

    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));

        int shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void getContentPromotionMeta_VideoContentPromotion_Success() throws IOException {
        doReturn(List.of(realLifeVideoBanner())).when(videoClient)
                .getMeta(anyList(), anyString(), nullable(Long.class), nullable(Long.class), anyString());

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(VIDEO)
                .withUrl("https://www.youtube.com/watch?v=0hCBBnZI2AU");

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl("https://www.youtube.com/watch?v=0hCBBnZI2AU")
                        .withPreviewUrl("https://avatars.mds.yandex.net/get-vthumb/892163/1fa3f319a9688257ef22f6c2a2c9da91")
                        .withTitle("BMW M5 F90 vs MB E63s Обзор Moscow Limma")
                        .withDescription("Мы в вк https://vk.com/limma_group Мы в инстаграм https://www.instagram.com/limma_group/ __ Кадры с..."));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_CollectionContentPromotion_Success() throws IOException {
        CollectionSerpData serpData = realLifeCollection();

        doReturn("collection_id").when(collectionsClient)
                .getCollectionId(anyString());
        doReturn(serpData).when(collectionsClient)
                .getCollectionSerpData(anyString());

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(COLLECTION)
                .withUrl(serpData.getUrl());

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl(serpData.getUrl())
                        .withPreviewUrl(convertToPreviewUrl(serpData.getThumbId(), EnvironmentType.TESTING)));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_VideoContentPromotion_NotFound() {
        doReturn(emptyList()).when(videoClient)
                .getMeta(anyList(), anyString(), nullable(Long.class), nullable(Long.class), anyString());

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(VIDEO)
                .withUrl("https://www.youtube.com/watch?v=0hCBBnZI2AU");

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withValidationResult(new GdValidationResult()
                        .withWarnings(emptyList())
                        .withErrors(singletonList(
                                new GdDefect()
                                        .withPath("url")
                                        .withCode("ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_NOT_FOUND"))));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_CollectionContentPromotion_NotFound() {
        doReturn(null).when(collectionsClient)
                .getCollectionId(anyString());

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(COLLECTION)
                .withUrl("https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/");

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withValidationResult(new GdValidationResult()
                        .withWarnings(emptyList())
                        .withErrors(singletonList(
                                new GdDefect()
                                        .withPath("url")
                                        .withCode("ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_NOT_FOUND"))));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_VideoContentPromotion_Inaccessible() throws IOException {
        VideoBanner banner = realLifeVideoBanner();
        ContentPromotionContent video = fromVideoBannerAsContent(banner, clientInfo.getClientId())
                .withIsInaccessible(true);

        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(), video);

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(VIDEO)
                .withUrl(video.getUrl());

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(emptyList())
                        .withWarnings(singletonList(
                                new GdDefect()
                                        .withCode("ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_INACCESSIBLE")
                                        .withPath(GdGetContentPromotionMetaInput.URL.name()))))
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl("https://www.youtube.com/watch?v=0hCBBnZI2AU")
                        .withPreviewUrl("//avatars.mds.yandex.net/get-vthumb/892163/1fa3f319a9688257ef22f6c2a2c9da91")
                        .withTitle("BMW M5 F90 vs MB E63s Обзор Moscow Limma")
                        .withDescription("Мы в вк https://vk.com/limma_group Мы в инстаграм https://www.instagram.com/limma_group/ __ Кадры с..."));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_CollectionContentPromotion_Inaccessible() throws IOException {
        CollectionSerpData serpData = realLifeCollection();
        ContentPromotionContent collection = fromSerpData(serpData, clientInfo.getClientId())
                .withIsInaccessible(true);
        doReturn(collection.getExternalId()).when(collectionsClient)
                .getCollectionId(anyString());

        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(), collection);

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(COLLECTION)
                .withUrl(collection.getUrl());

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withValidationResult(new GdValidationResult()
                        .withErrors(emptyList())
                        .withWarnings(singletonList(
                                new GdDefect()
                                        .withCode("ContentPromotionDefectIds.GeneralDefectIds.CONTENT_PROMOTION_INACCESSIBLE")
                                        .withPath(GdGetContentPromotionMetaInput.URL.name()))))
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl(collection.getUrl())
                        .withPreviewUrl(collection.getPreviewUrl()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_EmptyCollectionContentPromotion_NotInDbYet_ValidationError() throws IOException {
        CollectionSerpData serpData = realLifeCollection(Map.of("cards_count", 0, "items", emptyList()));

        ContentPromotionContent collection = fromSerpData(serpData, clientInfo.getClientId());
        doReturn(collection.getExternalId()).when(collectionsClient)
                .getCollectionId(anyString());
        doReturn(serpData).when(collectionsClient)
                .getCollectionSerpData(anyString());

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(COLLECTION)
                .withUrl(collection.getUrl());

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withValidationResult(new GdValidationResult()
                        .withWarnings(emptyList())
                        .withErrors(singletonList(
                                new GdDefect()
                                        .withPath("url")
                                        .withCode("ContentPromotionDefectIds.NumberDefectIds.NOT_ENOUGH_CARDS_IN_NEW_COLLECTION")
                                        .withParams(Map.of("min", 10)))))
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl(collection.getUrl())
                        .withPreviewUrl(collection.getPreviewUrl()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void getContentPromotionMeta_EmptyCollectionContentPromotion_InDbAlready_ValidationError() throws IOException {
        CollectionSerpData serpData = realLifeCollection(Map.of("cards_count", 0, "items", emptyList()));

        ContentPromotionContent collection = fromSerpData(serpData, clientInfo.getClientId());
        doReturn(collection.getExternalId()).when(collectionsClient)
                .getCollectionId(anyString());

        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(), collection);

        GdGetContentPromotionMetaInput request = new GdGetContentPromotionMetaInput()
                .withContentPromotionType(COLLECTION)
                .withUrl(collection.getUrl());

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        GdGetContentPromotionMetaPayload payload = getPayload(executionResult);
        GdGetContentPromotionMetaPayload expectedPayload = new GdGetContentPromotionMetaPayload()
                .withContentPromotionMeta(new GdContentPromotionMeta()
                        .withContentId(payload.getContentPromotionMeta().getContentId())
                        .withUrl(collection.getUrl())
                        .withPreviewUrl(collection.getPreviewUrl()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private String getQuery(GdGetContentPromotionMetaInput request) {
        return String.format(QUERY_TEMPLATE, GraphQlJsonUtils.graphQlSerialize(request));
    }

    private GdGetContentPromotionMetaPayload getPayload(ExecutionResult executionResult) {
        return convertValue(((Map<String, Object>) executionResult.getData()).get(QUERY_NAME),
                GdGetContentPromotionMetaPayload.class);
    }
}

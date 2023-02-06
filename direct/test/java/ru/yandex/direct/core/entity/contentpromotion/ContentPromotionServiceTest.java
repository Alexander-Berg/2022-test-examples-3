package ru.yandex.direct.core.entity.contentpromotion;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.type.ContentPromotionCoreTypeSupportFacade;
import ru.yandex.direct.core.entity.contentpromotion.type.collection.CollectionsContentPromotionMeta;
import ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.eda.ContentPromotionEdaCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.eda.EdaContentPromotionMeta;
import ru.yandex.direct.core.entity.contentpromotion.type.service.ContentPromotionServiceCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.service.ServicesContentPromotionMeta;
import ru.yandex.direct.core.entity.contentpromotion.type.video.ContentPromotionVideoCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.video.VideoContentPromotionMeta;
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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class ContentPromotionServiceTest {

    private static final String META_JSON =
            "{\"search.context.data.Grouping.[0].Group.[0].Document.[0].ArchiveInfo\":[[[{"
                    + " \"Title\": \"title\", "
                    + "\"Passage\": [\"description\"], "
                    + "\"GtaRelatedAttribute\":[{\"Key\":\"thmb_href\",\"Value\":\"//youtube.com\"}]"
                    + "}]]]}";

    private static final String DB_META_JSON =
            "{"
                    + " \"Title\": \"title\", "
                    + "\"Passage\": [\"description\"], "
                    + "\"thmb_href\":\"//youtube.com\""
                    + "}";
    private static final String URL = "https://www.youtube.com/watch?v=0hCBBnZI2AU";
    private static final String COLLECTION_URL = "somecollections.ru";
    private static final String EXTERNAL_COLLECTIONS_ID = "externalidfake";
    private static final String PREVIEW_URL = "preview_url.ru";
    private static final String REQUEST_ID = "request-id";
    private static final Long CAMPAIGN_ID = 1L;
    private static final Long ADGROUP_ID = null;

    private static final VideoBanner NEW_CLIP = new VideoBanner()
            .setUrl(URL.replace("https://", ""))
            .setTitle("new_title")
            .setPassage(singletonList("new_description"))
            .setThmbHref("//new_thmb");

    private static final String EFIR_URL = "https://yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final String VISIBLE_EFIR_URL =
            "https://yandex.ru/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082";
    private static final VideoBanner EFIR_VIDEO_BANNER = new VideoBanner()
            .setUrl("frontend.vh.yandex.ru/player/15247200752434461202")
            .setTitle("new_title")
            .setPassage(singletonList("new_description"))
            .setThmbHref("//new_thmb")
            .setVisibleUrl(VISIBLE_EFIR_URL);

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private Steps steps;

    private ContentPromotionService service;
    private VideoClient client;
    private CollectionsClient collectionsClient;

    private ClientInfo clientInfo;
    private ClientInfo anotherClientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        anotherClientInfo = steps.clientSteps().createDefaultClient();
        client = mock(VideoClient.class);
        collectionsClient = mock(CollectionsClient.class);
        ContentPromotionVideoCoreTypeSupport contentPromotionVideoCoreTypeSupport =
                new ContentPromotionVideoCoreTypeSupport(client);
        ContentPromotionCollectionCoreTypeSupport contentPromotionCollectionCoreTypeSupport =
                new ContentPromotionCollectionCoreTypeSupport(EnvironmentType.DEVELOPMENT, collectionsClient);
        ContentPromotionServiceCoreTypeSupport contentPromotionServiceCoreTypeSupport =
                new ContentPromotionServiceCoreTypeSupport();
        ContentPromotionEdaCoreTypeSupport contentPromotionEdaCoreTypeSupport =
                new ContentPromotionEdaCoreTypeSupport();
        ContentPromotionCoreTypeSupportFacade contentPromotionCoreTypeSupportFacade =
                new ContentPromotionCoreTypeSupportFacade(contentPromotionCollectionCoreTypeSupport,
                        contentPromotionVideoCoreTypeSupport, contentPromotionServiceCoreTypeSupport,
                        contentPromotionEdaCoreTypeSupport);
        service = new ContentPromotionService(contentPromotionRepository, shardHelper,
                contentPromotionCoreTypeSupportFacade, collectionsClient, EnvironmentType.DEVELOPMENT);
    }

    @Test
    public void getMeta_urlReceived_videoClientIsCalled() {
        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client).getMeta(eq(List.of(URL)), anyString(), anyLong(), any(), anyString());
    }

    @Test
    public void getMeta_clipReturnedFromClient_metaReturned() {
        var clip = new VideoBanner()
                .setUrl(URL)
                .setTitle("title")
                .setPassage(singletonList("description"))
                .setThmbHref("thumb.nail/href");
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString()))
                .thenReturn(List.of(clip));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat(meta.getMeta()).isNotNull();
    }

    @Test
    public void getMeta_clipIsNotReturnedFromClient_metaIsNotReturned() {
        when(client.getMeta(anyList(), anyString(), anyLong(), anyLong(), anyString())).thenReturn(null);

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.NOT_FOUND);
        assertThat(meta.getMeta()).isNull();
    }

    @Test
    public void getMeta_VideoAlreadyExists_ExistingVideoReturned() {
        var id = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.VIDEO)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(DB_META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL)
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withPreviewUrl("//youtube.com")
                        .withIsInaccessible(false));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client, never()).getMeta(eq(List.of(URL)), anyString(), anyLong(), any(), anyString());

        VideoContentPromotionMeta expected = new VideoContentPromotionMeta()
                .withTitle("title")
                .withDescription("description")
                .withPreviewUrl("//youtube.com")
                .withContentId(id)
                .withUrl(URL);

        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat((VideoContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_ServiceAlreadyExists_ExistingServiceReturned() {
        var id = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.SERVICE)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(DB_META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL)
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.SERVICE,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());

        ServicesContentPromotionMeta expected = new ServicesContentPromotionMeta()
                .withContentId(id)
                .withUrl(URL)
                .withPreviewUrl(null);

        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat((ServicesContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_EdaAlreadyExists_ExistingEdaReturned() {
        var id = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.EDA)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(DB_META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL)
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.EDA,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());

        EdaContentPromotionMeta expected = new EdaContentPromotionMeta()
                .withContentId(id)
                .withUrl(URL)
                .withPreviewUrl(null);

        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat((EdaContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_InaccessibleVideo_ExistingVideoReturnedButStatusIsInaccessible() {
        var id = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withClientId(clientInfo.getClientId().asLong())
                        .withType(ContentPromotionContentType.VIDEO)
                        .withMetadata(DB_META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL)
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withPreviewUrl("//youtube.com")
                        .withIsInaccessible(true));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client, never()).getMeta(eq(List.of(URL)), anyString(), anyLong(), any(), anyString());

        VideoContentPromotionMeta expected = new VideoContentPromotionMeta()
                .withTitle("title")
                .withDescription("description")
                .withPreviewUrl("//youtube.com")
                .withContentId(id)
                .withUrl(URL);

        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.INACCESSIBLE);
        assertThat((VideoContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_VideoWithSameHashButDifferentUrlExists_ExceptionThrown() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString())).thenReturn(List.of(NEW_CLIP));

        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.VIDEO)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL + "1")
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        assertThatThrownBy(() -> service.getMeta(
                URL, REQUEST_ID, ContentPromotionContentType.VIDEO, CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin()))
                .hasMessageStartingWith("md5 hash in content_promotion_video url collision");
    }

    @Test
    public void getMeta_CollectionWithSameHashButDifferentUrlExists_OldCollectionReturned() throws IOException {
        when(collectionsClient.getCollectionId(anyString())).thenReturn(EXTERNAL_COLLECTIONS_ID);
        CollectionSerpData serpData = realLifeCollection(Map.of("url", COLLECTION_URL, "thumb_id", PREVIEW_URL));
        when(collectionsClient.getCollectionSerpData(anyString())).thenReturn(serpData);

        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withPreviewUrl(PREVIEW_URL + "2")
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(COLLECTION_URL + "1")
                        .withExternalId(EXTERNAL_COLLECTIONS_ID)
                        .withIsInaccessible(false));

        var meta = service.getMeta(COLLECTION_URL, null, ContentPromotionContentType.COLLECTION, 0L, 0L,
                clientInfo.getLogin());

        CollectionsContentPromotionMeta expected = new CollectionsContentPromotionMeta()
                .withPreviewUrl(PREVIEW_URL + "2")
                .withContentId(meta.getMeta().getContentId())
                .withUrl(COLLECTION_URL + "1");

        assertThat((CollectionsContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_ServiceWithSameHashButDifferentUrlExists_ExceptionThrown() {
        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.SERVICE)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL + "1")
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        assertThatThrownBy(() -> service.getMeta(
                URL, REQUEST_ID, ContentPromotionContentType.SERVICE, CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin()))
                .hasMessageStartingWith("md5 hash in content_promotion service url collision");
    }

    @Test
    public void getMeta_EdaWithSameHashButDifferentUrlExists_ExceptionThrown() {
        contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.EDA)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMetadata(META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL + "1")
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        assertThatThrownBy(() -> service.getMeta(
                URL, REQUEST_ID, ContentPromotionContentType.EDA, CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin()))
                .hasMessageStartingWith("md5 hash in content_promotion eda url collision");
    }

    @Test
    public void getMeta_VideoAlreadyExistsButForAnotherClient_NewVideoReturned() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString())).thenReturn(List.of(NEW_CLIP));

        contentPromotionRepository.insertContentPromotion(anotherClientInfo.getClientId(),
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.VIDEO)
                        .withClientId(anotherClientInfo.getClientId().asLong())
                        .withMetadata(META_JSON)
                        .withMetadataHash(BigInteger.ONE)
                        .withUrl(URL)
                        .withExternalId(String.valueOf(HashingUtils.getMd5HalfHashUtf8(URL)))
                        .withIsInaccessible(false));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client).getMeta(eq(List.of(URL)), anyString(), anyLong(), any(), anyString());

        VideoContentPromotionMeta expected = new VideoContentPromotionMeta()
                .withTitle("new_title")
                .withDescription("new_description")
                .withPreviewUrl("https://new_thmb")
                .withContentId(meta.getMeta().getContentId())
                .withUrl(URL);

        assertThat((VideoContentPromotionMeta) meta.getMeta()).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getMeta_newVideo_previewUrlIsInserted() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString())).thenReturn(List.of(NEW_CLIP));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client).getMeta(eq(List.of(URL)), anyString(), anyLong(), any(), anyString());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);

        List<ContentPromotionContent> videos = contentPromotionRepository.getContentPromotion(
                clientInfo.getClientId(),
                singletonList(meta.getMeta().getContentId()));
        assertThat(videos).hasSize(1);

        assertThat(videos.get(0).getPreviewUrl()).isEqualTo("https:" + NEW_CLIP.getThmbHref());
    }

    @Test
    public void getMeta_newEfirVideo_correctUrlAndPreviewUrlAreInserted() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString()))
                .thenReturn(List.of(EFIR_VIDEO_BANNER));

        var meta = service.getMeta(EFIR_URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        verify(client).getMeta(eq(List.of(EFIR_URL)), anyString(), anyLong(), any(), anyString());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);

        List<ContentPromotionContent> videos = contentPromotionRepository.getContentPromotion(
                clientInfo.getClientId(),
                singletonList(meta.getMeta().getContentId()));
        assertThat(videos).hasSize(1);
        assertThat(videos.get(0).getUrl()).isEqualTo(EFIR_URL);
        assertThat(videos.get(0).getPreviewUrl()).isEqualTo("https:" + EFIR_VIDEO_BANNER.getThmbHref());
    }

    @Test
    public void getMeta_newService_serviceIsInsertedAndMetaReturned() {
        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.SERVICE,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat(meta.getMeta().getUrl()).isEqualTo(URL);
        assertThat(meta.getMeta().getPreviewUrl()).isNull();

        List<ContentPromotionContent> services = contentPromotionRepository.getContentPromotion(
                clientInfo.getClientId(),
                singletonList(meta.getMeta().getContentId()));
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getUrl()).isEqualTo(URL);
        assertThat(services.get(0).getType()).isEqualTo(ContentPromotionContentType.SERVICE);
    }

    @Test
    public void getMeta_newEda_edaIsInsertedAndMetaReturned() {
        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.EDA,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.OK);
        assertThat(meta.getMeta().getUrl()).isEqualTo(URL);
        assertThat(meta.getMeta().getPreviewUrl()).isNull();

        List<ContentPromotionContent> services = contentPromotionRepository.getContentPromotion(
                clientInfo.getClientId(),
                singletonList(meta.getMeta().getContentId()));
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getUrl()).isEqualTo(URL);
        assertThat(services.get(0).getType()).isEqualTo(ContentPromotionContentType.EDA);
    }

    @Test
    public void getMeta_getEmptyListFromVideoClient_returnsNull() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString())).thenReturn(List.of());

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.NOT_FOUND);
    }

    @Test
    public void getMeta_getListWithEmptyObjectFromVideoClient_returnsNull() {
        when(client.getMeta(anyList(), anyString(), anyLong(), any(), anyString())).thenReturn(
                List.of(new VideoBanner().setUrl("yandex.ru/video/suggest_video").setTitle("").setCharset("utf-8")));

        var meta = service.getMeta(URL, REQUEST_ID, ContentPromotionContentType.VIDEO,
                CAMPAIGN_ID, ADGROUP_ID, clientInfo.getLogin());
        assertThat(meta.getContentStatus()).isEqualTo(ContentStatus.NOT_FOUND);
    }
}

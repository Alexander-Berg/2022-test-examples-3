package ru.yandex.direct.core.entity.contentpromotion.type.collection;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionService;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.type.ContentPromotionCoreTypeSupportFacade;
import ru.yandex.direct.core.entity.contentpromotion.type.eda.ContentPromotionEdaCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.service.ContentPromotionServiceCoreTypeSupport;
import ru.yandex.direct.core.entity.contentpromotion.type.video.ContentPromotionVideoCoreTypeSupport;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.libs.collections.CollectionsClient;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@CoreTest
@RunWith(SpringRunner.class)
public class ContentPromotionServiceCollectionTest {

    private static final String COLLECTION_URL = "https://l7test.yandex.ru/collections/user/yakudzablr/tupye" +
            "-kartinochki/";
    private static final String METADATA = "{\"meta\" : \"json\"}";
    private static final String EXTERNAL_ID = "external-id";
    private static final String REQUEST_ID = "request-id";
    private static final String PREWIEW_URL = "ya.ru";

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionCollectionCoreTypeSupport contentPromotionCollectionTypeSupport;

    @Autowired
    private ContentPromotionVideoCoreTypeSupport contentPromotionVideoTypeSupport;

    @Autowired
    private ContentPromotionServiceCoreTypeSupport contentPromotionServiceCoreTypeSupport;

    @Autowired
    private ContentPromotionEdaCoreTypeSupport contentPromotionEdaCoreTypeSupport;

    private ContentPromotionCoreTypeSupportFacade contentPromotionCoreTypeSupportFacade;

    private ClientInfo clientInfo;
    private ClientId clientId;

    private ContentPromotionService service;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        contentPromotionCoreTypeSupportFacade = mock(ContentPromotionCoreTypeSupportFacade.class,
                withSettings().useConstructor(contentPromotionCollectionTypeSupport,
                        contentPromotionVideoTypeSupport, contentPromotionServiceCoreTypeSupport,
                        contentPromotionEdaCoreTypeSupport));
        service = new ContentPromotionService(contentPromotionRepository, shardHelper,
                contentPromotionCoreTypeSupportFacade, mock(CollectionsClient.class), EnvironmentType.DEVELOPMENT);
    }

    @Test
    public void getMeta_ExternalServiceSufficientCardCount_Success() {
        when(contentPromotionCoreTypeSupportFacade.getBasicDataFromExternalService(anyMap(), anyMap()))
                .thenReturn(Map.of(EXTERNAL_ID, new ContentPromotionContentCollectionData(METADATA, PREWIEW_URL,
                        COLLECTION_URL, true, 12)));
        when(contentPromotionCoreTypeSupportFacade.calcExternalIds(anyMap())).thenReturn(Map.of(0, EXTERNAL_ID));
        when(contentPromotionCoreTypeSupportFacade.validateBasicDataFromExternalService(anyList()))
                .thenCallRealMethod();
        var meta = service.getMeta(COLLECTION_URL, REQUEST_ID, ContentPromotionContentType.COLLECTION,
                -1L, 1L, clientInfo.getLogin());
        assertThat(meta.getValidationResult().getErrors(), hasSize(0));
    }

    @Test
    public void getMeta_ExternalServiceLowCardCount_ValidationError() {
        when(contentPromotionCoreTypeSupportFacade.getBasicDataFromExternalService(anyMap(), anyMap()))
                .thenReturn(Map.of(EXTERNAL_ID, new ContentPromotionContentCollectionData(METADATA, PREWIEW_URL,
                        COLLECTION_URL, true, 8)));
        when(contentPromotionCoreTypeSupportFacade.calcExternalIds(anyMap())).thenReturn(Map.of(0, EXTERNAL_ID));
        when(contentPromotionCoreTypeSupportFacade.validateBasicDataFromExternalService(anyList()))
                .thenCallRealMethod();
        var meta = service.getMeta(COLLECTION_URL, REQUEST_ID,
                ContentPromotionContentType.COLLECTION, -1L, 1L, clientInfo.getLogin());
        assertThat(meta.getValidationResult().getErrors(), hasSize(1));
    }

    @Test
    public void getMeta_FromDbLowCardCount_Success() {

        contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withUrl(COLLECTION_URL)
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withIsInaccessible(false)
                        .withExternalId(EXTERNAL_ID)
                        .withMetadata(METADATA)
                        .withPreviewUrl(PREWIEW_URL)
        );

        when(contentPromotionCoreTypeSupportFacade.buildBasicDataFromDbData(anyMap(), anyMap()))
                .thenReturn(Map.of(0, new ContentPromotionContentCollectionData(METADATA, PREWIEW_URL, COLLECTION_URL, true, 8)));
        when(contentPromotionCoreTypeSupportFacade.calcExternalIds(anyMap())).thenReturn(Map.of(0, EXTERNAL_ID));
        when(contentPromotionCoreTypeSupportFacade.validateBasicDataFromDb(anyList())).thenCallRealMethod();
        var meta = service.getMeta(COLLECTION_URL, REQUEST_ID,
                ContentPromotionContentType.COLLECTION, -1L, 1L, clientInfo.getLogin());
        assertThat(meta.getValidationResult().getErrors(), hasSize(0));
    }

    @Test
    public void getMeta_FromDbSufficientCardCount_Success() {

        contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withUrl(COLLECTION_URL)
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withIsInaccessible(false)
                        .withExternalId(EXTERNAL_ID)
                        .withMetadata(METADATA)
                        .withPreviewUrl(PREWIEW_URL)
        );

        when(contentPromotionCoreTypeSupportFacade.buildBasicDataFromDbData(anyMap(), anyMap()))
                .thenReturn(Map.of(0, new ContentPromotionContentCollectionData(METADATA, PREWIEW_URL,
                        COLLECTION_URL, true, 12)));
        when(contentPromotionCoreTypeSupportFacade.calcExternalIds(anyMap())).thenReturn(Map.of(0, EXTERNAL_ID));
        when(contentPromotionCoreTypeSupportFacade.validateBasicDataFromDb(anyList())).thenCallRealMethod();
        var meta = service.getMeta(COLLECTION_URL, REQUEST_ID,
                ContentPromotionContentType.COLLECTION, -1L, 1L, clientInfo.getLogin());
        assertThat(meta.getValidationResult().getErrors(), hasSize(0));
    }

}

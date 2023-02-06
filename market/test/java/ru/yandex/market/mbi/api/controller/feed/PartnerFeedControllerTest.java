package ru.yandex.market.mbi.api.controller.feed;

import javax.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.BpmnClientService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.mbi.open.api.client.model.FeedType;
import ru.yandex.market.mbi.open.api.client.model.PartnerFeedSourceType;
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedRequestDTO;
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedResponseDTO;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link PartnerFeedController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerFeedControllerTest extends FunctionalTest {

    @Autowired
    private BpmnClientService bpmnClientService;

    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogbrokerService;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @BeforeEach
    void init() {
        mockCamunda();
    }

    @Test
    @DisplayName("Добавление INTERNAL фида")
    @DbUnitDataSet(
            before = "PartnerFeedControllerTest.testAddFeed.before.csv",
            after = "PartnerFeedControllerTest.testAddFeed.after.csv"
    )
    void testAddFeed() {
        RefreshPartnerFeedResponseDTO response = getMbiOpenApiClient().addPartnerFeed(1001L, new RefreshPartnerFeedRequestDTO()
                        .feedSourceType(PartnerFeedSourceType.INTERNAL_FILE)
                        .url("http://url1.ru/")
                        .login("login1")
                        .password("pass1"),
                null
        );

        Assertions.assertEquals(1L, response.getFeedId());

        var captor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(captor.capture());
        FeedProcessorUpdateRequestEvent actualEvent = captor.getValue();
        FeedUpdateTaskOuterClass.FeedUpdateTask expected = ProtoTestUtil.getProtoMessageByJson(
                FeedUpdateTaskOuterClass.FeedUpdateTask.class,
                "proto/testAddInternalFeed.parsingTask.proto.json",
                getClass()
        );
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .ignoringFieldsMatchingRegexes(".*timestamp.*", ".*requestId.*", ".*requestedAt.*")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Добавление фида по ссылке магазину")
    @DbUnitDataSet(
            before = "PartnerFeedControllerTest.testAddShopFeed.before.csv",
            after = "PartnerFeedControllerTest.testAddShopFeed.after.csv"
    )
    void testAddShopFeed() throws InvalidProtocolBufferException {
        RefreshPartnerFeedResponseDTO response = getMbiOpenApiClient().addPartnerFeed(1001L, new RefreshPartnerFeedRequestDTO()
                        .feedSourceType(PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE)
                        .url("http://url1.ru/")
                        .login("login1")
                        .password("pass1"),
                FeedType.ASSORTMENT_FEED
        );
        checkAddShopFeed(response, "testAddShopFeed.parsingTask.proto.json");
    }

    @Test
    @DisplayName("Обновление фида по ссылке магазину")
    @DbUnitDataSet(
            before = "PartnerFeedControllerTest.testUpdateShopFeed.before.csv",
            after = "PartnerFeedControllerTest.testAddShopFeed.after.csv"
    )
    void testUpdateShopFeed() throws InvalidProtocolBufferException {
        RefreshPartnerFeedResponseDTO response = getMbiOpenApiClient().updatePartnerFeed(1001L, 1L, new RefreshPartnerFeedRequestDTO()
                        .feedSourceType(PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE)
                        .url("http://url1.ru/")
                        .login("login1")
                        .password("pass1"),
                FeedType.ASSORTMENT_FEED
        );
        checkAddShopFeed(response, "testAddShopFeed.parsingTask.proto.json");
    }

    @Test
    @DisplayName("Удаление фида по ссылке магазину")
    @DbUnitDataSet(
            before = "PartnerFeedControllerTest.testUpdateShopFeed.before.csv",
            after = "PartnerFeedControllerTest.testDeleteShopFeed.after.csv"
    )
    void testDeleteShopFeed() throws InvalidProtocolBufferException {
        RefreshPartnerFeedResponseDTO response = getMbiOpenApiClient().deletePartnerFeed(1001L, 1L,
                FeedType.ASSORTMENT_FEED
        );
        checkAddShopFeed(response, null);
    }

    void checkAddShopFeed(RefreshPartnerFeedResponseDTO response, @Nullable String expectedSamovarTask) throws InvalidProtocolBufferException {
        Assertions.assertEquals(1L, response.getFeedId());

        if (expectedSamovarTask == null) {
            return;
        }
        var captor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(captor.capture());
        FeedProcessorUpdateRequestEvent actualEvent = captor.getValue();

        FeedUpdateTaskOuterClass.FeedUpdateTask expected = ProtoTestUtil.getProtoMessageByJson(
                FeedUpdateTaskOuterClass.FeedUpdateTask.class,
                "proto/" + expectedSamovarTask,
                getClass()
        );
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .ignoringFieldsMatchingRegexes(".*requestedAt.*", ".*updatedAt.*", ".*requestId.*")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Добавление сайта для парсинга через Самовар")
    @DbUnitDataSet(
            before = "PartnerFeedControllerTest.testAddFeed.before.csv",
            after = "PartnerFeedControllerTest.testAddSite.after.csv"
    )
    void testAddSite() throws InvalidProtocolBufferException {
        RefreshPartnerFeedResponseDTO response = getMbiOpenApiClient().addPartnerFeed(1001L, new RefreshPartnerFeedRequestDTO()
                        .feedSourceType(PartnerFeedSourceType.SITE_PARSING)
                        .url("http://url1.ru/"),
                null
        );

        Assertions.assertEquals(1L, response.getFeedId());

        var captor = ArgumentCaptor.forClass(SamovarEvent.class);
        Mockito.verify(samovarLogbrokerService).publishEvent(captor.capture());
        SamovarEvent actualEvent = captor.getValue();
        Assertions.assertEquals("http://url1.ru/", actualEvent.getPayload().getUrl());
        Assertions.assertEquals("test-feed-site-processing", actualEvent.getPayload().getFeedName());

        SamovarContextOuterClass.SamovarContext actualSamovarContext = SamovarContextOuterClass.SamovarContext.parseFrom(
                actualEvent.getPayload().getFeedContext().getBytesValue()
        );
        SamovarContextOuterClass.SamovarContext expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class,
                "proto/testAddSite.samovarTask.proto.json",
                getClass()
        );
        ProtoTestUtil.assertThat(actualSamovarContext)
                .ignoringFieldsMatchingRegexes(".*forceRefreshStart.*")
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .ignoringFieldsMatchingRegexes(".*requestId.*")
                .isEqualTo(expected);
    }

    private void mockCamunda() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(bpmnClientService.startProcess(any())).thenReturn(response);
    }
}

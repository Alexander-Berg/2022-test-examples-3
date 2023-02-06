package ru.yandex.market.partner.mvc.controller.v3.feed;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarUtils;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType;
import ru.yandex.market.partner.mvc.controller.feed.model.FeedContentTypeDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link UnitedFeedController#removeFeed}
 * Date: 25.02.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class UnitedFeedControllerRemoveFeedTest extends FunctionalTest {

    private static final Instant NOW = Instant.now();

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @BeforeEach
    void init() {
        mockCamunda();
    }

    @AfterEach
    void after() {
        Mockito.verifyNoMoreInteractions(mbiBpmnClient);
        Mockito.verifyNoMoreInteractions(feedProcessorUpdateLogbrokerEventPublisher);
    }

    @Test
    @DisplayName("Удаление фида. Неизвестынй фид.")
    void removeFeed_unknownFeed_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(buildRemoveFeedUrl("777", "10")));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/removeFeed/errorFeedId.response.json");
    }

    @Test
    @DisplayName("Удаление синего фида. Фид становится дефолтным")
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/removeFeed/supplier.before.csv",
            after = "UnitedFeedController/csv/removeFeed/supplier.after.csv"
    )
    void removeSupplierFeed_supported_convertToDefaultAndHideOffers() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "11"));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "false",
                "partnerId", "774",
                "feedId", "11",
                "operationId", "1"
        );

        checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(11, FeedType.ASSORTMENT, FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление белого фида, если фид один. Фид делаем дефолтным. " +
            "Отправляем ивент на скрытие офферов")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/shopOneFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/shopOneFeedWithEnabledMigration.after.csv",
                    "UnitedFeedController/csv/removeFeed/shopBpmnOperation.after.csv"
            }
    )
    void removeFeed_shopOneFeedWithEnabledMigration_convertToDefaultAndHideOffers() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12"));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "false",
                "partnerId", "774",
                "feedId", "12",
                "operationId", "1"
        );

        checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(12, FeedType.ASSORTMENT, FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление белого ценового фида, если фид один. Фид делаем дефолтным. " +
            "Отправляем ивент на скрытие офферов")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/shopOneFeedPrice.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/shopOneFeedPriceWithEnabledMigration.after.csv",
                    "UnitedFeedController/csv/removeFeed/shopBpmnOperation.after.csv"
            }
    )
    void removeFeed_shopOneFeedWithEnabledMigration_convertToDefaultAndHideOffers_Price() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12"));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "false",
                "partnerId", "774",
                "feedId", "12",
                "operationId", "1"
        );

        checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(12, FeedType.PRICES, FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление фида. Магазин в черном списке. Миграция не начинается")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/shopOneFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/shopOneFeedWithEnabledMigration.after.csv"
            }
    )
    void removeFeedForPartnerFromBlacklist() throws URISyntaxException {
        environmentService.setValues("migrate-partner-offers.blacklist", List.of("774", "bbb"));
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12"));
        Mockito.verify(mbiBpmnClient, Mockito.never()).postProcess(any());
        checkFeedProcessorEvent(12, FeedType.ASSORTMENT, FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление белого фида, если фидов несколько. Однофидовость включена. Фид удаляем. " +
            "Отправляем ивент на скрытие и миграцию офферов")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/shopMultipleFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/shopMultipleFeed.after.csv",
                    "UnitedFeedController/csv/removeFeed/shopBpmnOperation.after.csv"
            }
    )
    void removeFeed_shopMultipleFeedWithEnabledMigration_delAndHideOffers() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "13"));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "true",
                "partnerId", "774",
                "feedId", "13",
                "operationId", "1"
        );

        checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(13, FeedType.ASSORTMENT, FeedUpdateType.UPDATE_TYPE_DELETE);

    }

    @Test
    @DisplayName("Удаление ДБС фида из базы, если фид один. Фид делаем дефолтным. " +
            "Отправляем ивент на миграцию офферов")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/dbsOneFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/dbsOneFeed.after.csv",
                    "UnitedFeedController/csv/removeFeed/dbsBpmnOperation.after.csv"
            }
    )
    void removeFeed_dbsOneFeedWithEnabledMigration_convertToDefaultAndHideOffers() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1007", "12", FeedContentTypeDTO.ASSORTMENT_WITH_PRICES));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "false",
                "partnerId", "777",
                "feedId", "12",
                "operationId", "1"
        );

        checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(12, FeedType.ASSORTMENT, FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление ценового фида у синих.")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/supplier.before.csv",
                    "UnitedFeedController/csv/removeFeed/supplierUtilityFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/supplierUtilityFeed.removePrice.after.csv"
            }
    )
    void removeFeed_priceSupplierFeed_successful() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12", FeedContentTypeDTO.PRICE));
        checkFeedProcessorEvent(12, FeedType.PRICES, FeedUpdateType.UPDATE_TYPE_DELETE);
    }

    @Test
    @DisplayName("Удаление стокового фида у синих.")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/supplier.before.csv",
                    "UnitedFeedController/csv/removeFeed/supplierUtilityFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/supplierUtilityFeed.removeStock.after.csv"
            }
    )
    void removeFeed_stockSupplierFeed_successful() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12", FeedContentTypeDTO.STOCK));
        checkFeedProcessorEvent(12, FeedType.STOCKS, FeedUpdateType.UPDATE_TYPE_DELETE);
    }

    @Test
    @DisplayName("Удаление стокового фида у белых.")
    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/removeFeed/shopInfo.before.csv",
                    "UnitedFeedController/csv/removeFeed/shopUtilityFeed.before.csv"
            },
            after = {
                    "UnitedFeedController/csv/removeFeed/shopUtilityFeed.removeStock.after.csv"
            }
    )
    void removeFeed_stockShopFeed_successful() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("1001", "12", FeedContentTypeDTO.STOCK));
        checkFeedProcessorEvent(12, FeedType.STOCKS, FeedUpdateType.UPDATE_TYPE_DELETE);
    }

    @SuppressWarnings("SameParameterValue")
    private String buildRemoveFeedUrl(String campaignId, String feedId,
                                      @Nonnull FeedContentTypeDTO feedContentTypeDTO) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", feedId)
                .addParameter("feed_type", feedContentTypeDTO.name())
                .build()
                .toString();
    }

    private String buildRemoveFeedUrl(String campaignId, String feedId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", feedId)
                .build()
                .toString();
    }

    private void mockCamunda() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        when(mbiBpmnClient.postProcess(any())).thenReturn(response);
    }

    private void checkCamundaRequest(Map<String, Object> expectedParams) {
        ArgumentCaptor<ProcessInstanceRequest> captor = ArgumentCaptor.forClass(ProcessInstanceRequest.class);
        Mockito.verify(mbiBpmnClient, Mockito.times(1)).postProcess(captor.capture());
        ProcessInstanceRequest actualRequest = captor.getValue();
        Map<String, Object> actualParams = actualRequest.getParams();
        Assertions.assertNotNull(actualParams);

        Instant actualTimestamp = Instant.parse(String.valueOf(actualParams.remove("timestamp")));
        Assertions.assertTrue(actualTimestamp.isAfter(NOW));

        Assertions.assertEquals(expectedParams, actualParams);
    }

    private void checkFeedProcessorEvent(long feedId, FeedType feedType, FeedUpdateType updateType) {
        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher,
                Mockito.times(1)).publishEvent(captor.capture());
        FeedUpdateTaskOuterClass.FeedUpdateTask event = captor.getValue().getPayload();
        Assertions.assertEquals(updateType,
                event.getFeedUpdateType());
        Assertions.assertEquals(feedId, event.getFeed().getFeedId());
        Assertions.assertEquals(SamovarUtils.toFeedType(feedType), event.getFeed().getFeedType());
    }
}

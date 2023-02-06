package ru.yandex.market.partner.mvc.controller.v3.feed;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StandardLocation;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.core.ff4shops.FF4ShopsOpenApiClient;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass.FeedUpdateTask;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupDto;
import ru.yandex.market.mbi.ff4shops.client.model.WarehouseDto;
import ru.yandex.market.mbi.ff4shops.client.model.WrappedStocksWarehouseGroupDto;
import ru.yandex.market.partner.mvc.dto.v3.feed.FeedParsingTypeRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты создания фида для {@link UnitedFeedController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class UnitedFeedControllerCreateFeedTest extends FunctionalTest {

    @Autowired
    private FeedFileStorage feedFileStorage;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @Autowired
    private TestEnvironmentService environmentService;

    @Autowired
    private FF4ShopsOpenApiClient ff4ShopsOpenApiClient;

    @BeforeEach
    void init() {
        environmentService.setEnvironmentType(EnvironmentType.DEVELOPMENT);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    @Test
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv"
    )
    @DisplayName("Создание фида партнера. Неверный id запроса на валидацию")
    void createFeed_wrongValidationInfo_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl("774", "1001", "COMPLETE")));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/wrongValidationInfo.response.json");
    }

    @Test
    @DisplayName("Создание фида партнера. Неизвестный тип")
    void createFeed_wrongType_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl("774", "1001", "AGA"))
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/wrongType.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/errorValidation.before.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Неверный запрос на валидацию - ошибочный результат")
    void createFeed_errorValidationInfo_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl("2012", "1001", "COMPLETE"))
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/errorValidationInfo.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv"
    )
    @ParameterizedTest(name = "campaignId = {1}")
    @CsvSource({
            "2011,1001",
            "2016,1007"
    })
    @DisplayName("Обновление существующего фида партнера по id. Неверный id загруженного файла")
    void createFeed_errorUploadId_error(String validationId, String campaignId) {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl(validationId, campaignId, "COMPLETE"))
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/errorUploadId.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopUrlFeedCreated.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. Первый фид. По ссылке")
    void createFeed_shopFirstUrl_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2013", "1007", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.1.response.json");
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUrlFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/shopCorrectData.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. Второй фид. По ссылке")
    void createFeed_shopCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2013", "1007", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.1.response.json");
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUploadFeed.before.csv",
            },
            after = "UnitedFeedController/csv/createFeed/shopUrlFeedUpdated.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. У магазина уже есть фид по файлу. Добавляем по ссылке")
    void createFeed_shopFirstUrlAfterUpload_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2013", "1007", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierCorrectData.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Синий. По ссылке")
    void createFeed_supplierCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2010", "1001", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.11.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidationWithFields.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierPricesWithFields.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Синий. По ссылке. С выбранными полями")
    void createFeed_supplierCorrectData_successful_with_fields() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2010", "1001", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");
        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);

        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, times(1))
                .publishEvent(eventCaptor.capture());

        FeedProcessorUpdateRequestEvent event = eventCaptor.getValue();
        assertEquals(1,event.getPayload().getParsingFieldsList().size());
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopUploadCreated.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. Первый фид. По файлу")
    void createFeed_shopUploadCreated_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2015", "1007", "UPDATE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.1.response.json");

        assertFeedParsingType(777, 1, FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUploadFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/uploadShopValidation.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. У магазина уже есть фид по файлу. Добавляем еще один по файлу")
    void createFeed_shopUploadUpdated_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2015", "1007", "UPDATE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");

        assertFeedParsingType(777, 12, FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierUploadCorrectData.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Синий. По файлу")
    void createFeed_supplierUploadCorrectData_successful() throws URISyntaxException {
        Mockito.when(feedFileStorage.getUrl(eq(StandardLocation.supplierFeed(774L, 11L))))
                .thenReturn("http://test.feed2.ru");
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2012", "1001", "UPDATE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.11.response.json");

        assertFeedParsingType(FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = {"UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/warehouseGroup.before.csv"},
            after = "UnitedFeedController/csv/createFeed/supplierStocks.after.csv"
    )
    @CsvSource({
            "STOCK",
            "COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Добавление нового стокового фида партнера из группы. Синий. По файлу")
    void createFeed_supplierStocks_successful(String type) throws URISyntaxException {
        when(ff4ShopsOpenApiClient.getGroupByPartner(anyLong()))
                .thenReturn(CompletableFuture.supplyAsync(() -> new WrappedStocksWarehouseGroupDto().result(
                        new StocksWarehouseGroupDto()
                                .id(1L)
                                .mainWarehouseId(10L)
                                .warehouses(List.of(new WarehouseDto().warehouseId(10L).partnerId(774L),
                                        new WarehouseDto().warehouseId(11L).partnerId(777L))))
                ));

        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2017", "1001", type));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        var feedProcessorUpdateRequestEvent = assertFeedParsingType(774, 11, FeedUpdateTask.FeedParsingType.COMPLETE);

        assertThat(feedProcessorUpdateRequestEvent.getPayload())
                .returns(List.of(
                                UpdateTask.WarehouseGroupInfo.newBuilder()
                                        .setShopId(774)
                                        .setWarehouseId(10)
                                        .setFeedId(1000)
                                        .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                        .build(),
                                UpdateTask.WarehouseGroupInfo.newBuilder()
                                        .setShopId(777)
                                        .setWarehouseId(11)
                                        .setFeedId(1001)
                                        .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                        .build()),
                        FeedUpdateTask::getPartnerWarehousesGroupList);

        verify(ff4ShopsOpenApiClient).getGroupByPartner(774);
        verifyNoMoreInteractions(ff4ShopsOpenApiClient);
    }

    @DbUnitDataSet(
            before = {"UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/multiWarehouse.before.csv"},
            after = "UnitedFeedController/csv/createFeed/supplierStocks.after.csv"
    )
    @Test
    @DisplayName("Добавление нового стокового фида партнера на конкретный склад. Синий. По файлу")
    void createFeed_supplierWarehouseStocks_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2017", "1001", "STOCK") +
                "&warehouse_id=111");
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(captor.capture());

        FeedUpdateTask expected = ProtoTestUtil.getProtoMessageByJson(
                FeedUpdateTask.class,
                "UnitedFeedController/proto/supplierWarehouseStocks.proto.json",
                getClass()
        );

        Assertions.assertEquals(1, captor.getAllValues().size());
        ProtoTestUtil.assertThat(captor.getValue().getPayload())
                .ignoringFieldsMatchingRegexes(".*requestedAt.*", ".*requestId.*")
                .isEqualTo(expected);
    }

    @DbUnitDataSet(
            before = {"UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/multiWarehouse.before.csv"}
    )
    @Test
    @DisplayName("Ошибка, если для многоскладового FBS не указан warehouseId")
    void createFeed_multiWarehouseStocks_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl("2017", "1001", "STOCK")));
        JsonTestUtil.assertResponseErrorMessage(exception,
                "[{\"code\":\"BAD_PARAM\",\"details\":{\"field\":\"warehouseId\",\"subcode\":\"MISSING\"}}]");
    }

    @Test
    @DisplayName("Добавление ценового фида по ссылке")
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierPricesByUrl.after.csv"
    )
    void createFeed_supplierPricesByUrl_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2019", "1001",
                FeedParsingTypeRequest.COMPLETE.name()));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        // Смотрим, что отправили в FP
        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(eventCaptor.capture());

        List<FeedProcessorUpdateRequestEvent> values = eventCaptor.getAllValues();
        Assertions.assertEquals(1, values.size());

        FeedUpdateTask expectedSamovarProto = ProtoTestUtil.getProtoMessageByJson(
                FeedUpdateTask.class,
                "UnitedFeedController/proto/supplierPricesByUrl.samovar.json",
                getClass()
        );

        ProtoTestUtil.assertThat(values.get(0).getPayload())
                .ignoringFieldsMatchingRegexes(".*requestId.*", ".*requestedAt.*", ".*updatedAt.*")
                .isEqualTo(expectedSamovarProto);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/supplierStocks.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/supplierStocks.after.csv"
    )
    @CsvSource({
            "STOCK",
            "COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Обновление стокового фида партнера. Синий. По файлу")
    void updateFeed_supplierStocks_successful(String type) throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2017", "1001", type));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        assertFeedParsingType(FeedUpdateTask.FeedParsingType.COMPLETE);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/shopStockPrices.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopStocks.after.csv"
    )
    @CsvSource({
            "STOCK",
            "COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Создание стокового фида партнера. Белый. По файлу")
    void createFeed_shopStocks_successful(String type) throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2002",
                "1007", type));

        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");


        assertFeedParsingType(777, 11, FeedUpdateTask.FeedParsingType.COMPLETE);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(captor.capture());


        FeedUpdateTask expected = ProtoTestUtil.getProtoMessageByJson(
                FeedUpdateTask.class,
                "UnitedFeedController/proto/shopStocks.proto.json",
                getClass()
        );

        Assertions.assertEquals(1, captor.getAllValues().size());
        ProtoTestUtil.assertThat(captor.getValue().getPayload())
                .ignoringFieldsMatchingRegexes(".*requestedAt.*", ".*requestId.*")
                .isEqualTo(expected);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/shopStockPricesDefaultFeed.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopStocks.after.csv"
    )
    @CsvSource({
            "STOCK",
            "COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Создание стокового фида партнера. Белый. Если ассортиментный фид дефолтовый")
    void createFeed_shopStocks_defaultFeed_successful(String type) throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2002",
                "1007", type));

        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");


        assertFeedParsingTypeAndFeedType(777, 11,
                FeedUpdateTask.FeedParsingType.COMPLETE, SamovarContextOuterClass.FeedInfo.FeedType.STOCKS);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/supplierStocks.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/supplierPrices.after.csv"
    )
    @CsvSource({
            "UPDATE,UPDATE",
            "COMPLETE,COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Добавление нового ценового фида партнера. Синий. По файлу")
    void createFeed_supplierPrice_successful(String type, FeedUpdateTask.FeedParsingType feedClass)
            throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2018", "1001", type));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        assertFeedParsingType(feedClass);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/shopStockPrices.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopPrices.after.csv"
    )
    @CsvSource({
            "UPDATE,UPDATE",
            "COMPLETE,COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("СДобавление нового ценового фида партнера. Белый. По файлу")
    void createFeed_shopPrices_successful(String type, FeedUpdateTask.FeedParsingType feedClass)
            throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2003",
                "1007", type));

        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.price.response.json");


        assertFeedParsingTypeAndFeedType(777, 1L, feedClass, SamovarContextOuterClass.FeedInfo.FeedType.PRICES);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/shopStockPrices.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopPricesUrl.after.csv"
    )
    @DisplayName("СДобавление нового ценового фида партнера. Белый. По ссылке. Только комплит")
    @Test
    void createFeed_shop_prices_url_successful()
            throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2004",
                "1007", "COMPLETE"));

        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.price.response.json");


        assertFeedParsingTypeAndFeedType(777, 1L, FeedUpdateTask.FeedParsingType.COMPLETE,
                SamovarContextOuterClass.FeedInfo.FeedType.PRICES);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/shopStockPricesWithFields.before.csv",
            after = "UnitedFeedController/csv/createFeed/shopPricesUrl.after.csv"
    )
    @DisplayName("СДобавление нового ценового фида партнера. Белый. По ссылке. С полями. Только комплит")
    @Test
    void createFeed_shop_prices_url_successful_With_fields()
            throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2004",
                "1007", "COMPLETE"));

        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.price.response.json");


        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);

        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, times(1))
                .publishEvent(eventCaptor.capture());

        FeedProcessorUpdateRequestEvent event = eventCaptor.getValue();
        assertEquals(1,event.getPayload().getParsingFieldsList().size());
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/supplierPrices.before.csv",
                    "UnitedFeedController/csv/createFeed/supplierStocks.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/supplierPrices.after.csv"
    )
    @CsvSource({
            "UPDATE,UPDATE",
            "COMPLETE,COMPLETE"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Обновление ценового фида партнера. Синий. По файлу")
    void updateFeed_supplierPrice_successful(String type, FeedUpdateTask.FeedParsingType feedClass)
            throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2018", "1001", type));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.empty.response.json");

        assertFeedParsingType(feedClass);
    }

    @Test
    @DisplayName("Загрузка стокового фида. Неверный тип парсинга")
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv"
    )
    void updateFeed_wrongParsingType_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildCreateFeedUrl("2017", "1001", "UPDATE"))
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/wrongParsingType.response.json");
    }

    @Test
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv"
    )
    @DisplayName("Обновление существующего фида партнера по id. Неверный id запроса на валидацию")
    void updateFeed_wrongValidationInfo_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        buildUpdateFeedUrl("774", "10", "1001", "COMPLETE"),
                        null
                )
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/wrongValidationInfo.response.json");
    }

    @Test
    @DisplayName("Обновление существующего фида партнера по id. Неизвестный тип")
    void updateFeed_wrongType_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        buildUpdateFeedUrl("774", "10", "1001", "aga"),
                        null
                )
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/wrongType.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/errorValidation.before.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Неверный запрос на валидацию - ошибочный результат")
    void updateFeed_errorValidationInfo_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        buildUpdateFeedUrl("2012", "10", "1001", "COMPLETE"),
                        null
                )
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/errorValidationInfo.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/errorValidation.before.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Неверный фид")
    void updateFeed_errorFeedId_error() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        buildUpdateFeedUrl("2015", "10", "1007", "COMPLETE"),
                        null
                )
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedController/json/createFeed/errorFeedId.response.json");
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUrlFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/shopUrlFeedUpdated.after.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Белый. По ссылке")
    void updateFeed_shopCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2013", "12", "1007", "COMPLETE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierCorrectData.after.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Синий. По ссылке")
    void updateFeed_supplierCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2010", "11", "1001", "COMPLETE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.11.response.json");
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUploadFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/uploadShopValidation.after.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Белый")
    void updateFeed_shopUploadCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2015", "12", "1007", "UPDATE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");

        assertFeedParsingType(777, 12, FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
            after = "UnitedFeedController/csv/createFeed/supplierUploadCorrectData.after.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. Синий")
    void updateFeed_supplierUploadCorrectData_successful() throws URISyntaxException {
        Mockito.when(feedFileStorage.getUrl(eq(StandardLocation.supplierFeed(774L, 11L))))
                .thenReturn("http://test.feed2.ru");
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2012", "11", "1001", "UPDATE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/supplier.11.response.json");

        assertFeedParsingType(FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUploadDbsFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/shopUploadDbsFeed.after.csv"
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id. ДБС")
    void updateFeed_shopDbsUploadCorrectData_replaceFeed() throws URISyntaxException {
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2015", "12", "1007", "UPDATE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");

        assertFeedParsingType(777, 12, FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopUploadFeedDefault.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/shopUploadFeedReplaceDefault.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. Белый. У магазина есть дефолтный фид. Дефолтный фид становится настоящим")
    void createFeed_shopUploadDefaultCorrectData_replaceFeed() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2015", "1007", "UPDATE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");

        assertFeedParsingType(777, 12, FeedUpdateTask.FeedParsingType.UPDATE);
    }

    @DbUnitDataSet(
            before = {
                    "UnitedFeedController/csv/createFeed/completeValidation.before.csv",
                    "UnitedFeedController/csv/createFeed/shopDbsFeed.before.csv"
            },
            after = "UnitedFeedController/csv/createFeed/shopDbsFeed.after.csv"
    )
    @Test
    @DisplayName("Создание фида партнера. ДБС. Первый фид. Добавляем по ссылке.")
    void createFeed_shopDbsUploadCorrectData_replaceFeed() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2013", "1007", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/shop.12.response.json");
    }

    private void assertFeedParsingType(FeedUpdateTask.FeedParsingType expectedType) {
        assertFeedParsingType(774, 11, expectedType);
    }

    private FeedProcessorUpdateRequestEvent assertFeedParsingType(long partnerId, long feedId,
                                                                  FeedUpdateTask.FeedParsingType expectedType) {
        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher)
                .publishEvent(eventCaptor.capture());

        FeedProcessorUpdateRequestEvent event = eventCaptor.getValue();
        assertThat(event.getPayload())
                .extracting(e -> e.getFeed().getShopId(), e -> e.getFeed().getFeedId(),
                        FeedUpdateTask::getFeedParsingType)
                .containsExactly(partnerId, feedId, expectedType);
        return event;
    }


    private FeedProcessorUpdateRequestEvent assertFeedParsingTypeAndFeedType(long partnerId, long feedId,
                                                                             FeedUpdateTask.FeedParsingType expectedType,
                                                                             SamovarContextOuterClass.FeedInfo.FeedType feedType) {
        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher)
                .publishEvent(eventCaptor.capture());

        FeedProcessorUpdateRequestEvent event = eventCaptor.getValue();
        assertThat(event.getPayload())
                .extracting(e -> e.getFeed().getShopId(), e -> e.getFeed().getFeedId(),
                        FeedUpdateTask::getFeedParsingType, e -> e.getFeed().getFeedType())
                .containsExactly(partnerId, feedId, expectedType, feedType);
        return event;
    }

    private String buildCreateFeedUrl(String validationId, String campaignId,
                                      String type) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed")
                .setParameter("validation_id", validationId)
                .setParameter("type", type)
                .build()
                .toString();
    }

    private String buildUpdateFeedUrl(String validationId, String feedId, String campaignId,
                                      String type) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", feedId)
                .setParameter("validation_id", validationId)
                .setParameter("type", type)
                .build()
                .toString();
    }
}

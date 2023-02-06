package ru.yandex.market.partner.mvc.controller.v3.feed.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import Market.DataCamp.API.UpdateTask;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feed.assortment.db.AssortmentValidationDao;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

/**
 * Date: 12.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(
        before = "UnitedFeedValidationController/csv/validate/before.csv"
)
class UnitedFeedValidationValidateControllerTest extends FunctionalTest {

    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogBrokerService;
    @Autowired
    @Qualifier("qParserLogBrokerService")
    private LogbrokerService qParserLogBrokerService;

    @Autowired
    private TestEnvironmentService environmentService;

    @Autowired
    private AssortmentValidationDao assortmentValidationDao;

    @BeforeEach
    void init() {
        environmentService.setEnvironmentType(EnvironmentType.DEVELOPMENT);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    @Test
    @DisplayName("Запуск процесса валидации фида. Пустое тело")
    void validate_emptyBody_error() {
        assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildUpdateFeedUrl("1001"))
        );
    }

    @CsvSource({
            "wrongType,1001",
            "nullFeed,1001",
            "emptyFeed,1001",
            "wrongResource,1001",
            "wrongResourceCredentials,1001",
            "wrongUpload,1001",
            "wrongCampaign,1001",
            "wrongUploadIdShop,1005",
            "wrongUploadIdSupplier,1008",
            "duplicateUrl,1011"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Запуск процесса валидации фида. Ошибка.")
    void validate_wrongParam_error(String test, String campaignId) {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        buildUpdateFeedUrl(campaignId),
                        IOUtils.toString(this.getClass()
                                .getResourceAsStream(
                                        "UnitedFeedValidationController/json/validate/" + test + ".body.json"
                                ), StandardCharsets.UTF_8
                        )
                )
        );
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "UnitedFeedValidationController/json/validate/" + test + ".json");
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/supplier.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Синий по ссылке.")
    void validate_supplier_successful()
            throws IOException, URISyntaxException {
        assertJson("supplier", "1003");
        Mockito.verifyZeroInteractions(qParserLogBrokerService);
        Mockito.verify(samovarLogBrokerService, times(1))
                .publishEvent(Mockito.any());
        var res = assortmentValidationDao.getValidationInfo(1);
        Assertions.assertThat(res).isPresent();
        Assertions.assertThat(res.get().request().parsingFields())
                .isEqualTo(null);
    }

    @Test
    @DisplayName("Запуск процесса валидации фида. Синий по ссылке. Проростание полей парсинга")
    void validate_supplier_parsing_fields_successful()
            throws IOException, URISyntaxException {
        assertJson("supplier.parsingFields", "1003");
        var res = assortmentValidationDao.getValidationInfo(1);
        Assertions.assertThat(res).isPresent();
        Assertions.assertThat(res.get().request().parsingFields())
                .isEqualTo(List.of("id", "price", "adult"));
        var eventCaptor = ArgumentCaptor.forClass(SamovarEvent.class);

        Mockito.verify(samovarLogBrokerService, times(1))
                .publishEvent(eventCaptor.capture());

        SamovarEvent event = eventCaptor.getValue();
       // assertEquals(3,event.getPayload().getFeedParsingTask().getParsingFieldsList().size());
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/supplierWithInternalUrl.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Синий по внутренней (mds) ссылке.")
    void validate_supplier_withInternalUrl_successful()
            throws IOException, URISyntaxException {
        assertJson("supplierWithInternalUrl", "1009");
        Mockito.verifyZeroInteractions(samovarLogBrokerService);
        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(Mockito.any());
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/shop.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Белый по ссылке.")
    void validate_shop_successful() throws IOException, URISyntaxException {
        assertJson("shop", "1007");

        Mockito.verifyZeroInteractions(qParserLogBrokerService);
        Mockito.verify(samovarLogBrokerService, times(1))
                .publishEvent(Mockito.any());
    }


    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/shopWithInternalUrl.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Белый по внутренней (mds) ссылке.")
    void validate_shop_withInternalUrl_successful() throws IOException, URISyntaxException {
        assertJson("shopWithInternalUrl", "1010");

        Mockito.verifyZeroInteractions(samovarLogBrokerService);
        var eventCaptor = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(eventCaptor.capture());

        FeedValidationLogbrokerEvent event = eventCaptor.getValue();
        UpdateTask.FeedClass actualType = event.getPayload()
                .getFeedParsingTask()
                .getType();
        Assertions.assertThat(event.getPayload().getFeedParsingTask().getShopsDatParameters().getIsUpload())
                .isEqualTo(true);
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/supplier.upload.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Синий по загруженному фиду.")
    void validate_supplierUpload_successful() throws IOException, URISyntaxException {
        assertJson("supplier.upload", "1005");

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(Mockito.any());
        Mockito.verifyZeroInteractions(samovarLogBrokerService);
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/supplier.stock.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Синий стоковый фид.")
    void validate_supplierStock_successful() throws IOException, URISyntaxException {
        assertJson("supplier.stock", "1005");

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(Mockito.any());
        Mockito.verifyZeroInteractions(samovarLogBrokerService);
    }

    @Test
    @DisplayName("Запуск процесса валидации фида. Синий ценовой фид по ссылке")
    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/supplier.pricesByUrl.after.csv"
    )
    void validate_supplierPricesByUrl_successful() throws IOException, URISyntaxException {
        assertJson("supplier.pricesByUrl", "1005");

        Mockito.verify(samovarLogBrokerService, times(1))
                .publishEvent(Mockito.any());
        Mockito.verifyZeroInteractions(qParserLogBrokerService);
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/shop.upload.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Белый по загруженному фиду.")
    void validate_shopUpload_successful() throws IOException, URISyntaxException {
        assertJson("shop.upload", "1008");

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(Mockito.any());
        Mockito.verifyZeroInteractions(samovarLogBrokerService);
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/shop.stock.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Белый стоковый фид.")
    void validate_shopStock_successful() throws IOException, URISyntaxException {
        assertJson("shop.stock", "1008");
        assertFeedParsingType(UpdateTask.FeedClass.FEED_CLASS_STOCK);
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/price.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации ценового фида по файлу")
    void validate_priceFeedEnabled_successful() throws IOException, URISyntaxException {
        assertJson("price", "1008");
        assertFeedParsingType(UpdateTask.FeedClass.FEED_CLASS_COMPLETE);
    }

    private void assertFeedParsingType(UpdateTask.FeedClass expectedType) {
        var eventCaptor = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(eventCaptor.capture());
        Mockito.verifyZeroInteractions(samovarLogBrokerService);

        FeedValidationLogbrokerEvent event = eventCaptor.getValue();
        UpdateTask.FeedClass actualType = event.getPayload()
                .getFeedParsingTask()
                .getType();
        Assertions.assertThat(actualType)
                .isEqualTo(expectedType);
    }

    private void assertJson(String test, String campaignId) throws URISyntaxException, IOException {
        ResponseEntity<String> response = FunctionalTestHelper.post(buildUpdateFeedUrl(campaignId),
                IOUtils.toString(this.getClass()
                        .getResourceAsStream(
                                "UnitedFeedValidationController/json/validate/" + test + ".body.json"
                        ), StandardCharsets.UTF_8
                )
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedValidationController/json/validate/" + test + ".json");
    }

    private String buildUpdateFeedUrl(String campaignId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", "validation")
                .build()
                .toString();
    }
}

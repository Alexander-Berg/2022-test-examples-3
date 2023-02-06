package ru.yandex.market.ff4shops.api.json.supplier.state;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.api.model.ErrorSubCode;
import ru.yandex.market.ff4shops.environment.EnvironmentService;
import ru.yandex.market.ff4shops.offer.model.PartnerOffer;
import ru.yandex.market.ff4shops.partner.service.StocksByPiExperiment;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "PartnerStateControllerTest.before.csv")
class PartnerStateControllerTest extends AbstractJsonControllerFunctionalTest {

    private static final int UID = 1;

    @Autowired
    MbiApiClient mbiApiClient;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void init() {
        environmentService.setValue(StocksByPiExperiment.NESU_EXP_VAR, "false");
    }

    private ResponseEntity<String> updateState(int supplierId, String requestBody) {
        String referenceUrl = FF4ShopsUrlBuilder.updateSupplierStateUrl(randomServerPort, supplierId);
        return FunctionalTestHelper.postForEntity(referenceUrl, requestBody, FunctionalTestHelper.jsonHeaders());
    }

    private ResponseEntity<String> updateStocksByPi(int partnerId, String requestBody) {
        String referenceUrl = FF4ShopsUrlBuilder.stocksByPiUrl(randomServerPort, partnerId, UID);
        return FunctionalTestHelper.putForEntity(referenceUrl,
                requestBody, FunctionalTestHelper.jsonHeaders());
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.createifnotexist.after.csv")
    @DisplayName("Создать поставщика, если еще нет")
    void testCreateSupplierIfNotExists() {
        mockMappings(103L, Arrays.asList(
                new PartnerOffer.Builder().setShopSku("sku11-1").setPartnerId(103).setArchived(false).build(),
                new PartnerOffer.Builder().setShopSku("sku11-2").setPartnerId(103).setArchived(false).build(),
                new PartnerOffer.Builder().setShopSku("sku11-3").setPartnerId(103).setArchived(false).build()
        ));

        updateState(103, "{\n" +
                "  \"partnerId\": \"103\",\n" +
                "  \"businessId\": \"1003\",\n" +
                "  \"featureType\": \"112\",\n" +
                "  \"featureStatus\": \"SUCCESS\",\n" +
                "  \"cpaIsPartnerInterface\": true\n" +
                "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.main.after.csv")
    @DisplayName("Обновить основные поля поставщика, статус фичи и способ размещения")
    void testUpdateSupplierMainFields() {
        updateState(101,
                "{\n" +
                        "  \"partnerId\": \"101\",\n" +
                        "  \"businessId\": \"1001\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"SUCCESS\",\n" +
                        "  \"cpaIsPartnerInterface\": true\n" +
                        "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.mainWithNesuSync.after.csv")
    @DisplayName("Обновить основные поля поставщика, статус фичи и способ размещения, с синхронизацией в Несу")
    void testUpdateSupplierMainFieldsWithNesuSync() {
        environmentService.setValue(StocksByPiExperiment.NESU_EXP_VAR, "true");

        updateState(101,
                "{\n" +
                        "  \"partnerId\": \"101\",\n" +
                        "  \"businessId\": \"1001\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"DONT_WANT\",\n" +
                        "  \"cpaIsPartnerInterface\": true\n" +
                        "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.feed.update.after.csv")
    @DisplayName("Обновить основные поля поставщика и фид")
    void testUpdateSupplierMainFieldsAndUpdateFeed() {
        updateState(101,
                "{\n" +
                        "  \"partnerId\": \"101\",\n" +
                        "  \"businessId\": \"1001\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"SUCCESS\",\n" +
                        "  \"cpaIsPartnerInterface\": true, \n" +
                        "  \"feed\" : {\n" +
                        "    \"id\": 1001,\n" +
                        "    \"updatedAt\": \"2019-12-12T12:12:12\"\n" +
                        "  }\n" +
                        "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.feed.create.after.csv")
    @DisplayName("Обновить основные поля поставщика и фид")
    void testUpdateSupplierMainFieldsAndCreateFeed() {
        updateState(102,
                "{\n" +
                        "  \"partnerId\": \"102\",\n" +
                        "  \"businessId\": \"1002\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"SUCCESS\",\n" +
                        "  \"cpaIsPartnerInterface\": true, \n" +
                        "  \"feed\" : {\n" +
                        "    \"id\": 1002,\n" +
                        "    \"updatedAt\": \"2019-12-12T13:13:13\"\n" +
                        "  }\n" +
                        "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.fflink.update.after.csv")
    @DisplayName("Обновить основные поля поставщика, статус фичи и обновить связь со складом")
    void testUpdateSupplierMainFieldsAndUpdateFFLink() {
        updateState(101,
                "{\n" +
                        "  \"partnerId\": \"101\",\n" +
                        "  \"businessId\": \"1001\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"SUCCESS\",\n" +
                        "  \"cpaIsPartnerInterface\": true,\n" +
                        "  \"fulfillmentLinks\": [\n" +
                        "    {\n" +
                        "      \"serviceId\": 11,\n" +
                        "      \"feedId\": 111,\n" +
                        "      \"deliveryServiceType\": \"dropship\",\n" +
                        "      \"partnerFeedId\": 111\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");
    }

    @Test
    @DbUnitDataSet(after = "PartnerStateControllerTest.fflink.create.after.csv")
    @DisplayName("Обновить основные поля поставщика, статус фичи и создать связь со складом")
    void testUpdateSupplierMainFieldsAndCreateFFLink() {
        updateState(102,
                "{\n" +
                        "  \"partnerId\": \"102\",\n" +
                        "  \"businessId\": \"1002\",\n" +
                        "  \"featureType\": \"112\",\n" +
                        "  \"featureStatus\": \"SUCCESS\",\n" +
                        "  \"cpaIsPartnerInterface\": true,\n" +
                        "  \"fulfillmentLinks\": [\n" +
                        "    {\n" +
                        "      \"serviceId\": 12,\n" +
                        "      \"feedId\": 112,\n" +
                        "      \"deliveryServiceType\": \"dropship\",\n" +
                        "      \"partnerFeedId\": 112\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");
    }

    @Test
    @DisplayName("Получение флага обновления стоков через ПИ")
    void testGetStocksByPiFlag() {
        String url = FF4ShopsUrlBuilder.stocksByPiUrl(randomServerPort, 102);
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(url);
        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_stocks_by_pi.json");
    }

    @Test
    @DisplayName("404, если партнер не найден")
    void testGetStocksByPiFlagForUnknownPartner() {
        String url = FF4ShopsUrlBuilder.stocksByPiUrl(randomServerPort, 9999);
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(url);
        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_stocks_by_pi_unknown_partner.json");
    }

    @Test
    @DisplayName("Обновление флага обновления стоков через ПИ")
    @DbUnitDataSet(after = "PartnerStateControllerTest.updateStocksByPi.after.csv")
    void testUpdateStocksByPiFlag() {
        String req = "{ \"stocksByPartnerInterface\": \"true\" }";
        updateStocksByPi(101, req);
    }

    @Test
    @DisplayName("Обновление флага обновления стоков через ПИ с синхронизацией в MBI")
    @DbUnitDataSet(after = "PartnerStateControllerTest.updateStocksByPiWithMbiSync.after.csv")
    void testUpdateStocksByPiFlagWithMbiSync() {
        environmentService.setValue(StocksByPiExperiment.MBI_EXP_VAR, "true");

        String req = "{ \"stocksByPartnerInterface\": \"true\" }";
        updateStocksByPi(101, req);
    }

    @Test
    @DisplayName("Обновление флага обновления стоков через ПИ с синхронизацией стратегии Несу")
    @DbUnitDataSet(after = "PartnerStateControllerTest.updateStocksByPiWithNesuSync.after.csv")
    void testUpdateStocksByPiFlagWithNesuSync() {
        environmentService.setValue(StocksByPiExperiment.NESU_EXP_VAR, "true");

        String req = "{ \"stocksByPartnerInterface\": \"true\" }";
        updateStocksByPi(101, req);
    }

    @Test
    @DisplayName("Обновление флага обновления стоков через ПИ, с импортом из MBI")
    @DbUnitDataSet(after = "PartnerStateControllerTest.updateStocksByPiWithImport.after.csv")
    void testUpdateStocksByPiFlagWithImport() {
        expectMbi(103).andRespond(
                withSuccess(//language=xml
                        "<partner-state business-id=\"103\" partner-id=\"103\" feature-type=\"112\" " +
                                "feature-status=\"NEW\" cpa-is-partner-interface=\"false\" " +
                                "push-stocks-is-enabled=\"false\"/>",
                        MediaType.APPLICATION_XML
                )
        );

        String req = "{ \"stocksByPartnerInterface\": \"true\" }";
        updateStocksByPi(103, req);
    }

    @Test
    @DisplayName("Обновление флага обновления стоков через ПИ, партнера нет ни в ff4shops, ни в MBI")
    void testUpdateStocksByPiPartnerNotFound() {
        expectMbi(103).andRespond(
                withStatus(HttpStatus.NOT_FOUND)
        );

        String req = "{ \"stocksByPartnerInterface\": \"true\" }";
        ResponseEntity<String> resp = updateStocksByPi(103, req);

        assertErrorResponse(resp, HttpStatus.NOT_FOUND, ErrorSubCode.NO_SUPPLIER);
    }

    @Test
    @DisplayName("Получить партнеров по значению флага работы со стоками через ПИ, значение = true")
    void testGetPartnersByStocksByPiFlagTrue() {
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(
                FF4ShopsUrlBuilder.getPartnersByStocksByPiUrl(randomServerPort, true, null, 1000));

        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_having_stocks_by_pi_true.json");
    }

    @Test
    @DisplayName("Получить партнеров по значению флага работы со стоками через ПИ, все элементы")
    void testGetPartnersByStocksByPiFlagFalseAll() {
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(
                FF4ShopsUrlBuilder.getPartnersByStocksByPiUrl(randomServerPort, false, null, 100));

        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_having_stocks_by_pi_false_all.json");
    }

    @Test
    @DisplayName("Получить партнеров по значению флага работы со стоками через ПИ, значение = false, страница 1")
    void testGetPartnersByStocksByPiFlagFalsePage1() {
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(
                FF4ShopsUrlBuilder.getPartnersByStocksByPiUrl(randomServerPort, false, null, 1));

        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_having_stocks_by_pi_false_page_1.json");
    }

    @Test
    @DisplayName("Получить партнеров по значению флага работы со стоками через ПИ, значение = false, страница 2")
    void testGetPartnersByStocksByPiFlagFalsePage2() {
        ResponseEntity<String> response = FunctionalTestHelper.getForEntity(
                FF4ShopsUrlBuilder.getPartnersByStocksByPiUrl(randomServerPort, false, "MTAx", 1));

        assertResponseBody(response.getBody(),
                "ru/yandex/market/ff4shops/api/json/partnerstate/get_having_stocks_by_pi_false_page_2.json");
    }
}

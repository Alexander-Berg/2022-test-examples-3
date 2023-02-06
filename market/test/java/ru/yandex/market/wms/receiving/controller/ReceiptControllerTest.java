package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.constraints.client.ConstraintsClient;
import ru.yandex.market.wms.constraints.core.dto.StorageCategoryDto;
import ru.yandex.market.wms.constraints.core.response.GetStorageCategoryResponse;
import ru.yandex.market.wms.core.base.request.GetParentContainersRequest;
import ru.yandex.market.wms.core.base.response.GetLostInventoriesResponse;
import ru.yandex.market.wms.core.base.response.GetParentContainersResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.wmsapi.WmsApiClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;


@DatabaseSetup(value = "/e-sso-user/data.xml", connection = "scprdd1DboConnection")
class ReceiptControllerTest extends ReceivingIntegrationTest {
    private static final String DB_PATH = "/controller/receipt/db.xml";
    private static final String EMPTY_DB_PATH = "/empty-db.xml";
    private static final String SERIAL_INVENTORY_DB_PATH =
            "/controller/receipt/get-products-by-pallet/serial-inventory-db.xml";
    private static final String ENABLE_MULTIPLE_ANOMALY_SQL =
            "UPDATE wmwhse1.NSQLCONFIG SET nsqlvalue = '1' where configkey = 'YM_ENABLE_MULTIPLE_ITEMTYPES'";
    private static final String DISABLE_MULTIPLE_ANOMALY_SQL =
            "UPDATE wmwhse1.NSQLCONFIG SET nsqlvalue = '0' where configkey = 'YM_ENABLE_MULTIPLE_ITEMTYPES'";

    @Autowired
    @MockBean
    private WmsApiClient wmsApiClient;

    @Configuration
    public static class TestConfig {
        @Bean
        DbConfigService configService() {
            return mock(DbConfigService.class);
        }
    }

    @Autowired
    private DbConfigService configService;

    @Autowired
    @MockBean
    private CoreClient coreClient;

    @Autowired
    @MockBean
    private ConstraintsClient constraintsClient;

    @BeforeEach
    public void initConfig() {
        when(configService.getConfigAsBoolean("YM_PROVIDE_OSG_ON_RECEIVING")).thenReturn(true);
        when(constraintsClient.getStorageCategory(isA(List.class)))
                .thenReturn(new GetStorageCategoryResponse(List.of()));
        Mockito.doNothing().when(wmsApiClient).updateAltSkuByReceiptIdAsync(anyString());

    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByReceiptKey() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-receipt/receiptkey-ok-request.json",
                "controller/receipt/get-receipt/receiptkey-ok-response.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-receipt/byExternalReceiptKey2/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-receipt/byExternalReceiptKey2/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByExternalReceiptKey2() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-receipt/byExternalReceiptKey2/request.json",
                "controller/receipt/get-receipt/byExternalReceiptKey2/response.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-receipt/byPallet/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-receipt/byPallet/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByPalletId() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-receipt/byPallet/request.json",
                "controller/receipt/get-receipt/byPallet/response.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-receipt/byPallet/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-receipt/byPallet/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByNotUniquePalletId() throws Exception {
        assertApiCallError(
                "controller/receipt/get-receipt/byPallet/request-not-unique.json",
                post("/receipts/get-receipt"),
                "CONTAINER_NOT_UNIQUE"
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByPallet() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-receipt/pallet-ok-request.json",
                "controller/receipt/get-receipt/pallet-ok-response.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByPalletNotFound() throws Exception {
        assertApiCallNotFound(
                "controller/receipt/get-receipt/pallet-not-found-request.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptByBox() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-receipt/box-ok-request.json",
                "controller/receipt/get-receipt/box-ok-response.json",
                post("/receipts/get-receipt")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void getReceiptDetailByPalletEmptyUnredeemed() throws Exception {
        assertApiCallBadRequest(
                "controller/receipt/get-detail-by-pallet/pallet-empty-unredeemed-request.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void getReceiptDetailByPalletEmptyReturn() throws Exception {
        assertApiCallBadRequest(
                "controller/receipt/get-detail-by-pallet/pallet-empty-return-request.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPallet() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/pallet-ok-request.json",
                "controller/receipt/get-detail-by-pallet/pallet-ok-response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    // При запросе деталей для паллеты передаём стол приёмки,
    // при этом паллета на данный момент не привязана ни к одному столу - привязываем её к переданному столу
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/pallet-not-linked-to-table.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/pallet-linked-to-table.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletAndLinkToTable() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                "controller/receipt/get-detail-by-pallet/pallet-ok-response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/pallet-not-linked-to-table.xml")
    void getReceiptDetailByPalletLocationIdNotExist() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-wrong-locationid-request.json",
                post("/receipts/get-detail-by-pallet"),
                "Ячейка 'stage13' не найдена."
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/pallet-and-table-wrong-type.xml")
    void getReceiptDetailByPalletLocationIdWrongType() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                post("/receipts/get-detail-by-pallet"),
                "Тип отсканированной ячейки 'STAGE15' не соответствует ожидаемому."
        );
    }

    // Поставка межсклад. При запросе деталей для паллеты передаём стол приёмки,
    // при этом паллета на данный момент не привязана ни к одному столу - привязываем её к переданному столу
    // Если паллета не на балансе, ставим на баланс (эмуляция первичной приемки для межсклада).
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/pallet-not-linked-to-table-inter-wh.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/pallet-linked-to-table-inter-wh.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletAndLinkToTableInterWarehouse() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                "controller/receipt/get-detail-by-pallet/pallet-ok-response-inter-warehouse.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    // Поставка межсклад. При запросе деталей для паллеты передаём стол приёмки,
    // при этом паллета на данный момент не привязана ни к одному столу - привязываем её к переданному столу
    // Палета принята первично. Первичная приемка завершена.
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/1/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailInterWhInitiallyReceived() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/request-pl.json",
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/response-pl.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    // Межсклад. Палета принята первично. Первичная приемка не завершена.
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/2/before.xml")
    void getReceiptDetailInterWhInitiallyReceivedError() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/request-pl.json",
                post("/receipts/get-detail-by-pallet"),
                "Поставка 0000000101 в недопустимом статусе Принято по ТМ"
        );
    }

    // Межсклад. Принимаем коробку с палеты. Палета принята первично. Первичная приемка завершена.
    // Для коробки эмулируется первичная приемка.
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/3/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailInterWhInitiallyReceivedBox() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/request-box.json",
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/response-box.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    // Межсклад. Принимаем коробку с палеты. Палета не принята первично. Первичная приемка завершена.
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/4/before.xml")
    void getReceiptDetailInterWhInitiallyReceivedBoxError() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/inter-warehouse/initially-received/request-box.json",
                post("/receipts/get-detail-by-pallet"),
                "Палета для коробки P123 должна быть первично принята"
        );
    }

    // Поставка возврата. При запросе деталей для паллеты передаём стол приёмки,
    // при этом паллета на данный момент не привязана ни к одному столу - привязываем её к переданному столу
    // Если паллета не на балансе, ставим на баланс (эмуляция первичной приемки для автовозвратов).
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/customer-return/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletAndLinkToTableCustomerReturn() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                "controller/receipt/get-detail-by-pallet/box-ok-response.json",
                post("/receipts/get-detail-by-pallet"));
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/before-not-finished-initial.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/customer-return/" +
            "before-not-finished-initial.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletCustomerReturnWhenInitialReceivingIsNotClosed() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                post("/receipts/get-detail-by-pallet"),
                "Поставка 0000000101 в недопустимом статусе Принято по ТМ");
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/" +
            "initially-received-by-pallet-success-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/customer-return/" +
            "initially-received-by-pallet-success-before.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletPalletInitiallyReceivedSuccess() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-request.json",
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Description("Разрешать вторичную приемку без первичной, если поставка является автодопоставкой")
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/auto-additional/initially-received-by" +
            "-pallet-additional-success-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/customer-return/auto-additional/initially" +
            "-received-by-pallet-additional-success-before.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletAutoAdditionalReturns() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-request.json",
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/" +
            "initially-received-by-pallet-fail-before.xml")
    void getReceiptDetailByPalletPalletInitiallyReceivedShouldFail() throws Exception {
        assertApiCallBadRequest("controller/receipt/get-detail-by-pallet/customer-return/box-ok-request.json",
                post("/receipts/get-detail-by-pallet"));
    }

    @Description("Разрешить принимать вторично если первично паллеты приняты и полеты дублируются в бд")
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/customer-return/duplicate/initially-received-by-pallet" +
            "-success-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/customer-return/duplicate/initially-received" +
            "-by-pallet-success-before.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletDuplicatePallets() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-request.json",
                "controller/receipt/get-detail-by-pallet/customer-return/box-ok-response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/before-any-label-can-be-scanned.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/after-any-label-can-be-scanned.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletWhenAnyLabelCanBeScanned() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/pallet-and-table-ok-request-1.json",
                "controller/receipt/get-detail-by-pallet/any-label-can-be-scanned-response.json",
                post("/receipts/get-detail-by-pallet"));
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/before-duplicate-boxes.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/after-duplicate-boxes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletWhenReceiptKeyGiven() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/box-and-table-ok-request.json",
                "controller/receipt/get-detail-by-pallet/duplicate-boxes-response.json",
                post("/receipts/get-detail-by-pallet"));
    }

    //одна аномальная партия имеет неверный статус на момент начала приемки
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/additional-receipt/wrong-lot-status/db.xml")
    void getReceiptDetailByPalletAdditionalWrongLotStatus() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/additional-receipt/wrong-lot-status/request.json",
                post("/receipts/get-detail-by-pallet"),
                "Партия с номером 1 в таре PLT123 имеет неверный статус SHIPPED"
        );
    }

    //одна из двух аномальных партий имеет неверный статус на момент начала приемки
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/additional-receipt/multiple-wrong-lot-status/db.xml")
    void getReceiptDetailByPalletAdditionalMultipleLotsWrongLotStatus() throws Exception {
        assertApiCallError(
                "controller/receipt/get-detail-by-pallet/additional-receipt/multiple-wrong-lot-status/request.json",
                post("/receipts/get-detail-by-pallet"),
                "Партия с номером 2 в таре PLT123 имеет неверный статус SHIPPED"
        );
    }

    //одна аномальная партия имеет верный статус на момент начала приемки - статус меняется
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-changed/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/" +
            "additional-receipt/lot-status-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletOfAdditionalReceiptWithLotStatusChanged() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-changed/request.json",
                "controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-changed/response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    //одна аномальная партия имеет верный статус на момент начала приемки - статус остается тот же если был RE_RECEIVING
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-not-changed/before.xml")
    @ExpectedDatabase(
            value = "/controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-not-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletWithAdditionalLotStatusChangedForReReceiving() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-not-changed/request.json",
                "controller/receipt/get-detail-by-pallet/additional-receipt/lot-status-not-changed/response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    //обе аномальных партии имеют верный статус на момент начала приемки - статус меняется
    @Test
    @DatabaseSetup("/controller/receipt/get-detail-by-pallet/additional-receipt/multiple-lot-status-changed/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-detail-by-pallet/" +
            "additional-receipt/multiple-lot-status-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getAdditionalReceiptDetailByPalletWithMultipleLotStatusChanged() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-detail-by-pallet/additional-receipt/multiple-lot-status-changed/request.json",
                "controller/receipt/get-detail-by-pallet/additional-receipt/multiple-lot-status-changed/response.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailByPalletOfClosedReceipt() throws Exception {
        assertApiCallBadRequest(
                "controller/receipt/get-detail-by-pallet/pallet-of-closed-receipt-request.json",
                post("/receipts/get-detail-by-pallet")
        );
    }

    /*
     * Тесты на get-receipt-sku-by-barcode
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcode() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-request.json",
                "controller/receipt/get-details-by-barcode/ok-response-with-label-type-ean.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/before-additional-receipt.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/before-additional-receipt.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeWhenMultipleDetailsWithOneSku() throws Exception {
        when(constraintsClient.getStorageCategory(isA(List.class)))
                .thenAnswer(invocation -> {
                    List<SkuId> skus = invocation.getArgument(0);
                    List<StorageCategoryDto> categories = skus
                            .stream()
                            .collect(Collectors.groupingBy(Function.identity()))
                            .keySet()
                            .stream()
                            .map(it -> new StorageCategoryDto(it.getStorerKey(), it.getSku(),
                                    "ROV0000000000000000358".equals(it.getSku()) ? "EXPENSIVE" : "REGULAR",
                                    List.of("CARGO_TYPE", "DIMENSION"))
                            )
                            .collect(Collectors.toList());
                    return new GetStorageCategoryResponse(categories);
                });
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-request.json",
                "controller/receipt/get-details-by-barcode/ok-response-additional-receipt.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/customer-return/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/customer-return/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsForCustomerReturnByBarcode() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/customer-return/request.json",
                "controller/receipt/get-details-by-barcode/customer-return/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/unredeemed/by-barcode/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/unredeemed/by-barcode/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsForUnredeemedByBarcode() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/unredeemed/by-barcode/request.json",
                "controller/receipt/get-details-by-barcode/unredeemed/by-barcode/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/unredeemed/by-uit/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/unredeemed/by-uit/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsForUnredeemedByUit() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/unredeemed/by-uit/request.json",
                "controller/receipt/get-details-by-barcode/unredeemed/by-uit/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/customer-return/before-not-declared.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/customer-return/before-not-declared.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getAnomalyReceiptDetailsForCustomerReturnWhenItemIsNotDeclared() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/customer-return/request-not-declared.json",
                "controller/receipt/get-details-by-barcode/customer-return/response-not-declared.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsWhenNoReceiptWithSuchCis() throws Exception {
        assertApiCallNotFound(
                "controller/receipt/get-details-by-barcode/no-receipt-with-such-cis-request.json",
                post("/receipts/get-details-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeWithShelflife() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-with-shelflife-request.json",
                "controller/receipt/get-details-by-barcode/ok-with-shelflife-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeMultipart() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-multipart-request.json",
                "controller/receipt/get-details-by-barcode/ok-multipart-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeForPackOfUnits() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-for-packOfUntis-request.json",
                "controller/receipt/get-details-by-barcode/ok-for-packOfUntis-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeForAnomalyNotInReceipt() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/anomaly-out-of-receipt-request.json",
                "controller/receipt/get-details-by-barcode/anomaly-out-of-receipt-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeWithZeroes() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-with-leadingZeros-request.json",
                "controller/receipt/get-details-by-barcode/ok-leadingZeros-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeWithoutZeroes() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-without-leadingZeros-request.json",
                "controller/receipt/get-details-by-barcode/ok-leadingZeros-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/1/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/1/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void lotOSGshouldBeFilledFromPreviouslyReceivedSkuSamePalletSameTable() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/1/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/1/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/2/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/2/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void lotOSGshouldBeEmptyIfNoSuchSkuWasReceived() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/2/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/2/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/3/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/3/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void lotOSGshouldBeFromLastSkuReceivedOnSameTableAndPallet() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/3/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/3/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/4/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/4/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void lotOSGshouldBeFilledFromPreviouslyReceivedSkuWithUit() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/4/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/4/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/5/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/5/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
        // Сроки годности должны подставляться только для товаров принимаемых из одной палеты на одном столе
    void lotOSGshouldBeFilledFromSamePallet() throws Exception {
        //принимаем товар на одном столе из разных паллет
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/5/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/5/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/6/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/6/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void lotOSGshouldBeFilledFromSameTable() throws Exception {
        // принимаем товар из одной паллеты на 2х столах
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/6/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/6/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-osg/1/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-osg/1/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void osgShouldBeFilledOnlyForSpecificRole() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-osg/1/request.json",
                "controller/receipt/get-details-by-barcode/with-osg/1/response.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-uit.xml")
    void getReceiptDetailsByBarcodeUit() throws Exception {
        assertApiCall(
                "controller/receipt/get-details-by-barcode/uit/ok-request.json",
                "controller/receipt/get-details-by-barcode/uit/ok-response.json",
                post("/receipts/get-receipt-sku-by-barcode"),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    void getReceiptDetailsByBarcodeNoSkusToProcess() throws Exception {
        assertApiCall(
                "controller/receipt/get-details-by-barcode/uit/error-request.json",
                null,
                post("/receipts/get-receipt-sku-by-barcode"),
                status().is4xxClientError(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoReceiptDetailsAnomalyIsException() throws Exception {
        assertApiCall(
                "controller/receipt/get-details-by-barcode/uit/error-request.json",
                null,
                post("/receipts/get-receipt-sku-by-barcode"),
                status().is4xxClientError(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusHasReceiptDetailsAnomalyIsException() throws Exception {
        assertApiCall(
                "controller/receipt/get-details-by-barcode/uit/ok-request-ean.json",
                "controller/receipt/get-details-by-barcode/uit/ok-response-ean.json",
                post("/receipts/get-receipt-sku-by-barcode"),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptDetailsByBarcodeWithLottable() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/ok-request-with-lottable.json",
                "controller/receipt/get-details-by-barcode/ok-response-with-lottable.json",
                post("/receipts/get-receipt-sku-by-barcode")
        );
    }


    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void failNoEanAnomaly() throws Exception {
        jdbc.update(ENABLE_MULTIPLE_ANOMALY_SQL);
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/no-receipt-with-such-cis-request.json",
                "controller/receipt/get-details-by-barcode/no-receipt-with-such-cis-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
                        .with(user("TEST").authorities((GrantedAuthority) () -> "OTHER_ROLE"))
        );
        jdbc.update(DISABLE_MULTIPLE_ANOMALY_SQL);
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void failNoSkuForEanAnomaly() throws Exception {
        jdbc.update(ENABLE_MULTIPLE_ANOMALY_SQL);
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/anomaly-unknown-sku-request.json",
                "controller/receipt/get-details-by-barcode/anomaly-unknown-sku-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
                        .with(user("TEST").authorities((GrantedAuthority) () -> "OTHER_ROLE"))
        );
        jdbc.update(DISABLE_MULTIPLE_ANOMALY_SQL);
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void failNoReceiptDetailForSkuAnomaly() throws Exception {
        jdbc.update(ENABLE_MULTIPLE_ANOMALY_SQL);
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/anomaly-out-of-receipt-request.json",
                "controller/receipt/get-details-by-barcode/anomaly-out-of-receipt-response.json",
                post("/receipts/get-receipt-sku-by-barcode")
                        .with(user("TEST").authorities((GrantedAuthority) () -> "OTHER_ROLE"))
        );
        jdbc.update(DISABLE_MULTIPLE_ANOMALY_SQL);
    }
    /*
     * Конец тестов на get-receipt-sku-by-barcode
     */

    @Test
    void getEan128Information() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-of-ean128/ok-ean128-request.json",
                "controller/receipt/get-details-of-ean128/ok-ean128-response.json",
                post("/receipts/get-details-of-ean128")
        );
    }

    @Test
    void getEan128WithDelimiter() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-of-ean128/ok-ean128-with-delimiter-request.json",
                "controller/receipt/get-details-of-ean128/ok-ean128-with-delimiter-response.json",
                post("/receipts/get-details-of-ean128")
        );
    }

    @Test
    void getEan128EmptyBarcodeReturnsBadRequest() throws Exception {
        assertApiCall("controller/receipt/get-details-of-ean128/ean128-empty-barcode-request.json",
                null,
                post("/receipts/get-details-of-ean128"),
                status().isBadRequest(),
                null);
    }

    @Test
    void getEan128okBulkTest() throws Exception {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        Object obj = parser.parse(getFileContent("controller/receipt/get-details-of-ean128/ok-bulk-test.json"));
        JSONArray tests = (JSONArray) obj;

        for (Object test : tests) {
            JSONObject testData = (JSONObject) test;

            String testId = testData.getAsString("testId");
            String request = testData.getAsString("request");
            String response = testData.getAsString("response");

            MvcResult mvcResult = mockMvc.perform(post("/receipts/get-details-of-ean128")
                            .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andReturn();

            JSONAssert.assertEquals(String.format("Bulk test failure on testId %s. ", testId),
                    response,
                    mvcResult.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getSkusFromReceipt() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-skus-by-receipt/ok-request.json",
                "controller/receipt/get-skus-by-receipt/ok-response.json",
                post("/receipts/get-skus-by-receipt")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-skus-by-receipt/db-assortment/common-before.xml")
    @DatabaseSetup("/controller/receipt/get-skus-by-receipt/db-assortment/receipts-before.xml")
    @DatabaseSetup("/controller/receipt/get-skus-by-receipt/db-assortment/details-before.xml")
    void getSkusFromReceiptAssortment() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-skus-by-receipt/assortment-ok-request.json",
                "controller/receipt/get-skus-by-receipt/assortment-ok-response.json",
                post("/receipts/get-skus-by-receipt")
        );
    }

    @Test
    @DatabaseSetup(SERIAL_INVENTORY_DB_PATH)
    @ExpectedDatabase(value = SERIAL_INVENTORY_DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getProductsByPallet() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-products-by-pallet/pallet-ok-request.json",
                "controller/receipt/get-products-by-pallet/pallet-ok-response.json",
                post("/receipts/get-products-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(value = "/controller/receipt/get-products-by-pallet/serial-inventory-boxes.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-products-by-pallet/serial-inventory-boxes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getProductsByPalletWithBoxes() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-products-by-pallet/pallet-ok-request.json",
                "controller/receipt/get-products-by-pallet/pallet-ok-response-boxes.json",
                post("/receipts/get-products-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(SERIAL_INVENTORY_DB_PATH)
    @ExpectedDatabase(value = SERIAL_INVENTORY_DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getProductsByNullPallet() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-products-by-pallet/null-pallet-request.json",
                "controller/receipt/get-products-by-pallet/null-pallet-response.json",
                post("/receipts/get-products-by-pallet")
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void findInventarizationReceipt() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-inventarization-receipt/request.json",
                "controller/receipt/get-inventarization-receipt/ok-response.json",
                post("/receipts/get-inventarization-receipt")
        );
    }

    @Test
    @DatabaseSetup(EMPTY_DB_PATH)
    void findNoInventarizationReceipt() throws Exception {
        assertApiCallNotFound(
                "controller/receipt/get-inventarization-receipt/request.json",
                post("/receipts/get-inventarization-receipt")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/db.xml")
    @ExpectedDatabase(value = "/controller/receipt/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSequentialPageLoadWithFilter() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/receipts")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "type")
                    .param("order", "desc")
                    .param("filter", "externreceiptkey==852944")
                    .param("limit", "3")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(content().json(getFileContent(String.format(
                            "controller/receipt/get-receipt/get-receipts-response-%s.json", i + 1))));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testReceiptDownloadXlsWithoutFilter() throws Exception {
        mockMvc.perform(get("/receipts/download/{format}", "xls"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testReceiptDownloadXlsWithFilter() throws Exception {
        mockMvc.perform(get("/receipts/download/{format}", "xls")
                        .param("filter", "TYPE==DEFAULT;STATUS==IN_RECEIVING"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testReceiptSummaryDownloadXls() throws Exception {
        mockMvc.perform(get("/receipts/receipt-summary/{receiptKey}/download/{format}", "0000000101", "xls"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testReceiptSummaryDownloadPdf() throws Exception {
        mockMvc.perform(get("/receipts/receipt-summary/{receiptKey}/download/{format}", "0000000101", "pdf"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testClosedReceiptSummaryNoDiscr() throws Exception {
        ResultActions result = mockMvc.perform(get("/receipts/receipt-summary/0000000105")
                .param("discrepancy", "true")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/receipt/receipt-summary/receipt-summary-no-discr.json"), true));
    }

    @Test
    @DatabaseSetup(DB_PATH)
    void testReceiptSummaryFilter() throws Exception {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

        ResultActions[] resultActions = {
                mockMvc.perform(get("/receipts/receipt-summary/0000000101")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()),
                mockMvc.perform(get("/receipts/receipt-summary/0000000101")
                        .param("discrepancy", "true")
                        .param("filter", "SKU==ROV0000000000000000360")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        };

        String[] responsesTotal = new String[resultActions.length];

        for (int i = 0; i < resultActions.length; i++) {
            JSONObject obj = (JSONObject) parser.parse(resultActions[i].andReturn().getResponse().getContentAsString());
            responsesTotal[i] = obj.getAsString("totalsContent");
        }

        assertions.assertThat(responsesTotal[0]).isEqualTo(responsesTotal[1]);
    }

    @Test
    @DatabaseSetup("/controller/receipt/receipt-summary/multiplace.xml")
    void testReceiptSummaryWithMultiplace() throws Exception {
        ResultActions result =
                mockMvc.perform(get("/receipts/receipt-summary/0000000101")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/receipt/receipt-summary/receipt-summary-response-multiplace.json"), false));
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH,
            assertionMode = NON_STRICT_UNORDERED)
    void testGetReceiptStatusHistoryFilterByStatus() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/receipts/receipt-status-history/0000000101")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "adddate")
                    .param("order", "DESC")
                    .param("filter", "status==IN_RECEIVING;addWho==ad2")
                    .param("limit", "7")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk()).andExpect(content().json(getFileContent(String.format(
                    "controller/receipt/get-receipt-status-history/get-receipt-status-history-response-%s.json",
                    i + 1))));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH,
            assertionMode = NON_STRICT_UNORDERED)
    void testReceiptSummaryController() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/receipts/receipt-summary/0000000101")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "SKU")
                    .param("order", "DESC")
                    .param("filter", "sku!=ROV0000000000000000359")
                    .param("limit", "4")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk()).andExpect(content().json(getFileContent(String.format(
                    "controller/receipt/receipt-summary/receipt-summary-response-%s.json", i + 1)), true));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup("/controller/receipt/post-reopen-receipts/db/receipts-before.xml")
    @DatabaseSetup("/controller/receipt/post-reopen-receipts/db/common-before.xml")
    @DatabaseSetup("/controller/receipt/post-reopen-receipts/db/details-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/post-reopen-receipts/db/receipts-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/receipt/post-reopen-receipts/db/common-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/receipt/post-reopen-receipts/db/details-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void reopenReceiptBatchOk() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipts/reopen-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt/post-reopen-receipts/request/batch-receiptkeys.json")));
        result.andExpect(status().isOk()).andExpect(content()
                .json(getFileContent("controller/receipt/post-reopen-receipts/response/all-ok-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt/post-reopen-receipts/db/receipts-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/post-reopen-receipts/db/receipts-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void reopenReceiptWrongStatus() throws Exception {
        assertApiCall("controller/receipt/post-reopen-receipts/request/batch-receiptkeys-wrong-status.json",
                "controller/receipt/post-reopen-receipts/response/wrong-status-response.json",
                post("/receipts/reopen-receipt"),
                status().isBadRequest(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("/controller/receipt/post-reopen-receipts/db/receipt-before.xml")
    @ExpectedDatabase(value = "/controller/receipt/post-reopen-receipts/db/receipt-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void reopenReceiptToPalletAcceptance() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipts/reopen-receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt/post-reopen-receipts/request/receipt.json")));
        result.andExpect(status().isOk()).andExpect(content()
                .json(getFileContent("controller/receipt/post-reopen-receipts/response/receipt.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-1.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification/after-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceiptWithVerification() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-response-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-2.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification/after-2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceiptWithVerificationOneWithError() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/response-ok-with-error.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-3.xml")
    void closeReceiptWithVerificationNotApplicableStatus() throws Exception {
        assertApiCall("controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/bad-request-response.json",
                post("/receipts/close-receipt-with-verification"),
                status().isBadRequest(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-4.xml")
    void closeReceiptWithVerificationClosed() throws Exception {
        assertApiCall("controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-closed-response-nok.json",
                post("/receipts/close-receipt-with-verification"),
                status().isBadRequest(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-5.xml")
    void closeReceiptWithVerificationWithAnomalies() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-anomalies-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-6.xml")
    void closeReceiptWithVerificationWithDiscrepancyInMultiplaceSku() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-multiplace-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-8.xml")
    void closeReceiptWithVerificationWithBomLackInMultiplaceSku() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-multiplace-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-12.xml")
    void closeReceiptWithVerificationWithOnlyOneBomInMultiplaceSku() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-interwh-bom-request.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-one-bom-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-9.xml")
    void closeReceiptWithVerificationWithNoBomsInMultiplaceSku() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-response-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-7.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification/after-3.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeAdditionalReceiptWithUntouchedAnomalyLots() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-response-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-10.xml")
    void closeAdditionalReceiptWithReReceiving() throws Exception {
        assertApiCallOk("controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-re_receiving-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-11.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification/after-11.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeInterWhDmgFlagOff() throws Exception {
        when(configService.getConfigAsBoolean("YM_VALIDATE_BOM_ON_RECEIPT_CLOSE")).thenReturn(false);
        assertApiCallOk("controller/receipt/close-receipt-with-verification/close-interwh-bom-request.json",
                "controller/receipt/close-receipt-with-verification/close-interwh-bom-response-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-11.xml")
    void closeInterWhDmgFlagOn() throws Exception {
        when(configService.getConfigAsBoolean("YM_VALIDATE_BOM_ON_RECEIPT_CLOSE")).thenReturn(true);
        assertApiCallOk("controller/receipt/close-receipt-with-verification/close-interwh-bom-request.json",
                "controller/receipt/close-receipt-with-verification/close-interwh-bom-response-nok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/before-without-details.xml")
    void closeReceiptWithVerificationWithoutDetails() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/bad-request-response-without-details.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification/balance/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification/balance/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void subtractBalance() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification/close-receipt-request-ok.json",
                "controller/receipt/close-receipt-with-verification/close-receipt-response-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    // InitiallyReceived by box; force = false
    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-box/before-1.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-box/after-1" +
            ".xml",
            assertionMode = NON_STRICT_UNORDERED)
    void notReceivedBoxesInitialByBox() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-box/close-receipt-request" +
                        "-ok.json",
                "controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-box/close-receipt-response" +
                        "-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    // InitiallyReceived by pallet; force = false
    @Test
    @DatabaseSetup("/controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-pl/before-1.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-pl/after-1" +
            ".xml",
            assertionMode = NON_STRICT_UNORDERED)
    void notReceivedBoxesInitialByPl() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-pl/close-receipt-request" +
                        "-ok.json",
                "controller/receipt/close-receipt-with-verification-precheck/unredeemed-by-pl/close-receipt-response" +
                        "-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-without-details.xml")
    void closeReceiptWithoutDetails() throws Exception {
        assertApiCallOk("controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/bad-request-response-without-details.json",
                post("/receipts/close")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-1.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt/after-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceipt() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/close-receipt-response-ok.json",
                post("/receipts/close")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-2.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt/after-2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceiptOneWithError() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/response-ok-with-error.json",
                post("/receipts/close")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-4.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt/after-4.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceiptOneWithErrorShouldNotCloseTrailer() throws Exception {
        assertApiCallOk(
                "controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/response-ok-with-error.json",
                post("/receipts/close")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-3.xml")
    void closeReceiptNotApplicableStatus() throws Exception {
        assertApiCall("controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/bad-request-response.json",
                post("/receipts/close"),
                status().isBadRequest(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-initially-received.xml")
    void closeReceiptNotApplicableStatusPalletAcceptance() throws Exception {
        assertApiCall("controller/receipt/close-receipt/close-receipt-request-ok.json",
                "controller/receipt/close-receipt/bad-request-response-initially-received.json",
                post("/receipts/close"),
                status().isBadRequest(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("/controller/receipt/close-receipt/before-5.xml")
    @ExpectedDatabase(value = "/controller/receipt/close-receipt/after-5.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeReceiptBbxd() throws Exception {
        when(coreClient.getLostSerials("0000000101")).thenReturn(
                new GetLostInventoriesResponse(Collections.emptyList()));
        when(coreClient.getParentContainers(new GetParentContainersRequest(List.of("BOX1"))))
                .thenReturn(new GetParentContainersResponse(List.of("PLT123")));
        assertApiCallOk(
                "controller/receipt/close-receipt/close-receipt-request-single-ok.json",
                "controller/receipt/close-receipt/close-receipt-response-single-ok.json",
                post("/receipts/close-receipt-with-verification")
        );
    }

    @Test
    @DatabaseSetup("/controller/receipt/get-details-by-barcode/with-gold-sku/before.xml")
    @ExpectedDatabase(value = "/controller/receipt/get-details-by-barcode/with-gold-sku/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void goldSkuDataShouldBeFilled() throws Exception {
        assertApiCallOk(
                "controller/receipt/get-details-by-barcode/with-gold-sku/request.json",
                "controller/receipt/get-details-by-barcode/with-gold-sku/response.json",
                post("/receipts/get-receipt-sku-by-barcode"));
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptsBadRequest() throws Exception {
        assertApiCallBadRequest(
                get("/receipts?filter=%28expectedreceiptdate%3D%3D%27CLOSED%27%29&offset=0&limit=35")
        );
    }

    private void assertApiCallBadRequest(String requestFile, MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, null, request, status().isBadRequest(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private void assertApiCallBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(request, status().isBadRequest());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptInfoByReceiptKey() throws Exception {
        String receiptKey = getValueFromFile(
                "controller/receipt/get-receipt/receipt-info-ok-request.json",
                "receiptKey");

        assertGetApiCall(
                "controller/receipt/get-receipt/receipt-info-ok-response.json",
                get("/receipt/" + receiptKey),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptInfoByReceiptKeyWithEmptyComment() throws Exception {
        String receiptKey = getValueFromFile(
                "controller/receipt/get-receipt/receipt-info-with-empty-comment-ok-request.json",
                "receiptKey");

        assertGetApiCall(
                "controller/receipt/get-receipt/receipt-info-with-empty-comment-ok-response.json",
                get("/receipt/" + receiptKey),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptInfoByReceiptKeyWithZeroPallets() throws Exception {
        String receiptKey = getValueFromFile(
                "controller/receipt/get-receipt/receipt-info-ok-zero-pallet-request.json",
                "receiptKey");
        assertGetApiCall(
                "controller/receipt/get-receipt/receipt-info-ok-zero-pallet-response.json",
                get("/receipt/" + receiptKey),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getNonExistingReceipt() throws Exception {
        String receiptKey = getValueFromFile(
                "controller/receipt/get-receipt/non-existing-receipt-request.json",
                "receiptKey");

        mockMvc.perform(get("/receipt/" + receiptKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getReceiptProcessedByMultipleUsers() throws Exception {
        String receiptKey = getValueFromFile(
                "controller/receipt/get-receipt/receipt-info-ok-multi-receiving-users-request.json",
                "receiptKey");

        assertGetApiCall(
                "controller/receipt/get-receipt/receipt-info-ok-multi-receiving-users-response.json",
                get("/receipt/" + receiptKey),
                status().isOk(), JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void toggleReceiptServiceCoeff() throws Exception {
        String receiptKey = "0000000101";
        String serviceCode = "HIGH_PRIORITY";
        assertApiCallOk(
                "controller/receipt/services/receipt-services-ok-request.json",
                null,
                put("/receipts/" + receiptKey + "/services/" + serviceCode));
    }

    private String getValueFromFile(String filePath, String key) throws Exception {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        Object obj = parser.parse(getFileContent(filePath));
        JSONObject data = (JSONObject) obj;
        return (String) data.get(key);
    }

    private void assertGetApiCall(String responseFile,
                                  MockHttpServletRequestBuilder request, ResultMatcher status,
                                  JSONCompareMode mode) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status)
                .andReturn();

        if (responseFile != null) {
            JsonAssertUtils.assertFileEquals(
                    responseFile,
                    mvcResult.getResponse().getContentAsString(),
                    mode
            );
        }
    }

    private void assertApiCallNotFound(String requestFile, MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, null, request, status().isNotFound(), null);
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isOk(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private void assertApiCallError(String requestFile, MockHttpServletRequestBuilder request, String errorInfo)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo);
    }

    private void assertApiCall(MockHttpServletRequestBuilder request, ResultMatcher status) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status)
                .andReturn();
    }

    private void assertApiCall(String requestFile, String responseFile,
                               MockHttpServletRequestBuilder request, ResultMatcher status,
                               JSONCompareMode mode) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status)
                .andReturn();
        String res = mvcResult.getResponse().getContentAsString();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(
                    responseFile,
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                    mode
            );
        }
    }
}

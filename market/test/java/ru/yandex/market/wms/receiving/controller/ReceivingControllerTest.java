// CHECKSTYLE:OFF: FileLength

package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailItem;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailUit;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailIdentityDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailItemDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailUitDao;
import ru.yandex.market.wms.common.spring.dto.subs.SubtitlesDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptAnomalyTagDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptFitTagDto;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailIdentity;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.service.CacheableFileReader;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.constraints.client.ConstraintsClient;
import ru.yandex.market.wms.constraints.core.dto.StorageCategoryDto;
import ru.yandex.market.wms.constraints.core.response.GetStorageCategoryResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.dimensionmanagement.client.DimensionManagementClient;
import ru.yandex.market.wms.dimensionmanagement.core.dto.MeasurementOrderDto;
import ru.yandex.market.wms.dimensionmanagement.core.request.GetActiveMeasurementOrderRequest;
import ru.yandex.market.wms.dimensionmanagement.core.response.GetMeasurementOrdersResponse;
import ru.yandex.market.wms.dimensionmanagement.core.response.RemainingCapacityResponse;
import ru.yandex.market.wms.dimensionmanagement.core.response.ValidateDimensionsResponse;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.straight.ReceivingSerialInventoryService;
import ru.yandex.market.wms.shared.libs.business.logger.BusinessLogger;
import ru.yandex.market.wms.shared.libs.label.printer.domain.dto.SerialNumberPrinterData;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.SerialNumberPrinter;
import ru.yandex.market.wms.transportation.client.TransportationClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
@DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
class ReceivingControllerTest extends ReceivingIntegrationTest {

    @SpyBean
    @Autowired
    TransportationClient transportationClient;
    @MockBean
    @Autowired
    private CacheableFileReader cacheableFileReader;
    @SpyBean
    @Autowired
    private ReceivingSerialInventoryService receivingSerialInventoryService;
    @SpyBean
    @Autowired
    private SerialNumberPrinter serialNumberPrinter;
    @Autowired
    @SpyBean
    private ServicebusClient servicebusClient;
    @SpyBean
    @Autowired
    private SerialInventoryService serialInventoryService;
    @SpyBean
    @Autowired
    private BusinessLogger businessLogger;
    @MockBean
    @Autowired
    private FulfillmentCteClientApi cteClient;
    @MockBean
    @Autowired
    private DimensionManagementClient dimensionManagementClient;

    @MockBean
    @Autowired
    private ConstraintsClient constraintsClient;

    @Autowired
    private ReceiptDetailIdentityDao receiptDetailIdentityDao;
    @Autowired
    private ReceiptDetailUitDao receiptDetailUitDao;
    @Autowired
    private ReceiptDetailItemDao receiptDetailItemDao;

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @BeforeEach
    public void init() {
        super.init();
        Mockito.doReturn("SERIAL_INVENTORY_TEMPLATE").when(cacheableFileReader)
                .readFile(eq("/opt/infor/sce/scprd/wm/labels/"), eq("SERIAL_RF.zpl"));
        Mockito.doReturn("^XA^XZ^XA$@BOXID@$^XZ").when(cacheableFileReader)
                .readFile(eq("/opt/infor/sce/scprd/wm/labels/"), eq("box_label.zpl"));
        Mockito.doReturn(new GetStorageCategoryResponse(Collections.emptyList())).when(constraintsClient)
                .getStorageCategory(anyList());
    }

    @AfterEach
    public void reset() {
        Mockito.reset(cacheableFileReader, receivingSerialInventoryService, serialNumberPrinter);
        Mockito.reset(
                dimensionManagementClient, servicebusClient, transportationClient, serialInventoryService, cteClient,
                constraintsClient, coreClient);
    }

    /* Срок годности в будущем и превосходит inbound. Товар отмечен как Stage */
    @Test
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/expirationDateOnly/1/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/expirationDateOnly/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createExpirationDateOnlySerialInventoriesStageLoc() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/expirationDateOnly/1/request.json",
                "controller/crossdock/create-serials/expirationDateOnly/1/response.json",
                post("/crossdock/receive-items"));
    }

    /* Срок годности текущий день. Товар помещен отмечен как Damage */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/expirationDateOnly/2/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/expirationDateOnly/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createExpirationDateOnlySerialInventoriesCurrentDateDamageLoc() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/expirationDateOnly/2/request.json",
                "controller/crossdock/create-serials/expirationDateOnly/2/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Когда срок годности вычислялся в будущем при шаблоне EXPIRATION_DATE (не путать с EXPIRATION_DATE_ONLY)
     * Случай, когда случался косяк, если приёмка случалась _после_ 11 часов по UTC (UI слал datetime > 11:00 UTC)
     * В тестовых бинах время сервера в 12:34UTC, => переставил время в запросе, хоть это и отличается от боевой
     * реальности.
     * Допущение: пусть в будущем, но зато того же дня. Пока клиент не научится слать свой часовой пояс.
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/expirationDate/1/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/expirationDate/1/after_later.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createExpirationDateSerialInventoriesFutureCreateDateLater() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/expirationDate/1/request_later_hour_than_server_clock.json",
                "controller/crossdock/create-serials/expirationDate/1/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Когда срок годности вычислялся в будущем при шаблоне EXPIRATION_DATE (не путать с EXPIRATION_DATE_ONLY)
     * Случай, когда было изначально ОК, если приёмка случалась _до_ 11 часов по UTC (UI слал datetime  с 11:00 UTC)
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/expirationDate/1/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/expirationDate/1/after_earlier.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createExpirationDateSerialInventoriesFutureCreateDateEarlier() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/expirationDate/1/request_earlier_hour_than_server_clock.json",
                "controller/crossdock/create-serials/expirationDate/1/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Когда срок годности вычислялся в будущем при шаблоне EXPIRATION_DATE (не путать с EXPIRATION_DATE_ONLY)
     * Неоднозначный случай, если будет указано время 24:00 (эквивалент 00:00 следующего дня)
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/expirationDate/1/before.xml",
            type = DatabaseOperation.INSERT)
    void createExpirationDateSerialInventoriesAmbigousHour() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/expirationDate/1/request_ambiguous_hour_of_day.json",
                post("/crossdock/receive-items"),
                "Дата производства не должна быть в будущем");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok-identity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithCisAndValidGtin() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-validation-error.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithCisAndQuantityMoreThanOne() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/cis/request-several-items-at-once.json",
                post("/crossdock/receive-items"),
                "Для товаров с идентификаторами не может быть сформировано более одного УИТ за раз");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku-gtin13.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok-identity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithCisAndValidGtin13() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-validation-error.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithCisAndInvalidGtin() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response-anomaly-invalid-gtin.json",
                post("/crossdock/receive-items"));

    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt-inter-warehouse.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-damage-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventorieForInterWarehouseWithNoCisInRequest() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/invalid-request-with-no-cis.json",
                "controller/crossdock/create-serials/cis/response-identity-absent.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before-when-accept-only-declared-cis.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/inbound-cis-identity.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok-identity-declared.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithDeclaredCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response.json",
                post("/crossdock/receive-items"));

        // check that uit and cis both have the same uuid
        List<ReceiptDetailUit> receivedUits = receiptDetailUitDao.findReceived("0000000101");
        Optional<ReceiptDetailIdentity> receivedCis = receiptDetailIdentityDao.findByPrimaryKey(
                ReceiptDetailIdentity.PK.builder()
                        .receiptDetailKey(new ReceiptDetailKey("0000000101", "00003"))
                        .type(TypeOfIdentity.CIS)
                        .value("010467003301005321gJk6o54AQBJfX#GS#2406401#GS#91ffd0#GS#92LGYcm3FRQrRdNOO+8t0pz78QT" +
                                "yxxBmYKhLXaAS03jKV7oy+DWGy1SeU+BZ8o7B8+hs9LvPdNA7B6NPGjrCm34A==")
                        .build());
        Assertions.assertEquals(1, receivedUits.size());
        Assertions.assertTrue(receivedCis.isPresent());
        Assertions.assertEquals(receivedUits.get(0).getUuid(), receivedCis.get().getUuid());
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before-when-accept-only-declared-cis.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-validation-error.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithMustBeButNotDeclaredCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response-anomaly-not-declared.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt-inter-warehouse.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/inbound-cis-identity.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok-identity-declared.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithDeclaredCisInInterWarehouseInbound() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request.json",
                "controller/crossdock/create-serials/cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt-inter-warehouse.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-damage-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithInvalidCisInInterWarehouseInbound() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request-with-invalid-cis.json",
                "controller/crossdock/create-serials/cis/response-damage-cis.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/cis/altsku.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/cis/after-ok-identity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoriesWithCisAndPrefix() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/cis/request-prefix.json",
                "controller/crossdock/create-serials/cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/fefo-notice/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/fefo-notice/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createExpirationDateOnlySerialInventoriesWithFefoNotice() throws Exception {
        doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            String template = (String) args[1];
            Assertions.assertEquals("Для партии SKU рекомендовано размещение на хранение.", template);
            return null;
        }).when(businessLogger).info(anyString(), anyString(), anyString(), any());

        assertApiCallOk(
                "controller/crossdock/create-serials/fefo-notice/request.json",
                "controller/crossdock/create-serials/fefo-notice/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/fefo-notice/before-second.xml",
            type = DatabaseOperation.INSERT)
    void createExpirationDateOnlySerialInventoriesWithFefoNoticeSecond() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/fefo-notice/request.json",
                "controller/crossdock/create-serials/fefo-notice/response-second.json",
                post("/crossdock/receive-items"));
    }

    /*
    принимаемый товар - не излишек
     */
    @Test
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-ok/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-ok/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusOk() throws Exception {
        assertCheckConfirmedSku("surplus-ok");
    }

    /*
    принимаемый товар - разрешенный излишек
     */
    @Test
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-warn/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-warn/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusWarn() throws Exception {
        assertCheckConfirmedSku("surplus-warn");
    }

    /*
    принимаемый товар - запрещенный излишек
     */
    @Test
    @Disabled
    @Deprecated(forRemoval = true)
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-error/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-error/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusError() throws Exception {
        assertCheckConfirmedSku("surplus-error");
    }

    /*
    все 5 штук принимаемого товара - не излишек
     */
    @Test
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-ok/before-qty.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-ok/before-qty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusOkQty() throws Exception {
        assertApiCallOk(
                "controller/crossdock/check-confirmed-sku/surplus-ok/request-qty5.json",
                "controller/crossdock/check-confirmed-sku/surplus-ok/response.json",
                post("/crossdock/check-confirmed-sku"));
    }

    /*
    2 штуки принимаемого товара - разрешенный излишек
     */
    @Test
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-warn/before-qty.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-warn/before-qty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusWarnQty() throws Exception {
        assertApiCallOk(
                "controller/crossdock/check-confirmed-sku/surplus-warn/request-qty5.json",
                "controller/crossdock/check-confirmed-sku/surplus-warn/response.json",
                post("/crossdock/check-confirmed-sku"));
    }

    /*
    2 штуки принимаемого товара - запрещенный излишек
     */
    @Test
    @DatabaseSetup("/controller/crossdock/check-confirmed-sku/surplus-error/before-qty.xml")
    @ExpectedDatabase(value = "/controller/crossdock/check-confirmed-sku/surplus-error/before-qty.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuWhenSurplusErrorQty() throws Exception {
        assertApiCallOk(
                "controller/crossdock/check-confirmed-sku/surplus-error/request-qty5.json",
                "controller/crossdock/check-confirmed-sku/surplus-error/response-qty5.json",
                post("/crossdock/check-confirmed-sku"));
    }

    /*
    тест на создание 2 УИТов
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/standard/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/standard/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTwoSerialInventories() throws Exception {

        assertApiCallOk(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-2.json",
                "controller/crossdock/create-serials/standard/response-standard.json",
                post("/crossdock/receive-items"));
        Mockito.verify(receivingSerialInventoryService).printSerialNumbers(
                anyList(), isA(String.class), isA(String.class)
        );
        // check that a row in RECEIPTDETAILITEM was created for every row in RECEIPRDETAILUIT with the same uuid
        List<ReceiptDetailUit> receivedUits = receiptDetailUitDao.findReceived("0000000101");
        Assertions.assertEquals(2, receivedUits.size());
        Optional<ReceiptDetailItem> receiptDetailItem1 = receiptDetailItemDao.findByUuid(receivedUits.get(0).getUuid());
        Assertions.assertTrue(receiptDetailItem1.isPresent());
        Optional<ReceiptDetailItem> receiptDetailItem2 = receiptDetailItemDao.findByUuid(receivedUits.get(1).getUuid());
        Assertions.assertTrue(receiptDetailItem2.isPresent());
    }

    /*
    тест на пересоздание УИТа, не имеющего префикса
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/standard/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/standard/after-reprint-uit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createNewUitWhenOldInRequest() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-1-with-old-uit.json",
                "controller/crossdock/create-serials/standard/response-reprinted-uit.json",
                post("/crossdock/receive-items"));
    }

    /*
    тест на создание 150 УИТов
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/standard/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/standard/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTooMuchInventories() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-150.json",
                post("/crossdock/receive-items"),
                "некорректное количество");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_ok/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_ok/after" +
            ".xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkAdditionalDelivery1PSkuDeclaredOnStock() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_ok/request.json",
                "controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_ok/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_surplus/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_surplus" +
            "/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkAdditionalDelivery1PSkuDeclaredQtySurplus() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_surplus/request.json",
                "controller/crossdock/receive-items/additional-delivery/1p/sku_declared/qty_surplus/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/additional-delivery/1p/sku_not_declared/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/additional-delivery/1p/sku_not_declared/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkAdditionalDelivery1PSkuNotDeclared() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/additional-delivery/1p/sku_not_declared/request.json",
                "controller/crossdock/receive-items/additional-delivery/1p/sku_not_declared/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/additional-delivery/3p/qty_ok/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/additional-delivery/3p/qty_ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkAdditionalDelivery3PSkuDeclaredOnStock() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/additional-delivery/3p/" +
                        "qty_ok/request.json",
                "controller/crossdock/receive-items/additional-delivery/3p/qty_ok/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/additional-delivery/3p/qty_surplus/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/additional-delivery/3p/qty_surplus/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkAdditionalDelivery3PSkuDeclaredSurplusOnStock() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/additional-delivery/3p/" +
                        "qty_surplus/request.json",
                "controller/crossdock/receive-items/additional-delivery/3p/qty_surplus/response.json",
                post("/crossdock/receive-items"));
    }

    /*
    тест на создание 150 УИТов для виртуального принтера
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/nouit/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/nouit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesWithVirtualPrinter() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-virtual.json",
                "controller/crossdock/create-serials/nouit/response-150.json",
                post("/crossdock/receive-items"));
        Mockito.verifyNoMoreInteractions(receivingSerialInventoryService);
    }

    /*
    тест на приемку коробами
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/receive-boxes-wrong-status/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes/before.xml",
            type = DatabaseOperation.INSERT)
    void createInventoriesBoxesWrongReceiptStatus() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes/request.json",
                post("/crossdock/receive-item-boxes"),
                "Поставка 0000000101 в недопустимом статусе Получено"
        );
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    тест на приемку коробами в неверном статусе
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxes() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/receive-boxes/request.json",
                "controller/crossdock/create-serials/receive-boxes/response.json",
                post("/crossdock/receive-item-boxes"));
    }

    /*
    тест на приемку на тару с неверным префиксом
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-wrong-prefix/before.xml",
            type = DatabaseOperation.INSERT)
    void createInventoriesBoxesWrongTarePrefix() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes-wrong-prefix/request.json",
                post("/crossdock/receive-item-boxes"),
                "Container id AN123123 does not match regex"
        );
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    негативный тест на приемку коробами c излишками
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-surplus/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-surplus/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesWithSurplus() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes-surplus/request.json",
                post("/crossdock/receive-item-boxes"),
                "Прием излишков запрещен, можно принять не более 12"
        );
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    негативный тест на приемку коробами (приемка коробами запрещена на sku)
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-prohibited/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-prohibited/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesWhenSkuProhibited() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes/request.json",
                post("/crossdock/receive-item-boxes"),
                "ROV0000000000000000359 - запрещен для приемки без УИТов");
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    негативный тест на приемку коробами (приемка коробами запрещена для многоместного товара)
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-multi-seat/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-multi-seat/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesWhenSkuMultiSeat() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes/request.json",
                post("/crossdock/receive-item-boxes"),
                "ROV0000000000000000359 многоместный товар запрещен для приемки без УИТов");
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    негативный тест на приемку коробами (приемка коробками запрещена с честным знаком) данные в запросе
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-with-cis/before-without-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-with-cis/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesWhenWithCisFromRequest() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes-with-cis/request-with-cis.json",
                post("/crossdock/receive-item-boxes"),
                "ROV0000000000000000359 товар с маркировками запрещен для приемки без УИТов");
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    негативный тест на приемку коробами (приемка коробками запрещена с честным знаком) данные в бд
     */

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-with-cis/before-with-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-with-cis/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesWhenWithCisFromDb() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/receive-boxes-with-cis/request-without-cis.json",
                post("/crossdock/receive-item-boxes"),
                "ROV0000000000000000359 товар с маркировками запрещен для приемки без УИТов");
        Mockito.verifyNoInteractions(receivingSerialInventoryService);
    }

    /*
    тест на создание -150 УИТов
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/standard/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/standard/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTooLessInventories() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-negative.json",
                post("/crossdock/receive-items"),
                "некорректное количество");
    }

    /*
    тест создания УИТов: 2 норм, 3 - запрещённый излишек (новая ручка, новый запрет)
    */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/2ok2surplusnok-new/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/2ok2surplusnok-new/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createUIGs2Ok2SurplusNok() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/measured/new-surplus/request-5.json",
                "controller/crossdock/create-serials/2ok2surplusnok-new/response-standard.json",
                post("/crossdock/receive-items"));
        assertApiCallError("controller/crossdock/create-serials/requests/measured/new-surplus/request-3.json",
                post("/crossdock/receive-items"),
                "На столе приемки stage01 есть товары в незавершенном состоянии");
    }

    /*
     *  тест создания серийников: необмеренный
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-unmeasured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/crossdockUnmeasured/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createUITsUnmeasured() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));
    }

    /*
     *  тест создания серийников: необмеренный, нет активного задания на обмер в сервисе Dimension Management
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-unmeasured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-checking-active-measurement-order-existence.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/crossdockUnmeasured/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createUITsUnmeasuredWhenActiveMeasurementOrderDoesNotExist() throws Exception {
        when(dimensionManagementClient
                .getActiveMeasurementOrder(new GetActiveMeasurementOrderRequest("465852", "ROV0000000000000000360")))
                .thenReturn(new GetMeasurementOrdersResponse(20, 0, 0, Collections.emptyList()));
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));
    }

    /*
     *  тест создания серийников: необмеренный, уже есть активное задание на обмер в сервисе Dimension Management
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-unmeasured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-checking-active-measurement-order-existence.xml")
    @ExpectedDatabase(
            value = "/controller/crossdock/create-serials/crossdockUnmeasured/after-already-sent-to-measurement.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createUITsUnmeasuredWhenActiveMeasurementOrderExists() throws Exception {
        MeasurementOrderDto measurementOrder = new MeasurementOrderDto.Builder()
                .serialNumber("1234567890")
                .storer("465852")
                .sku("ROV0000000000000000360")
                .build();
        when(dimensionManagementClient
                .getActiveMeasurementOrder(new GetActiveMeasurementOrderRequest("465852", "ROV0000000000000000360")))
                .thenReturn(new GetMeasurementOrdersResponse(20, 0, 1, List.of(measurementOrder)));
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-already-sent-to-measurement.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Печать виртуальных УИТов и превращение их в настоящие. Позитивный сценарий.
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldPrintVirtualSerialInventories() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/print-fake/request.json",
                "controller/crossdock/create-serials/print-fake/response.json",
                post("/crossdock/print-real-uit"));

        Mockito.verify(serialNumberPrinter).print(
                argThat((List<SerialNumberPrinterData> dataUnits) -> dataUnits.size() == 10), eq("PRN123"));
    }

    /**
     * Должна вернуться ошибка, так как запрашивают слишком большое количество
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailWhenRequestTooMuch() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-too-much.json",
                post("/crossdock/print-real-uit"),
                "В НЗН 12 товаров");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Должна вернуться ошибка, так как балансы нулевые
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailWhenEmptyBalances() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-empty-balances.json",
                post("/crossdock/print-real-uit"),
                "В НЗН 0 товаров");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Должна вернуться ошибка, так как не заданы обязательные параметры
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailWithoutIdAndLoc() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-no-loc-no-id.json",
                post("/crossdock/print-real-uit"),
                "ID or LOC are required");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Должна вернуться ошибка, так как превышает заданное в конфиге количество
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailConfigLimitExceed() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-limit.json",
                post("/crossdock/print-real-uit"),
                "30 - некорректное количество");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Должна вернуться ошибка, так как пытаются печатать на виртуальном принтере
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailForVirtualPinter() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-virtual-printer.json",
                post("/crossdock/print-real-uit"),
                "Printer Virtual Printer is virtual");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Должна вернуться ошибка, так как разные sku
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-fake/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldFailDifferentSku() throws Exception {
        assertApiCallError(
                "controller/crossdock/create-serials/print-fake/request-no-sku.json",
                post("/crossdock/print-real-uit"),
                "SerialInventories with different SKU are found");
        Mockito.verifyNoInteractions(serialNumberPrinter);
    }

    /**
     * Печать виртуальных УИТов и превращение их в настоящие. Перемещение балансов. Позитивный сценарий.
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-and-move-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-and-move-fake/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldPrintAndMoveVirtualSerialInventories() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/print-and-move-fake/request.json",
                "controller/crossdock/create-serials/print-and-move-fake/response.json",
                post("/crossdock/print-and-move-real-uit"));

        Mockito.verify(serialNumberPrinter).print(
                argThat((List<SerialNumberPrinterData> dataUnits) -> dataUnits.size() == 10), eq("PRN123"));
    }

    /**
     * Печать виртуальных УИТов и превращение их в настоящие. Перемещение балансов. Сценарий без передачи
     * quantity(должен распечатать все имеющиеся УИТы). Используется при пополнении
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/print-and-move-fake/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/print-and-move-fake/after-no-quantity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldPrintAndMoveVirtualSerialInventoriesWithoutQuantity() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/print-and-move-fake/request-no-quantity.json",
                "controller/crossdock/create-serials/print-and-move-fake/response-no-quantity.json",
                post("/crossdock/print-and-move-real-uit"));

        Mockito.verify(serialNumberPrinter).print(
                argThat((List<SerialNumberPrinterData> dataUnits) -> dataUnits.size() == 12), eq("PRN123"));
    }

    /*
    товар multi
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/multi/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/multi/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryMulti() throws Exception {
        assertCreateSerialInventory("multi");
    }


    /*
    товар single and multi, разделение по порогу YM_CROSSDOCK_SINGLE_HOURS, ветка multi
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/single_multi_time_margin/multi/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/single_multi_time_margin/multi/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryForSingleAndMultiTimeMarginMulti() throws Exception {
        assertCreateSerialInventory("single_multi_time_margin/multi");
    }

    /*
    годный товар без СГ
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/basic/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/basic/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryBasic() throws Exception {
        assertCreateSerialInventory("basic");
    }

    /*
    бракованный товар без СГ
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/damaged/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/damaged/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryDamaged() throws Exception {
        assertCreateSerialInventory("damaged");
    }

    /*
    бракованный товар без СГ c IMEI и некорректным SN
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/damaged/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/damaged/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryDamagedWithIdentity() throws Exception {
        assertCreateSerialInventory("damaged-with-identity");
    }

    /*
    годный товар с СГ
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/unexpired/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/unexpired/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryUnexpired() throws Exception {
        assertCreateSerialInventory("unexpired");
    }

    /*
    просроченный товар по ОСГ
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/expired/before.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/expired/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryExpired() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serial/expired/request.json",
                "controller/crossdock/create-serial/expired/response.json",
                post("/crossdock/receive-items"));
    }

    /*
        просроченный товар по ОСГ на вход, нормальный на выход
    */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/expired-at-in-on-movement/before.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/expired-at-in-on-movement/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryExpired2() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serial/expired-at-in-on-movement/request.json",
                "controller/crossdock/create-serial/expired-at-in-on-movement/response.json",
                post("/crossdock/receive-items"));
    }


    /*
        просроченный товар по ОСГ на вход, нормальный на выход.
        через дату приема трейлера
    */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/expired-at-in-on-movement-trailer/before.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/expired-at-in-on-movement-trailer/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryExpired2ByTrailer() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serial/expired-at-in-on-movement-trailer/request.json",
                "controller/crossdock/create-serial/expired-at-in-on-movement-trailer/response.json",
                post("/crossdock/receive-items"));
    }

    /*
       просроченный товар по ОСГ на выход
   */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serial/expired-at-out-on-movement/before.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/nsqlconfig.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serial/expired-at-out-on-movement/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createSerialInventoryExpired3() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serial/expired-at-out-on-movement/request.json",
                "controller/crossdock/create-serial/expired-at-out-on-movement/response.json",
                post("/crossdock/receive-items"));
    }

    /*
    закончить приемку не последней палеты
    должен поменяться только статус соотвествущей строки в receiptdetail
     */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/not-last/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/not-last/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishNotLastPallet() throws Exception {
        assertFinishPallet("not-last");
    }

    /*
        Списание коробки с балансов (SKUxLOC, LOTxLOCxId, ID, ITRN)
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/default-box/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/default-box/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishBox() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/crossdock/finish-pallet/default-box/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    /*
        Списание палеты с балансов (SKUxLOC, LOTxLOCxId, ID, ITRN)
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/default-pallet/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/default-pallet/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishPallet() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/crossdock/finish-pallet/default-pallet/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    /*
        Закончить приемку последней коробки в заказе, когда принят 1 айтем из двух ожидаемых в невыкупе
     */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/return/before-last-box.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/return/before-last-box.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastUnredeemedBoxWhenOneItemNotReceived() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/crossdock/finish-pallet/return/request.json")))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/crossdock/finish-pallet/return/response-last-box-nok.json", result);
    }

    /*
        Закончить приемку НЕпоследней коробки в заказе, когда принят 1 айтем из двух ожидаемых в невыкупе
     */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/return/before-not-last-box.xml")
    void finishNotLastUnredeemedBoxWhenOneItemNotReceived() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/crossdock/finish-pallet/return/request.json")))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/crossdock/finish-pallet/return/response-last-box-ok.json", result);
    }

    /*
        Закончить приемку последней коробки в заказе, когда принят 1 айтем из двух ожидаемых в невыкупе с флагом force
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/return/before-last-box.xml")
    void finishLastUnredeemedBoxWhenOneItemNotReceivedForced() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/crossdock/finish-pallet/return/request-forced.json")))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/crossdock/finish-pallet/return/response-last-box-ok.json", result);
    }

    /*
        Закончить приемку последней коробки в заказе,
        когда из двух ожидаемых в невыкупе 1 айтем принят на сток, а второй - в аномалии
     */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/return/before-last-box-anomaly.xml")
    void finishLastUnredeemedBoxWhenAllItemsReceivedWithAnomaly() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/crossdock/finish-pallet")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/crossdock/finish-pallet/return/request.json")))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/crossdock/finish-pallet/return/response-last-box-ok.json", result);
    }

    /*
    закончить приемку последней палеты
    приемка завершена без расхождений
    должен поменяться статус приемки на 11
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/last/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/last/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastPallet() throws Exception {
        assertFinishPallet("last");
    }

    /*
    закончить приемку последней палеты
    приемка завершена с расхождениями
    должен поменяться статус приемки на 12
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/last-with-discrepancies/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/last-with-discrepancies/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastPalletWithDiscrepancies() throws Exception {
        assertFinishPallet("last-with-discrepancies");
    }

    /*
    закончить приемку последней палеты с многоместкой
    приемка завершена без расхождений
    должен поменяться статус приемки на 11
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/last-multiplace/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/last-multiplace/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastPalletMultiplace() throws Exception {
        assertFinishPallet("last-multiplace");
    }

    /*
    закончить приемку последней палеты с многоместкой
    приемка завершена с расхождениями (недостача 2/2 + 1/2)
    должен поменяться статус приемки на 12
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/last-multiplace-with-discrepancies/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/last-multiplace-with-discrepancies/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastPalletMultiplaceWithDiscrepancies() throws Exception {
        assertFinishPallet("last-multiplace-with-discrepancies");
    }

    /*
    закончить приемку последней палеты с многоместкой
    приемка завершена с расхождениями (пересорт 3/2 + 1/2)
    должен поменяться статус приемки на 12
    */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/last-multiplace-with-discrepancies2/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/last-multiplace-with-discrepancies2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishLastPalletMultiplaceWithDiscrepanciesMismatch() throws Exception {
        assertFinishPallet("last-multiplace-with-discrepancies2");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/assortment/1/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/assortment/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishPalletAssortment() throws Exception {
        assertFinishPallet("assortment/1");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/assortment/2/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/assortment/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishPalletAssortmentWithDiscrepancies() throws Exception {
        assertFinishPallet("assortment/2");
    }

    /*
    передана корректная информация об Identity
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/basic/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/basic/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void identityInfoPassedAndIsValid() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/basic/request.json",
                "controller/crossdock/identity-check/basic/response.json",
                post("/crossdock/receive-items"));
    }

    /*
    Ошибка форматного контроля
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/format/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/format/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void identityInfoNotValid() throws Exception {
        assertApiCallOk("controller/crossdock/identity-check/format/request.json",
                "controller/crossdock/identity-check/format/response.json",
                post("/crossdock/receive-items"));
    }

    /*
    Количество переданных Identity не соответствует требуемому
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/qnty/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/qnty/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void badIdentityQty() throws Exception {
        assertApiCallOk("controller/crossdock/identity-check/qnty/request.json",
                "controller/crossdock/identity-check/qnty/response-identity-absent.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/qnty/before_optional_requirements.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/qnty/after_1_optional_requirement.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptOneOptionalIdentity() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/qnty/request_1_optional_requirement.json",
                "controller/crossdock/identity-check/qnty/response_optional_requirements.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/qnty/before_optional_requirements.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/qnty/after_no_optional_requirement.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptWithNoOptionalIdentity() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/qnty/request_no_optional_requirement.json",
                "controller/crossdock/identity-check/qnty/response_optional_requirements.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/qnty/before_optional_requirements.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/qnty/before_optional_requirements.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void failWithTwoOptionalIdentity() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/qnty/request_2_optional_requirements.json",
                "controller/crossdock/identity-check/qnty/response-more-than-1-optional.json",
                post("/crossdock/receive-items"));
    }

    /*
    Ошибка проверки уникальности
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/uniqueness/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/uniqueness/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void identityShouldBeUnique() throws Exception {
        assertApiCallOk("controller/crossdock/identity-check/uniqueness/request.json",
                "controller/crossdock/identity-check/uniqueness/response.json",
                post("/crossdock/receive-items"));
    }

    /*
    Ошибка количества товара(количество > 1)
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/batch/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/batch/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void quantityTooLarge() throws Exception {
        assertApiCallError("controller/crossdock/identity-check/batch/request.json",
                post("/crossdock/receive-items"),
                "Для товаров с идентификаторами не может быть сформировано более одного УИТ за раз");
    }

    /*
    Некорректный IMEI
     */
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/incorrect-imei/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/incorrect-imei/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void incorrectImei() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/incorrect-imei/request.json",
                "controller/crossdock/identity-check/incorrect-imei/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Тест на разрешение перелинковки пустой тары независимо от её типа и старого линка
     */
    @Test
    @DatabaseSetup("/controller/crossdock/link-container/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/link-container/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void relockEmptyId() throws Exception {
        assertApiCallOk(
                "controller/crossdock/link-container/request.json",
                null,
                post("/locs/link-container"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/uit-lot10-found/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/uit-lot10-found/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUitFoundByLot10() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/uit-lot10-found/request.json",
                "controller/crossdock/identity-check/uit-lot10-found/response.json",
                post("/crossdock/receive-items"));
    }

    //товар,который не был заявлен в родительской поставке
    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/validate-undescr/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/validate-undescr/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsAdditionalReceiptSkuUndescribed() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/validate-undescr/request.json",
                "controller/crossdock/identity-check/validate-undescr/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/uit-lot10-not-found/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/uit-lot10-not-found/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUitNotFoundByLot10() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/uit-lot10-not-found/request.json",
                "controller/crossdock/identity-check/uit-lot10-not-found/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/uit-empty/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/identity-check/uit-empty/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUitEmpty() throws Exception {
        assertApiCallOk(
                "controller/crossdock/identity-check/uit-empty/request.json",
                "controller/crossdock/identity-check/uit-empty/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/identity-check/uit-qty-not-equal-to-one/before.xml")
    void receiveItemsUitQtyNotEqualToOne() throws Exception {
        assertApiCallError(
                "controller/crossdock/identity-check/uit-qty-not-equal-to-one/request.json",
                post("/crossdock/receive-items"),
                "2 - некорректное количество"
        );
    }

    // Принимаем 2 годных товара - проверяем, что ручка pushSubtitles вызывается 1 раз со списком их 2-х событий
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/create-serials/standard/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/standard/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void pushSubtitlesWhileReceiving2FitItems() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/measured/any-surplus/request-2.json",
                "controller/crossdock/create-serials/standard/response-standard.json",
                post("/crossdock/receive-items"));

        ReceiptFitTagDto.ReceiptFitTagDtoBuilder expectedTagTemplate = ReceiptFitTagDto.builder()
                .receiptKey("0000000101")
                .sku("ROV0000000000000000359")
                .storer("465852")
                .lot("0000000701")
                .location("STAGE01")
                .user("TEST")
                .receiptType("1");

        ReceiptFitTagDto expectedTag1 = expectedTagTemplate.uit("997010000801").build();
        ReceiptFitTagDto expectedTag2 = expectedTagTemplate.uit("997010000802").build();

        ArgumentCaptor<SubtitlesDto> receiptTagArgumentCaptor = ArgumentCaptor.forClass(SubtitlesDto.class);
        verify(servicebusClient, times(1)).pushSubtitles(any());
        verify(servicebusClient).pushSubtitles(receiptTagArgumentCaptor.capture());

        List<ReceiptFitTagDto> resultTags = receiptTagArgumentCaptor
                .getValue().getTags().stream()
                .map(it -> (ReceiptFitTagDto) it).toList();
        Assertions.assertEquals(2, resultTags.size());
        Assertions.assertTrue(resultTags.containsAll(Arrays.asList(expectedTag1, expectedTag2)));
    }

    // Принимаем 10 единиц товара в аномалии -
    // проверяем, что ручка pushSubtitles вызывается 1 раз со списком из 1 события
    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-skuDetected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void pushSubtitlesWhileReceivingAnomalyItems() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));

        ReceiptAnomalyTagDto expectedTag = ReceiptAnomalyTagDto.builder()
                .receiptKey("0000000101")
                .sku("ROV0000000000000000359")
                .storer("465852")
                .anomalyType("DAMAGED")
                .container("ANOM00001")
                .quantity(10)
                .location("STAGE01")
                .user("TEST")
                .altSku("EAN359")
                .manufacturerSku("ROV0000000000000000359")
                .build();

        ArgumentCaptor<SubtitlesDto> receiptTagArgumentCaptor = ArgumentCaptor.forClass(SubtitlesDto.class);
        verify(servicebusClient, times(1)).pushSubtitles(any());
        verify(servicebusClient).pushSubtitles(receiptTagArgumentCaptor.capture());

        List<ReceiptAnomalyTagDto> resultTags = receiptTagArgumentCaptor
                .getValue().getTags().stream()
                .map(it -> (ReceiptAnomalyTagDto) it).toList();
        Assertions.assertEquals(1, resultTags.size());
        Assertions.assertEquals(expectedTag, resultTags.get(0));
    }

    /*
     * Задание на перемещение аномалий должно иметь повышенный приоритет в случае включения YM_ANOMALY_MV_PRIORITY
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/anomaly/1/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/anomaly/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void anomalyMovementTaskShouldHaveElevatedPriority() throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/anomaly/1/request.json",
                null,
                post("/crossdock/finish-pallet"));
    }

    //в плановом реестре допоставки 5 айтемов, на сток встало 3, в аномалии попал 1 - расхождение 5 - (3 + 1) = 1
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/additional-receipt-incorrect-qty/before.xml")
    void closeContainerAdditionalReceiptIncorrectQty() throws Exception {
        doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            String template = (String) args[1];
            Assertions.assertEquals("Для допоставки обнаружено расхождение количества товаров.", template);
            return null;
        }).when(businessLogger).info(anyString(), anyString(), anyString(), any());

        assertApiCallOk(
                "controller/crossdock/finish-pallet/additional-receipt-incorrect-qty/request.json",
                null,
                post("/crossdock/finish-pallet"));
        Mockito.verify(businessLogger, times(1)).info(anyString(), anyString(), any(), any());
    }

    //в плановом реестре допоставки 5 айтемов, на сток встало 3, в аномалии попало 2. расхождений нет
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/additional-receipt-correct-qty/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/additional-receipt-correct-qty/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerAdditionalReceiptCorrectQty() throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/additional-receipt-correct-qty/request.json",
                null,
                post("/crossdock/finish-pallet"));
    }

    /*
     * Задание на перемещение аномалий должно иметь обычный приоритет в случае выключения YM_ANOMALY_MV_PRIORITY
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/finish-pallet/anomaly/2/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/anomaly/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void anomalyMovementTaskShouldNotHaveElevatedPriority() throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/anomaly/2/request.json",
                null,
                post("/crossdock/finish-pallet"));
    }

    //одна аномальная партия находится в неверном статусе
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/wrong-lot-status/before.xml")
    void closeContainerWrongLotStatus() throws Exception {
        assertApiCallError(
                "controller/crossdock/finish-pallet/wrong-lot-status/request.json",
                post("/crossdock/finish-pallet"),
                "Партия с номером 1 в таре PLT123 имеет неверный статус WAIT_RE_REC");
    }

    //одна из двух аномальных партий находится в неверном статусе
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/wrong-multiple-lot-status/before.xml")
    void closeContainerWrongMultipleLotStatus() throws Exception {
        assertApiCallError(
                "controller/crossdock/finish-pallet/wrong-multiple-lot-status/request.json",
                post("/crossdock/finish-pallet"),
                "Партия с номером 2 в таре PLT123 имеет неверный статус WAIT_RE_REC");
    }

    //аномальная партия находится в нужном статусе RE_RECEIVING и переводится в статус RE_RECEIVED
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/single-lot-status-changed/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/single-lot-status-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerStatusIsUpdated() throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/single-lot-status-changed/request.json",
                null,
                post("/crossdock/finish-pallet"));
    }

    //аномальная партия находится в нужном статусе RE_RECEIVING и переводится в статус RE_RECEIVED,
    //несколько аномалий доприемки
    @Test
    @DatabaseSetup(value = "/controller/crossdock/finish-pallet/multiple-lot-status-changed/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/finish-pallet/multiple-lot-status-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerMultipleLotStatusIsUpdated() throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/multiple-lot-status-changed/request.json",
                null,
                post("/crossdock/finish-pallet"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/pallet-id/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/pallet-id/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void palletIdShouldBeSavedIfProvided() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/pallet-id/request.json",
                "controller/crossdock/receive-items/pallet-id/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/receive-items/no-parent-detail/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/no-parent-detail/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsDetailCreatedNoParentDetail() throws Exception {
        assertApiCallOk(
                "controller/crossdock/receive-items/no-parent-detail/request.json",
                "controller/crossdock/receive-items/no-parent-detail/response.json",
                post("/crossdock/receive-items"));
    }

    /*
     * sku входит в ассортимент и услуга сортировки включена
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/before_common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/1/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/assortment/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsAssortmentSkuSortBySkuEnabled() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/assortment/1/request.json",
                "controller/crossdock/create-serials/assortment/1/response.json",
                post("/crossdock/receive-items"));
    }

    /*
     * sku входит в ассортимент, но услуга сортировки выключена
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/before_common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/2/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/assortment/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsAssortmentSkuSortBySkuDisabled() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/assortment/2/request.json",
                "controller/crossdock/create-serials/assortment/2/response.json",
                post("/crossdock/receive-items"));
    }

    /*
     * sku не входит в ассортимент
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/before_common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/3/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/assortment/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsNonAssortmentSku() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/assortment/3/request.json",
                "controller/crossdock/create-serials/assortment/3/response.json",
                post("/crossdock/receive-items"));
    }

    /*
     * sku входит в ассортимент и услуга сортировки включена, есть излишек
     * */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/assortment/before_common.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/assortment/4/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/assortment/4/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsAssortmentSkuSurplus() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/assortment/4/request.json",
                "controller/crossdock/create-serials/assortment/4/response.json",
                post("/crossdock/receive-items")
        );
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/storage-category/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/storage-category/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsRegularStorageCategory() throws Exception {
        SkuId sku = new SkuId("465852", "ROV0000000000000000359");
        when(constraintsClient.getStorageCategory(anyList()))
                .thenReturn(new GetStorageCategoryResponse(List.of(
                        new StorageCategoryDto(sku.getStorerKey(), sku.getSku(), "REGULAR", List.of()))));

        assertApiCallOk(
                "controller/crossdock/create-serials/storage-category/request.json",
                "controller/crossdock/create-serials/storage-category/response-stock.json",
                post("/crossdock/receive-items")
        );

        verify(constraintsClient).getStorageCategory(List.of(sku));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/storage-category/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/storage-category/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsThermalStorageCategory() throws Exception {
        SkuId sku = new SkuId("465852", "ROV0000000000000000359");
        when(constraintsClient.getStorageCategory(anyList()))
                .thenReturn(new GetStorageCategoryResponse(List.of(
                        new StorageCategoryDto(sku.getStorerKey(), sku.getSku(), "THERMAL", List.of()))));

        assertApiCallOk(
                "controller/crossdock/create-serials/storage-category/request.json",
                "controller/crossdock/create-serials/storage-category/response-thermal.json",
                post("/crossdock/receive-items")
        );

        verify(constraintsClient).getStorageCategory(List.of(sku));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-unmeasured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-checking-active-measurement-order-existence.xml")
    void createUITsUnmeasuredCreatesNewMeasurementOrder() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));

        verify(dimensionManagementClient, times(1)).createMeasurementOrderBySkuId(any());
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/datasets/skus-unmeasured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-check-need-measure-new.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/new-check-need-measure/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createUITsUnmeasuredWithNewCheckNeedMeasurementFlowNotValid() throws Exception {
        when(dimensionManagementClient.validateDimensions(any()))
                .thenReturn(new ValidateDimensionsResponse("HIGH_DENSITY"));

        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));

        verify(dimensionManagementClient, times(1)).validateDimensions(any());
        verify(dimensionManagementClient, times(1)).createMeasurementOrderBySkuId(any());
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/new-check-need-measure/before-force-measurement.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-check-need-measure-new.xml")
    void createUITsUnmeasuredWithNewCheckNeedMeasurementFlowForceMeasurement() throws Exception {
        when(dimensionManagementClient.validateDimensions(any()))
                .thenReturn(new ValidateDimensionsResponse("OK"));

        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));

        verify(dimensionManagementClient, times(1)).validateDimensions(any());
        verify(dimensionManagementClient, times(1)).createMeasurementOrderBySkuId(any());
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/new-check-need-measure/before-no-measurement.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-check-need-measure-new.xml")
    void createUITsUnmeasuredWithNewCheckNeedMeasurementFlowNoMeasurementStatus() throws Exception {
        when(dimensionManagementClient.validateDimensions(any()))
                .thenReturn(new ValidateDimensionsResponse("OK"));
        when(dimensionManagementClient.remainingCapacity())
                .thenReturn(new RemainingCapacityResponse(1));

        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/create-serials/crossdockUnmeasured/response-standard.json",
                post("/crossdock/receive-items"));

        verify(dimensionManagementClient, times(1)).validateDimensions(any());
        verify(dimensionManagementClient, times(1)).createMeasurementOrderBySkuId(any());
        verify(dimensionManagementClient, times(1)).remainingCapacity();
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/new-check-need-measure/before-already-measured.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup("/controller/crossdock/create-serials/crossdockUnmeasured/before.xml")
    @DatabaseSetup("/controller/crossdock/config-check-need-measure-new.xml")
    void createUITsUnmeasuredWithNewCheckNeedMeasurementFlowAlreadyMeasuredStatus() throws Exception {
        when(dimensionManagementClient.validateDimensions(any()))
                .thenReturn(new ValidateDimensionsResponse("OK"));

        assertApiCallOk(
                "controller/crossdock/create-serials/requests/unmeasured/request-1.json",
                "controller/crossdock/receive-items/new-check-need-measure/response.json",
                post("/crossdock/receive-items"));

        verify(dimensionManagementClient, times(1)).validateDimensions(any());
        verify(dimensionManagementClient, times(0)).createMeasurementOrderBySkuId(any());
        verify(dimensionManagementClient, times(0)).remainingCapacity();
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt-bbxd.xml")
    @DatabaseSetup(value = "/controller/crossdock/create-serials/receive-boxes-bbxd/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/create-serials/receive-boxes-bbxd/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createInventoriesBoxesBbxd() throws Exception {
        assertApiCallOk(
                "controller/crossdock/create-serials/receive-boxes-bbxd/request.json",
                "controller/crossdock/create-serials/receive-boxes-bbxd/response.json",
                post("/crossdock/receive-item-boxes"));
    }

    private void assertCheckConfirmedSku(String dir) throws Exception {
        assertApiCallOk("controller/crossdock/check-confirmed-sku/" + dir + "/",
                post("/crossdock/check-confirmed-sku"));
    }

    private void assertCreateSerialInventory(String dir) throws Exception {
        assertApiCallOk("controller/crossdock/create-serial/" + dir + "/",
                post("/crossdock/receive-items"));
    }

    private void assertFinishPallet(String dir) throws Exception {
        assertApiCallOk("controller/crossdock/finish-pallet/" + dir + "/request.json", null,
                post("/crossdock/finish-pallet"));
    }

    private void assertApiCallOk(String subPath, MockHttpServletRequestBuilder request) throws Exception {
        assertApiCallOk(subPath + "request.json", subPath + "response.json", request);
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().isOk())
                .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile, result);
        }
    }

    private void assertApiCallError(String requestFile, MockHttpServletRequestBuilder request, String errorInfo)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo);
    }

    private void assertApiCallErrorAndCheckResponse(String requestFile, MockHttpServletRequestBuilder request,
                                                    String responseFile)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                mvcResult.getResponse().getContentAsString());
    }
}
// CHECKSTYLE:ON: FileLength
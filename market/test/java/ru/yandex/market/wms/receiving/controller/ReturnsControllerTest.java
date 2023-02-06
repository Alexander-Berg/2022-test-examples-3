package ru.yandex.market.wms.receiving.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.logistics.cte.client.dto.SupplyDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemRequestDTO;
import ru.yandex.market.logistics.cte.client.enums.RegistryType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.constraints.client.ConstraintsClient;
import ru.yandex.market.wms.constraints.core.response.GetStorageCategoryResponse;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.straight.ReceivingSerialInventoryService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
@DatabaseSetup("/controller/crossdock/create-serials/datasets/receipt.xml")
public class ReturnsControllerTest extends ReceivingIntegrationTest {

    @MockBean
    @Autowired
    private FulfillmentCteClientApi cteClient;

    @SpyBean
    @Autowired
    private ReceivingSerialInventoryService receivingSerialInventoryService;

    @MockBean
    @Autowired
    private ConstraintsClient constraintsClient;

    @BeforeEach
    public void init() {
        super.init();
        Mockito.doReturn(new GetStorageCategoryResponse(Collections.emptyList()))
                .when(constraintsClient).getStorageCategory(anyList());
    }

    @AfterEach
    public void reset() {
        Mockito.reset(receivingSerialInventoryService);
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/fit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfFitCustomerReturnReceipt() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit/request.json",
                "controller/crossdock/receive-items/customer-return/fit/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/receipt-details-items/with-item" +
            "-before.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/receipt-details-items/with-item" +
            "-after.xml", assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsGetItemsFromReceiptDetailItems() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit/request.json",
                "controller/crossdock/receive-items/customer-return/fit/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/receipt-details-items/without-item" +
            "-before.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/receipt-details-items/without-item" +
            "-after.xml", assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsGetItemsFromReceiptDetail() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit/request.json",
                "controller/crossdock/receive-items/customer-return/fit/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Проверка излишка должна происходить с учетом номера заказа возврата.
     * Одна поставка с двумя заказами с одинаковыми товарами.
     * В одной коробке приняли весь товар.
     * Принимаем из другой, ожидаем принять на сток.
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsFromDifferentOrderSameSkuReturns() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/two-orders-one-sku/request.json",
                "controller/crossdock/receive-items/customer-return/two-orders-one-sku/response.json",
                post("/crossdock/receive-items"));
    }

    /**
     * Проверка излишка должна происходить с учетом номера заказа возврата.
     * Одна поставка с двумя заказами с одинаковыми товарами.
     * В одной коробке приняли весь товар.
     * Принимаем из той же, ожидаем излишек.
     */
    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku-surplus/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
            value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku-surplus/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsFromDifferentOrderSameSkuSurplusReturns() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallError(
                "controller/crossdock/receive-items/customer-return/two-orders-one-sku-surplus/request.json",
                post("/crossdock/receive-items"),
                "Эти товары - запрещённый излишек");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku-surplus/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
            value = "/controller/crossdock/receive-items/customer-return/two-orders-one-sku-surplus/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void checkConfirmedSkuForCustomerReturnSurplus() throws Exception {
        assertApiCallOk(
                "controller/crossdock/check-confirmed-sku/surplus-anomaly/request.json",
                "controller/crossdock/check-confirmed-sku/surplus-anomaly/response.json",
                post("/crossdock/check-confirmed-sku"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/fit-multipart/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/fit-multipart/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfFitCustomerReturnReceiptMultipartSku() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit-multipart/request.json",
                "controller/crossdock/receive-items/customer-return/fit-multipart/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/fit-multiple-boxes/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/fit-multiple-boxes/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfCustomerReturnWhenTwoBoxesWithTheSameSkuButDifferentOrders() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit-multiple-boxes/request.json",
                "controller/crossdock/receive-items/customer-return/fit-multiple-boxes/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/damage/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveToStockItemsOfDamagedCustomerReturn() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.DAMAGE_DISPOSAL).build()).when(cteClient)
                .evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/damage/request.json",
                "controller/crossdock/receive-items/customer-return/damage/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/damage/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveToStockItemsOfDamagedCustomerReturnWithCteDefinedAttrs() throws Exception {
        Mockito.doReturn(
                SupplyItemDTO.builder()
                        .stockType(StockType.DAMAGE_DISPOSAL)
                        .build())
                .when(cteClient)
                .evaluateResupplyItem(
                        eq(123L),
                        any(),
                        argThat(new SupplyItemRequestDtoMatcher(
                                SupplyItemRequestDTO.builder()
                                        .externalSku("ROV0000000000000000359")
                                        .vendorId(465852)
                                        .qualityAttributeIds(Set.of(1L))
                                        .supplyDTO(
                                                SupplyDTO.builder()
                                                        .consignorSupplyId("465852")
                                                        .consignorName("465852")
                                                        .fulfillmentSupplyId("0000000101")
                                                        .registryType(RegistryType.REFUND)
                                                        .build()
                                        )
                                        .price(BigDecimal.valueOf(0.0))
                                        .expiredAndDamaged(false)
                                        .shopSku(null)
                                        .marketShopSku(null)
                                        .categoryId(null)
                                        .boxId("PLT1234")
                                        .orderId("234")
                                        .warehouseId("0")
                                        .name("TEST")
                                        .identifiers(null)
                                        .criteria(null)
                                        .createdBy(null)
                                        .build()
                        )));
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/damage/request-cte-defined-attrs.json",
                "controller/crossdock/receive-items/customer-return/damage/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/damage-cis/before-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/damage-cis/before-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfCustomerReturnWithNotDeclaredCisToAnomaly() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/damage-cis/request.json",
                "controller/crossdock/receive-items/customer-return/damage-cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/no-required-cis/before-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/no-required-cis/before-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfCustomerReturnWithoutRequiredCisToAnomaly() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/no-required-cis/request.json",
                "controller/crossdock/receive-items/customer-return/no-required-cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/fit-with-cis/before-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/fit-with-cis/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveToStockFitCustomerReturnWithCis() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/fit-with-cis/request.json",
                "controller/crossdock/receive-items/customer-return/fit-with-cis/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before-different-return-ids.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/not-declared-imei/before.xml",
            type = DatabaseOperation.INSERT)
    void receiveToAnomalyCustomerReturnWithoutImei() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk("controller/crossdock/receive-items/customer-return/not-declared-imei/request.json",
                "controller/crossdock/receive-items/customer-return/not-declared-imei/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before-different-return-ids.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/not-declared-sn/before.xml",
            type = DatabaseOperation.INSERT)
    void receiveToAnomalyCustomerReturnWithoutSN() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk("controller/crossdock/receive-items/customer-return/not-declared-sn/request.json",
                "controller/crossdock/receive-items/customer-return/not-declared-sn/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before-different-return-ids.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/different-return-ids/before-cis.xml",
            type = DatabaseOperation.INSERT)
    void receiveToStockFitCustomerReturnCisAndBoxHaveDifferentReturns() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk("controller/crossdock/receive-items/customer-return/different-return-ids/request.json",
                "controller/crossdock/receive-items/customer-return/different-return-ids/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/different-order-keys/before.xml",
            type = DatabaseOperation.INSERT)
    void receiveUnredeemedCisAndBoxHaveDifferentOrderKeys() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk("controller/crossdock/receive-items/unredeemed/different-order-keys/request.json",
                "controller/crossdock/receive-items/unredeemed/different-order-keys/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/fit/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/fit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfFitUnredeemedReceipt() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/fit/request.json",
                "controller/crossdock/receive-items/unredeemed/fit/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/damage/1/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/damage/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveToStockItemsOfDamagedUnredeemed() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                        .stockType(StockType.DAMAGE_DISPOSAL).build()).when(cteClient)
                .evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/damage/1/request.json",
                "controller/crossdock/receive-items/unredeemed/damage/1/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/unredeemed/damage/2/before.xml")
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/damage/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsWithCisToDamage() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.DAMAGE).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/damage/2/request.json",
                "controller/crossdock/receive-items/unredeemed/damage/2/response.json",
                post("/crossdock/receive-items")
        );
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/no-required-cis/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/no-required-cis/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsWithNotRequiredCis() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/no-required-cis/request.json",
                "controller/crossdock/receive-items/unredeemed/no-required-cis/response.json",
                post("/crossdock/receive-items")
        );
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/damage-imei/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/damage-imei/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUnredeemedDuplicateIdsToDamage() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/damage-imei/request.json",
                "controller/crossdock/receive-items/unredeemed/damage-imei/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/uit-new-lot-and-print/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/uit-new-lot-and-print/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUnredeemedWithUitNewLot() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());

        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/uit-new-lot-and-print/request.json",
                "controller/crossdock/receive-items/unredeemed/uit-new-lot-and-print/response.json",
                post("/crossdock/receive-items"));

        Mockito.verify(receivingSerialInventoryService, VerificationModeFactory.noInteractions())
                .printSerialNumbers(any(), any(), any());
    }

    @Test
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUnredeemedWithUitExistsLotAndNoPrint() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());

        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print/request.json",
                "controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print/response.json",
                post("/crossdock/receive-items"));

        Mockito.verify(receivingSerialInventoryService, VerificationModeFactory.noInteractions())
                .printSerialNumbers(any(), any(), any());
    }

    @Test
    @DatabaseSetup(value = "/controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print2/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsUnredeemedWithUitExistsLotAndNoPrint2() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.DAMAGE).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());

        assertApiCallOk(
                "controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print2/request.json",
                "controller/crossdock/receive-items/unredeemed/uit-exists-lot-and-no-print2/response.json",
                post("/crossdock/receive-items"));
        Mockito.verify(receivingSerialInventoryService, VerificationModeFactory.noInteractions())
                .printSerialNumbers(any(), eq("0000000700"), any());
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/before.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/expired/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveItemsOfExpiredCustomerReturnToHoldStock() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.EXPIRED).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/expired/request.json",
                "controller/crossdock/receive-items/customer-return/expired/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/surplus/before-surplus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/surplus/after-surplus.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveSurplusItemsOfCustomerReturnToAnomaly() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallError(
                "controller/crossdock/receive-items/customer-return/surplus/request.json",
                post("/crossdock/receive-items"),
                "Эти товары - запрещённый излишек");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
    @DatabaseSetup("/controller/crossdock/receive-items/customer-return/receipt-customer-return-process.xml")
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/anomaly-container/before-2-anomaly.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/receive-items/customer-return/no-required-cis/before-cis.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/receive-items/customer-return/no-required-cis/before-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveSecondAnomalyItemOfCustomerReturnToTheSameContainer() throws Exception {
        Mockito.doReturn(SupplyItemDTO.builder()
                .stockType(StockType.OK).build()).when(cteClient).evaluateResupplyItem(anyLong(), any(), any());
        assertApiCallOk(
                "controller/crossdock/receive-items/customer-return/damage-cis/request.json",
                "controller/crossdock/receive-items/customer-return/anomaly-container/response.json",
                post("/crossdock/receive-items"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/customer-return-enabled.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/before-container-of-other-subreceipt.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/before-container-of-other-subreceipt.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyOfCustomerReturnAndInvalidContainerLinkedToAnotherSubreceipt() throws Exception {
        assertApiCallError("controller/crossdock/add-anomaly/requests/skuDetected.json",
                post("/crossdock/add-anomaly"), "CONTAINER_OF_OTHER_RECEIPT");
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

    private static final class SupplyItemRequestDtoMatcher implements ArgumentMatcher<SupplyItemRequestDTO> {
        SupplyItemRequestDTO it;

        SupplyItemRequestDtoMatcher(SupplyItemRequestDTO supplyItemRequestDTO) {
            it = supplyItemRequestDTO;
        }

        @Override
        public boolean matches(SupplyItemRequestDTO argument) {
            if (argument == null && it == null) {
                return true;
            }
            if (argument == null || it == null) {
                return false;
            }
            return StringUtils.equalsIgnoreCase(argument.toString(), it.toString());
        }
    }
}

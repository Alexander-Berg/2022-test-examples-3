package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AnomalyContainerType;
import ru.yandex.market.wms.common.model.enums.AnomalyLotStatus;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyContainer;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyLot;
import ru.yandex.market.wms.common.spring.enums.ReceivingItemType;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.enums.ScanningOperationType;
import ru.yandex.market.wms.receiving.service.ScanningOperationLog;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AddAnomalyTest extends ReceivingIntegrationTest {

    @SpyBean
    @Autowired
    private ScanningOperationLog scanningOperationLog;

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-described.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyOnlyDescription() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/described.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-skuDetected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyOnlySku() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));

    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-multiItemTypes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyOnlySkuMultipleItemtypes() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/multipleItemtypes.json",
                null,
                post("/crossdock/add-anomaly"));

    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-undescribed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyInsufficientData() throws Exception {
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/undescribed.json",
                post("/crossdock/add-anomaly"),
                "Insufficient data: you need to set up either description or sku");

    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-identities.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithMultipleIdentities() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-identities.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/only-cis-and-imei.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-one-blank-identity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithOneBlankIdentity() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-one-blank-identity.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/inbound-identity-cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-valid-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithValidCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-valid-cis.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-no-required-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithNoRequiredCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-no-required-cis.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-skuDetected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyDamagedAndNoRequiredCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-no-required-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithEmptyIdentityAndQtyMoreThanOne() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-empty-identity-and-qty-more-than-one.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/inbound-identity-cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-invalid-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithInValidByRegexCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-invalid-cis.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-identities.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithOneItemAndIdentitiesInFlatList() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-one-item-and-identities.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-no-actions-due-to-invalid-request.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithSeveralItemsAndIdentitiesInFlatListAndQtyMoreThanOne() throws Exception {
        assertApiCallError("controller/crossdock/add-anomaly/requests/" +
                        "anomaly-with-several-items-and-identities-and-qty-more-than-one.json",
                post("/crossdock/add-anomaly"),
                "Number of identity groups not equals to the quantity of items");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/customer-return-enabled.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-identities-returns.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithCustomerReturnReceiptTypeEnabled() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-one-item-and-identities-returns.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/unredeemed-enabled.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-identities-unredeemed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithUnredeemedReceiptTypeEnabled() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-one-item-and-identities-returns.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-identities.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithOneItemAndIdentitiesGrouped() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-one-item-and-identities-grouped.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value =
            "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-several-items-and-identities.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithSeveralItemsAndIdentitiesGrouped() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-several-items-and-identities-grouped.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-no-actions-due-to-invalid-request.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithSeveralItemsButOnlyOneGroupOfIdentities() throws Exception {
        assertApiCallError("controller/crossdock/add-anomaly/requests/" +
                        "anomaly-with-several-items-but-only-one-group-of-identities.json",
                post("/crossdock/add-anomaly"),
                "Number of identity groups not equals to the quantity of items");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/multiple-identities.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-duplicated-imei.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithDuplicatedImeiInTheSameRequest() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-duplicated-imei.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-not-declared-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithNotDeclaredCis() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-not-declared-cis.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-not-declared-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithNotDeclaredCisWhichStartsFromGS() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-not-declared-cis-starts-with-gs.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis-with-no-mark-handle-mode.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/cis-on-ok-stock.xml")
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-anomaly-with-duplicated-cis.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithDuplicatedCisAlreadyExistingOnOkStock() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/anomaly-with-valid-duplicated-cis.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/crossdock/add-anomaly/datasets/after-cis-as-barcode.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addAnomalyWithCisAsBarcode() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/cis-as-barcode.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addUnknownEanAnomalyWithNoEan() throws Exception {
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/unwnown-ean-with-no-ean.json",
                post("/crossdock/add-anomaly"),
                "UNKNOWN_EAN nonconformity requires not empty EAN");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addUnknownEanAnomalyWithSku() throws Exception {
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/unwnown-ean-with-sku.json",
                post("/crossdock/add-anomaly"),
                "UNKNOWN_EAN nonconformity requires empty SKU");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addAnomalyWithIncorrectEan() throws Exception {
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/incorrect-ean.json",
                post("/crossdock/add-anomaly"),
                "Отсканированный ШК является тарой");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addUnrecognizableAnomalyWithNoEan() throws Exception {
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/unrecognizable-with-ean-and-sku.json",
                post("/crossdock/add-anomaly"),
                "UNRECOGNIZABLE nonconformity requires empty EAN and empty SKU");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addTwoUnrecognizableAnomalyInOneContainerFail() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/unrecognizable.json",
                null,
                post("/crossdock/add-anomaly"));
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/unrecognizable.json",
                post("/crossdock/add-anomaly"),
                "В один контейнер можно положить аномалии только одного типа! Возьмите другой контейнер.");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addTwoAnomalyWithSameTypesInOneContainerOk() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void addTwoAnomalyWithDiffTypesInOneContainerFail() throws Exception {
        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/skuDetected.json",
                null,
                post("/crossdock/add-anomaly"));
        assertApiCallError(
                "controller/crossdock/add-anomaly/requests/anomaly-with-invalid-cis.json",
                post("/crossdock/add-anomaly"),
                "В один контейнер можно положить аномалии только одного типа! Возьмите другой контейнер.");
    }

    @Test
    @DatabaseSetup("/controller/crossdock/add-anomaly/datasets/common.xml")
    @DatabaseSetup(value = "/controller/crossdock/add-anomaly/datasets/skus.xml",
            type = DatabaseOperation.INSERT)
    void checkRunScanningOperationLog() throws Exception {
        Mockito.reset(scanningOperationLog);

        var uuid = UUID.fromString("87ed5c9a-dbd3-11ec-9d64-0242ac120002");
        Mockito.mockStatic(UUID.class);
        Mockito.when(UUID.randomUUID()).thenReturn(uuid);

        assertApiCallOk(
                "controller/crossdock/add-anomaly/requests/described.json",
                null,
                post("/crossdock/add-anomaly"));

        var anomalyContainer = AnomalyContainer.builder(AnomalyContainerType.SECONDARY)
                .status(AnomalyLotStatus.NEW)
                .transportUnitId("ANOM00001")
                .receiptKey("0000000101")
                .loc("DAMAGE01")
                .build();

        var anomalyLot = AnomalyLot.builder(anomalyContainer)
                .id(uuid.toString())
                .sku("")
                .storerKey("465852")
                .altSku("EAN364")
                .manufacturerSku("")
                .description("sku description from request")
                .types(Set.of(ReceivingItemType.DAMAGED))
                .amount(10)
                .mfgDate(Instant.parse("2020-02-01T00:00:00Z"))
                .expDate(Instant.parse("2020-05-03T00:00:00Z"))
                .build();

        Mockito.verify(scanningOperationLog).writeAnomaliesOperation(
                ScanningOperationType.ADD_ANOMALY, Collections.singletonList(anomalyLot),
                "DAMAGE01", "ANOM00001");

        Mockito.when(UUID.randomUUID()).thenCallRealMethod();
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
}

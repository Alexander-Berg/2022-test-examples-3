package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup("/controller/identity-validate/common.xml")
public class IdentityValidationControllerTest extends ReceivingIntegrationTest {

    private final String defaultStorer = "465852";
    private final String defaultSku = "ROV0000000000000000359";
    private final String defaultReceipt = "0000000101";
    private final String defaultPallet = "PLT123";
    private final String urlTemplate = "/items/%s/%s/identity/validate?receiptKey=%s&palletId=%s";
    private final String path = String.format(urlTemplate, defaultStorer, defaultSku, defaultReceipt, defaultPallet);
    private final int invalidStatus = 299;

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateValidRequiredCis() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateValidRequiredCisAlreadyScanned() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-already-scanned-cis.json",
                "controller/identity-validate/cis/response/response-invalid-already-scanned-cis.json",
                put(formatRequestWithPalletAndReceipt("0000000105", "BOX105")));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/imei/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/imei/datasets/sku-identity.xml")
    @DatabaseSetup("/controller/identity-validate/imei/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/imei/datasets/inbound-identity.xml")
    public void validateValidRequiredImeiAlreadyScanned() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/imei/request/request-already-scanned-imei.json",
                "controller/identity-validate/imei/response/response-invalid-already-scanned-imei.json",
                put(formatRequestWithPalletAndReceipt("0000000106", "BOX106")));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateValidOptionalCis() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateRequiredCisNotMatchingRegexp() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid.json",
                "controller/identity-validate/cis/response/response-invalid-by-regex-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateOptionalCisNotMatchingRegexp() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid.json",
                "controller/identity-validate/cis/response/response-invalid-by-regex-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    public void cisIsTooLong() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-too-big.json",
                "controller/identity-validate/cis/response/response-invalid-cis-too-long-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    public void validateRequiredCisWithNotFoundGtin() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-invalid-gtin-not-found-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/item-identity-and-serial-inventory.xml")
    public void validateRequiredCisNotUnique() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-valid-prefix.json",
                "controller/identity-validate/cis/response/response-invalid-not-unique-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/unredeemed-duplicate-cis/item-identity-and-serial-inventory.xml")
    public void validateRequiredCisNotUniqueUnredeemedSuccess() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid-prefix.json",
                "controller/identity-validate/unredeemed-duplicate-cis/response-valid-not-unique.json",
                put(String.format(urlTemplate, defaultStorer, defaultSku, "0000000103", defaultPallet)));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/item-identity.xml")
    public void validateReturnedIdentifier() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisIsNotDeclaredButMustBe() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-invalid-not-declared-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisIsNotDeclaredInInterWarehouse() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(formatRequestWithPalletAndReceipt("0000000102", defaultPallet)));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateCisIsDeclaredInDifferentReceipt() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-invalid-not-declared-v2.json",
                put(formatRequestWithReceiptNumber()));
    }

    private String formatRequestWithReceiptNumber() {
        return formatRequestWithPalletAndReceipt("0000000103", defaultPallet);
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateCisIsDeclared() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity-same-return-ids.xml")
    public void validateCisIsDeclaredReturns() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(formatRequestWithPalletAndReceipt("0000000104", "BOX104")));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateCisIsDeclaredButCaseMismatched() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid-case.json",
                "controller/identity-validate/cis/response/response-invalid-not-declared-v3.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateCisWithUnwantedPrefix() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid-prefix.json",
                "controller/identity-validate/cis/response/response-valid-prefix-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required-must-be-declared.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/inbound-identity.xml")
    public void validateCisWithUnwantedSuffix() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid-suffix.json",
                "controller/identity-validate/cis/response/response-valid-suffix.json",
                put(path));
    }

    @Test
    public void validateIdentityWithUnknownType() throws Exception {
        assertApiCall(BAD_REQUEST.value(),
                "controller/identity-validate/request-unknown-type.json",
                "controller/identity-validate/cis/response/response-invalid-unknown-identity-type-v2.json",
                put(path));
    }

    /**
     * ЧЗ отсканирован более одного раза (повторяется в строке).
     */
    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisWithInvalidTail() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid-tail.json",
                "controller/identity-validate/cis/response/response-invalid-tail.json",
                put(path));
    }

    /**
     * В хвост ЧЗ записан идентификатор контейнера.
     */
    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisWithInvalidTail2() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid-tail-2.json",
                "controller/identity-validate/cis/response/response-invalid-tail-2.json",
                put(path));
    }

    /**
     * Хвост ЧЗ не соответствует формату строки base64.
     */
    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisWithInvalidTail3() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid-tail-3.json",
                "controller/identity-validate/cis/response/response-invalid-tail-3.json",
                put(path));
    }

    /**
     * Хвост ЧЗ соответствует формату строки base64, но все символы в одном регистре.
     * Валидация под отдельным флагом - YM_ENABLE_CIS_CASE_VALIDATION.
     */
    @Test
    @DatabaseSetup("/controller/identity-validate/cis/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-optional.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateCisWithInvalidTail4() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid-tail-4.json",
                "controller/identity-validate/cis/response/response-invalid-tail-4.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/imei/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/imei/datasets/sku-identity.xml")
    public void validateValidImei() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/imei/request/request-valid.json",
                "controller/identity-validate/imei/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/imei/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/imei/datasets/sku-identity.xml")
    public void validateInvalidImei() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/imei/request/request-invalid.json",
                "controller/identity-validate/imei/response/response-invalid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/sn/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/sn/datasets/sku-identity.xml")
    public void validateValidSn() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/sn/request/request-valid.json",
                "controller/identity-validate/sn/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/sn/datasets/identity-type.xml")
    @DatabaseSetup("/controller/identity-validate/sn/datasets/sku-identity.xml")
    public void validateInvalidSnNotMatchingRegexp() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/sn/request/request-invalid.json",
                "controller/identity-validate/sn/response/response-invalid-v2.json",
                put(path));
    }


    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateValidCisWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/cis/request/request-valid.json",
                "controller/identity-validate/cis/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/altsku.xml")
    public void validateInValidCisWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/cis/request/request-invalid.json",
                "controller/identity-validate/cis/response/response-invalid-by-regex-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    public void validateValidImeiWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/imei/request/request-valid.json",
                "controller/identity-validate/imei/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    public void validateInValidImeiWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/imei/request/request-invalid.json",
                "controller/identity-validate/imei/response/response-invalid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    public void validateValidSnWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/sn/request/request-valid.json",
                "controller/identity-validate/sn/response/response-valid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sku-identity-multiple.xml")
    public void validateInValidSnWhenMultipleIdentityTypesRequired() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/sn/request/request-invalid.json",
                "controller/identity-validate/sn/response/response-invalid-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/cis/datasets/sku-identity-required.xml")
    public void validateInValidSnWhenNoSnRequired() throws Exception {
        assertApiCall(OK.value(),
                "controller/identity-validate/sn/request/request-valid.json",
                "controller/identity-validate/sn/response/response-not-required-v2.json",
                put(path));
    }

    @Test
    @DatabaseSetup("/controller/identity-validate/identity-type-multiple.xml")
    @DatabaseSetup("/controller/identity-validate/sn/datasets/sku-identity.xml")
    public void validateInvalidSnNumericRequired() throws Exception {
        assertApiCall(invalidStatus,
                "controller/identity-validate/sn/request/request-invalid-numeric.json",
                "controller/identity-validate/sn/response/response-invalid-numeric.json",
                put(path));
    }

    private void assertApiCall(int expectedStatus, String requestFile, String responseFile,
                               MockHttpServletRequestBuilder request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        }
    }

    private String formatRequestWithPalletAndReceipt(String receiptId, String palletId) {
        return String.format(urlTemplate, defaultStorer, defaultSku, receiptId, palletId);
    }
}

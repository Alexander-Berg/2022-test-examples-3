package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.PutWithdrawRequestDTO;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для ручки: POST /withdraws
 *
 * @see RequestUploadController#submitWithdrawRequest(PutWithdrawRequestDTO)
 */
class UploadWithdrawTest extends MvcIntegrationTest {

    /**
     * Happy-path тест на создание пустой заявки на изъятие в лог. точку.
     */
    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-withdraw.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersWithdrawLogisticsPointNotExistsLocally() throws Exception {
        final String data = getJsonFromFile("valid-empty-withdraw.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-withdraw.json", mvcResult);
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-withdraw.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersWithdrawWithBookingId() throws Exception {
        final String data = getJsonFromFile("valid-empty-withdraw.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-withdraw.json", mvcResult);
    }

    /**
     * Создание пустой заявки на изъятие в лог. точку. без указания supplierId.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-withdraw-with-supplierId-as-null.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersWithdrawWithoutSupplierId() throws Exception {
        final String data = getJsonFromFile("valid-empty-withdraw-without-supplier-id.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-withdraw-without-supplier.json", mvcResult);
    }

    /**
     * Happy-path тест на создание пустой заявки на изъятие в лог. точку.
     * Точка уже создана, но еще пустая
     */
    @Test
    @DatabaseSetup({"classpath:controller/upload-request/before.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml"})
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-withdraw.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersWithdrawLogisticPointExistsLocally() throws Exception {
        final String data = getJsonFromFile("valid-empty-withdraw.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-withdraw.json", mvcResult);
    }

    /**
     * Happy-path тест на создание пустой заявки с данными о курьере
     */
    @Test
    @DatabaseSetup({"classpath:controller/upload-request/before.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml"})
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-withdraw-with-receiver.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/courier-for-2nd-order.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty-2.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/after-orders-withdraw-receiver.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersWithdrawWithCourier() throws Exception {
        final String data = getJsonFromFile("valid-empty-withdraw-with-courier.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-withdraw.json", mvcResult);
    }

    /**
     * Happy-path тест на создание межскладской заявки.
     */
    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-movement-withdraw.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersMovementWithdraw() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-withdraw.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-movement-withdraw.json", mvcResult);
    }

    /**
     * Happy-path тест на создание межскладской заявки для брака.
     */
    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-movement-withdraw-for-defect.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersMovementWithdrawForDefect() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-withdraw-for-defect.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-movement-withdraw-for-defect.json", mvcResult);
    }

    @Test
    @DatabaseSetup({
            "classpath:service/shop-request-validation/logistics-point-empty.xml",
            "classpath:controller/upload-request/before-movement-withdraw-date-in-past.xml"})
    void successfulUpdatingWithDateInPast() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-withdraw-date-in-past.json");

        doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup({
            "classpath:service/shop-request-validation/logistics-point-empty.xml",
            "classpath:controller/upload-request/before-movement-withdraw-date-in-past-wrong-status.xml"})
    void wrongStatusWhenUpdatingWithDateInPast() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-withdraw-date-in-past.json");

        MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertions.assertThat("{\"message\":\"Trying to modify request in invalid status PROCESSED\"," +
                "\"type\":\"INVALID_STATUS_FOR_REQUEST_MODIFICATION\"}")
                .isEqualTo(mvcResult.getResponse().getContentAsString());
    }

    /**
     * Happy-path тест на создание заявки "Возврат заказов".
     */
    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-orders-return-withdraw.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersReturnWithdraw() throws Exception {
        final String data = getJsonFromFile("valid-empty-orders-return-withdraw.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-return-withdraw.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
      @DatabaseSetup(value = "classpath:controller/upload-request/before.xml"),
      @DatabaseSetup(value = "classpath:params/check-withdraw-limits.xml")
    }
    )
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/submit-withdraw-with-registry.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void succesfullSubmitOrderWithdrawWithRegistry() throws Exception {
        final String data = getJsonFromFile("valid-return-withdraw.json");
        final MvcResult mvcResult = doPostWithdrawWithRegistry(data)
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-submit-withdraw-with-registry.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
     @DatabaseSetup(value = "classpath:controller/upload-request/before.xml"),
     @DatabaseSetup(value = "classpath:params/check-withdraw-limits.xml")
    }
    )
    void unsupportedTypeSubmitOrderWithdrawWithRegistry() throws Exception {
        final String data = getJsonFromFile("invalid-return-withdraw.json");
        final MvcResult mvcResult = doPostWithdrawWithRegistry(data)
                .andExpect(status().isBadRequest()).andReturn();
        assertions.assertThat("{\"message\":\"Submit with registry is not allowed for type = 1001\"}")
                .isEqualTo(mvcResult.getResponse().getContentAsString());
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private ResultActions doPostSupply(final String data) throws Exception {
        return mockMvc.perform(
                post("/withdraws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    private ResultActions doPostWithdrawWithRegistry(String data) throws Exception {
        return mockMvc.perform(post("/withdraws-with-registry").contentType(MediaType.APPLICATION_JSON)
                .content(data));
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}

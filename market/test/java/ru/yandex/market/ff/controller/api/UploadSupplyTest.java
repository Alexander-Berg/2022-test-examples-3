package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.PutSupplyRequestDTO;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для ручки: POST /supplies
 *
 * @see RequestUploadController#submitSupplyRequest(PutSupplyRequestDTO)
 */
class UploadSupplyTest extends MvcIntegrationTest {

    /**
     * Happy-path тест на создание пустой заявки на поставку в лог. точку.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyLogisticsPointNotExistsLocally() throws Exception {
        final String data = getJsonFromFile("valid-empty-supply.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на создание заявки на поставку возвратов в лог. точку.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-return-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersReturnSupplyWithRegistry() throws Exception {
        String data = getJsonFromFile("valid-return-supply.json");
        MvcResult mvcResult = doPostSupplyWithInobund(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-return-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на создание заявки на поставку невыкупов в лог. точку.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before-with-log-point.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-unredeemed-supply.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitUnredeemedOrdersSupplyWithRegistry() throws Exception {
        final String data = getJsonFromFile("valid-unredeemed-supply.json");
        final MvcResult mvcResult = doPostSupplyWithInobund(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-unredeemed-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на создание заявки на поставку возвратов в лог. точку.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-updating-return-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersUpdatingReturnSupplyWithRegistry() throws Exception {
        String data = getJsonFromFile("valid-updating-return-supply.json");
        MvcResult mvcResult = doPostSupplyWithInobund(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-updating-return-supply.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    void unsupportedTypeSubmitSupplyWithRegistry() throws Exception {
        String data = getJsonFromFile("invalid-supply-with-registry.json");
        MvcResult mvcResult = doPostSupplyWithInobund(data)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat("{\"message\":\"Submit with registry is not allowed for type = 25\"}")
                .isEqualTo(mvcResult.getResponse().getContentAsString());
    }

    /**
     * Happy-path тест на создание пустой заявки на поставку в лог. точку. с идентификатором брони
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-supply-with-booking-id.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyWithBookingId() throws Exception {
        final String data = getJsonFromFile("valid-empty-supply-with-booking-id.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-supply.json", mvcResult);
    }


    /**
     * Создание пустой заявки на поставку в лог. точку. без указания supplierId.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-supply-with-supplierId-as-null.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyWithoutSupplierId() throws Exception {
        final String data = getJsonFromFile("valid-empty-supply-without-supplier-id.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-supply-without-supplier.json", mvcResult);
    }

    /**
     * Happy-path тест на создание пустой заявки на поставку в лог. точку.
     * Точка уже содзана, но еще пустая
     */
    @Test
    @DatabaseSetup({"classpath:controller/upload-request/before.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml"})
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyLogisticPointExistsLocally() throws Exception {
        final String data = getJsonFromFile("valid-empty-supply.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на обновление заявки с изменёнными данными о курьере
     */
    @Test
    @DatabaseSetup({
        "/service/shop-request-validation/logistics-point-empty.xml",
        "/controller/upload-request/before-with-courier.xml"
    })
    @ExpectedDatabase(
        value = "/service/shop-request-validation/courier-changed.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyWithChangedCourier() throws Exception {
        final String newData = getJsonFromFile("valid-empty-movement-supply-courier-second.json");
        final MvcResult newResult = doPostSupply(newData)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-supply-courier-update.json", newResult);
    }

    @Test
    @DatabaseSetup({
            "/service/shop-request-validation/logistics-point-empty.xml",
            "/controller/upload-request/before-updating-movement-supply.xml"
    })
    void successfulUpdatingWithDateInPast() throws Exception {
        final String newData = getJsonFromFile("valid-empty-movement-supply-date-in-past.json");
        doPostSupply(newData)
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Happy-path тест на создание пустой заявки с данными о курьере
     */
    @Test
    @DatabaseSetup({"classpath:controller/upload-request/before.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml"})
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-supply-with-next-receiver.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/courier-for-2nd-order.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty-3.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
        value = "classpath:service/shop-request-validation/after-orders-supply-shipper-outbounds.xml",
        assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
        value = "classpath:service/shop-request-validation/after-orders-supply-next-receiver.xml",
        assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersSupplyWithCourier() throws Exception {
        final String data = getJsonFromFile("valid-empty-supply-with-courier.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на создание межскладской заявки.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-movement-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersMovementSupply() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-supply.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-movement-supply.json", mvcResult);
    }

    /**
     * Happy-path тест на создание межскладской заявки для брака.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-movement-supply-for-defect.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitOrdersMovementSupplyForDefect() throws Exception {
        final String data = getJsonFromFile("valid-empty-movement-supply-for-defect.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-orders-movement-supply-for-defect.json", mvcResult);
    }

    /**
     * Happy-path тест на создание xDoc заявки на РЦ.
     */
    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-empty-xdoc-supply.xml",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "classpath:service/shop-request-validation/logistics-point-empty.xml",
            assertionMode = NON_STRICT
    )
    void successfulSubmitXDocSupply() throws Exception {
        final String data = getJsonFromFile("valid-empty-xdoc-supply.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-xdoc-supply.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before-supplies-arrived-to-xdoc-service.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-supplies-arrived-to-xdoc-service.xml",
            assertionMode = NON_STRICT
    )
    void updateSlotsForArrivedToXdocService() throws Exception {
        final String data = getJsonFromFile("supplies-arrived-to-xdoc-service.json");
        final MvcResult mvcResult = doPostSupply(data)
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("supplies-arrived-to-xdoc-service-response.json", mvcResult);
    }


    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private ResultActions doPostSupply(final String data) throws Exception {
        return mockMvc.perform(
                post("/supplies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    private ResultActions doPostSupplyWithInobund(final String data) throws Exception {
        return mockMvc.perform(
                post("/supplies-with-registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }


    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}

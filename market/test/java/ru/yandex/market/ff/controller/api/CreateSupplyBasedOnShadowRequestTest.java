package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.TestUtils.dontCare;

/**
 * Функциональный тест для ручки: /requests/{requestId}/create-supply-based-on-shadow-request
 */
class CreateSupplyBasedOnShadowRequestTest extends MvcIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void validateMocks() {
        verify(csClient, dontCare()).getTimezoneByWarehouseId(anyLong());
        verifyNoMoreInteractions(csClient);
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnNonExistShadowSupply() throws Exception {
        var shadowSupplyId = "47";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Failed to find [REQUEST] with id [47]\",\"resourceType\":\"REQUEST\"," +
                        "\"identifier\":\"47\"}"));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnShadowSupplyInInvalidStatus() throws Exception {
        var shadowSupplyId = "1";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"It's prohibited to create real request based on shadow request in status = " +
                        "CREATED. " +
                        "Valid statuses: [VALIDATED, WAITING_FOR_CONFIRMATION]\"}"));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnShadowSupplyWithoutAnyBookedTimeSlots() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 150");
        var shadowSupplyId = "3";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":" +
                        "\"Couldn't find any booked active time slot for shadow supply with requestId=3\"}"));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnShadowSupplyWithTimeSlotInNotValidStatus() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 150");
        var shadowSupplyId = "4";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":" +
                        "\"Couldn't find any booked active time slot for shadow supply with requestId=4\"}"));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnNotShadowSupplyRequestType() throws Exception {
        var shadowSupplyId = "5";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Given request with id=5 is not shadow supply\"}"));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/on-create-supply-based-on-draft.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void tryToCreateBasedOnShadowSupplyWithNullServiceId() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 150");
        var shadowSupplyId = "7";
        final MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Shadow supply must have positive items total count.\"}"));
    }

    @Test
    @DatabaseSetup(
            value = "classpath:controller/upload-request/before-create-supply-based-on-draft-with-cs-booking.xml"
    )
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-supply-based-on-draft-with-cs-booking.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void createSupplyBasedOnShadowRequestWithCsBooking() throws Exception {
        var shadowSupplyId = "0";
        MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect(
                "created-supply-based-on-draft-response-with-cs-booking.json", mvcResult);
    }

    @Test
    @DatabaseSetup(
            value = "classpath:controller/upload-request/before-create-supply-based-on-draft-with-subtype.xml"
    )
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-supply-based-on-draft-with-subtype.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void createSupplyBasedOnShadowRequestWithSubtype() throws Exception {
        var shadowSupplyId = "0";
        MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect(
                "created-supply-based-on-draft-response-with-subtype.json", mvcResult);
    }

    @Test
    @DatabaseSetup(
            value = "classpath:controller/upload-request/" +
                    "before-create-supply-based-on-draft-for-request-in-consolidated-shipping.xml"
    )
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/" +
                    "after-create-supply-based-on-draft-for-request-in-consolidated-shipping.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void createSupplyBasedOnShadowRequestForRequestInConsolidatedShipping() throws Exception {
        var shadowSupplyId = "0";
        MvcResult mvcResult = doCreatingSupply(shadowSupplyId)
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect(
                "created-supply-based-on-draft-response-with-cs-booking.json", mvcResult);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private ResultActions doCreatingSupply(final String requestId) throws Exception {
        return mockMvc.perform(
                post("/requests/" + requestId + "/create-supply-based-on-shadow-request")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}

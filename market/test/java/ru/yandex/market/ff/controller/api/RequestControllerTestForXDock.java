package ru.yandex.market.ff.controller.api;

import java.time.LocalDateTime;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RequestControllerTestForXDock extends MvcIntegrationTest {
    @Autowired
    private DateTimeService dateTimeService;

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-change-xdock-status.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-change-xdock-status.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void changeXDockRequestStatus() throws Exception {
        Mockito.when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.parse("2021-09-13T09:00:00"));

        MvcResult result = performChangeXDockRequestStatus(1, RequestStatus.SENT_TO_XDOC_SERVICE)
            .andExpect(status().isOk())
            .andReturn();

        JSONAssert.assertEquals(
            FileContentUtils.getFileContent("controller/request-api/response/xdoc-request-status-change.json"),
            result.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    void changeXDockRequestStatusIncorrectRequestType() throws Exception {
        MvcResult result = performChangeXDockRequestStatus(1, RequestStatus.ACCEPTED_BY_XDOC_SERVICE)
            .andExpect(status().isBadRequest())
            .andReturn();
        assertThat(result.getResponse().getContentAsString(), equalTo(
            "{\"message\":\"Applicable only for request type 21 X_DOC_PARTNER_SUPPLY_TO_FF\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-change-xdock-status.xml")
    void changeXDockRequestStatusIncorrectNewStatus() throws Exception {
        MvcResult result = performChangeXDockRequestStatus(1, RequestStatus.ACCEPTED_BY_SERVICE)
            .andExpect(status().isBadRequest())
            .andReturn();
        assertThat(result.getResponse().getContentAsString(), equalTo(
            "{\"message\":\"Request status should be one of "
                + "[SENT_TO_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, REJECTED_BY_XDOC_SERVICE, "
                + "INITIAL_ACCEPTANCE_COMPLETED_BY_XDOC_SERVICE, ARRIVED_TO_XDOC_SERVICE] "
                + "but was ACCEPTED_BY_SERVICE\"}"
        ));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-change-xdock-status-inapplicable-status.xml")
    void changeXDockRequestStatusIncorrectOldStatus() throws Exception {
        MvcResult result = performChangeXDockRequestStatus(1, RequestStatus.ACCEPTED_BY_XDOC_SERVICE)
            .andExpect(status().isBadRequest())
            .andReturn();
        assertThat(result.getResponse().getContentAsString(), equalTo(
            "{\"message\":\"It's not allowed to change status for request 1 "
                + "from REJECTED_BY_SERVICE to ACCEPTED_BY_XDOC_SERVICE\","
                + "\"type\":\"INCONSISTENT_REQUEST_MODIFICATION\"}"
        ));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-change-xdock-status-inapplicable-status.xml")
    void changeXDockRequestStatusMissingRequestId() throws Exception {
        MvcResult result = performChangeXDockRequestStatus(2, RequestStatus.ACCEPTED_BY_XDOC_SERVICE)
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @DatabaseSetup("classpath:controller/request-api/xdock-cargo-unit-counts/success/before.xml")
    @Test
    void pushXDockCargoUnitCounts() throws Exception {
        long requestId = 1L;
        String request = FileContentUtils.getFileContent(
            "controller/request-api/xdock-cargo-unit-counts/success/request.json"
        );
        mockMvc.perform(
            put("/requests/" + requestId + "/xdock-cargo-unit-counts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isOk())
            .andReturn();
    }


    @DatabaseSetup("classpath:controller/request-api/xdock-cargo-unit-counts/incorrect-type/before.xml")
    @Test
    void pushXDockCargoUnitCountsIncorrectType() throws Exception {
        long requestId = 1L;
        String request = FileContentUtils.getFileContent(
            "controller/request-api/xdock-cargo-unit-counts/incorrect-type/request.json"
        );
        mockMvc.perform(
            put("/requests/" + requestId + "/xdock-cargo-unit-counts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().is4xxClientError())
            .andReturn();
    }

    private ResultActions performChangeXDockRequestStatus(long id, RequestStatus newStatus) throws Exception {
        return mockMvc.perform(
            put("/requests/" + id + "/xDockStatus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Objects.toString(newStatus.getId()))
        );
    }
}

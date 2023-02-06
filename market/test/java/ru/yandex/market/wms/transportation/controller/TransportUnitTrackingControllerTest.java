package ru.yandex.market.wms.transportation.controller;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.transportation.service.TransportUnitTrackingService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class TransportUnitTrackingControllerTest extends IntegrationTest {

    @BeforeEach
    protected void reset() {
        Mockito.reset(servicebusClient);
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/successful-push-tracking/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/successful-push-tracking/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessPushTransportUnitTracking() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/successful-push-tracking/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/successful-push-when-several-loc/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/successful-push-when-several-loc/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessPushTransportUnitTrackingWhenSeveralLocsSutied() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/transport-unit-tracking/successful-push-when-several-loc/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/successful-push-tracking/empty-tote/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/successful-push-tracking/empty-tote/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessPushTransportUnitTrackingEmptyTote() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/transport-unit-tracking/successful-push-tracking/empty-tote/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/successful-push-with-check-point-loc/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/successful-push-with-check-point-loc/final-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessPushTransportUnitTrackingToInTransitLoc() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/successful-push-with-check-point-loc/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldNoSuccessPushIfUnitIdIsNotPresent() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/no-successful-push-without-unit-id/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldNoSuccessPushIfConveyorLocNotExists() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/no-successful-push-if-conveyor-loc-not-exists/request" +
                                ".json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/no-successful-push-if-unit-status-overflow/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/no-successful-push-if-unit-status-overflow" +
            "/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldNoSuccessPushIfTransportUnitStatusIsOverflow() throws Exception {
        ListAppender<ILoggingEvent> appender = attachLogListAppender(TransportUnitTrackingService.class);

        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/no-successful-push-if-unit-status-overflow/request.json")))
                .andExpect(status().isOk());

        assertEquals(1, appender.list.stream().filter(f -> f.getMessage().contains(
                "Abort process, tot has not reached NOK station"
        )).count());
    }

    @Test
    @DatabaseSetup("/controller/transport-unit-tracking/successful-push-tracking-to-canceled/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-unit-tracking/successful-push-tracking-to-canceled/final-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void shouldSuccessPushIfTransportUnitStatusIsManualCanceled() throws Exception {
        mockMvc.perform(post("/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-unit-tracking/successful-push-tracking-to-canceled/request.json")))
                .andExpect(status().isOk());

        verify(servicebusClient, never()).deleteTransportOrder(any());
    }
}

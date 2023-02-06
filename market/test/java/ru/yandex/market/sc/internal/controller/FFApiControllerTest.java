package ru.yandex.market.sc.internal.controller;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.exception.ScInvalidTransitionException;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.ds.LogisticApiRequestProcessingService;
import ru.yandex.market.tpl.common.util.monitoring.Monitorings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.test.TestFactory.ffOrder;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScIntControllerTest
class FFApiControllerTest {

    @SpyBean
    LogisticApiRequestProcessingService<SortingCenter> fulfillmentLogisticApiRequestProcessingService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;

    @BeforeEach
    void setUp() {
        Monitorings.getMonitoring().getOrCreateUnit(FFApiController.MONITORING_NAME).ok();
    }

    @Test
    void monitoringOkOnBadStatusTransition() throws Exception {
        var sortingCenter = testFactory.storedSortingCenter();
        var order = testFactory.create(
                order(sortingCenter).request(ffOrder("ff_create_order.xml", sortingCenter.getToken())).build()
        ).updateCourier(testFactory.storedCourier()).updateShipmentDate(LocalDate.now(clock)).accept().sort().get();
        doCallRealMethod()
                .when(fulfillmentLogisticApiRequestProcessingService).process(any(HttpServletRequest.class));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(updateOrderBody(sortingCenter.getToken(), order.getExternalId()))
        )
                .andExpect(xpath("/root/requestState/isError").string("true"));
        assertThat(Monitorings.getMonitoring().getResult(FFApiController.MONITORING_NAME).getStatus())
                .isEqualTo(MonitoringStatus.OK);
    }

    private String updateOrderBody(String token, String externalId) throws Exception {
        return String.format(
                IOUtils.toString(
                        Objects.requireNonNull(
                                TestFactory.class.getClassLoader().getResourceAsStream(
                                        "ff_update_order.xml"
                                )
                        ),
                        StandardCharsets.UTF_8
                ),
                token,
                externalId
        );
    }

    @Test
    void monitoringCriticalOnRuntimeException() throws Exception {
        doCallRealMethod()
                .when(fulfillmentLogisticApiRequestProcessingService).process(any(HttpServletRequest.class));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(updateOrderBody("not_existing", "not_existing"))
        )
                .andExpect(xpath("/root/requestState/isError").string("true"));
        assertThat(Monitorings.getMonitoring().getResult(FFApiController.MONITORING_NAME).getStatus())
                .isEqualTo(MonitoringStatus.CRITICAL);
    }

    @Test
    void monitoringCriticalOnException() throws Exception {
        doThrow(RuntimeException.class)
                .when(fulfillmentLogisticApiRequestProcessingService).process(any(HttpServletRequest.class));
        makeFFRequest();
        assertThat(Monitorings.getMonitoring().getResult(FFApiController.MONITORING_NAME).getStatus())
                .isEqualTo(MonitoringStatus.CRITICAL);
    }

    @Test
    void monitoringOkOnScInvalidTransitionException() throws Exception {
        doThrow(ScInvalidTransitionException.class)
                .when(fulfillmentLogisticApiRequestProcessingService).process(any(HttpServletRequest.class));
        makeFFRequest();
        assertThat(Monitorings.getMonitoring().getResult(FFApiController.MONITORING_NAME).getStatus())
                .isEqualTo(MonitoringStatus.OK);
    }

    private void makeFFRequest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content("")
        );
    }

}

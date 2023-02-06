package ru.yandex.market.ff.controller.health;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.health.cache.TrackingProblemCache;
import ru.yandex.market.ff.model.bo.RequestStatusInfo;
import ru.yandex.market.ff.model.bo.RequestStatusResult;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.MonitoringEventService;
import ru.yandex.market.ff.service.ShopRequestModificationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.enums.TrackingProblemMonitoring.WRONG_STATUS_CODE_ERROR;

class TrackingProblemControllerTest  extends MvcIntegrationTest {

    @Autowired
    private MonitoringEventService monitoringEventService;
    @Autowired
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private ShopRequestModificationService shopRequestModificationService;
    @Autowired
    private TrackingProblemCache trackingProblemCache;

    @BeforeEach
    void invalidateCache() {
        trackingProblemCache.invalidateCache();
    }


    @Test
    @DatabaseSetup("classpath:empty.xml")
    void wrongStatusOk() throws Exception {
        mockMvc.perform(get("/health/wrong-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    void wrongStatusFoundThenProcessed() throws Exception {
        monitoringEventService.addErrorIfNotExists(WRONG_STATUS_CODE_ERROR.value(), 1, "Error one");
        mockMvc.perform(get("/health/wrong-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Wrong status code received for entityId: 1, error: Error one"));
        monitoringEventService.setProcessed(WRONG_STATUS_CODE_ERROR.value(), 1);
        invalidateCache();
        mockMvc.perform(get("/health/wrong-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }

    @Test
    @DatabaseSetup("classpath:controller/tracker/before-request-update-statuses.xml")
    void wrongStatusFired() throws Exception {
        List<RequestStatusInfo> statuses = List.of(
                new RequestStatusInfo(RequestStatusResult.of("Unknown status"),
                        dateTimeService.localDateTimeNow().minusSeconds(2)),
                new RequestStatusInfo(RequestStatusResult.of(RequestStatus.SENT_TO_SERVICE),
                        dateTimeService.localDateTimeNow().minusSeconds(3))
        );
        var request = shopRequestFetchingService.getRequestOrThrow(1L);
        shopRequestModificationService.mergeStatusesTransactionally(request, statuses);
        mockMvc.perform(get("/health/wrong-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Wrong status code received for entityId: 1, error: Unknown status"));
    }

}

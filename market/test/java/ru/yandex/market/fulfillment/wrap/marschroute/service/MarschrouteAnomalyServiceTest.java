package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.fulfillment.wrap.marschroute.api.AnomalyInfoClient;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.anomaly.AnomalyInfoResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.anomaly.AnomalyResponseData;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class MarschrouteAnomalyServiceTest extends IntegrationTest {

    @MockBean
    private AnomalyInfoClient anomalyInfoClient;

    @Autowired
    private MarschrouteAnomalyService anomalyService;

    @Test
    void checkSyncAnomalyInfo() {
        when(anomalyInfoClient.getAnomalies()).thenReturn(getInfoResponse());

        anomalyService.syncAnomalyInfo();

        verify(anomalyInfoRepository, times(1)).saveAll(anyList());
    }

    @Test
    void checkFailedSyncAnomalyInfo() {
        when(anomalyInfoClient.getAnomalies()).thenReturn(new AnomalyInfoResponse());

        anomalyService.syncAnomalyInfo();

        verifyZeroInteractions(anomalyInfoRepository);
    }

    private AnomalyInfoResponse getInfoResponse() {
        AnomalyInfoResponse response = AnomalyInfoResponse.builder()
                .data(Collections.singletonList(new AnomalyResponseData()))
                .build();

        response.setSuccess(true);

        return response;
    }

}

package ru.yandex.market.crm.operatorwindow.utils;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudAttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsClient;
import ru.yandex.market.crm.operatorwindow.services.fraud.SmartcallsResults;

import static org.mockito.ArgumentMatchers.any;

@Component
public class MockSmartcalls implements MockService {
    private final SmartcallsResults smartcallsResults;
    private final SmartcallsClient smartcallsClient;

    public MockSmartcalls(SmartcallsResults smartcallsResults,
                          SmartcallsClient smartcallsClient) {
        this.smartcallsResults = smartcallsResults;
        this.smartcallsClient = smartcallsClient;
    }

    public void mockGet(List<ConfirmFraudAttemptResult> results) {
        Mockito.when(smartcallsResults.get(any())).thenReturn(results);
    }

    public void mockAppendToCampaign() {
        Mockito.when(smartcallsClient.appendToCampaign(any(), any(), any()))
                .thenReturn(true);
    }


    @Override
    public void clear() {
        Mockito.reset(smartcallsClient, smartcallsResults);
    }
}

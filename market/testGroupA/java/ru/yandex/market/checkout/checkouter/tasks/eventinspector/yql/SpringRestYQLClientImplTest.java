package ru.yandex.market.checkout.checkouter.tasks.eventinspector.yql;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

public class SpringRestYQLClientImplTest extends AbstractWebTestBase {

    @Autowired
    private YQLClient yqlClient;
    @Autowired
    private YQLMockConfigurer yqlMockConfigurer;

    @Test
    public void shouldExecuteQuery() throws InterruptedException {
        Operation createResponse = new Operation();
        createResponse.setId("deadbeaf");
        createResponse.setStatus("PENDING");

        Operation statusResponse = new Operation();
        statusResponse.setId("deadbeaf");
        statusResponse.setStatus("COMPLETED");

        Map<String, Object> result = new HashMap<>();
        result.put("column0", "1");

        YQLMockParameters parameters = new YQLMockParameters();
        parameters.setCreateResponse(createResponse);
        parameters.setStatusResponse(statusResponse);
        parameters.setOperationData(result);

        yqlMockConfigurer.configure(parameters);

        Map<String, Object> result2 = yqlClient.executeYQL("select 1");
        Assertions.assertEquals("1", result2.get("column0"));
    }

}

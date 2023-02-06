package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.common.web.ServicePingResult;
import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.ServiceInfo;

public class ServicePingResultJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {

        CheckResult result = new CheckResult(CheckResult.Level.OK, "asdasd");
        ServiceInfo serviceInfo = new ServiceInfo("name", "description");
        ServicePingResult servicePingResult = new ServicePingResult(serviceInfo, result);

        String json = write(servicePingResult);
        checkJson(json, "$." + Names.ServicePingResult.RESULT, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.ServicePingResult.SERVICE, JsonPathExpectationsHelper::assertValueIsMap);
    }
}

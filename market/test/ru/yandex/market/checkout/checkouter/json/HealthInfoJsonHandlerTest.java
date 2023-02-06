package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.common.web.HealthInfo;

public class HealthInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        HealthInfo healthInfo = new HealthInfo(Collections.singletonList(EntityHelper.getServicePingResult()));

        String json = write(healthInfo);

        checkJson(json, "$." + Names.HealthInfo.MAX_LEVEL, "OK");
        checkJson(json, "$." + Names.HealthInfo.FULL_RESULT, JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$." + Names.HealthInfo.ERROR_SERVICE_NAMES, JsonPathExpectationsHelper::assertValueIsArray);
    }
}

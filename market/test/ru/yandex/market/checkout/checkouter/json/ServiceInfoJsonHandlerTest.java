package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.ping.ServiceInfo;

public class ServiceInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void shouldSerialize() throws IOException, ParseException {
        ServiceInfo serviceInfo = new ServiceInfo("name", "description");
        serviceInfo.putExtraProperty("key", "value");

        String json = write(serviceInfo);

        checkJson(json, "$." + Names.ServiceInfo.NAME, "name");
        checkJson(json, "$." + Names.ServiceInfo.DESCRIPTION, "description");
        checkJson(json, "$." + Names.ServiceInfo.EXTRA_PROPERTIES + ".key", "value");
    }
}

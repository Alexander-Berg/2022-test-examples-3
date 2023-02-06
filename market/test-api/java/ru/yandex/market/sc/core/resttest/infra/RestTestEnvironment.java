package ru.yandex.market.sc.core.resttest.infra;

import lombok.Value;

/**
 * @author valter
 */
public class RestTestEnvironment {

    public static Settings TESTING_SC_INT = new Settings(
            443,
            "https://sc-int.tst.vs.market.yandex.net"
    );

    public static Settings TESTING_SC_API = new Settings(
            443,
            "https://sc-api.tst.vs.market.yandex.net"
    );

    @Value
    public static class Settings {

        int port;
        String baseUri;

    }

}

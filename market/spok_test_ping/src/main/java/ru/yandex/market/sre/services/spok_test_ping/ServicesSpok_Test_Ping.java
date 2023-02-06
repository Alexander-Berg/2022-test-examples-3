package ru.yandex.market.sre.services.spok_test_ping;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import ru.yandex.market.application.MarketApplicationCommonConfig;

/**
 * ServicesSpok_Test_Ping.
 */
public class ServicesSpok_Test_Ping extends MarketApplicationCommonConfig {

    public ServicesSpok_Test_Ping() {
        //TODO MARKETINFRA-3254
        //Create trace module
        super(null, false);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ServicesSpok_Test_Ping.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

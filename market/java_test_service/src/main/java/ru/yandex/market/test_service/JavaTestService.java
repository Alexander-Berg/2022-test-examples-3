package ru.yandex.market.test_service;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import ru.yandex.market.application.MarketApplicationCommonConfig;

/**
 * JavaTestService.
 */
public class JavaTestService extends MarketApplicationCommonConfig {

    public JavaTestService() {
        //TODO MARKETINFRA-3254
        //Create trace module
        super(null, false);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(JavaTestService.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

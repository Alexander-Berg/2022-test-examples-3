package ru.yandex.market.pharmatestshop;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ru.yandex.market.pharmatestshop.config.SpringApplicationConfig;

/**
 * PharmaTestShop.
 */
@SpringBootApplication
public class PharmaTestShop  {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringApplicationConfig.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}

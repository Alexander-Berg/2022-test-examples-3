package ru.yandex.market.tpl.integration.tests.configuration;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.integration.tests.service.Courier;

@Getter
@Component
@ConfigurationProperties("courier")
public class CourierProperties {

    private final List<Courier> list = new ArrayList<>();
}

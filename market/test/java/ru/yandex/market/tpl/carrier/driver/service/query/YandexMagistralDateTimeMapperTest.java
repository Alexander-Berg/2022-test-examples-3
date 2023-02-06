package ru.yandex.market.tpl.carrier.driver.service.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;

class YandexMagistralDateTimeMapperTest extends BaseDriverApiIntTest {

    @Autowired
    private YandexMagistralDateTimeMapper yandexMagistralDateTimeMapper;

    @Test
    void mapDateTime() {
        yandexMagistralDateTimeMapper.mapDateTime("20.07.2022 14:00");
    }
}
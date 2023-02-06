package ru.yandex.market.wms.packing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.pickbylight.client.mock.PickByLightMockClient;

@Configuration
@Import(PickByLightMockClient.class)
public class PickByLightTestConfig {
}

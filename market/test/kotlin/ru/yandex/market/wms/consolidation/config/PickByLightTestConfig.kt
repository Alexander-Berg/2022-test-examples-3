package ru.yandex.market.wms.consolidation.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.wms.pickbylight.client.mock.PickByLightMockClient

@Configuration
@Import(PickByLightMockClient::class)
class PickByLightTestConfig

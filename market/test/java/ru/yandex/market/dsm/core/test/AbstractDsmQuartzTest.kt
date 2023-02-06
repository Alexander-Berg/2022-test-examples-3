package ru.yandex.market.dsm.core.test

import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import ru.yandex.market.starter.quartz.config.MjQuartzAutoConfiguration

@TestPropertySource(
    properties =
    ["quartzStubsEnabled=false"]
)
@ContextConfiguration(classes = [MjQuartzAutoConfiguration::class])
abstract class AbstractDsmQuartzTest : AbstractTest()

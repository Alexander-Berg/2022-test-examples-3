package ru.yandex.market.markup3.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import ru.yandex.inside.solomon.pusher.SolomonPusher

@TestConfiguration
open class TestMarkupSolomonConfig : MarkupSolomonConfig() {
    override fun solomonPusher(): SolomonPusher = Mockito.mock(SolomonPusher::class.java)
}

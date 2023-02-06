package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.MulticardSet
import ru.yandex.adv.direct.banner.resources.OptionalMulticardSet
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerMulticardSetHandlerTest {

    @Autowired
    lateinit var handler: BannerMulticardSetHandler

    @Test
    fun testEmpty() {
        val builder = createBuilder()

        handler.mapResourceToProto()(null, builder)

        val expected = createBuilder().apply {
            this.multicardSet = OptionalMulticardSet.newBuilder().build()
        }.build()

        assertThat(builder.build()).isEqualTo(expected)
    }

    @Test
    fun testFilled() {
        val builder = createBuilder()

        val multicardSet = MulticardSet.newBuilder().build()

        handler.mapResourceToProto()(multicardSet, builder)

        val expected = createBuilder().apply {
            this.multicardSet = OptionalMulticardSet.newBuilder().setValue(multicardSet).build()
        }.build()

        assertThat(builder.build()).isEqualTo(expected)
    }

    private fun createBuilder(): BannerResources.Builder = BannerResources.newBuilder().apply {
        exportId = 1
        adgroupId = 2
        bannerId = 3
        orderId = 4
        iterId = 5
        updateTime = 6
    }

}

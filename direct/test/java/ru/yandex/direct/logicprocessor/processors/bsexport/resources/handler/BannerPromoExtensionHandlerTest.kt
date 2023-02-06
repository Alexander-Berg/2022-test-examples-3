package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import java.time.Instant.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.OptionalPromoExtension
import ru.yandex.adv.direct.banner.resources.PromoExtension
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerPromoExtensionHandlerTest {

    @Autowired
    lateinit var handler: BannerPromoExtensionHandler

    @Test
    fun testEmpty() {
        val builder = createBuilder()

        handler.mapResourceToProto()(null, builder)

        val expected = createBuilder().apply {
            this.promoExtension = OptionalPromoExtension.newBuilder().build()
        }.build()

        assertThat(builder.build()).isEqualTo(expected)
    }

    @Test
    fun testFilled() {
        val builder = createBuilder()

        val promoExtension = PromoExtension.newBuilder().apply {
            promoExtensionId = 12345L
            description = "описательное описание с ₽ значком"
            finishTime = now().epochSecond
        }.build()

        handler.mapResourceToProto()(promoExtension, builder)

        val expected = createBuilder().apply {
            this.promoExtension = OptionalPromoExtension.newBuilder().setValue(promoExtension).build()
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

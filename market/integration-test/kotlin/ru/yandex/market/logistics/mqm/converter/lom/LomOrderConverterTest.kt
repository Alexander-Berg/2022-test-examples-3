package ru.yandex.market.logistics.mqm.converter.lom

import com.fasterxml.jackson.databind.JsonNode
import io.github.benas.randombeans.EnhancedRandomBuilder
import io.github.benas.randombeans.util.ReflectionUtils
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.lom.model.dto.OrderDto
import ru.yandex.market.logistics.lom.model.enums.OrderStatus
import ru.yandex.market.logistics.lom.model.enums.PartnerType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.randomJsonNode

@DisplayName("Тесты конвертертера заказа из LOM")
class LomOrderConverterTest : AbstractContextualTest() {
    @Autowired
    lateinit var lomOrderConverter: LomOrderConverter

    private val random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .overrideDefaultInitialization(true)
        .randomize(JsonNode::class.java, ReflectionUtils.asRandomizer { randomJsonNode() })
        .randomize(PartnerType::class.java, ReflectionUtils.asRandomizer { randomPartnerType() })
        .randomize(OrderStatus::class.java, ReflectionUtils.asRandomizer { randomOrderStatus() })
        .collectionSizeRange(1, 3)
        .build()

    @Test
    @DisplayName("Проверка конвертации заказа LOM")
    fun testLomOrderConverter() {
        val lomOrder = lomOrderConverter.fromLomOrderDto(random.nextObject(OrderDto::class.java))
        assertSoftly {
            lomOrder shouldNotBe null
            lomOrder.externalId shouldNotBe null
            lomOrder.waybill.forEach { it shouldNotBe null }
            lomOrder.cancellationOrderRequests.forEach { it shouldNotBe null }
            lomOrder.orderTags?.isEmpty() shouldNotBe  true
            lomOrder.orderTags?.forEach { it shouldNotBe null }
            lomOrder.items.forEach { it.instances shouldNotBe null }
        }
    }

    private fun randomPartnerType() = PartnerType.values().filter { v -> v != PartnerType.UNKNOWN }.random()

    private fun randomOrderStatus() = OrderStatus.values().filter { v -> v != OrderStatus.UNKNOWN }.random()
}

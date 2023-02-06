package ru.yandex.market.logistics.mqm.converter.lom

import com.fasterxml.jackson.databind.JsonNode
import io.github.benas.randombeans.EnhancedRandomBuilder
import io.github.benas.randombeans.util.ReflectionUtils
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.randomJsonNode

@DisplayName("Тесты конвертера сегмента из LOM")
class LomWaybillSegmentConverterTest : AbstractContextualTest() {

    @Autowired
    private lateinit var segmentConverter: LomWaybillSegmentConverter

    private val random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .overrideDefaultInitialization(true)
        .randomize(JsonNode::class.java, ReflectionUtils.asRandomizer { randomJsonNode() })
        .collectionSizeRange(1, 3)
        .build()

    @Test
    @DisplayName("Проверка сохранения юридического имени партнера при конвертации сегмента")
    fun testPartnerLegalNameConversion() {
        val segment = random.nextObject(WaybillSegmentDto::class.java)
        segmentConverter.fromDto(segment).partnerLegalName shouldNotBe null
    }

    @Test
    @DisplayName("Проверка сохранения даты и времени отгрузки")
    fun testShipmentDateTimeConversion() {
        val segment = random.nextObject(WaybillSegmentDto::class.java)
        segmentConverter.fromDto(segment).shipment.dateTime shouldNotBe null
    }

    @Test
    @DisplayName("Проверка сохранения идентификаторов сегментаов комбинатора")
    fun testCombinatorSegmentIdsConversion() {
        val segment = random.nextObject(WaybillSegmentDto::class.java)
        val combinatorSegmentIds = segmentConverter.fromDto(segment).combinatorSegmentIds!!
        combinatorSegmentIds.isEmpty() shouldBe false
        combinatorSegmentIds.forEach { it shouldNotBe null }
    }

    @Test
    @DisplayName("Проверка сохранения трекер статуса")
    fun testTrackerStatusConversion() {
        val segment = random.nextObject(WaybillSegmentDto::class.java)
        segmentConverter.fromDto(segment).waybillSegmentStatusHistory.forEach {
            it.trackerStatus shouldNotBe null
        }
    }

    @Test
    fun testPartnerAddressAndEmailConversion() {
        val segment = random.nextObject(WaybillSegmentDto::class.java)
        segmentConverter.fromDto(segment).partnerAddress shouldNotBe null
        segmentConverter.fromDto(segment).partnerEmail shouldNotBe null
    }
}

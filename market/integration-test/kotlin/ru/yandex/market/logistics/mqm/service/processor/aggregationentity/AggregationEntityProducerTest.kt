package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType

class AggregationEntityProducerTest: AbstractContextualTest() {
    @Autowired
    lateinit var producers: List<AggregationEntityProducer>

    @DisplayName("Проверка, что все типы агрегации покрыты конвертерами")
    @ParameterizedTest
    @EnumSource(
        value = AggregationType::class,
        mode = EnumSource.Mode.EXCLUDE,
        //в исключениях - Deprecated элементы
        names = [
            "NONE",
            "DATE",
            "DATE_ORDER",
            "DATE_TIME_PARTNER_RELATION_FROM",
            "DATE_PICKUP_POINT_TYPE",
            "DATE_PARTNER_RELATION_FROM_ON_DEMAND"
        ]
    )
    fun allTypesCovered(aggregationType: AggregationType) {
        assertSoftly { producers.any { it.getAggregationType() === aggregationType } shouldBe true }
    }
}

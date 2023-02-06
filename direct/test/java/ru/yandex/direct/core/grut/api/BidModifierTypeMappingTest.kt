package ru.yandex.direct.core.grut.api

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.mysql2grut.enummappers.BidModifierEnumMappers

class BidModifierTypeMappingTest {
    /**
     * Проверяет, что числовые константы типов корректировок в Грут совпадают с внешними префиксами корректировок
     */
    @Test
    fun testPrefixes() {
        val unsupportedTypes = setOf(
            BidModifierType.BANNER_TYPE_MULTIPLIER, // объединилась с InventoryType-корректировкой DIRECT-170219
            BidModifierType.EXPRESS_CONTENT_DURATION_MULTIPLIER,
            BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER,
            )

        SoftAssertions.assertSoftly { softly ->
            for ((bidModifierType, prefix) in BidModifierService.TYPE_PREFIXES.minus(unsupportedTypes)) {
                val grutType = BidModifierEnumMappers.toGrut(BidModifierType.toSource(bidModifierType)!!)
                softly.assertThat(Integer.parseInt(prefix))
                    .`as`("type $bidModifierType check")
                    .isEqualTo(grutType.number)
            }
        }
    }
}

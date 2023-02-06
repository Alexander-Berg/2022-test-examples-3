package ru.yandex.market.wms.achievement.serialization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.wms.achievement.fromJson
import ru.yandex.market.wms.achievement.model.metric.AchievementGrantedMetric
import ru.yandex.market.wms.achievement.model.metric.AchievementTakenAwayMetric
import ru.yandex.market.wms.achievement.model.metric.Metric
import ru.yandex.market.wms.achievement.model.metric.PackingParcelMetric
import ru.yandex.market.wms.achievement.model.metric.PickingElectronicsMetric
import ru.yandex.market.wms.achievement.model.metric.PickingItemMetric
import ru.yandex.market.wms.achievement.resourceAsString
import ru.yandex.market.wms.achievement.serialize
import ru.yandex.market.wms.achievement.toTree
import java.time.Instant

internal class MetricSerializationTest {

    @ParameterizedTest
    @MethodSource("serialize")
    fun checkSerialization(jsonPath: String, metric: Metric) {
        val actualSerialized = metric.serialize().toTree()
        val expectedSerialized = resourceAsString(jsonPath).toTree()

        assertEquals(expectedSerialized, actualSerialized)
    }

    @ParameterizedTest
    @MethodSource("serialize")
    fun checkDeserialization(jsonPath: String, metric: Metric) {
        val deserialized: Metric = resourceAsString(jsonPath).fromJson()

        assertEquals(metric, deserialized)
    }

    companion object {
        @JvmStatic
        fun serialize() = listOf(
            Arguments.of("serialization/packingParcelMetric.json", PackingParcelMetric(count = 1)),
            Arguments.of("serialization/pickingItemMetric.json", PickingItemMetric(count = 1, area = "MEZ1")),
            Arguments.of("serialization/pickingElectronicsMetric.json", PickingElectronicsMetric(count = 1)),
            Arguments.of(
                "serialization/achievementGrantedMetric.json",
                AchievementGrantedMetric(grantedLevel = 1, userId = 30, achievementId = 6)
            ),
            Arguments.of(
                "serialization/achievementTakenAwayMetric.json",
                AchievementTakenAwayMetric(takenLevel = 1, userId = 30, achievementId = 6)
            ),
        )
    }
}

package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexuidAgeAdGroupAdditionalTargeting
import ru.yandex.direct.test.utils.randomPositiveInt

private val AGE_1 = randomPositiveInt()
private val AGE_2 = randomPositiveInt()

val YANDEXUID_AGE_TARGETING_1: YandexuidAgeAdGroupAdditionalTargeting
    get() = YandexuidAgeAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = CoreAdditionalTargetingValue<Int>().withValue(AGE_1)
    }
val YANDEXUID_AGE_VALUES_1 = listOf(AdditionalTargetingValue(TARGETING, ANY, AGE_1))

val YANDEXUID_AGE_TARGETING_2: YandexuidAgeAdGroupAdditionalTargeting
    get() = YandexuidAgeAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ALL
        value = CoreAdditionalTargetingValue<Int>().withValue(AGE_2)
    }
val YANDEXUID_AGE_VALUES_2 = listOf(AdditionalTargetingValue(FILTERING, ALL, AGE_2))

@RunWith(JUnitParamsRunner::class)
class YandexuidAgeFieldOperationsTest :
    AbstractAdditionalTargetingFieldOperationsTest<Int>(YandexuidAgeFieldOperations) {
    override fun extract_targeting_params() = listOf(
        listOf(
            "From empty",
            emptyList<List<AdGroupAdditionalTargeting>>(),
            emptyList<List<AdditionalTargetingValue<Int>>>()
        ),
        listOf(
            "Positive yandexuidAge",
            listOf(YANDEXUID_AGE_TARGETING_1),
            YANDEXUID_AGE_VALUES_1
        ),
        listOf(
            "Negative yandexuidAge",
            listOf(YANDEXUID_AGE_TARGETING_2),
            YANDEXUID_AGE_VALUES_2
        ),
        listOf(
            "YandexuidAge and other type of targeting",
            listOf(YANDEXUID_AGE_TARGETING_2, CLIDS_TARGETING_1),
            YANDEXUID_AGE_VALUES_2
        )
    )

    override fun remove_targeting_params() = listOf(
        listOf(
            "From empty",
            listOf<AdGroupAdditionalTargeting>(),
            YANDEXUID_AGE_VALUES_1,
            listOf<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Non-existing",
            listOf(YANDEXUID_AGE_TARGETING_1),
            YANDEXUID_AGE_VALUES_2,
            listOf(YANDEXUID_AGE_TARGETING_1),
        ),
        listOf(
            "Existing positive yandexuidAge",
            listOf(YANDEXUID_AGE_TARGETING_1),
            YANDEXUID_AGE_VALUES_1,
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Existing negative yandexuidAge",
            listOf(YANDEXUID_AGE_TARGETING_2),
            YANDEXUID_AGE_VALUES_2,
            emptyList<AdGroupAdditionalTargeting>()
        )
    )

    override fun add_targeting_params() = listOf(
        listOf(
            "New positive yandexuidAge",
            emptyList<AdGroupAdditionalTargeting>(),
            YANDEXUID_AGE_VALUES_1,
            listOf(YANDEXUID_AGE_TARGETING_1)
        ),
        listOf(
            "New negative yandexuidAge",
            emptyList<AdGroupAdditionalTargeting>(),
            YANDEXUID_AGE_VALUES_2,
            listOf(YANDEXUID_AGE_TARGETING_2)
        ),
        listOf(
            "Existing yandexuidAge",
            listOf(YANDEXUID_AGE_TARGETING_1),
            YANDEXUID_AGE_VALUES_1,
            listOf(YANDEXUID_AGE_TARGETING_1)
        ),
    )
}

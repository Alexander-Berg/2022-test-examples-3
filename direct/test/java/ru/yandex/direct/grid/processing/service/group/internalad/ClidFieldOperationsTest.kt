package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting

val CLIDS_TARGETING_1: ClidsAdGroupAdditionalTargeting
    get() = ClidsAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = mutableSetOf(2235264L)
    }
val CLIDS_VALUE_1: AdditionalTargetingValue<Long> = AdditionalTargetingValue(TARGETING, ANY, 2235264L)

val CLIDS_TARGETING_2: ClidsAdGroupAdditionalTargeting
    get() = ClidsAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ALL
        value = mutableSetOf(2235263L, 2235264L, 2233628L)
    }
val CLIDS_VALUES_2: List<AdditionalTargetingValue<Long>> = listOf(
    AdditionalTargetingValue(FILTERING, ALL, 2235263L),
    AdditionalTargetingValue(FILTERING, ALL, 2235264L),
    AdditionalTargetingValue(FILTERING, ALL, 2233628L),
)

@RunWith(JUnitParamsRunner::class)
class ClidFieldOperationsTest : AbstractAdditionalTargetingFieldOperationsTest<Long>(ClidFieldOperations) {

    override fun extract_targeting_params() = listOf(
        listOf(
            "From empty",
            emptyList<List<AdGroupAdditionalTargeting>>(),
            emptyList<List<AdditionalTargetingValue<Long>>>()
        ),
        listOf(
            "Positive clid with one value",
            listOf(CLIDS_TARGETING_1),
            listOf(CLIDS_VALUE_1)
        ),
        listOf(
            "Negative clid with multiple values",
            listOf(CLIDS_TARGETING_2),
            CLIDS_VALUES_2
        ),
        listOf(
            "Clid and other type of targeting",
            listOf(CLIDS_TARGETING_2, QUERY_REFERERS_TARGETING_1),
            CLIDS_VALUES_2
        )
    )

    override fun remove_targeting_params() = listOf(
        listOf(
            "From empty",
            listOf<AdGroupAdditionalTargeting>(),
            listOf(CLIDS_VALUE_1),
            listOf<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Clid with one value",
            listOf(CLIDS_TARGETING_1),
            listOf(CLIDS_VALUE_1),
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Complete removal of the clid with multiple values",
            listOf(CLIDS_TARGETING_2),
            CLIDS_VALUES_2,
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Partial removal of the clid with multiple values",
            listOf(CLIDS_TARGETING_2),
            CLIDS_VALUES_2.dropLast(1),
            listOf(convertTargetingValueToClidTargeting(CLIDS_VALUES_2.last()))
        )
    )

    override fun add_targeting_params() = listOf(
        listOf(
            "New positive clid with one value",
            emptyList<AdGroupAdditionalTargeting>(),
            listOf(CLIDS_VALUE_1),
            listOf(CLIDS_TARGETING_1)
        ),
        listOf(
            "New negative clid with multiple values",
            emptyList<AdGroupAdditionalTargeting>(),
            CLIDS_VALUES_2,
            listOf(CLIDS_TARGETING_2)
        ),
        listOf(
            "New both positive and negative clids",
            emptyList<AdGroupAdditionalTargeting>(),
            CLIDS_VALUES_2 + CLIDS_VALUE_1,
            listOf(CLIDS_TARGETING_1, CLIDS_TARGETING_2)
        ),
        listOf(
            "Existing positive clid with one value",
            listOf(CLIDS_TARGETING_1),
            listOf(CLIDS_VALUE_1),
            listOf(CLIDS_TARGETING_1)
        ),
        listOf(
            "Existing positive clid with multiple values",
            listOf(CLIDS_TARGETING_2),
            CLIDS_VALUES_2,
            listOf(CLIDS_TARGETING_2)
        )
    )

    private fun convertTargetingValueToClidTargeting(targetingValue: AdditionalTargetingValue<Long>) =
        ClidsAdGroupAdditionalTargeting().apply {
            targetingMode = targetingValue.targetingMode
            joinType = targetingValue.joinType
            value = mutableSetOf(targetingValue.innerValue)
        }
}

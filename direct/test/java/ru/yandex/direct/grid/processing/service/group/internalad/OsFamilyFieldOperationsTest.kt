package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily

val OS_FAMILY_IOS = OsFamily().apply {
    targetingValueEntryId = 3L
    minVersion = "9.0"
    maxVersion = "13.0"
}

val OS_FAMILY_ANDROID = OsFamily().apply {
    targetingValueEntryId = 2L
    minVersion = "8.1"
    maxVersion = null
}

val OS_FAMILY_WINDOWS = OsFamily().apply {
    targetingValueEntryId = 33L
    minVersion = null
    maxVersion = null
}

val OS_FAMILIES_TARGETING_1: OsFamiliesAdGroupAdditionalTargeting
    get() = OsFamiliesAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = mutableListOf(OS_FAMILY_IOS)
    }
val OS_FAMILIES_VALUE_1: AdditionalTargetingValue<OsFamily> =
    AdditionalTargetingValue(TARGETING, ANY, OS_FAMILY_IOS)

val OS_FAMILIES_TARGETING_2: OsFamiliesAdGroupAdditionalTargeting
    get() = OsFamiliesAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ALL
        value = mutableListOf(OS_FAMILY_ANDROID, OS_FAMILY_WINDOWS)
    }
val OS_FAMILIES_VALUES_2: List<AdditionalTargetingValue<OsFamily>> = listOf(
    AdditionalTargetingValue(FILTERING, ALL, OS_FAMILY_ANDROID),
    AdditionalTargetingValue(FILTERING, ALL, OS_FAMILY_WINDOWS),
)

@RunWith(JUnitParamsRunner::class)
class OsFamilyFieldOperationsTest : AbstractAdditionalTargetingFieldOperationsTest<OsFamily>(OsFamilyFieldOperations) {

    override fun extract_targeting_params() = listOf(
        listOf(
            "From empty",
            emptyList<List<AdGroupAdditionalTargeting>>(),
            emptyList<List<AdditionalTargetingValue<OsFamily>>>()
        ),
        listOf(
            "Positive OS family with one value",
            listOf(OS_FAMILIES_TARGETING_1),
            listOf(OS_FAMILIES_VALUE_1)
        ),
        listOf(
            "Negative OS family with multiple values",
            listOf(OS_FAMILIES_TARGETING_2),
            OS_FAMILIES_VALUES_2
        ),
        listOf(
            "OS family and other type of targeting",
            listOf(OS_FAMILIES_TARGETING_2, CLIDS_TARGETING_1),
            OS_FAMILIES_VALUES_2
        )
    )

    override fun remove_targeting_params() = listOf(
        listOf(
            "From empty",
            listOf<AdGroupAdditionalTargeting>(),
            listOf(OS_FAMILIES_VALUE_1),
            listOf<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "OS family with one value",
            listOf(OS_FAMILIES_TARGETING_1),
            listOf(OS_FAMILIES_VALUE_1),
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Complete removal of the OS family with multiple values",
            listOf(OS_FAMILIES_TARGETING_2),
            OS_FAMILIES_VALUES_2,
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Partial removal of the query referer with multiple values",
            listOf(OS_FAMILIES_TARGETING_2),
            OS_FAMILIES_VALUES_2.dropLast(1),
            listOf(convertTargetingValueToOsFamilyTargeting(OS_FAMILIES_VALUES_2.last()))
        )
    )

    override fun add_targeting_params() = listOf(
        listOf(
            "New positive OS family with one value",
            emptyList<AdGroupAdditionalTargeting>(),
            listOf(OS_FAMILIES_VALUE_1),
            listOf(OS_FAMILIES_TARGETING_1)
        ),
        listOf(
            "New negative OS family with multiple values",
            emptyList<AdGroupAdditionalTargeting>(),
            OS_FAMILIES_VALUES_2,
            listOf(OS_FAMILIES_TARGETING_2)
        ),
        listOf(
            "New both positive and negative OS families",
            emptyList<AdGroupAdditionalTargeting>(),
            OS_FAMILIES_VALUES_2 + OS_FAMILIES_VALUE_1,
            listOf(OS_FAMILIES_TARGETING_1, OS_FAMILIES_TARGETING_2)
        )
    )

    private fun convertTargetingValueToOsFamilyTargeting(targetingValue: AdditionalTargetingValue<OsFamily>) =
        OsFamiliesAdGroupAdditionalTargeting().apply {
            targetingMode = targetingValue.targetingMode
            joinType = targetingValue.joinType
            value = mutableListOf(targetingValue.innerValue)
        }
}

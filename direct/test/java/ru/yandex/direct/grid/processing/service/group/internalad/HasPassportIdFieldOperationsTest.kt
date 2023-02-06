package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasPassportIdAdGroupAdditionalTargeting

val HAS_PASSPORT_ID_TARGETING_1: HasPassportIdAdGroupAdditionalTargeting
    get() = HasPassportIdAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ALL
    }
val HAS_PASSPORT_ID_VALUES_1 = listOf(AdditionalTargetingValue(TARGETING, ALL, Unit))

val HAS_PASSPORT_ID_TARGETING_2: HasPassportIdAdGroupAdditionalTargeting
    get() = HasPassportIdAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ANY
    }
val HAS_PASSPORT_ID_VALUES_2 = listOf(AdditionalTargetingValue(FILTERING, ANY, Unit))


@RunWith(JUnitParamsRunner::class)
class HasPassportIdFieldOperationsTest : AbstractAdditionalTargetingFieldOperationsTest<Unit>(HasPassportIdFieldOperations) {
    override fun extract_targeting_params() = listOf(
        listOf(
            "From empty",
            emptyList<List<AdGroupAdditionalTargeting>>(),
            emptyList<List<AdditionalTargetingValue<Unit>>>()
        ),
        listOf(
            "HasPassportId=true",
            listOf(HAS_PASSPORT_ID_TARGETING_1),
            HAS_PASSPORT_ID_VALUES_1
        ),
        listOf(
            "HasPassportId=false",
            listOf(HAS_PASSPORT_ID_TARGETING_2),
            HAS_PASSPORT_ID_VALUES_2
        ),
        listOf(
            "HasPassportId and other type of targeting",
            listOf(HAS_PASSPORT_ID_TARGETING_2, CLIDS_TARGETING_1),
            HAS_PASSPORT_ID_VALUES_2
        )
    )

    override fun remove_targeting_params() = listOf(
        listOf(
            "From empty",
            listOf<AdGroupAdditionalTargeting>(),
            HAS_PASSPORT_ID_VALUES_1,
            listOf<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Non-existing",
            listOf(HAS_PASSPORT_ID_TARGETING_1),
            HAS_PASSPORT_ID_VALUES_2,
            listOf(HAS_PASSPORT_ID_TARGETING_1),
        ),
        listOf(
            "Existing HasPassportId=true",
            listOf(HAS_PASSPORT_ID_TARGETING_1),
            HAS_PASSPORT_ID_VALUES_1,
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Existing HasPassportId=false",
            listOf(HAS_PASSPORT_ID_TARGETING_2),
            HAS_PASSPORT_ID_VALUES_2,
            emptyList<AdGroupAdditionalTargeting>()
        ),
    )

    override fun add_targeting_params() = listOf(
        listOf(
            "New HasPassportId=true",
            emptyList<AdGroupAdditionalTargeting>(),
            HAS_PASSPORT_ID_VALUES_1,
            listOf(HAS_PASSPORT_ID_TARGETING_1)
        ),
        listOf(
            "New HasPassportId=false",
            emptyList<AdGroupAdditionalTargeting>(),
            HAS_PASSPORT_ID_VALUES_2,
            listOf(HAS_PASSPORT_ID_TARGETING_2)
        ),
        listOf(
            "Existing HasPassportId",
            listOf(HAS_PASSPORT_ID_TARGETING_1),
            HAS_PASSPORT_ID_VALUES_1,
            listOf(HAS_PASSPORT_ID_TARGETING_1)
        ),
    )
}

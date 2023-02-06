package ru.yandex.direct.grid.processing.service.group.internalad

import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryReferersAdGroupAdditionalTargeting

val QUERY_REFERERS_TARGETING_1: QueryReferersAdGroupAdditionalTargeting
    get() = QueryReferersAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = mutableListOf("%clid=2236846%")
    }
val QUERY_REFERERS_VALUE_1: AdditionalTargetingValue<String> =
    AdditionalTargetingValue(TARGETING, ANY, "%clid=2236846%")

val QUERY_REFERERS_TARGETING_2: QueryReferersAdGroupAdditionalTargeting
    get() = QueryReferersAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ALL
        value = mutableListOf("%clid=2236846%", "%clid=2235263%", "%clid=2235264%")
    }
val QUERY_REFERERS_VALUES_2: List<AdditionalTargetingValue<String>> = listOf(
    AdditionalTargetingValue(FILTERING, ALL, "%clid=2236846%"),
    AdditionalTargetingValue(FILTERING, ALL, "%clid=2235263%"),
    AdditionalTargetingValue(FILTERING, ALL, "%clid=2235264%"),
)

@RunWith(JUnitParamsRunner::class)
class QueryRefererFieldOperationsTest :
    AbstractAdditionalTargetingFieldOperationsTest<String>(QueryRefererFieldOperations) {

    override fun extract_targeting_params() = listOf(
        listOf(
            "From empty",
            emptyList<List<AdGroupAdditionalTargeting>>(),
            emptyList<List<AdditionalTargetingValue<String>>>()
        ),
        listOf(
            "Positive query referer with one value",
            listOf(QUERY_REFERERS_TARGETING_1),
            listOf(QUERY_REFERERS_VALUE_1)
        ),
        listOf(
            "Negative query referer with multiple values",
            listOf(QUERY_REFERERS_TARGETING_2),
            QUERY_REFERERS_VALUES_2
        ),
        listOf(
            "Query referer and other type of targeting",
            listOf(QUERY_REFERERS_TARGETING_2, CLIDS_TARGETING_1),
            QUERY_REFERERS_VALUES_2
        )
    )

    override fun remove_targeting_params() = listOf(
        listOf(
            "From empty",
            listOf<AdGroupAdditionalTargeting>(),
            listOf(QUERY_REFERERS_VALUE_1),
            listOf<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Query referer with one value",
            listOf(QUERY_REFERERS_TARGETING_1),
            listOf(QUERY_REFERERS_VALUE_1),
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Complete removal of the query referer with multiple values",
            listOf(QUERY_REFERERS_TARGETING_2),
            QUERY_REFERERS_VALUES_2,
            emptyList<AdGroupAdditionalTargeting>()
        ),
        listOf(
            "Partial removal of the query referer with multiple values",
            listOf(QUERY_REFERERS_TARGETING_2),
            QUERY_REFERERS_VALUES_2.dropLast(1),
            listOf(convertTargetingValueToQueryRefererTargeting(QUERY_REFERERS_VALUES_2.last()))
        )
    )

    override fun add_targeting_params() = listOf(
        listOf(
            "New positive query referer with one value",
            emptyList<AdGroupAdditionalTargeting>(),
            listOf(QUERY_REFERERS_VALUE_1),
            listOf(QUERY_REFERERS_TARGETING_1)
        ),
        listOf(
            "New negative query referer with multiple values",
            emptyList<AdGroupAdditionalTargeting>(),
            QUERY_REFERERS_VALUES_2,
            listOf(QUERY_REFERERS_TARGETING_2)
        ),
        listOf(
            "New both positive and negative query referers",
            emptyList<AdGroupAdditionalTargeting>(),
            QUERY_REFERERS_VALUES_2 + QUERY_REFERERS_VALUE_1,
            listOf(QUERY_REFERERS_TARGETING_1, QUERY_REFERERS_TARGETING_2)
        )
    )

    private fun convertTargetingValueToQueryRefererTargeting(targetingValue: AdditionalTargetingValue<String>) =
        QueryReferersAdGroupAdditionalTargeting().apply {
            targetingMode = targetingValue.targetingMode
            joinType = targetingValue.joinType
            value = mutableListOf(targetingValue.innerValue)
        }
}

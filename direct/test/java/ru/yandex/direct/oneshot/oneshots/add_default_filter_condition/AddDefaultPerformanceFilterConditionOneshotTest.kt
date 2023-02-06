package ru.yandex.direct.oneshot.oneshots.add_default_filter_condition

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter
import ru.yandex.direct.core.entity.performancefilter.model.Operator
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository
import ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter
import ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest

@OneshotTest
@RunWith(SpringRunner::class)
class AddDefaultPerformanceFilterConditionOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var performanceFilterRepository: PerformanceFilterRepository

    @Autowired
    lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    lateinit var bannerCommonRepository: BannerCommonRepository

    lateinit var oneshot: AddDefaultPerformanceFilterConditionOneshot

    private var clientInfo: ClientInfo? = null

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()

        oneshot = AddDefaultPerformanceFilterConditionOneshot(
            dslContextProvider,
            performanceFilterRepository,
            bannerCommonRepository,
            adGroupRepository
        )
    }

    @Test
    fun execute_noDefaultBefore_success() {
        val adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo)

        val condition = PerformanceFilterCondition<List<DecimalRange>>(
            "price", Operator.RANGE, "[\"3000.00-100000.00\",\"111.00-222.00\"]")
            .withParsedValue(listOf(DecimalRange("3000.00-100000.00"), DecimalRange("111.00-222.00")))
        val filter = defaultPerformanceFilter(adGroupInfo.adGroupId, adGroupInfo.feedId)
            .withSource(Source.SITE)
            .withConditions(listOf(condition))
        steps.performanceFilterSteps().addPerformanceFilter(
            PerformanceFilterInfo().withFilter(filter).withAdGroupInfo(adGroupInfo))

        oneshot.execute(null, null, adGroupInfo.shard)

        val queryFilter = PerformanceFiltersQueryFilter.newBuilder()
            .withAdGroupIds(listOf(adGroupInfo.adGroupId))
            .withoutDeleted()
            .build()
        val actualFilter = performanceFilterRepository.getFilters(clientInfo!!.shard, queryFilter)[0]

        assertThat(actualFilter.conditions).`as`("conditions").isEqualTo(listOf(
            defaultSiteFilterCondition().withParsedValue(null),
            condition
        ))
    }

    @Test
    fun execute_hasDefaultBefore_success() {
        val adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo)
        val filter = defaultPerformanceFilter(adGroupInfo.adGroupId, adGroupInfo.feedId)
            .withSource(Source.SITE)
            .withConditions(listOf(defaultSiteFilterCondition().withParsedValue(true)))
        steps.performanceFilterSteps().addPerformanceFilter(
            PerformanceFilterInfo().withFilter(filter).withAdGroupInfo(adGroupInfo))

        oneshot.execute(null, null, adGroupInfo.shard)

        val actualFilter = performanceFilterRepository
            .getFiltersByAdGroupIds(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[adGroupInfo.adGroupId]!!

        assertThat(actualFilter[0].conditions).`as`("conditions").isEqualTo(listOf(
            defaultSiteFilterCondition().withParsedValue(null)
        ))
    }

    @Test
    fun execute_notSiteFeed_success() {
        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo)
        val filter = defaultPerformanceFilter(adGroupInfo.adGroupId, adGroupInfo.feedId)
            .withConditions(emptyList())
        steps.performanceFilterSteps().addPerformanceFilter(
            PerformanceFilterInfo().withFilter(filter).withAdGroupInfo(adGroupInfo))

        oneshot.execute(null, null, adGroupInfo.shard)

        val actualFilter = performanceFilterRepository
            .getFiltersByAdGroupIds(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[adGroupInfo.adGroupId]!!

        assertThat(actualFilter[0].conditions).`as`("conditions").isEmpty()
    }
}

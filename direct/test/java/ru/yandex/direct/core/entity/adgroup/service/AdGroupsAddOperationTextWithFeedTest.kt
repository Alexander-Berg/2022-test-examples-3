package ru.yandex.direct.core.entity.adgroup.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.assertj.core.api.HamcrestCondition.matching
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterTab
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFeedFilters
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupsAddOperationTextWithFeedTest : AdGroupsAddOperationTestBase() {

    @Test
    fun prepareAndApply_noFilter_success() {
        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)
        val adGroup = TestGroups.clientTextAdGroup(campaignId, feedInfo.feedId, null)

        val expected = TextAdGroup().apply {
            type = adGroup.type!!
            campaignId = adGroup.campaignId!!
            feedId = adGroup.feedId!!
        }

        val addOperation = createAddOperation(Applicability.FULL, listOf(adGroup))
        val result = addOperation.prepareAndApply()
        assertThat(result).`is`(HamcrestCondition(isFullySuccessful()))

        val adGroupId = result[0].result

        val actual = adGroupRepository.getAdGroups(clientInfo.shard, listOf(adGroupId)).single()

        assertThat(actual).`is`(HamcrestCondition(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())))
    }

    @Test
    fun prepareAndApply_noConditions_failCannotBeEmpty() {
        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)
        val feedFilter = FeedFilter().withTab(FeedFilterTab.CONDITION).withConditions(emptyList())
        val adGroup = TestGroups.clientTextAdGroup(campaignId, feedInfo.feedId, feedFilter)

        val addOperation = createAddOperation(Applicability.FULL, listOf(adGroup))
        val result = addOperation.prepareAndApply()
        assertThat(result.validationResult).`is`(
            matching(
                hasDefectWithDefinition<Any>(
                    validationError(
                        path(index(0), field(TextAdGroup.FEED_FILTER), field(FeedFilter.CONDITIONS)),
                        CollectionDefects.notEmptyCollection()
                    )
                )
            )
        )
    }

    @Test
    fun prepareAndApply_manyValidConditions_success() {
        val feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)
        val filter = TestFeedFilters.FILTER_MANY_CONDITIONS
        val adGroup = TestGroups.clientTextAdGroup(campaignId, feedInfo.feedId, filter)

        val expected = TextAdGroup().apply {
            type = adGroup.type!!
            campaignId = adGroup.campaignId!!
            feedId = adGroup.feedId!!
            feedFilter = adGroup.feedFilter!!
        }

        val addOperation = createAddOperation(Applicability.FULL, listOf(adGroup))
        val result = addOperation.prepareAndApply()
        assertThat(result).`is`(HamcrestCondition(isFullySuccessful()))

        val adGroupId = result[0].result

        val actual = adGroupRepository.getAdGroups(clientInfo.shard, listOf(adGroupId)).single()

        assertThat(actual).`is`(HamcrestCondition(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())))
    }
}

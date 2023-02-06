package ru.yandex.direct.core.entity.adgroup.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFeedFilters
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful

@CoreTest
@RunWith(JUnitParamsRunner::class)
class AdGroupsUpdateOperationTextWithFeedTest : AdGroupsUpdateOperationTestBase() {
    @Rule
    @JvmField
    val methodRule = SpringMethodRule()

    sealed class ValueChange<T>() {
        abstract fun newValue(oldValue: T): T
        abstract fun <U> ifChanged(action: (T) -> U): ValueChange<U>

        class Keep<T>() : ValueChange<T>() {
            override fun newValue(oldValue: T) = oldValue
            override fun <U> ifChanged(action: (T) -> U) = Keep<U>()
        }

        data class Change<T>(val newValue: T) : ValueChange<T>() {
            override fun newValue(oldValue: T) = newValue
            override fun <U> ifChanged(action: (T) -> U) = Change(action(newValue))
        }
    }

    data class SuccessTestCase(
        val makeFeedBefore: Boolean,
        val feedFilterBefore: FeedFilter?,
        val makeFeedAfter: ValueChange<Boolean>,
        val feedFilterAfter: ValueChange<FeedFilter?>,
        val onInitAction: (AdGroupsUpdateOperationTextWithFeedTest.() -> Unit)? = null,
        val finalAction: (AdGroupsUpdateOperationTextWithFeedTest.() -> Unit)? = null,
    )

    @Test
    @Parameters(method = "prepareAndApply_updateFeedAndFilter_success_params")
    @TestCaseName("prepareAndApply should be successful: {0}")
    fun prepareAndApply_updateFeedAndFilter_success(testCaseName: String, case: SuccessTestCase) {
        fun maybeNewFeedId(makeFeed: Boolean): Long? =
            if (makeFeed) steps.feedSteps().createDefaultFeed(clientInfo).feedId else null

        val feedIdBefore = maybeNewFeedId(case.makeFeedBefore)

        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo, feedIdBefore, case.feedFilterBefore)

        val feedIdAfter = case.makeFeedAfter.ifChanged(::maybeNewFeedId)

        val changes = ModelChanges(adGroupInfo.adGroupId, TextAdGroup::class.java).apply {
            feedIdAfter.ifChanged { process(it, TextAdGroup.FEED_ID) }
            case.feedFilterAfter.ifChanged { process(it, TextAdGroup.FEED_FILTER) }
        }

        val result = createUpdateOperation(Applicability.FULL, listOf(changes.castModel(AdGroup::class.java)))
            .prepareAndApply()

        assertThat(result).`is`(HamcrestCondition(isFullySuccessful()))

        val actualAdGroup = adGroupRepository.getAdGroups(shard, listOf(adGroupInfo.adGroupId)).single()

        val expected = TextAdGroup().apply {
            this.feedId = feedIdAfter.newValue(feedIdBefore)
            this.feedFilter = case.feedFilterAfter.newValue(case.feedFilterBefore)
        }

        assertThat(actualAdGroup).`is`(
            HamcrestCondition(
                beanDiffer(expected)
                    .useCompareStrategy(
                        onlyFields(
                            newPath(TextAdGroup.FEED_ID.name()),
                            newPath(TextAdGroup.FEED_FILTER.name())
                        )
                    )
            )
        )
    }

    fun prepareAndApply_updateFeedAndFilter_success_params(): List<List<Any?>> {
        val filter1 = TestFeedFilters.FILTER_ONE_CONDITION
        val filter2 = TestFeedFilters.FILTER_MANY_CONDITIONS

        return listOf(
            listOf(
                "Add feed", SuccessTestCase(
                    false, null,
                    ValueChange.Change(true), ValueChange.Keep()
                )
            ),
            listOf(
                "Remove feed", SuccessTestCase(
                    true, null,
                    ValueChange.Change(false), ValueChange.Keep()
                )
            ),
            listOf(
                "Update feed", SuccessTestCase(
                    true, null,
                    ValueChange.Change(true), ValueChange.Keep()
                )
            ),
            listOf(
                "Add filter", SuccessTestCase(
                    true, null,
                    ValueChange.Keep(), ValueChange.Change(filter1)
                )
            ),
            listOf(
                "Remove filter", SuccessTestCase(
                    true, filter1,
                    ValueChange.Keep(), ValueChange.Change(null)
                )
            ),
            listOf(
                "Update filter", SuccessTestCase(
                    true, filter1,
                    ValueChange.Keep(), ValueChange.Change(filter2)
                )
            ),
            listOf(
                "Update feed and filter", SuccessTestCase(
                    true, filter1,
                    ValueChange.Change(true), ValueChange.Change(filter2)
                )
            )
        )
    }

    companion object {
        @ClassRule
        @JvmField
        val classRule = SpringClassRule()

        private fun <U, V> (() -> U).andThen(next: (U) -> V): () -> V = { next(this()) }
    }
}

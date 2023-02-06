package ru.yandex.direct.grid.processing.service.group

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.assertj.core.api.HamcrestCondition.matching
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterCondition
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterTab
import ru.yandex.direct.core.entity.performancefilter.model.Operator
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects
import ru.yandex.direct.core.testing.data.TestFeedFilters
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.exception.GridValidationException
import ru.yandex.direct.grid.processing.model.feedfilter.GdFeedFilter
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterCondition
import ru.yandex.direct.grid.processing.service.feedfilter.FeedFilterGdConverter
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class AdGroupMutationServiceAddTextGroupWithFeedTest : AdGroupMutationServiceTextGroupWithFeedTestBase() {
    @Test
    @Parameters(method = "addTextAdGroups_success_params")
    @TestCaseName("addTextAdGroups_success: {0}")
    fun addTextAdGroups_success(
        testName: String,
        addFeed: Boolean,
        feedFilterToAdd: FeedFilter?
    ) {
        val feedIdToAdd = feedInfo.feedId.takeIf { addFeed }

        val expected = TextAdGroup().apply {
            this.feedId = feedIdToAdd
            this.feedFilter = feedFilterToAdd
        }

        val payload = addTextAdGroups {
            this.smartFeedId = feedIdToAdd
            this.smartFeedFilter = feedFilterToAdd?.toGd()
        }.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.addedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    fun addTextAdGroups_success_params(): List<List<Any?>> = listOf(
        listOf("Feed with no filter", true, null),
        listOf("Feed with filter, no conditions", true, TestFeedFilters.FILTER_ALL_PRODUCTS),
        listOf("Feed with filter, many conditions", true, TestFeedFilters.FILTER_MANY_CONDITIONS)
    )

    @Test
    fun addTextAdGroups_filterWithNoFeed_error() {
        thrown.expect(GridValidationException::class.java)
        thrown.expect(
            hasValidationResult(
                hasErrorsWith(
                    gridDefect(
                        path(
                            field(GdAddTextAdGroup.ADD_ITEMS), index(0),
                            field(GdAddTextAdGroupItem.SMART_FEED_FILTER)
                        ),
                        CommonDefects.isNull()
                    )
                )
            )
        )

        addTextAdGroups {
            this.smartFeedFilter = TestFeedFilters.FILTER_ALL_PRODUCTS.toGd()
        }
    }

    @Test
    fun addTextAdGroups_filterWithInvalidFormat_validationErrors() {
        val validationResult = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = FeedFilter().apply {
                this.tab = FeedFilterTab.TREE
                this.conditions = listOf<FeedFilterCondition<Any>>(
                    FeedFilterCondition("what_is_this_field", Operator.EQUALS, "true"),
                    FeedFilterCondition("price", Operator.CONTAINS, """["hello"]"""),
                    FeedFilterCondition("price", Operator.EQUALS, "false")
                )
            }.toGd()
        }.validationResult

        val conditionsPathPrefix = arrayOf(
            field(GdAddTextAdGroup.ADD_ITEMS), index(0),
            field(GdAddTextAdGroupItem.SMART_FEED_FILTER), field(GdFeedFilter.CONDITIONS)
        )

        val expectedValidationResult = toGdValidationResult(
            toGdDefect(
                path(*conditionsPathPrefix, index(0), field(GdSmartFilterCondition.FIELD)),
                PerformanceFilterDefects.unknownField()
            ),
            toGdDefect(
                path(*conditionsPathPrefix, index(1), field(GdSmartFilterCondition.OPERATOR)),
                PerformanceFilterDefects.invalidOperator()
            ),
            toGdDefect(
                path(*conditionsPathPrefix, index(2), field(GdSmartFilterCondition.STRING_VALUE)),
                CommonDefects.invalidFormat()
            )
        )

        assertThat(validationResult).`is`(
            matching(
                beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())
            )
        )
    }

    fun addTextAdGroups_error_params(): List<List<Any?>> = listOf(
        listOf(
            "Filter with no feed", false, TestFeedFilters.FILTER_ALL_PRODUCTS,
            listOf(path(field(GdAddTextAdGroupItem.SMART_FEED_FILTER)) to CommonDefects.isNull())
        ),
        listOf(
            "Invalid fields/operators/values in filter", true, FeedFilter().apply {
                this.tab = FeedFilterTab.TREE
                this.conditions = listOf<FeedFilterCondition<Any>>(
                    FeedFilterCondition("what_is_this_field", Operator.EQUALS, "true"),
                    FeedFilterCondition("price", Operator.CONTAINS, """["hello"]"""),
                    FeedFilterCondition("price", Operator.EQUALS, "false")
                )
            },
            listOf(path(field(GdAddTextAdGroupItem.SMART_FEED_FILTER)) to CommonDefects.isNull())
        )
    )

    companion object {
        private fun List<AdGroup>.checkFeedAndFilter(expected: TextAdGroup) {
            assertThat(this).asList()
                .singleElement()
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    TextAdGroup.FEED_ID.name(),
                    TextAdGroup.FEED_FILTER.name()
                )
                .isEqualTo(expected);
        }

        private fun FeedFilter.toGd(): GdFeedFilter = FeedFilterGdConverter.toGdFeedFilter(this, TestFeedFilters.SCHEMA)
    }
}

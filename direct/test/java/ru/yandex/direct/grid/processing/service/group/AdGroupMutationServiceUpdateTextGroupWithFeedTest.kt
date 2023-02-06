package ru.yandex.direct.grid.processing.service.group

import junitparams.JUnitParamsRunner
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.assertj.core.api.HamcrestCondition.matching
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.rules.SpringClassRule
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
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.exception.GridValidationException
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers
import ru.yandex.direct.grid.processing.model.feedfilter.GdFeedFilter
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterCondition
import ru.yandex.direct.grid.processing.service.feedfilter.FeedFilterGdConverter
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers
import ru.yandex.direct.regions.Region.MOSCOW_REGION_ID
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class AdGroupMutationServiceUpdateTextGroupWithFeedTest : AdGroupMutationServiceTextGroupWithFeedTestBase() {

    private lateinit var anotherFeedInfo: FeedInfo

    @Before
    fun beforeForUpdate() {
        anotherFeedInfo = steps.feedSteps().createDefaultFeed(clientInfo)
    }

    private fun makeGdUpdateTextAdGroup(adGroupId: Long, additionalConfig: GdUpdateTextAdGroupItem.() -> Unit) =
        GdUpdateTextAdGroup().apply {
            this.updateItems = listOf(
                GdUpdateTextAdGroupItem().apply {
                    this.adGroupId = adGroupId
                    this.adGroupName = ADGROUP_NAME
                    this.adGroupMinusKeywords = listOf()
                    this.libraryMinusKeywordsIds = listOf()
                    this.bidModifiers = GdUpdateBidModifiers()
                    this.regionIds = listOf(MOSCOW_REGION_ID.toInt())
                }.apply(additionalConfig)
            )
        }

    private fun updateTextAdGroup(
        adGroupId: Long,
        additionalConfig: GdUpdateTextAdGroupItem.() -> Unit
    ): GdUpdateAdGroupPayload =
        adGroupMutationService.updateTextAdGroup(
            UidAndClientId.of(uid, clientId),
            uid,
            makeGdUpdateTextAdGroup(adGroupId, additionalConfig)
        )

    @Test
    fun updateTextAdGroups_addFeedNoFilter_success() {
        val adGroupId = addTextAdGroups {}.addedAdGroupItems.single().adGroupId

        val expected = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
        }

        val payload = updateTextAdGroup(adGroupId) {
            this.smartFeedId = feedInfo.feedId
        }.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.updatedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    @Test
    fun updateTextAdGroups_addFeedAndFilterAllProducts_success() {
        val adGroupId = addTextAdGroups {}.addedAdGroupItems.single().adGroupId

        val expected = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_ALL_PRODUCTS
        }

        val payload = updateTextAdGroup(adGroupId) {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_ALL_PRODUCTS.toGd()
        }.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.updatedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    @Test
    fun updateTextAdGroups_updateFeedAndFilter_success() {
        val adGroupId = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_ONE_CONDITION.toGd()
        }.addedAdGroupItems.single().adGroupId

        val expectedBefore = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_ONE_CONDITION
        }

        adGroupRepository.getAdGroups(shard, listOf(adGroupId))
            .checkFeedAndFilter(expectedBefore)

        // Обновляем группу

        val expected = TextAdGroup().apply {
            this.feedId = anotherFeedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_MANY_CONDITIONS
        }

        val payload = updateTextAdGroup(adGroupId) {
            this.smartFeedId = anotherFeedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_MANY_CONDITIONS.toGd()
        }.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.updatedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    @Test
    fun updateTextAdGroups_removeFeedAndFilter_success() {
        val adGroupId = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_ONE_CONDITION.toGd()
        }.addedAdGroupItems.single().adGroupId

        val expectedBefore = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_ONE_CONDITION
        }

        adGroupRepository.getAdGroups(shard, listOf(adGroupId))
            .checkFeedAndFilter(expectedBefore)

        // Обновляем группу

        val expected = TextAdGroup()

        val payload = updateTextAdGroup(adGroupId) {}.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.updatedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    @Test
    fun updateTextAdGroups_removeFilter_success() {
        val adGroupId = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_ONE_CONDITION.toGd()
        }.addedAdGroupItems.single().adGroupId

        val expectedBefore = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_ONE_CONDITION
        }

        adGroupRepository.getAdGroups(shard, listOf(adGroupId))
            .checkFeedAndFilter(expectedBefore)

        // Обновляем группу

        val expected = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
        }

        val payload = updateTextAdGroup(adGroupId) {
            this.smartFeedId = feedInfo.feedId
        }.also { validateResponseSuccessful(it) }

        adGroupRepository.getAdGroups(shard, payload.updatedAdGroupItems.map { it.adGroupId })
            .checkFeedAndFilter(expected)
    }

    @Test
    fun updateTextAdGroups_removeOnlyFeed_error() {
        val adGroupId = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = TestFeedFilters.FILTER_ONE_CONDITION.toGd()
        }.addedAdGroupItems.single().adGroupId

        val expectedBefore = TextAdGroup().apply {
            this.feedId = feedInfo.feedId
            this.feedFilter = TestFeedFilters.FILTER_ONE_CONDITION
        }

        adGroupRepository.getAdGroups(shard, listOf(adGroupId))
            .checkFeedAndFilter(expectedBefore)

        // Обновляем группу
        thrown.expect(GridValidationException::class.java)
        thrown.expect(
            GridValidationMatchers.hasValidationResult(
                GridValidationMatchers.hasErrorsWith(
                    GridValidationMatchers.gridDefect(
                        path(
                            field(GdUpdateTextAdGroup.UPDATE_ITEMS), index(0),
                            field(GdUpdateTextAdGroupItem.SMART_FEED_FILTER)
                        ),
                        CommonDefects.isNull()
                    )
                )
            )
        )

        updateTextAdGroup(adGroupId) {
            this.smartFeedFilter = TestFeedFilters.FILTER_ONE_CONDITION.toGd()
        }
    }

    @Test
    fun updateTextAdGroups_addInvalidFilter_error() {
        val adGroupId = addTextAdGroups {}.addedAdGroupItems.single().adGroupId

        val expectedValidationResult = toGdValidationResult(
            path(
                field(AdGroupMutationService.PATH_FOR_UPDATE_TEXT_AD_GROUP), index(0),
                field(GdUpdateTextAdGroupItem.SMART_FEED_FILTER), field(GdFeedFilter.CONDITIONS), index(0),
                field(GdSmartFilterCondition.FIELD)
            ), PerformanceFilterDefects.unknownField()
        )

        val validationResult = updateTextAdGroup(adGroupId) {
            this.smartFeedId = feedInfo.feedId
            this.smartFeedFilter = FeedFilter().apply {
                this.tab = FeedFilterTab.CONDITION
                this.conditions = listOf(FeedFilterCondition<Any>("what_is_this_field", Operator.EQUALS, "true"))
            }.toGd()
        }.validationResult

        assertThat(validationResult).`is`(
            matching(
                beanDiffer(expectedValidationResult).useCompareStrategy(
                    onlyExpectedFields()
                )
            )
        )
    }

    companion object {
        @ClassRule
        @JvmField
        val classRule = SpringClassRule()

        private val ADGROUP_NAME = RandomStringUtils.randomAlphanumeric(16)

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

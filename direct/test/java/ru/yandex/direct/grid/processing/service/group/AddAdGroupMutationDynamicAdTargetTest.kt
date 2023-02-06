package ru.yandex.direct.grid.processing.service.group

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository
import ru.yandex.direct.core.entity.feed.validation.FeedDefects
import ru.yandex.direct.feature.FeatureName.ENABLED_DYNAMIC_FEED_AD_TARGET_IN_TEXT_AD_GROUP
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.exception.GridValidationException
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupDynamicFeedAdTargetItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup.ADD_ITEMS
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem.DYNAMIC_FEED_AD_TARGET
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
class AddAdGroupMutationDynamicAdTargetTest : AdGroupMutationServiceTextGroupWithFeedTestBase() {
    @Autowired
    lateinit var dynamicTextAdTargetRepository: DynamicTextAdTargetRepository

    @Before
    fun init() {
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId!!, ENABLED_DYNAMIC_FEED_AD_TARGET_IN_TEXT_AD_GROUP, true)
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_add_withDisabledFeature() {
        thrown.expect(GridValidationException::class.java)
        thrown.expect(
            hasValidationResult(
                hasErrorsWith(gridDefect(path(field(ADD_ITEMS)), CommonDefects.invalidValue()))
            )
        )
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId!!, ENABLED_DYNAMIC_FEED_AD_TARGET_IN_TEXT_AD_GROUP, false)
        addTextAdGroups {
            this.dynamicFeedAdTarget = GdAddAdGroupDynamicFeedAdTargetItem().withIsActive(true)
            this.smartFeedId = feedInfo.feedId
        }
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_stayOffWithNull() {
        val payload = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
        }.also { GraphQlTestExecutor.validateResponseSuccessful(it) }

        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems.single().adGroupId

        val dynamicTextAdTargets = dynamicTextAdTargetRepository.getDynamicFeedAdTargetsByAdGroupIds(
            shard,
            clientId,
            setOf(adGroupId)
        )
        assertThat(dynamicTextAdTargets).isEmpty()
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_addDynamicFeedAdTarget() {
        val payload = addTextAdGroups {
            this.smartFeedId = feedInfo.feedId
            this.dynamicFeedAdTarget = GdAddAdGroupDynamicFeedAdTargetItem().withIsActive(true)
        }.also { GraphQlTestExecutor.validateResponseSuccessful(it) }

        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems.single().adGroupId

        val dynamicTextAdTargets = dynamicTextAdTargetRepository.getDynamicFeedAdTargetsByAdGroupIds(
            shard,
            clientId,
            setOf(adGroupId)
        )
        assertThat(dynamicTextAdTargets).hasSize(1)
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_addDynamicFeedAdTargetWithoutFeed() {
        thrown.expect(GridValidationException::class.java)
        thrown.expect(
            hasValidationResult(
                hasErrorsWith(
                    gridDefect(
                        path(field(ADD_ITEMS), index(0), field(DYNAMIC_FEED_AD_TARGET)),
                        FeedDefects.feedIsNotSet()
                    )
                )
            )
        )
        addTextAdGroups {
            this.dynamicFeedAdTarget = GdAddAdGroupDynamicFeedAdTargetItem().withIsActive(true)
        }
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_stayOff() {
        val payload = addTextAdGroups {
            this.dynamicFeedAdTarget = GdAddAdGroupDynamicFeedAdTargetItem().withIsActive(false)
        }.also { GraphQlTestExecutor.validateResponseSuccessful(it) }

        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems.single().adGroupId

        val dynamicTextAdTargets = dynamicTextAdTargetRepository.getDynamicFeedAdTargetsByAdGroupIds(
            shard,
            clientId,
            setOf(adGroupId)
        )
        assertThat(dynamicTextAdTargets).isEmpty()
    }

    @Test
    fun checkAddAdGroupDynamicAdTarget_addWithRelevanceMatch() {
        thrown.expect(GridValidationException::class.java)
        thrown.expect(
            hasValidationResult(
                hasErrorsWith(
                    gridDefect(
                        path(field(ADD_ITEMS), index(0), field(DYNAMIC_FEED_AD_TARGET)),
                        CommonDefects.invalidValue()
                    )
                )
            )
        )
        addTextAdGroups {
            this.feedId = feedInfo.feedId
            this.dynamicFeedAdTarget = GdAddAdGroupDynamicFeedAdTargetItem().withIsActive(true)
            this.relevanceMatch = GdUpdateAdGroupRelevanceMatchItem().withIsActive(true)
        }
    }
}

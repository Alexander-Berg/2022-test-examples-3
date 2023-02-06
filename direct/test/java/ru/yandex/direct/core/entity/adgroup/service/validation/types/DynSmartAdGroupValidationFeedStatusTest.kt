package ru.yandex.direct.core.entity.adgroup.service.validation.types

import org.assertj.core.api.Assertions
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup
import ru.yandex.direct.core.entity.feed.model.MasterSystem
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.validation.FeedDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFeeds
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(Parameterized::class)
class DynSmartAdGroupValidationFeedStatusTest(
    private val expectValid: String,
    private val feedUpdateStatus: UpdateStatus,
    private val feedSource: Source,
    private val feedMasterSystem: MasterSystem,
    private val matcher: Matcher<ValidationResult<Any, Defect<Any>>>
) {
    @get:Rule
    var springMethodRule = SpringMethodRule()

    @Autowired
    lateinit var steps: Steps

    // Пока что проверяем общую логику DynSmart на одном конкретном типе групп
    // Когда-нибудь все DynSmart станут одним классом
    @Autowired
    lateinit var dynamicFeedAdGroupValidation: DynamicFeedAdGroupValidation

    companion object {
        private fun getMatcher(updateStatus: UpdateStatus): Matcher<ValidationResult<Any, Defect<Any>>> =
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(DynamicFeedAdGroup.FEED_ID.name())),
                    FeedDefects.feedStatusWrong(updateStatus)
                )
            )

        @JvmStatic
        @Parameterized.Parameters(name = "valid = {0}, feed status = {1}, feed source = {2}, feed master system = {3}")
        fun testData() = arrayOf(
            arrayOf("No", UpdateStatus.NEW, Source.FILE, MasterSystem.DIRECT, getMatcher(UpdateStatus.NEW)),
            arrayOf("Yes", UpdateStatus.UPDATING, Source.FILE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("No", UpdateStatus.ERROR, Source.FILE, MasterSystem.DIRECT, getMatcher(UpdateStatus.ERROR)),
            arrayOf("Yes", UpdateStatus.DONE, Source.FILE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.OUTDATED, Source.FILE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("No", UpdateStatus.NEW, Source.URL, MasterSystem.DIRECT, getMatcher(UpdateStatus.NEW)),
            arrayOf("Yes", UpdateStatus.UPDATING, Source.URL, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("No", UpdateStatus.ERROR, Source.URL, MasterSystem.DIRECT, getMatcher(UpdateStatus.ERROR)),
            arrayOf("Yes", UpdateStatus.DONE, Source.URL, MasterSystem.DIRECT, Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.OUTDATED, Source.URL, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.NEW, Source.SITE, MasterSystem.DIRECT, Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.UPDATING, Source.SITE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.DONE, Source.SITE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("No", UpdateStatus.ERROR, Source.SITE, MasterSystem.DIRECT, getMatcher(UpdateStatus.ERROR)),
            arrayOf("Yes", UpdateStatus.OUTDATED, Source.SITE, MasterSystem.DIRECT,
                Matchers.hasNoDefectsDefinitions<Any>()),
            arrayOf("Yes", UpdateStatus.NEW, Source.FILE, MasterSystem.MANUAL, Matchers.hasNoDefectsDefinitions<Any>()),
        )
    }

    private lateinit var clientInfo: ClientInfo
    private lateinit var campaign: CampaignInfo
    private lateinit var feedInfo: FeedInfo
    private lateinit var dynamicFeedAdGroup: DynamicFeedAdGroup

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        campaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)

        val feed = TestFeeds.defaultFeed(clientInfo.clientId!!)
        feedInfo = FeedInfo()
        feedInfo.clientInfo = clientInfo
        feedInfo.feed = feed

        dynamicFeedAdGroup = DynamicFeedAdGroup()
            .withCampaignId(campaign.campaignId)
            .withType(AdGroupType.DYNAMIC)
    }

    @Test
    fun test() {
        feedInfo.feed.updateStatus = feedUpdateStatus
        feedInfo.feed.source = feedSource
        feedInfo.feed.masterSystem = feedMasterSystem
        steps.feedSteps().createFeed(feedInfo)

        dynamicFeedAdGroup.feedId = feedInfo.feedId

        val actual = dynamicFeedAdGroupValidation.validateAddAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        Assertions.assertThat(actual).`is`(Conditions.matchedBy(matcher))
    }
}


package ru.yandex.direct.core.entity.adgroup.service.validation.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType.DYNAMIC
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.feedNotExist
import ru.yandex.direct.core.entity.feed.model.UpdateStatus.ERROR
import ru.yandex.direct.core.entity.feed.validation.FeedDefects
import ru.yandex.direct.core.entity.feed.validation.FeedDefects.feedIsNotSet
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration
import ru.yandex.direct.core.testing.data.TestFeeds
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@ContextConfiguration(classes = [CoreTestingConfiguration::class])
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner::class)
class DynamicFeedAdGroupValidationTest {

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dynamicFeedAdGroupValidation: DynamicFeedAdGroupValidation

    private lateinit var clientInfo: ClientInfo
    private lateinit var feedInfo: FeedInfo
    private lateinit var campaign: CampaignInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)
        campaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)
    }

    private fun getDynamicFeedAdGroup(): DynamicFeedAdGroup =
            DynamicFeedAdGroup()
                    .withCampaignId(campaign.campaignId)
                    .withType(DYNAMIC)
                    .withFeedId(feedInfo.feedId)

    @Test
    fun validateAdGroup_success() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        val actual = dynamicFeedAdGroupValidation
                .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAddAdGroup_successWhenEmptyListOfGroups() {
        val actual = dynamicFeedAdGroupValidation
                .validateAddAdGroups(clientInfo.clientId!!, emptyList())
        assertThat(actual).`is`(matchedBy(hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAddAdGroup_failureWhenFeedNotExist() {
        val notExistFeedId = Int.MAX_VALUE - 1L
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        dynamicFeedAdGroup.feedId = notExistFeedId
        val actual = dynamicFeedAdGroupValidation
                .validateAddAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
                validationError(path(index(0), field(DynamicFeedAdGroup.FEED_ID.name())),
                        feedNotExist(notExistFeedId)))))
    }

    @Test
    fun validateAddAdGroup_failureWhenFeedIdIsNull() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        dynamicFeedAdGroup.feedId = null
        val actual = dynamicFeedAdGroupValidation
                .validateAddAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
                validationError(path(index(0), field(DynamicFeedAdGroup.FEED_ID.name())),
                        feedIsNotSet()))))
    }

    @Test
    fun validateAdGroup_successWhenFeedStatusWrong() {
        val errorUpdateStatus = ERROR
        val errorFeed = TestFeeds.defaultFeed(clientInfo.clientId!!)
        errorFeed.updateStatus = errorUpdateStatus
        val errorFeedInfo = FeedInfo()
        errorFeedInfo.clientInfo = clientInfo
        errorFeedInfo.feed = errorFeed
        steps.feedSteps().createFeed(errorFeedInfo)

        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        dynamicFeedAdGroup.feedId = errorFeedInfo.feedId
        val actual = dynamicFeedAdGroupValidation
                .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAddAdGroup_failureWhenFeedStatusWrong() {
        val errorUpdateStatus = ERROR
        val errorFeed = TestFeeds.defaultFeed(clientInfo.clientId!!)
        errorFeed.updateStatus = errorUpdateStatus
        val errorFeedInfo = FeedInfo()
        errorFeedInfo.clientInfo = clientInfo
        errorFeedInfo.feed = errorFeed
        steps.feedSteps().createFeed(errorFeedInfo)

        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        dynamicFeedAdGroup.feedId = errorFeedInfo.feedId
        val actual = dynamicFeedAdGroupValidation
                .validateAddAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
                validationError(path(index(0), field(DynamicFeedAdGroup.FEED_ID.name())),
                        FeedDefects.feedStatusWrong(errorUpdateStatus)))))
    }

    @Test
    fun adGroupClass_success() {
        assertThat(dynamicFeedAdGroupValidation.adGroupClass!!).isEqualTo(DynamicFeedAdGroup::class.java)
    }

}

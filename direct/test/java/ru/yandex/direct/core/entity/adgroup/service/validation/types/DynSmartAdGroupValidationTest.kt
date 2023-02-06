package ru.yandex.direct.core.entity.adgroup.service.validation.types

import org.apache.commons.lang.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated
import ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_BODY_LENGTH
import ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_NAME_LENGTH
import ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_TRACKING_PARAMS_SIZE
import ru.yandex.direct.core.entity.feed.validation.FeedDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.StringDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class DynSmartAdGroupValidationTest {

    @Autowired
    lateinit var steps: Steps

    // Пока что проверяем общую логику DynSmart на одном конкретном типе групп
    // Когда-нибудь все DynSmart станут одним классом
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
            .withType(AdGroupType.DYNAMIC)
            .withFeedId(feedInfo.feedId)
            .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
            .withFieldToUseAsName("name1")
            .withFieldToUseAsBody("body1")

    @Test
    fun validateAdGroup_success() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(Matchers.hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAdGroup_successWhenFieldToUseAsNameIsMaxLength() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsName(StringUtils.repeat("a", MAX_NAME_LENGTH))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(Matchers.hasNoErrorsAndWarnings<Any>()))
    }
    @Test
    fun validateAdGroup_successWhenFieldToUseAsBodyIsMaxLength() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsBody(StringUtils.repeat("b", MAX_BODY_LENGTH))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(Matchers.hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAdGroup_successWhenTrackingParamsIsMaxLength() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withTrackingParams(StringUtils.repeat("a", MAX_TRACKING_PARAMS_SIZE))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(Matchers.hasNoErrorsAndWarnings<Any>()))
    }

    @Test
    fun validateAdGroup_failureWhenFieldToUseAsNameIsTooLong() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsName(StringUtils.repeat("a", MAX_NAME_LENGTH + 1))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.FIELD_TO_USE_AS_NAME.name())),
                FeedDefects.feedNameFieldIsTooLong(MAX_NAME_LENGTH)))))
    }

    @Test
    fun validateAdGroup_failureWhenFieldToUseAsBodyIsTooLong() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsBody(StringUtils.repeat("a", MAX_BODY_LENGTH + 1))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.FIELD_TO_USE_AS_BODY.name())),
                FeedDefects.feedBodyFieldIsTooLong(MAX_BODY_LENGTH)))))
    }

    @Test
    fun validateAdGroup_failureWhenTrackingParamsIsTooLong() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withTrackingParams(StringUtils.repeat("a", MAX_TRACKING_PARAMS_SIZE + 1))
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.TRACKING_PARAMS.name())),
                CollectionDefects.maxStringLength(MAX_TRACKING_PARAMS_SIZE)))))
    }

    @Test
    fun validateAdGroup_failureWhenFieldToUseAsNameHasWrongSymbols() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsName(EMOJI_SYMBOL)
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.FIELD_TO_USE_AS_NAME.name())),
                StringDefects.admissibleChars()))))
    }

    @Test
    fun validateAdGroup_failureWhenFieldToUseAsBodyHasWrongSymbols() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withFieldToUseAsBody(EMOJI_SYMBOL)
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.FIELD_TO_USE_AS_BODY.name())),
                StringDefects.admissibleChars()))))
    }

    @Test
    fun validateAdGroup_failureWhenTrackingParamsHasWrongSymbols() {
        val dynamicFeedAdGroup = getDynamicFeedAdGroup()
            .withTrackingParams(EMOJI_SYMBOL)
        val actual = dynamicFeedAdGroupValidation
            .validateAdGroups(clientInfo.clientId!!, listOf(dynamicFeedAdGroup))
        assertThat(actual).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(path(index(0), field(DynamicFeedAdGroup.TRACKING_PARAMS.name())),
                StringDefects.admissibleChars()))))
    }

    companion object {
        const val EMOJI_SYMBOL = "\uD83D\uDEA9"
    }
}

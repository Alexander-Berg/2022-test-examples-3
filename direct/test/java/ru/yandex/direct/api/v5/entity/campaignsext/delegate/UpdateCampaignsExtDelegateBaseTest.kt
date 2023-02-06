package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.yandex.direct.api.v5.campaignsext.CampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.EmailSettings
import com.yandex.direct.api.v5.campaignsext.Notification
import com.yandex.direct.api.v5.campaignsext.ObjectFactory
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsUpdateItem
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsUpdateSetting
import com.yandex.direct.api.v5.campaignsext.SmsEventsEnum
import com.yandex.direct.api.v5.campaignsext.SmsSettings
import com.yandex.direct.api.v5.campaignsext.TimeTargeting
import com.yandex.direct.api.v5.campaignsext.TimeTargetingOnPublicHolidays
import com.yandex.direct.api.v5.campaignsext.UpdateRequest
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.OperationEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.api.v5.context.ApiContext
import ru.yandex.direct.api.v5.context.ApiContextHolder
import ru.yandex.direct.api.v5.converter.ResultConverter
import ru.yandex.direct.api.v5.entity.GenericApiService
import ru.yandex.direct.api.v5.entity.campaignsext.converter.CampaignsExtUpdateRequestConverter
import ru.yandex.direct.api.v5.entity.campaignsext.converter.ContentPromotionCampaignUpdateItemConverter
import ru.yandex.direct.api.v5.entity.campaignsext.converter.OtherCampaignsUpdateItemConverter
import ru.yandex.direct.api.v5.entity.campaignsext.delegate.UpdateCampaignsExtDelegate
import ru.yandex.direct.api.v5.entity.campaignsext.validation.CampaignsExtUpdateRequestValidator
import ru.yandex.direct.api.v5.entity.campaignsext.validation.OtherCampaignsUpdateRequestValidator
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter
import ru.yandex.direct.api.v5.units.ApiUnitsService
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.SmsFlag
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository
import ru.yandex.direct.core.entity.time.model.TimeInterval
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.data.TestRetargetingConditions
import ru.yandex.direct.core.testing.data.TestRoleRelation
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.libs.timetarget.TimeTarget
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.EnumSet

abstract class UpdateCampaignsExtDelegateBaseTest {

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    protected lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    protected lateinit var requestValidator: CampaignsExtUpdateRequestValidator

    @Autowired
    protected lateinit var resultConverter: ResultConverter

    @Autowired
    protected lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    protected lateinit var featureService: FeatureService

    @Autowired
    protected lateinit var shardHelper: ShardHelper

    @Autowired
    protected lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    protected lateinit var sspPlatformsRepository: SspPlatformsRepository

    @Autowired
    protected lateinit var otherCampaignsUpdateItemConverter: OtherCampaignsUpdateItemConverter

    @Autowired
    protected lateinit var contentPromotionCampaignUpdateItemConverter: ContentPromotionCampaignUpdateItemConverter

    @Autowired
    protected lateinit var otherCampaignsUpdateRequestValidator: OtherCampaignsUpdateRequestValidator

    @Autowired
    protected lateinit var clientRepository: ClientRepository

    @Autowired
    protected lateinit var campaignRepository: CampaignRepository

    protected lateinit var requestConverter: CampaignsExtUpdateRequestConverter

    protected lateinit var genericApiService: GenericApiService

    protected lateinit var delegate: UpdateCampaignsExtDelegate

    protected lateinit var clientInfo: ClientInfo

    protected lateinit var managerInfo: UserInfo

    @Before
    fun prepareTestData() {
        val roleRelationInfo = steps
            .rolesSteps()
            .getRoleRelationInfo(TestRoleRelation.IDM_PRIMARY_MANAGER_AND_CLIENT)
        clientInfo = roleRelationInfo.ownerClientInfo
        managerInfo = roleRelationInfo.operatorUserInfo

        val user = ApiUser()
            .withUid(clientInfo.uid)
            .withClientId(clientInfo.clientId)

        val auth = mock<ApiAuthenticationSource> { auth ->
            on(auth.operator) doReturn user
            on(auth.chiefSubclient) doReturn user
        }

        val apiContextHolder = mock<ApiContextHolder> { apiContextHolder ->
            on(apiContextHolder.get()) doReturn ApiContext()
        }

        genericApiService = GenericApiService(
            apiContextHolder,
            mock<ApiUnitsService>(),
            mock<AccelInfoHeaderSetter>(),
            mock<RequestCampaignAccessibilityCheckerProvider>(),
        )

        val geoTimezoneRepository = mock<GeoTimezoneRepository> { geoTimezoneRepository ->
            on(geoTimezoneRepository.getByTimeZones(eq(listOf(AMSTERDAM_TIMEZONE.timezone.id)))) doReturn
                mapOf(AMSTERDAM_TIMEZONE.timezone.id to AMSTERDAM_TIMEZONE)
        }

        requestConverter = CampaignsExtUpdateRequestConverter(
            geoTimezoneRepository,
            shardHelper,
            campaignTypedRepository,
            sspPlatformsRepository,
            otherCampaignsUpdateItemConverter,
            contentPromotionCampaignUpdateItemConverter,
            otherCampaignsUpdateRequestValidator
        )

        delegate = UpdateCampaignsExtDelegate(
            auth,
            ppcPropertiesSupport,
            featureService,
            requestValidator,
            requestConverter,
            campaignOperationService,
            resultConverter,
        )

        metrikaClientStub.addUserCounter(clientInfo.uid, COUNTER_ID.toInt())
        metrikaClientStub.addCounterGoal(
            COUNTER_ID.toInt(),
            VALID_GOAL_ID.toInt()
        )
        metrikaClientStub.addGoals(
            clientInfo.uid, setOf(
                (TestRetargetingConditions.defaultGoal(VALID_GOAL_ID) as Goal)
                    .withCounterId(COUNTER_ID.toInt()) as Goal
            )
        )
    }

    protected inline fun <reified C : BaseCampaign> getUpdatedCampaign(
        updateItem: CampaignUpdateItem,
    ): C {
        val updateRequest = UpdateRequest().withCampaigns(updateItem)
        val actionResult = genericApiService.doAction(delegate, updateRequest)
        assertThat(actionResult.updateResults[0].errors)
            .describedAs("errors")
            .isNullOrEmpty()
        assertThat(actionResult.updateResults[0].warnings)
            .describedAs("warnings")
            .isNullOrEmpty()

        return campaignTypedRepository
            .getStrictlyFullyFilled(
                clientInfo.shard,
                listOf(updateItem.id),
                C::class.java,
            )
            .single()
    }

    protected companion object TestData {
        val OBJECT_FACTORY = ObjectFactory()

        private val TIME_TARGETING_SCHEDULE = listOf(
            "1,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "2,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "3,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "4,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "6,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100",
            "7,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100"
        )

        val TIME_TARGETING_UPDATE = TimeTargeting().apply {
            schedule = ArrayOfString().withItems(TIME_TARGETING_SCHEDULE)
            holidaysSchedule = OBJECT_FACTORY.createTimeTargetingHolidaysSchedule(
                TimeTargetingOnPublicHolidays().apply {
                    bidPercent = 50
                    startHour = 9
                    endHour = 23
                    suspendOnHolidays = YesNoEnum.NO
                }
            )
            considerWorkingWeekends = YesNoEnum.NO
        }

        val EXPECTED_TIME_TARGET: TimeTarget = TimeTarget.parseRawString(
            "1ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX2ABCDEFGHIJ" +
                "bKcLdMeNOfPgQhRqSrTsUtVuWpX3ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX4ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtV" +
                "uWpX5ABCDEFGHIJKLMNOPQRSTUVWX6AbBcCdDfEFGHIJKLbMcNdOfPcQdReSjTiUhVgWfX7AbBcCdDfEFGHIJKLbMcNdOfPcQdR" +
                "eSjTiUhVgWfX8JfKfLfMfNfOfPfQfRfSfTfUfVfWf;p:o"
        )

        val AMSTERDAM_TIMEZONE: GeoTimezone = GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L)

        val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        const val COUNTER_ID = 1L

        const val VALID_GOAL_ID = 55L

        const val SSP_PLATFORM_1 = "Rubicon"

        const val SSP_PLATFORM_2 = "sspplatform.ru"

        /* notification fields */
        const val EXPECTED_WARNING_BALANCE = 20

        const val EXPECTED_SEND_WARN = true

        const val EXPECTED_SEND_NEWS = true

        val EXPECTED_CHECK_POSITION_INTERVAL_EVENT = CampaignWarnPlaceInterval._30

        const val EXPECTED_EMAIL = "test@email.com"

        val EXPECTED_SMS_INTERVAL = TimeInterval()
            .withStartHour(18)
            .withStartMinute(0)
            .withEndHour(20)
            .withEndMinute(0)

        val EXPECTED_SMS_EVENTS = EnumSet.of(SmsFlag.NOTIFY_METRICA_CONTROL_SMS, SmsFlag.CAMP_FINISHED_SMS)

        val NOTIFICATION_UPDATE = Notification().apply {
            emailSettings = EmailSettings().apply {
                email = EXPECTED_EMAIL
                checkPositionInterval = 30
                sendAccountNews = YesNoEnum.YES
                sendWarnings = YesNoEnum.YES
                warningBalance = EXPECTED_WARNING_BALANCE
            }
            smsSettings = SmsSettings().apply {
                events = listOf(SmsEventsEnum.MONITORING, SmsEventsEnum.FINISHED)
                timeFrom = "18:00"
                timeTo = "20:00"
            }
        }

        val EXPECTED_MEANINGFUL_GOAL = listOf(MeaningfulGoal().apply {
            goalId = VALID_GOAL_ID
            conversionValue = BigDecimal("13")
        })
        val PRIORITY_GOALS_UPDATE_SETTINGS = PriorityGoalsUpdateSetting().apply {
            items = listOf(PriorityGoalsUpdateItem().apply {
                value = 13000000
                goalId = VALID_GOAL_ID
                operation = OperationEnum.SET
            })
        }
    }
}

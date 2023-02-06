package ru.yandex.direct.jobs.walletswarnings

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.common.TranslationService
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.eventlog.model.DaysLeftNotificationType.THREE_DAYS_REMAIN
import ru.yandex.direct.core.entity.eventlog.service.EventLogService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.notification.repository.SmsQueueRepository
import ru.yandex.direct.core.entity.statistics.service.OrderStatService
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.repository.TestSmsQueueRepository
import ru.yandex.direct.core.testing.repository.TestUserRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.dbqueue.steps.DbQueueSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.sender.YandexSenderClient
import ru.yandex.direct.sender.YandexSenderTemplateParams
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.math.BigDecimal

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WalletsWarningsSenderJobSendEmailTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dbQueueSteps: DbQueueSteps

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var eventLogService: EventLogService

    @Autowired
    private lateinit var smsQueueRepository: SmsQueueRepository

    @Autowired
    private lateinit var translationService: TranslationService

    @Autowired
    private lateinit var walletsWarningsMailTemplateResolver: WalletsWarningsMailTemplateResolver

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    private lateinit var testUserRepository: TestUserRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Mock
    private lateinit var orderStatService: OrderStatService

    @Mock
    private lateinit var senderClient: YandexSenderClient

    private lateinit var walletsWarningsSenderJob: WalletsWarningsSenderJob

    private lateinit var clientInfo: ClientInfo
    private var walletId: Long = 0
    private lateinit var campaignSlug: String
    private lateinit var templateArgs: Map<String, String>

    @BeforeEach
    fun before() {
        MockitoAnnotations.initMocks(this)

        clientInfo = steps.clientSteps().createDefaultClientAnotherShard()
        dbQueueSteps.registerJobType(DbQueueJobTypes.UPDATE_AGGREGATOR_DOMAINS)
        dbQueueSteps.clearQueue(DbQueueJobTypes.UPDATE_AGGREGATOR_DOMAINS)

        walletsWarningsSenderJob = WalletsWarningsSenderJob(clientInfo.shard, senderClient, orderStatService,
            campaignService, campaignRepository, userService, featureService, smsQueueRepository,
            translationService, eventLogService, walletsWarningsMailTemplateResolver)

        steps.featureSteps().addClientFeature(
            clientInfo.clientId, FeatureName.NEW_WALLET_WARNINGS_ENABLED, true)

        // остаток 90_000
        val walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo)
        steps.campaignSteps()
            .createCampaignUnderWallet(clientInfo, walletInfo.campaignId, BigDecimal.valueOf(300))
        walletId = walletInfo.campaignId

        // за 7 дней потратили 217_000, или 31_000 за день (90_000 остаток) -> осталось менее 3ех дней
        Mockito.`when`(orderStatService.getOrdersSumSpent(anyList(), anyList(), any()))
            .thenReturn(mapOf("week" to Money.valueOf(BigDecimal.valueOf(217_000L), CurrencyCode.RUB)))

        campaignSlug = walletsWarningsMailTemplateResolver
            .resolveTemplateId(clientInfo.chiefUserInfo?.user?.lang, THREE_DAYS_REMAIN)

        templateArgs = mapOf(
            WalletsWarningsSenderJob.LOGIN to clientInfo.login,
            WalletsWarningsSenderJob.CLIENT_ID to clientInfo.clientId.toString())
    }

    @AfterEach
    fun after() {
        testCampaignRepository.deleteCampaign(clientInfo.shard, walletId)
    }

    /**
     * У кошелька не валидный email в camp_options -> оповещение отправляется на email пользователя (из users)
     */
    @Test
    fun checkSendNotificationToUserEmail() {
        val email = clientInfo.chiefUserInfo?.user?.email

        testCampaignRepository.updateEmail(clientInfo.shard, walletId, "")

        executeJob()

        val expectTemplateParams = YandexSenderTemplateParams.Builder()
            .withToEmail(email)
            .withCampaignSlug(campaignSlug)
            .withArgs(templateArgs)
            .build()

        val argument = ArgumentCaptor.forClass(YandexSenderTemplateParams::class.java)
        verify(senderClient).sendTemplate(argument.capture(), any())

        assertThat(argument.value)
            .`as`("Параметры шаблона")
            .`is`(matchedBy(beanDiffer(expectTemplateParams).useCompareStrategy(onlyExpectedFields())))
    }

    /**
     * У кошелька указан валидный email в camp_options -> оповещение отправляется на email из camp_options
     */
    @Test
    fun checkSendNotificationToWalletEmail() {
        val email = "email123@yandex.ru"

        testCampaignRepository.updateEmail(clientInfo.shard, walletId, email)

        executeJob()

        val expectTemplateParams = YandexSenderTemplateParams.Builder()
            .withToEmail(email)
            .withCampaignSlug(campaignSlug)
            .withArgs(templateArgs)
            .build()

        val argument = ArgumentCaptor.forClass(YandexSenderTemplateParams::class.java)
        verify(senderClient).sendTemplate(argument.capture(), any())

        assertThat(argument.value)
            .`as`("Параметры шаблона")
            .`is`(matchedBy(beanDiffer(expectTemplateParams).useCompareStrategy(onlyExpectedFields())))
    }

    /**
     * У кошелька и у пользователя указан не валидный email -> оповещение не отправляется
     */
    @Test
    fun checkDoNotSendNotificationWithUnvalidatedEmails() {
        testCampaignRepository.updateEmail(clientInfo.shard, walletId, "")
        testUserRepository.setUnvalidatedUserEmail(clientInfo.shard, clientInfo.uid)

        executeJob()

        verify(senderClient, never()).sendTemplate(any(), any())
    }

    private fun executeJob() {
        Assertions.assertThatCode { walletsWarningsSenderJob.execute() }
            .doesNotThrowAnyException()
    }
}

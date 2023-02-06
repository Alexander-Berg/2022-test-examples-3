package ru.yandex.direct.api.v5.entity.campaigns.converter

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.api.v5.entity.campaigns.StatusClarificationTranslations
import ru.yandex.direct.api.v5.entity.campaigns.container.CampaignAnyFieldEnum
import ru.yandex.direct.api.v5.entity.campaigns.container.GetCampaignsContainer
import ru.yandex.direct.api.v5.entity.campaigns.service.CampaignStatusCalculator
import ru.yandex.direct.api.v5.entity.campaigns.service.CampaignSumAvailableForTransferCalculator
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.TranslationService
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusWallet
import ru.yandex.direct.core.entity.campaign.aggrstatus.WalletStatus
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign
import ru.yandex.direct.core.entity.campaign.service.TimeTargetStatusService
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperationName
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone
import ru.yandex.direct.core.entity.timetarget.model.GroupType
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.i18n.Translatable
import ru.yandex.direct.i18n.types.ConcatTranslatable
import ru.yandex.direct.libs.timetarget.HoursCoef
import ru.yandex.direct.libs.timetarget.TimeTargetUtils
import ru.yandex.direct.libs.timetarget.WeekdayType
import ru.yandex.direct.utils.DateTimeUtils
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Api5Test
@RunWith(JUnitParamsRunner::class)
class CampaignStatusCalculatorTest {

    @Rule
    @JvmField
    val springMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var translationService: TranslationService

    @Autowired
    private lateinit var timeTargetStatusService: TimeTargetStatusService

    private lateinit var campaignStatusCalculator: CampaignStatusCalculator

    private val translations = StatusClarificationTranslations

    @Before
    fun before() {
        campaignStatusCalculator = CampaignStatusCalculator(
            timeTargetStatusService = timeTargetStatusService,
            clock = CLOCK,
        )
    }

    fun convertedStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1905-1938
        arrayOf(
            "Сконвертирована",
            createContainer(
                campaign = createCampaign {
                    currency = CurrencyCode.YND_FIXED
                    currencyConverted = true
                    statusActive = false
                    statusArchived = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.converted(),
        ),
    )

    fun archivedStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L836-868
        arrayOf(
            "Кампания перенесена в архив",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusArchived = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.archived(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L869-902
        arrayOf(
            "Кампания перенесена в архив. Идет активизация",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = true
                    statusArchived = true
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = false,
            ),
            concat(
                translations.INSTANCE.archived(),
                translations.INSTANCE.activating(),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1778-1811
        arrayOf(
            "Кампания перенесена в архив (кампания без баннеров)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusArchived = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = false,
                hasActiveBanners = false, // в оригинальном тесте hasActiveBanners = true, это выглядит неправильно
            ),
            translations.INSTANCE.archived(),
        ),

        // ещё были тесты на несконвертированные кампании мультивалютного клиента
        // но, кажется, валюта клиента никак не используется при вычислении статусов
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1872-1904
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1939-1971
    )

    fun inProgressStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L78-107
        arrayOf(
            "Идут показы",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.campaignIsInProgress(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L108-138
        arrayOf(
            "Идут показы. Идёт активизация",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.campaignIsInProgress(),
                translations.INSTANCE.activating(),
            ),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1034-1065
        arrayOf(
            "Идут показы. Дата окончания кампании XX.XX.XXXX (последний день работы кампании - сегодня)",
            createContainer(
                campaign = createCampaign {
                    endDate = TODAY
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.campaignIsInProgress(),
                translations.endDate(TODAY),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1286-1316
        arrayOf(
            "Идут показы. Дата окончания кампании XX.XX.XXXX (последний день работы кампании - завтра)",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.campaignIsInProgress(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1318-1349
        arrayOf(
            "Идут показы. Идет активизация. Дата окончания кампании XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.campaignIsInProgress(),
                translations.INSTANCE.activating(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1571-1601
        arrayOf(
            "Идут показы (дневной бюджет установлен, но ещё не закончился)",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте ещё фигурирует поле spent_today, но оно в расчётах не используется
                    dayBudget = 10.toBigDecimal()
                    dayBudgetStopTime = null
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 100.toBigDecimal()
                    sumSpent = 80.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.campaignIsInProgress(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1571-1601
        arrayOf(
            "Идут показы (установлен дневной бюджет на кампанию, но ещё не закончился)",
            createContainer(
                campaign = createCampaign {
                    // в тесте ещё фигурирует поле spent_today, но оно в расчётах не используется
                    dayBudget = 10.toBigDecimal()
                    dayBudgetStopTime = null
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 100.toBigDecimal()
                    sumSpent = 80.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.campaignIsInProgress(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1675-1682
        arrayOf(
            "Идут показы (установлен дневной бюджет на общий счёт, но он закончился вчера)",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте ещё фигурирует поле spent_today, но оно в расчётах не используется
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 1234.toBigDecimal()
                    walletId = 2L
                },
                hasBanners = true,
                hasActiveBanners = true,
                aggregatedStatusWallet = AggregatedStatusWallet().apply {
                    id = 2L
                    sum = 1000.toBigDecimal()
                    autoOverdraftAddition = 300.toBigDecimal()
                    status = WalletStatus().withBudgetLimitationStopTime(NOW.minusDays(1))
                },
                wallet = Campaign()
                    .withId(2L)
                    .withSum(1000.toBigDecimal())
            ),
            translations.INSTANCE.campaignIsInProgress()
        ),
    )

    fun stoppedStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L442-471
        arrayOf(
            "Кампания остановлена",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.SENT
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "1".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.stopped(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L472-502
        arrayOf(
            "Кампания остановлена. Идёт активизация",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "1".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.stopped(),
                translations.INSTANCE.activating(),
            ),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1131-1161
        arrayOf(
            "Кампания остановлена. Дата окончания кампании XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.SENT
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "1".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.stopped(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1131-1161
        arrayOf(
            "Кампания остановлена. Кампания закончилась XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.SENT
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = false
                    sum = 10.toBigDecimal()
                    sumSpent = "1".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.stopped(),
                translations.ended(YESTERDAY),
            )
        ),
    )

    fun fundsRunOutStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L382-411
        arrayOf(
            "Средства на счете закончились",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.fundsRunOut(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1255-1285
        arrayOf(
            "Средства на счете закончились. Дата окончания кампании %s",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.fundsRunOut(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1255-1285
        arrayOf(
            "Средства на счете закончились. Кампания закончилась %s",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.fundsRunOut(),
                translations.ended(YESTERDAY),
            )
        ),
    )

    fun awaitingPaymentStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L592-621
        arrayOf(
            "Ждёт оплаты",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.awaitingPayment(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1163-1193
        arrayOf(
            "Ждёт оплаты. Дата окончания кампании XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.awaitingPayment(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1163-1193
        arrayOf(
            "Ждёт оплаты. Кампания закончилась XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.awaitingPayment(),
                translations.ended(YESTERDAY),
            )
        ),
    )

    fun draftStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L532-561
        arrayOf(
            "Черновик. Ждёт оплаты",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NEW
                    statusPostModerate = CampaignStatusPostmoderate.NEW
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.draft(),
                translations.INSTANCE.awaitingPayment(),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1101-1130
        arrayOf(
            "Черновик. Дата окончания кампании XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NEW
                    statusPostModerate = CampaignStatusPostmoderate.NEW
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.draft(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1351-1380
        arrayOf(
            "Черновик. Кампания закончилась XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NEW
                    statusPostModerate = CampaignStatusPostmoderate.NEW
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.draft(),
                translations.ended(YESTERDAY),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1747-1777
        arrayOf(
            "Черновик (кампания без объявлений)",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NEW
                    statusPostModerate = CampaignStatusPostmoderate.NEW
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = BigDecimal.ZERO
                },
                hasBanners = false,
                hasActiveBanners = false, // в оригинальном тесте has_active_banners было true
            ),
            translations.INSTANCE.draft()
        ),
    )

    fun awaitingModerationStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L412-441
        arrayOf(
            "Ожидает модерации",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.SENT
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.awaitingModeration(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L562-591
        arrayOf(
            "Ожидает модерации. Ждёт оплаты",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.awaitingModeration(),
                translations.INSTANCE.awaitingPayment(),
            ),
        ),

        arrayOf(
            "Ожидает модерации (для остановленной кампании)",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = true
                    stopTime = DEFAULT_STOP_TIME
                    sum = 10.toBigDecimal()
                    sumSpent = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.awaitingModeration(),
        ),
    )

    fun acceptedOnModerationStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L503-531
        arrayOf(
            "Допущено модератором",
            createContainer(
                campaign = createCampaign {
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.acceptedOnModeration(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L903-933
        arrayOf(
            "Допущено модератором (orderId = 0)",
            createContainer(
                campaign = createCampaign {
                    orderId = 0
                    startDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.acceptedOnModeration(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1194-1223
        arrayOf(
            "Допущено модератором. Дата окончания кампании XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.acceptedOnModeration(),
                translations.endDate(TOMORROW),
            )
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1444-1473
        arrayOf(
            "Допущено модератором. Кампания закончилась XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.INSTANCE.acceptedOnModeration(),
                translations.ended(YESTERDAY),
            )
        ),
    )

    fun rejectedOnModerationStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L651-679
        arrayOf(
            "Отклонено модератором",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NO
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = true
                    stopTime = DEFAULT_STOP_TIME
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.rejectedOnModeration(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L680-709
        arrayOf(
            "Отклонено модератором (sumToPay > 0)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.NO
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = true
                    stopTime = DEFAULT_STOP_TIME
                    sum = BigDecimal.ZERO
                    sumSpent = BigDecimal.ZERO
                    sumToPay = 10.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.INSTANCE.rejectedOnModeration(),
        ),
    )

    fun noAdsStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1685-1716
        arrayOf(
            "Нет объявлений (промодерированная кампания без объявлений)",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = false,
                hasActiveBanners = false, // в оригинальном тесте hasActiveBanners = true, это выглядит неправильно
            ),
            translations.INSTANCE.noAds(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1717-1746
        arrayOf(
            "Нет объявлений (непромодерированная кампания без объявлений)",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.SENT
                    statusPostModerate = CampaignStatusPostmoderate.YES
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = false,
                hasActiveBanners = false, // в оригинальном тесте hasActiveBanners = true, это выглядит неправильно
            ),
            translations.INSTANCE.noAds(),
        ),
    )

    fun noActiveBannersStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L710-740
        arrayOf(
            "Нет активных объявлений (statusActive = true)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = false,
            ),
            translations.INSTANCE.noActiveAds(),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L741-772
        arrayOf(
            "Нет активных объявлений. Идет активизация (statusActive = true, statusBsSynced = No)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = false,
            ),
            concat(
                translations.INSTANCE.noActiveAds(),
                translations.INSTANCE.activating()
            ),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L773-804
        arrayOf(
            "Нет активных объявлений. Идет активизация (statusActive = false, statusBsSynced = No)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = false,
            ),
            concat(
                translations.INSTANCE.noActiveAds(),
                translations.INSTANCE.activating()
            ),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L805-835
        arrayOf(
            "Нет активных объявлений (statusActive = false)",
            createContainer(
                campaign = createCampaign {
                    startDate = TODAY
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = false,
            ),
            translations.INSTANCE.noActiveAds(),
        ),
    )

    fun startDateStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L934-966
        arrayOf(
            "Начало XX.XX.XXXX. Идет активизация",
            createContainer(
                campaign = createCampaign {
                    orderId = 0
                    startDate = TOMORROW
                    statusActive = false
                    statusBsSynced = CampaignStatusBsSynced.NO
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            concat(
                translations.startDate(TOMORROW),
                translations.INSTANCE.activating(),
            ),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L967-998
        arrayOf(
            "Начало XX.XX.XXXX (дата окончания кампании известна, но кампания ещё не началась)",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте был orderId = 0
                    // не понятно, как такое может быть при statusBsSynced = Yes
                    endDate = TOMORROW
                    orderId = 1L
                    startDate = TOMORROW
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.startDate(TOMORROW),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1000-1032
        arrayOf(
            "Начало XX.XX.XXXX",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте был orderId = 0
                    // не понятно, как такое может быть при statusBsSynced = Yes
                    orderId = 1L
                    startDate = TOMORROW
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = BigDecimal.ZERO
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.startDate(TOMORROW),
        ),
    )

    fun endedStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1067-1099
        arrayOf(
            "Кампания закончилась XX.XX.XXXX (на кампании есть деньги, дата окончания наступила)",
            createContainer(
                campaign = createCampaign {
                    endDate = YESTERDAY
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.ended(YESTERDAY),
        ),
    )

    fun dayBudgetEndedStatuses() = listOf(
        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1634-1665
        arrayOf(
            "Показы приостановлены по дневному ограничению в XX:XX",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте ещё фигурирует поле spent_today, но оно в расчётах не используется
                    dayBudget = 10.toBigDecimal()
                    dayBudgetStopTime = NOW
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 100.toBigDecimal()
                    sumSpent = 80.toBigDecimal()
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.pausedByDayBudget(NOW),
        ),

        // https://a.yandex-team.ru/arc_vcs/direct/perl/unit_tests/Campaign/get_camp_status_info.t?rev=r4927484#L1666-1674
        arrayOf(
            "Показы приостановлены по дневному ограничению общего счёта в XX:XX (ограничение наступило сейчас)",
            createContainer(
                campaign = createCampaign {
                    // в оригинальном тесте ещё фигурирует поле spent_today, но оно в расчётах не используется
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 1234.toBigDecimal()
                    walletId = 2L
                },
                hasBanners = true,
                hasActiveBanners = true,
                aggregatedStatusWallet = AggregatedStatusWallet().apply {
                    id = 2L
                    sum = 1000.toBigDecimal()
                    autoOverdraftAddition = 300.toBigDecimal()
                    status = WalletStatus().withBudgetLimitationStopTime(NOW)
                },
                wallet = Campaign()
                    .withId(2L)
                    .withSum(1000.toBigDecimal())
            ),
            translations.pausedByWalletDayBudget(NOW),
        ),
    )

    fun timeTargetStatuses() = listOf(
        arrayOf(
            "Показы начнутся XX.XX в XX:XX",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                    timeTarget = NO_IMPRESSIONS_TIME_TARGET
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.impressionsWillBeginLaterThanThisWeek(
                startDateTime = TODAY
                    .atTime(12, 0)
                    .atZone(DateTimeUtils.MSK)
                    .plusDays(30)
                    .withZoneSameInstant(AMSTERDAM_TIMEZONE.timezone)
                    .toOffsetDateTime(),
                geoTimezone = AMSTERDAM_TIMEZONE,
            ),
        ),

        arrayOf(
            "Показы начнутся завтра в XX:XX",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                    timeTarget = TOMORROW_AT_NOON_ONLY_TIME_TARGET
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.impressionsWillBeginTomorrow(
                startDateTime = TOMORROW
                    .atTime(12, 0)
                    .atZone(AMSTERDAM_TIMEZONE.timezone)
                    .toOffsetDateTime(),
                geoTimezone = AMSTERDAM_TIMEZONE,
            ),
        ),

        arrayOf(
            "Показы начнутся on ${YESTERDAY.dayOfWeek.name} в XX:XX",
            createContainer(
                campaign = createCampaign {
                    statusActive = true
                    statusBsSynced = CampaignStatusBsSynced.YES
                    statusModerate = CampaignStatusModerate.YES
                    statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
                    statusShow = true
                    sum = 10.toBigDecimal()
                    sumSpent = "9.999999".toBigDecimal()
                    timeTarget = YESTERDAY_AT_NOON_ONLY_TIME_TARGET
                },
                hasBanners = true,
                hasActiveBanners = true,
            ),
            translations.impressionsWillBeginThisWeek(
                startDateTime = YESTERDAY
                    .atTime(12, 0)
                    .atZone(AMSTERDAM_TIMEZONE.timezone)
                    .toOffsetDateTime(),
                geoTimezone = AMSTERDAM_TIMEZONE,
            ),
        ),
    )

    @Test
    @Parameters(
        method = "convertedStatuses," +
            "archivedStatuses," +
            "inProgressStatuses," +
            "stoppedStatuses," +
            "fundsRunOutStatuses," +
            "awaitingPaymentStatuses," +
            "draftStatuses," +
            "awaitingModerationStatuses," +
            "acceptedOnModerationStatuses," +
            "rejectedOnModerationStatuses," +
            "noAdsStatuses," +
            "noActiveBannersStatuses," +
            "startDateStatuses," +
            "endedStatuses," +
            "dayBudgetEndedStatuses," +
            "timeTargetStatuses"
    )
    @TestCaseName("{0}")
    fun test(
        @Suppress("UNUSED_PARAMETER") name: String,
        container: GetCampaignsContainer,
        expectedClarification: Translatable,
    ) {
        val actualClarification = campaignStatusCalculator
            .calculateStatusClarification(container)

        assertThat(translationService.translate(actualClarification))
            .isEqualTo(translationService.translate(expectedClarification))
    }

    private fun createContainer(
        campaign: CommonCampaign,
        geoTimezone: GeoTimezone = AMSTERDAM_TIMEZONE,
        aggregatedStatusWallet: AggregatedStatusWallet? = null,
        wallet: WalletCampaign? = null,
        queueOperations: Set<CampQueueOperationName> = emptySet(),
        hasBanners: Boolean = false,
        hasActiveBanners: Boolean = false,
    ): GetCampaignsContainer =
        GetCampaignsContainer(
            campaign = campaign,
            requestedFields = setOf(CampaignAnyFieldEnum.STATUS_CLARIFICATION),
            clientUid = 0L,
            ndsRatioSupplier = ::unsupported,
            sumForTransferSupplier = ::unsupported,
            managerFioSupplier = ::unsupported,
            agencyNameSupplier = ::unsupported,
            timezoneSupplier = { geoTimezone },
            aggregatedStatusWalletSupplier = { aggregatedStatusWallet },
            walletSupplier = { wallet },
            queueOperationsSupplier = { queueOperations },
            hasBannersSupplier = { hasBanners },
            hasActiveBannersSupplier = { hasActiveBanners },
            advancedGeoTargetingSupplier = { true },
        )

    private fun createCampaign(init: TextCampaign.() -> Unit): TextCampaign =
        defaultCampaign().apply(init)

    private fun defaultCampaign(): TextCampaign =
        TextCampaign().apply {
            id = CAMPAIGN_ID
            currency = CurrencyCode.RUB
            currencyConverted = false
            dayBudget = BigDecimal.ZERO
            dayBudgetDailyChangeCount = 0
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            dayBudgetStopTime = null
            endDate = null
            orderId = 110L
            startDate = YESTERDAY
            statusActive = false
            statusArchived = false
            statusBsSynced = CampaignStatusBsSynced.YES
            statusModerate = CampaignStatusModerate.YES
            statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
            statusShow = true
            stopTime = null
            strategy = TestCampaignsStrategy.defaultStrategy()
            sum = BigDecimal.ZERO
            sumSpent = BigDecimal.ZERO
            sumToPay = BigDecimal.ZERO
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
        }

    private fun unsupported(): Nothing =
        error("Значение отсутствует, так как это поле не используется при расчёте StatusClarification")

    private fun concat(vararg translatables: Translatable): Translatable =
        ConcatTranslatable(". ", translatables.asList())

    private companion object {
        @JvmField
        @ClassRule
        val springClassRule = SpringClassRule()

        private val CAMPAIGN_ID = 1L

        // Wed May 18 16:58:59 MSK 2022
        private val CLOCK = Clock.fixed(Instant.ofEpochSecond(1652882339), DateTimeUtils.MSK)
        private val NOW = LocalDateTime.now(CLOCK)
        private val TODAY = NOW.toLocalDate()
        private val YESTERDAY = TODAY.minusDays(1)
        private val TOMORROW = TODAY.plusDays(1)
        private val DEFAULT_STOP_TIME = NOW
            .minus(CampaignSumAvailableForTransferCalculator.TRANSFER_DELAY_AFTER_STOP)
            .minusMinutes(10)

        private val AMSTERDAM_TIMEZONE = GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L)
            .withGroupType(GroupType.WORLD)

        private val NO_IMPRESSIONS_TIME_TARGET = TimeTargetUtils
            .defaultTimeTarget()
            .apply {
                val coefMap = enumValues<WeekdayType>().associateWith { HoursCoef() }
                setWeekdayCoefMap(coefMap)
            }

        // кампания будет показываться завтра в 12:00 в таймзоне рекламодателя
        private val TOMORROW_AT_NOON_ONLY_TIME_TARGET = NO_IMPRESSIONS_TIME_TARGET
            .copy()
            .apply {
                val tomorrowWeekday = WeekdayType.getById(TOMORROW.dayOfWeek.value)
                val coef = HoursCoef().apply { setCoef(12, 100) }
                setWeekdayCoef(tomorrowWeekday, coef)
            }

        // кампания могла бы показываться вчера в 12:00 в таймзоне рекламодателя
        private val YESTERDAY_AT_NOON_ONLY_TIME_TARGET = NO_IMPRESSIONS_TIME_TARGET
            .copy()
            .apply {
                val yesterdayWeekday = WeekdayType.getById(YESTERDAY.dayOfWeek.value)
                val coef = HoursCoef().apply { setCoef(12, 100) }
                setWeekdayCoef(yesterdayWeekday, coef)
            }
    }
}

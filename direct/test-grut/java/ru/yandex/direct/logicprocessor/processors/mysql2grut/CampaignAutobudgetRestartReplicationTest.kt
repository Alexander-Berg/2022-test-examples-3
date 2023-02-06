package ru.yandex.direct.logicprocessor.processors.mysql2grut

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.autobudget.restart.service.Reason
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.GRUT_CAMP_REPL_CALC_AUTOBUDGET_RESTART_PERCENT
import ru.yandex.direct.common.db.PpcPropertyNames.GRUT_USE_MYSQL_TABLE_FOR_AUTOBUDGET_RESTART_REPLICATION
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.grut.api.AutoBudgetRestartData
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.averageBidStrategy
import ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.Tables.CAMP_AUTOBUDGET_RESTART
import ru.yandex.direct.dbschema.ppc.tables.Campaigns
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.utils.DateTimeUtils
import ru.yandex.grut.objects.proto.CampaignV2.TCampaignV2Spec.TAutoBudgetRestart

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignAutobudgetRestartReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientInfo.client!!, listOf())))
        ppcPropertiesSupport.set(GRUT_USE_MYSQL_TABLE_FOR_AUTOBUDGET_RESTART_REPLICATION, "false")
        ppcPropertiesSupport.set(GRUT_CAMP_REPL_CALC_AUTOBUDGET_RESTART_PERCENT, "100")
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    @Test
    fun testInitAutoBudgetRestart() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)


        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.SUM, BigDecimal.valueOf(2000))
            .set(CAMPAIGNS.SUM_SPENT, BigDecimal.valueOf(0))
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val now = Instant.now().epochSecond.toInt()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_INIT.number)
        softAssertions.assertThat(autoBudgetRestart.hasMoney).isEqualTo(true)
        softAssertions.assertThat(autoBudgetRestart.hasStopTime()).isFalse
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertThat(autoBudgetRestart.softRestartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertAll()
    }

    @Test
    fun initAutoBudgetRestart_NoMoneyTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)


        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.SUM, BigDecimal.valueOf(2000))
            .set(CAMPAIGNS.SUM_SPENT, BigDecimal.valueOf(2000))
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val now = Instant.now().epochSecond.toInt()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_INIT.number)
        softAssertions.assertThat(autoBudgetRestart.hasMoney).isEqualTo(false)
        softAssertions.assertThat(autoBudgetRestart.hasStopTime()).isTrue
        softAssertions.assertThat(autoBudgetRestart.stopTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertThat(autoBudgetRestart.softRestartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertAll()
    }

    /**
     * Тест проверяет, что если на кошельке появился дневной бюджет, то будет рестарт
     */
    @Test
    fun initAutoBudgetRestart_WalletWithDayBudgetTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(3000))
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(CAMP_AUTOBUDGET_RESTART, CAMP_AUTOBUDGET_RESTART.CID, CAMP_AUTOBUDGET_RESTART.RESTART_REASON, CAMP_AUTOBUDGET_RESTART.RESTART_TIME, CAMP_AUTOBUDGET_RESTART.SOFT_RESTART_TIME, CAMP_AUTOBUDGET_RESTART.STOP_TIME, CAMP_AUTOBUDGET_RESTART.STRATEGY_DATA)
            .values(
                campaignInfo.campaignId,
                "INIT",
                LocalDateTime.of(2022, 3, 10, 0, 0),
                LocalDateTime.of(2022, 3, 10, 0, 0),
                null,
                """
                    {"avg_bid": null, "avg_cpa": null, "avg_cpm": null, "avg_cpv": null, "goal_id": null, "platform": null, "roi_coef": null, "strategy": "default", "has_money": false, "day_budget": null, "start_time": [2019, 2, 12], "finish_time": null, "status_show": true, "strategy_id": null, "time_target": null, "limit_clicks": null, "strategy_start": null, "auto_budget_sum": null, "enable_cpc_hold": false, "manual_strategy": null, "strategy_finish": null, "has_combined_goals": false, "pay_for_conversion": null}
                """.trimIndent()
            )
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val now = Instant.now().epochSecond.toInt()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_DAY_BUDGET_START.number)
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertAll()
    }

    /**
     * В тесте проверяется, что если стратегия сильно изменилась относительно той, которая в таблице mysql, то в GRuT запишется полный рестарт
     * Изменение в данном случае - включение enable_cpc_hold
     */
    @Test
    fun getInfoFromMysqlTable_FullRestartTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 3, 9))
            .withStrategy(manualStrategy()
                .withDayBudget(DayBudget()
                    .withDayBudget(BigDecimal.valueOf(0))
                    .withShowMode(DayBudgetShowMode.DEFAULT)
                    .withDailyChangeCount(0L)
                    .withStopNotificationSent(false))

                .withPlatform(CampaignsPlatform.BOTH))

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        // выключаем cpc_hold
        val campaignOpts = "enable_cpc_hold"
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .set(CAMPAIGNS.TIME_TARGET, "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789")
            .where(Campaigns.CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(CAMP_AUTOBUDGET_RESTART, CAMP_AUTOBUDGET_RESTART.CID, CAMP_AUTOBUDGET_RESTART.RESTART_REASON, CAMP_AUTOBUDGET_RESTART.RESTART_TIME, CAMP_AUTOBUDGET_RESTART.SOFT_RESTART_TIME, CAMP_AUTOBUDGET_RESTART.STOP_TIME, CAMP_AUTOBUDGET_RESTART.STRATEGY_DATA)
            .values(
                campaignInfo.campaignId,
                "INIT",
                LocalDateTime.of(2022, 3, 10, 0, 0),
                LocalDateTime.of(2022, 3, 10, 0, 0),
                null,
                """
                    {"avg_bid": null, "avg_cpa": null, "avg_cpm": null, "avg_cpv": null, "goal_id": null, "platform": "both", "roi_coef": null, "strategy": "default", "has_money": true, "day_budget": "300.00", "start_time": [2022, 3, 9], "finish_time": null, "status_show": true, "time_target": "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789", "limit_clicks": null, "strategy_start": null, "auto_budget_sum": null, "enable_cpc_hold": false, "manual_strategy": null, "strategy_finish": null, "has_combined_goals": false, "pay_for_conversion": false}
                """.trimIndent()
            )
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val now = Instant.now().epochSecond.toInt()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_CPC_HOLD_ENABLED.number)
        softAssertions.assertThat(autoBudgetRestart.hasMoney).isEqualTo(true)
        softAssertions.assertThat(autoBudgetRestart.hasStopTime()).isFalse
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertThat(autoBudgetRestart.softRestartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertAll()
    }

    @Test
    fun getInfoFromMysqlTable_NoRestartTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 3, 9))
            .withStrategy(manualStrategy()
                .withDayBudget(DayBudget()
                    .withDayBudget(BigDecimal.valueOf(300))
                    .withShowMode(DayBudgetShowMode.DEFAULT)
                    .withDailyChangeCount(0L)
                    .withStopNotificationSent(false))

                .withPlatform(CampaignsPlatform.BOTH))

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val campaignOpts = ""
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .set(CAMPAIGNS.TIME_TARGET, "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789")
            .where(Campaigns.CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        val restartTime = LocalDateTime.of(2022, 3, 10, 3, 0)
        val restartTimeSec = DateTimeUtils.moscowDateTimeToEpochSecond(restartTime).toInt()
        val softRestartTime = LocalDateTime.of(2022, 3, 10, 5, 0)
        val softRestartTimeSec = DateTimeUtils.moscowDateTimeToEpochSecond(softRestartTime).toInt()
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(CAMP_AUTOBUDGET_RESTART, CAMP_AUTOBUDGET_RESTART.CID, CAMP_AUTOBUDGET_RESTART.RESTART_REASON, CAMP_AUTOBUDGET_RESTART.RESTART_TIME, CAMP_AUTOBUDGET_RESTART.SOFT_RESTART_TIME, CAMP_AUTOBUDGET_RESTART.STOP_TIME, CAMP_AUTOBUDGET_RESTART.STRATEGY_DATA)
            .values(
                campaignInfo.campaignId,
                "INIT",
                restartTime,
                softRestartTime,
                null,
                """
                    {"avg_bid": null, "avg_cpa": null, "avg_cpm": null, "avg_cpv": null, "goal_id": null, "platform": "both", "roi_coef": null, "strategy": "default", "has_money": true, "day_budget": "300.00", "start_time": [2022, 3, 9], "finish_time": null, "status_show": true, "time_target": "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789", "limit_clicks": null, "strategy_start": null, "auto_budget_sum": null, "enable_cpc_hold": false, "manual_strategy": null, "strategy_finish": null, "has_combined_goals": false, "pay_for_conversion": false}
                """.trimIndent()
            )
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_INIT.number)
        softAssertions.assertThat(autoBudgetRestart.hasMoney).isEqualTo(true)
        softAssertions.assertThat(autoBudgetRestart.hasStopTime()).isFalse
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(restartTimeSec + 1000).isGreaterThan(restartTimeSec - 1000)
        softAssertions.assertThat(autoBudgetRestart.softRestartTime).isLessThan(softRestartTimeSec + 1000).isGreaterThan(softRestartTimeSec - 1000)
        softAssertions.assertAll()
    }

    /**
     * В тесте проверяется, что если стратегия сильно изменилась относительно той, которая в Grut, то,
     * во-первых, будет взята предыдущая стратегия именно из GRuT, а не из mysql
     * во-вторых, будет пересчитаны времена рестартс
     *
     * Изменение в данном случае - включение enable_cpc_hold
     */
    @Test
    @Disabled("В данный момент старое состояние кампании берется из таблицы camp_autobudget_restart всегда")
    fun getInfoFromGrut_FullRestartTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 3, 9))
            .withOrderId(0)
            .withStrategy(manualStrategy()
                .withDayBudget(DayBudget()
                    .withDayBudget(BigDecimal.valueOf(300))
                    .withShowMode(DayBudgetShowMode.DEFAULT)
                    .withDailyChangeCount(0L)
                    .withStopNotificationSent(false))
                .withPlatform(CampaignsPlatform.BOTH))

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        var campaignOpts = ""
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .set(CAMPAIGNS.TIME_TARGET, "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789")
            .where(Campaigns.CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        val mysqlCampaign = campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        // делаем создание кампании в GRuT со старым значением cpc_hold
        val autoBudgetRestartData = AutoBudgetRestartData(
            restartReason = Reason.INIT,
            restartTime = LocalDateTime.of(2022, 3, 8, 11, 24),
            softRestartTime = LocalDateTime.of(2022, 3, 8, 15, 11),
            hasMoney = true,
            stopTime = null
        )
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, 9, autoBudgetRestart = autoBudgetRestartData))

        // выключаем cpc_hold
        campaignOpts = "enable_cpc_hold"
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .where(Campaigns.CAMPAIGNS.CID.eq(campaign.id))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )
        // при этом в таблице camp_autobudget_restart уже обновилось значение cpc_hold, в процессоре оно должно проигнорироваться
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(CAMP_AUTOBUDGET_RESTART, CAMP_AUTOBUDGET_RESTART.CID, CAMP_AUTOBUDGET_RESTART.RESTART_REASON, CAMP_AUTOBUDGET_RESTART.RESTART_TIME, CAMP_AUTOBUDGET_RESTART.SOFT_RESTART_TIME, CAMP_AUTOBUDGET_RESTART.STOP_TIME, CAMP_AUTOBUDGET_RESTART.STRATEGY_DATA)
            .values(
                campaignInfo.campaignId,
                "INIT",
                LocalDateTime.of(2022, 3, 10, 0, 0),
                LocalDateTime.of(2022, 3, 10, 0, 0),
                null,
                """
                    {"avg_bid": null, "avg_cpa": null, "avg_cpm": null, "avg_cpv": null, "goal_id": null, "platform": "both", "roi_coef": null, "strategy": "default", "has_money": true, "day_budget": "300.00", "start_time": [2022, 3, 9], "finish_time": null, "status_show": true, "time_target": "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789", "limit_clicks": null, "strategy_start": null, "auto_budget_sum": null, "enable_cpc_hold": true, "manual_strategy": null, "strategy_finish": null, "has_combined_goals": false, "pay_for_conversion": false}
                """.trimIndent()
            )
            .execute()


        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasAutobudgetRestart()).isTrue
        val softAssertions = SoftAssertions()
        val now = Instant.now().epochSecond.toInt()
        val autoBudgetRestart = createdCampaign.spec.autobudgetRestart
        softAssertions.assertThat(autoBudgetRestart.restartReason).isEqualTo(TAutoBudgetRestart.ERestartReason.RR_CPC_HOLD_ENABLED.number)
        softAssertions.assertThat(autoBudgetRestart.hasMoney).isEqualTo(true)
        softAssertions.assertThat(autoBudgetRestart.hasStopTime()).isFalse
        softAssertions.assertThat(autoBudgetRestart.restartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertThat(autoBudgetRestart.softRestartTime).isLessThan(now + 1000).isGreaterThan(now - 1000)
        softAssertions.assertAll()
    }

    /**
     * Тест проверяет, что если в стратегии есть значения с точностью больше 6 знака, то не будет ArithmeticExpection
     */
    @Test
    @Disabled("В данный момент старое состояние кампании берется из таблицы camp_autobudget_restart всегда")
    fun getInfoFromGrutScaleMoreThan6_FullRestartTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 3, 9))
            .withOrderId(0)
            .withStrategy(averageBidStrategy()
                .withAverageBid(BigDecimal.valueOf(3.8135593220339))
                .withDayBudget(DayBudget()
                    .withDayBudget(BigDecimal.valueOf(300))
                    .withShowMode(DayBudgetShowMode.DEFAULT)
                    .withDailyChangeCount(0L)
                    .withStopNotificationSent(false)
                )
                .withUnknownFields(mapOf("name" to "autobudget_avg_click"))
            )

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.TIME_TARGET, "1HIJKLMNOPQ2HIJKLMNOPQ3HIJKLMNOPQ4HIJKLMNOPQ5HIJKLMNOPQ6789")
            .set(CAMPAIGNS.PLATFORM, ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform.search)
            .where(Campaigns.CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        val mysqlCampaign = campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign

        val autoBudgetRestartData = AutoBudgetRestartData(
            restartReason = Reason.INIT,
            restartTime = LocalDateTime.of(2022, 3, 8, 11, 24),
            softRestartTime = LocalDateTime.of(2022, 3, 8, 15, 11),
            hasMoney = true,
            stopTime = null
        )
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, 9, autoBudgetRestart = autoBudgetRestartData))

        processor.withShard(campaignInfo.shard)
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
            )
        }.doesNotThrowAnyException()
    }
}

package ru.yandex.direct.core.autobudget.restart.service

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.autobudget.restart.model.CampStrategyRestartData
import ru.yandex.direct.autobudget.restart.model.StrategyDto
import ru.yandex.direct.autobudget.restart.service.AutobudgetRestartService
import ru.yandex.direct.autobudget.restart.service.Reason
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import java.time.LocalDate

@RunWith(SpringRunner::class)
@CoreTest
open class AutobudgetRestartServiceTest {
    @Autowired
    lateinit var service: AutobudgetRestartService

    @Autowired
    lateinit var steps: Steps

    lateinit var camp: CampaignInfo

    @Test
    fun moveStartTimeToFuture() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val shard = clientInfo.shard
        camp = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo)
        val today = LocalDate.now()
        service.calculateRestartsAndSave(
            shard,
            listOf(
                CampStrategyRestartData(
                    cid = camp.campaignId,
                    strategyDto = StrategyDto(
                        strategy = "autobudget",
                        startTime = today.minusDays(3),
                        statusShow = false,
                    )
                )
            )
        )

        service.calculateRestartsAndSave(
            shard,
            listOf(
                CampStrategyRestartData(
                    cid = camp.campaignId,
                    strategyDto = StrategyDto(
                        strategy = "autobudget",
                        autoBudgetSum = "12".toBigDecimal(),
                        startTime = today.minusDays(3),
                        statusShow = false,
                    )
                )
            )
        )

        val future = today.plusDays(3)
        val retList = service.calculateRestartsAndSave(
            shard,
            listOf(
                CampStrategyRestartData(
                    cid = camp.campaignId,
                    strategyDto = StrategyDto(
                        strategy = "autobudget",
                        autoBudgetSum = "12".toBigDecimal(),
                        startTime = future,
                        statusShow = false,
                    )
                )
            )
        )

        val soft = SoftAssertions()
        soft.assertThat(retList).hasSize(1)
        val ret = retList[0].restartResult
        soft.assertThat(ret.cid).isEqualTo(camp.campaignId)
        soft.assertThat(ret.restartReason).isEqualTo(Reason.CHANGED_START_TIME_TO_FUTURE.name)
        soft.assertThat(ret.restartTime).isEqualTo(future.atStartOfDay())
        soft.assertThat(ret.softRestartTime).isEqualTo(future.atStartOfDay())
        soft.assertAll()
    }
}

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.autobudget.restart.model.StrategyDto
import ru.yandex.direct.autobudget.restart.repository.CampaignAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.CampRestartData
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.autobudget.restart.service.StrategyState
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps

@RunWith(SpringRunner::class)
@CoreTest
open class CampaignAutobudgetRestartRepositoryTest {
    @Autowired
    lateinit var repo: CampaignAutobudgetRestartRepository

    @Autowired
    lateinit var steps: Steps

    lateinit var camp: CampaignInfo

    @Before
    fun setUp() {
        camp = steps.campaignSteps().createDefaultCampaign()

        repo.saveAutobudgetRestartData(
            camp.shard, listOf(
            defaultRestartDbData(camp.campaignId)
        )
        )

        assertThat(repo.getAutobudgetRestartData(camp.shard, listOf(camp.campaignId)))
            .hasSize(1)
    }

    @Test
    fun repositoryCanUpdateStoptime() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        repo.saveAutobudgetRestartData(
            camp.shard, listOf(
            defaultRestartDbData(camp.campaignId).copy(state = StrategyState(stopTime = now))
        )
        )

        val data = repo.getAutobudgetRestartData(camp.shard, listOf(camp.campaignId))
        assertThat(data).hasSize(1)
        assertThat(data[0].state.stopTime).isEqualTo(now)
    }

    @Test
    fun longBigDecimalWorksWell() {
        val template = defaultRestartDbData(camp.campaignId)
        val original = template.copy(
            strategyData = template.strategyData.copy(dayBudget = "99999999999999.99".toBigDecimal())
        )
        repo.saveAutobudgetRestartData(camp.shard, listOf(original))

        val result = repo.getAutobudgetRestartData(camp.shard, listOf(camp.campaignId))

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(original)
    }

    private fun defaultRestartDbData(cid: Long) =
        CampRestartData(
            cid = cid,
            times = RestartTimes(
                restartTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                softRestartTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                restartReason = ""
            ),
            strategyData = StrategyDto(
                strategy = "default", platform = "both", manualStrategy = "different_places",
                startTime = LocalDate.parse("2020-01-01"), statusShow = true,
            ),
            state = StrategyState(stopTime = null)
        )
}

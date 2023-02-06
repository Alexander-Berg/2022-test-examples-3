package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetAggregatedHourlyProblem
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblem
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.ADD_DYNAMIC_AD_TARGETS
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.ADD_KEYWORDS
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.ADD_PERFORMANCE_FILTERS
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.DISABLE_BROAD_MATCH_LIMIT
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.DISABLE_CONTEXT_LIMIT
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.ENABLE_BROAD_MATCH
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.ENABLE_CONTEXT
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.INCREASE_EFFICIENCY
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemRecommendation.INCREASE_MAX_BID
import ru.yandex.direct.grid.model.campaign.GdAutobudgetProblemType
import ru.yandex.direct.grid.model.campaign.GdiCampaign
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.service.campaign.converter.AutobudgetProblemConverter

@GridProcessingTest
class CampaignAutobudgetProblemsConverterTest {

    @Test
    fun nullProblem() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), null)

        assertThat(gdAutobudgetProblem).isNull()
    }

    @Test
    fun noProblem() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), AutobudgetAggregatedHourlyProblem.NO_PROBLEM)

        assertThat(gdAutobudgetProblem).isNull()
    }

    @Test
    fun maxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED)

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        DISABLE_CONTEXT_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun upperPositionsReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), AutobudgetAggregatedHourlyProblem.UPPER_POSITIONS_REACHED)

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.UPPER_POSITIONS_REACHED)
                .withRecommendations(
                    listOf(
                        DISABLE_CONTEXT_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun engineMinCostLimited() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), AutobudgetAggregatedHourlyProblem.ENGINE_MIN_COST_LIMITED)

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.ENGINE_MIN_COST_LIMITED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        DISABLE_CONTEXT_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun walletDayBudgetReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(createCampaign(), AutobudgetAggregatedHourlyProblem.WALLET_DAY_BUDGET_REACHED)

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.WALLET_DAY_BUDGET_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        DISABLE_CONTEXT_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun dynamicMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(type = CampaignType.DYNAMIC),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        ADD_DYNAMIC_AD_TARGETS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun smartMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(type = CampaignType.PERFORMANCE),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        ADD_PERFORMANCE_FILTERS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun searchMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(platform = CampaignsPlatform.SEARCH),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        ENABLE_CONTEXT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun contextLimitedMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(contextLimit = 0),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        DISABLE_CONTEXT_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun noBroadMatchMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(contextLimit = 100, hasBroadMatch = false),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        ENABLE_BROAD_MATCH,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun broadMatchLimitedMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(contextLimit = 100, hasBroadMatch = true, broadMatchLimit = 50),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        DISABLE_BROAD_MATCH_LIMIT,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    @Test
    fun broadMatchNonLimitedMaxBidReached() {
        val gdAutobudgetProblem = AutobudgetProblemConverter
            .toGdAutobudgetProblem(
                createCampaign(contextLimit = 100, hasBroadMatch = true, broadMatchLimit = 0),
                AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED
            )

        assertThat(gdAutobudgetProblem).isEqualTo(
            GdAutobudgetProblem()
                .withProblem(GdAutobudgetProblemType.MAX_BID_REACHED)
                .withRecommendations(
                    listOf(
                        INCREASE_MAX_BID,
                        ADD_KEYWORDS,
                        INCREASE_EFFICIENCY
                    )
                )
        )
    }

    private fun createCampaign(
        type: CampaignType = CampaignType.TEXT,
        platform: CampaignsPlatform = CampaignsPlatform.BOTH,
        contextLimit: Int = 0,
        hasBroadMatch: Boolean = false,
        broadMatchLimit: Int = 0,
    ) = GdiCampaign()
        .withType(type)
        .withPlatform(platform)
        .withContextLimit(contextLimit)
        .withHasBroadMatch(hasBroadMatch)
        .withBroadMatchLimit(broadMatchLimit)

}

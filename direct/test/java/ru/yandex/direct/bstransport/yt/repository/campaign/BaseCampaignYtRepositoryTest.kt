package ru.yandex.direct.bstransport.yt.repository.campaign

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.StringList
import ru.yandex.adv.direct.UInt32List
import ru.yandex.adv.direct.UInt64List
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.campaign.MeaningfulGoal
import ru.yandex.adv.direct.campaign.MeaningfulGoalList
import ru.yandex.adv.direct.campaign.OptionalMultipliers
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.multipliers.Multiplier
import ru.yandex.adv.direct.showcondition.RfOptions

internal class BaseCampaignYtRepositoryTest {
    private val campaignYtRepository = mock<CampaignYtRepository> {
        on(it.schemaWithMapping).thenCallRealMethod()
        on(it.getNonKeysColumnSchema()).thenCallRealMethod()
    }

    private val campaignDeleteYtRepository = mock<CampaignDeleteYtRepository> {
        on(it.schemaWithMapping).thenCallRealMethod()
        on(it.getNonKeysColumnSchema()).thenCallRealMethod()
    }

    @Test
    fun getSchemaWithMappingTest() {
        val gotColumnNameToValue = campaignYtRepository.schemaWithMapping
            .associateBy({ it.columnSchema.name }) {
                it.fromProtoToYtMapper(CAMPAIGN)
            }

        val expectedColumnNameToValue = mapOf(
            "OrderID" to 12L,
            "ExportID" to 123L,
            "ClientID" to 15L,
            "Type" to "text",
            "Archive" to false,
            "Stop" to true,
            "CampaignName" to "Имя кампании",
            "CampaignLatName" to "Imya_campanii",
            "AllowedOnAdultContent" to true,
            "MeaningfulGoals" to MEANINGFUL_GOALS_LIST,
            "MeaningfulGoalsHash" to 123L,
            "MinusPhrases" to listOf("минус1", "минус2"),
            "Multipliers" to OPTIONAL_MULTIPLIERS,
            "ShowConditions" to SHOW_CONDITIONS,
            "MobileAppIds" to MOBILE_APP_IDS,
            "DisallowedTargetTypes" to listOf(1, 2, 3),
            "RotationGoalId" to 10L,
            "RfOptions" to RF_OPTIONS,
            "ClientChiefRepLogin" to "Login",
            "ContextID" to 1234567890L,
            "WidgetPartnerId" to 9876543210L,
            "MetrikaCounterIds" to METRIKA_COUNTER_IDS,
            "Source" to 1L,
            "Metatype" to 0L,
            "StrategyId" to 1L,
            "IsWwManagedOrder" to false,
            "IsCpmGlobalAbSegment" to false,
        )

        assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue)
    }

    @Test
    fun getSchemaWithMappingForDeletedTimeTest() {
        val gotColumnNameToValue = campaignDeleteYtRepository.schemaWithMapping
            .associateBy({ it.columnSchema.name }) {
                it.fromProtoToYtMapper(CAMPAIGN)
            }
        val expectedColumnNameToValue = mapOf(
            "OrderID" to 12L,
            "ExportID" to 123L,
            "DeleteTime" to 12343245L
        )
        assertThat(gotColumnNameToValue).isEqualTo(expectedColumnNameToValue)
    }

    companion object {
        const val GOAL_ID = 1999123

        val MOBILE_APP_IDS = UInt32List.newBuilder().addValues(1).build()

        val MEANINGFUL_GOALS_LIST = MeaningfulGoalList.newBuilder()
            .addMeaningfulGoal(
                MeaningfulGoal.newBuilder()
                    .setValue(3000000)
                    .setGoalId(GOAL_ID.toLong())
            )
            .build()

        private val OPTIONAL_MULTIPLIERS = OptionalMultipliers.newBuilder()
            .addMultiplier(Multiplier.newBuilder().setValue(42))
            .build()

        private val SHOW_CONDITIONS = TargetingExpression.newBuilder().run {
            addAndBuilder().run {
                addOrBuilder().run {
                    value = "1"
                    keyword = 2
                    operation = 3
                }
            }
            build()
        }

        private val RF_OPTIONS = RfOptions.newBuilder().run {
            maxShowsCount = 321
            maxShowsPeriod = 77
            stopShowsPeriod = 77
            build()
        }

        private val METRIKA_COUNTER_IDS = UInt64List.newBuilder().addValues(1).build()

        private val CAMPAIGN = Campaign.newBuilder().run {
            orderId = 12L
            exportId = 123L
            iterId = 1234L
            updateTime = 2423434235L
            clientId = 15L
            type = "text"
            archive = false
            stop = true
            name = "Имя кампании"
            latName = "Imya_campanii"
            allowedOnAdultContent = true
            meaningfulGoals = MEANINGFUL_GOALS_LIST
            meaningfulGoalsHash = 123L
            minusPhrases = StringList.newBuilder().addAllValues(listOf("минус1", "минус2")).build()
            multipliers = OPTIONAL_MULTIPLIERS
            mobileAppIds = MOBILE_APP_IDS
            disallowedTargetTypes = UInt32List.newBuilder().addAllValues(listOf(1, 2, 3)).build()
            rotationGoalId = 10L
            rfOptions = RF_OPTIONS
            showConditions = SHOW_CONDITIONS
            clientChiefRepLogin = "Login"
            deleteTime = 12343245L
            contextId = 1234567890L
            widgetPartnerId = 9876543210L
            metrikaCounterIds = METRIKA_COUNTER_IDS
            source = 1
            metatype = 0
            strategyId = 1
            isWwManagedOrder = false
            isCpmGlobalAbSegment = false
            build()
        }
    }
}

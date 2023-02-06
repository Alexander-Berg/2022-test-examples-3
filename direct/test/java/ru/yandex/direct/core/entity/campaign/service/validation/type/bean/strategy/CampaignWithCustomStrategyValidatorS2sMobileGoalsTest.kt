package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.model.OsType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith
import ru.yandex.direct.validation.result.Defect
import java.math.BigDecimal
import java.util.function.Function
import java.util.function.Supplier

@RunWith(JUnitParamsRunner::class)
class CampaignWithCustomStrategyValidatorS2sMobileGoalsTest {
    companion object {

        data class TestData(
            val description: String,
            val campaignGoals: Set<Goal>,
            val mobileContentFunction: Function<Set<Long>, List<MobileContent>>,
            val expectedDefect: Defect<Void>?
        ) {
            override fun toString(): String = description

            val meaningfulGoals = meaningfulGoals(campaignGoals)
        }

        val osMobileContent1 = MobileContent().withOsType(OsType.IOS).withId(1)
        val osMobileContent2 = MobileContent().withOsType(OsType.IOS).withId(2)
        val androidMobileContent1 = MobileContent().withOsType(OsType.ANDROID).withId(3)
        val androidMobileContent2 = MobileContent().withOsType(OsType.ANDROID).withId(4)

        fun mobileGoal(): Goal {
            val goal = Goal()
            goal.withId(1).withIsMobileGoal(true).withMobileAppId(1)
            return goal
        }

        fun notMobileGoal(): Goal {
            val goal = Goal()
            goal.withId(2)
            return goal
        }

        fun meaningfulGoals(goals: Set<Goal>) =
            goals.map { MeaningfulGoal().withGoalId(it.id).withConversionValue(BigDecimal.ONE) }
    }

    private val getCampaignBannersSupplier = Supplier<List<BannerWithSystemFields>> { emptyList() }
    private val campaignAdGroupsSupplier = Supplier<List<AdGroupSimple>> { emptyList() }
    private val getBannersSiteLinkSetsFunction =
        Function<List<BannerWithSystemFields?>, List<SitelinkSet>> { banners: List<BannerWithSystemFields?>? -> emptyList() }

    private val campaign =
        TestCampaigns.defaultTextCampaign().withStrategy(TestCampaigns.defaultAutobudgetRoiStrategy(1))

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    fun testParameters() =
        listOf(
            listOf(
                TestData(
                    "Нет ошибок - если по 1-му мобильному приложению на каждую тип ОС",
                    setOf(mobileGoal()),
                    { appIds -> listOf(osMobileContent1, androidMobileContent1) },
                    null
                )
            ),
            listOf(
                TestData(
                    "Нет ошибок - если по 1-му уникальному мобильному приложению на каждую тип ОС",
                    setOf(mobileGoal()),
                    { appIds -> listOf(osMobileContent1, androidMobileContent1, osMobileContent1) },
                    null
                )
            ),
            listOf(
                TestData(
                    "INCORRECT_SET_OF_MOBILE_GOALS - если больше 1-го мобильного приложения на IOS",
                    setOf(mobileGoal()),
                    { appIds -> listOf(osMobileContent1, osMobileContent2, androidMobileContent1) },
                    StrategyDefects.incorrectSetOfMobileGoals()
                )
            ),
            listOf(
                TestData(
                    "INCORRECT_SET_OF_MOBILE_GOALS - если больше 1-го мобильного приложения на Android",
                    setOf(mobileGoal()),
                    { appIds -> listOf(osMobileContent1, androidMobileContent2, androidMobileContent1) },
                    StrategyDefects.incorrectSetOfMobileGoals()
                )
            ),
            listOf(
                TestData(
                    "Нет ошибок - по 1-му уникальному мобильному приложению на android",
                    setOf(mobileGoal()),
                    { appIds -> listOf(osMobileContent1, androidMobileContent1, androidMobileContent1) },
                    null
                )
            ),
            listOf(
                TestData(
                    "Нет ошибок - нет мобильных целей",
                    setOf(notMobileGoal()),
                    { appIds -> listOf(osMobileContent1, androidMobileContent1, androidMobileContent1) },
                    null
                )
            ),
            listOf(
                TestData(
                    "Нет ошибок - если нет мобильного контента",
                    setOf(notMobileGoal()),
                    { appIds -> listOf() },
                    null
                )
            )
        )

    @Test
    @Parameters(method = "testParameters")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        if (testData.meaningfulGoals.isNotEmpty()) {
            campaign.withMeaningfulGoals(testData.meaningfulGoals)
            campaign.strategy.strategyData.withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
        }
        val vr = CampaignWithCustomStrategyValidator(
            currency,
            testData.campaignGoals,
            getCampaignBannersSupplier,
            campaignAdGroupsSupplier,
            getBannersSiteLinkSetsFunction,
            campaign,
            StrategyName.values().toSet(),
            CampOptionsStrategy.values().toSet(),
            CampaignsPlatform.values().toSet(),
            CommonStrategyValidatorConstants(currency),
            emptySet(),
            CampaignValidationContainer.create(0, 0L, ClientId.fromLong(0L)),
            null,
            false,
            testData.mobileContentFunction,
            { _ -> null },
            false,
            null
        )
        if (testData.expectedDefect == null) {
            vr.apply(campaign).check(Matchers.hasNoDefectsDefinitions())
        } else {
            vr.apply(campaign).check(hasDefectWithDefinition(matchesWith(testData.expectedDefect)))
        }
    }
}

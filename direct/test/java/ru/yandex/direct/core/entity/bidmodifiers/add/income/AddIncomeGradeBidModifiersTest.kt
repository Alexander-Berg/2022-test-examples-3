package ru.yandex.direct.core.entity.bidmodifiers.add.income

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpression
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpressionAdjustment
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIncomeGradeAdjustment
import ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIncomeGradeAdjustments
import ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyIncomeGradeModifier
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.checkContainsInAnyOrder
import ru.yandex.direct.test.utils.checkSize

@CoreTest
@RunWith(JUnitParamsRunner::class)
class AddIncomeGradeBidModifiersTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        data class TestData(val bidModifiers: List<BidModifierExpression>, val level: BidModifierLevel, val description: String) {
            override fun toString() = description
        }
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo

    private lateinit var adGroupInfo: AdGroupInfo

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, true)
    }

    fun testData() =
        listOf(
            listOf(TestData(oneIncomeGradeBidModifier(), BidModifierLevel.ADGROUP, "One income grade modifier for ad group")),
            listOf(TestData(twoIncomeGradeBidModifier(), BidModifierLevel.ADGROUP, "Income grad modifier with complex condition for campaign")),

            //Создается отедльно на кампанию
            listOf(TestData(oneIncomeGradeBidModifier(), BidModifierLevel.CAMPAIGN, "One income grade modifier for ad group")),
            listOf(TestData(twoIncomeGradeBidModifier(), BidModifierLevel.CAMPAIGN, "Income grad modifier with complex condition for campaign")),
        )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("Проверяем запись и чтение корректировки: {0}")
    fun addAndGetBidModifiersTest(data: TestData) {
        val clientId = clientInfo.clientId!!
        val operator = clientInfo.uid
        data.bidModifiers.forEach {
            if (data.level == BidModifierLevel.ADGROUP) {
                it.withAdGroupId(adGroupInfo.adGroupId)
            } else {
                it.withCampaignId(adGroupInfo.campaignId)
            }
        }

        val result = bidModifierService.add(data.bidModifiers, clientId, operator)
        val cids = listOf(adGroupInfo.campaignId)
        val types = data.bidModifiers.map { it.type }.toSet()
        val allLevels = BidModifierLevel.values().toSet()
        val addedModifiers = bidModifierService.getByCampaignIds(clientId, cids, types, allLevels, operator)
        checkResult(data, result, addedModifiers as List<BidModifierExpression>)
    }

    @Test
    @TestCaseName("Проверяем что не создается корректировка на несоответствующий тип кампании")
    fun addModifierToUnsupportedCampaignType_failure() {
        val clientId = clientInfo.clientId!!
        val operator = clientInfo.uid
        val campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo)
        val modifier = oneIncomeGradeBidModifier().map { it.withCampaignId(campaignInfo.campaignId) }
        val result = bidModifierService.add(modifier, clientId, operator)

        result.validationResult.flattenErrors().map { it.defect }.checkContainsInAnyOrder(BidModifiersDefects.notSupportedMultiplier())
    }

    @Test
    @TestCaseName("Проверяем что не создается корректировка на несоответствующий тип группы")
    fun addModifierToUnsupportedAdGroupType_failure() {
        val clientId = clientInfo.clientId!!
        val operator = clientInfo.uid
        val campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo)
        val modifier = oneIncomeGradeBidModifier().map { it.withAdGroupId(adGroupInfo.adGroupId) }
        val result = bidModifierService.add(modifier, clientId, operator)

        result.validationResult.flattenErrors().map { it.defect }.checkContainsInAnyOrder(BidModifiersDefects.notSupportedMultiplier())
    }

    @Test
    @TestCaseName("Проверяем что не создается корректировка при выключенной фиче")
    fun addModifierIfFeatureDisabled_failure() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val clientId = clientInfo.clientId!!
        val operator = clientInfo.uid
        val campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo)
        val modifier = oneIncomeGradeBidModifier().map { it.withCampaignId(campaignInfo.campaignId) }
        val result = bidModifierService.add(modifier, clientId, operator)

        result.validationResult.flattenErrors().map { it.defect }.checkContainsInAnyOrder(BidModifiersDefects.notSupportedMultiplier())
    }

    private fun checkResult(data: TestData, result: MassResult<List<Long>>, addedModifiers: List<BidModifierExpression>) {
        val adjustmentById = addedModifiers
            .flatMap { it.expressionAdjustments.map { adj -> BidModifierService.getExternalId(adj.id, it.type) to adj } }
            .toMap()

        val modifierById = addedModifiers.map { it.id to it }.toMap()

        val resultContents = result.result.flatMap { it.result }
        assertThat(result.validationResult.hasAnyErrors(), `is`(false))
        resultContents.checkSize(adjustmentById.keys.size)
        assertThat(resultContents, containsInAnyOrder(*adjustmentById.keys.toTypedArray()))

        assertThat(data.bidModifiers, containsInAnyOrder(*addedModifiers.map { beanDiffer(it) }.toTypedArray()))

        data.bidModifiers.map { it to modifierById.getValue(it.id) }
            .forEach {
                val expected = it.first.expressionAdjustments
                val actual = it.second.expressionAdjustments
                assertThat(actual, containsInAnyOrder(*expected.map { beanDiffer(it) }.toTypedArray()))
            }
    }

    private fun beanDiffer(expected: BidModifierExpression) =
        beanDifferExceptFields(expected, "lastChange", "expressionAdjustments")

    private fun beanDiffer(expected: BidModifierExpressionAdjustment) =
        beanDifferExceptFields(expected, "lastChange")

    private fun <T> beanDifferExceptFields(expected: T, vararg excepts: String) =
        beanDiffer(expected).useCompareStrategy(
            DefaultCompareStrategies.allFieldsExcept(*excepts.map { BeanFieldPath.newPath(it) }.toTypedArray())
        )!!

    private fun oneIncomeGradeBidModifier(): List<BidModifierExpression> {
        val modifier =
            createEmptyIncomeGradeModifier()
                .withExpressionAdjustments(createDefaultIncomeGradeAdjustments())
        return listOf(modifier)
    }

    private fun twoIncomeGradeBidModifier(): List<BidModifierExpression> {
        val condition = listOf(
            listOf(
                BidModifierExpressionLiteral()
                    .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                    .withOperation(BidModifierExpressionOperator.EQ)
                    .withValueString("0"),
                BidModifierExpressionLiteral()
                    .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                    .withOperation(BidModifierExpressionOperator.EQ)
                    .withValueString("1"),
            )
        )
        val modifier =
            createEmptyIncomeGradeModifier()
                .withExpressionAdjustments(
                    listOf(createDefaultIncomeGradeAdjustment().withCondition(condition))
                )
        return listOf(modifier)
    }

}

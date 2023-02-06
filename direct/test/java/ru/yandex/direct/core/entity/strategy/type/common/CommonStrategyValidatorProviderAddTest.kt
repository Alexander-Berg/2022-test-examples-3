package ru.yandex.direct.core.entity.strategy.type.common

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CLICK
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.StringDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class CommonStrategyValidatorProviderAddTest {

    companion object {

        private val strategyValidName = "simpleName"

        data class TestData(
            val strategyType: StrategyName = AUTOBUDGET_AVG_CPA,
            val campaignType: CampaignType = TEXT,
            val clientId: Long? = null,
            val name: String = strategyValidName,
            val walletId: Long? = null,
            val isDifferentPlacesEnabled: Boolean? = false,
            val attributionModel: StrategyAttributionModel? = LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE,
            val cids: List<Long>? = listOf(),
            val isPublic: Boolean = true,
            val expectedPreValidationDefects: List<DefectWithField> = listOf(),
            val expectedValidationDefects: List<DefectWithField> = listOf()
        )

        data class DefectWithField(val defect: Defect<*>, val field: ModelProperty<*, *>?)

        @JvmStatic
        fun testData(): List<List<TestData>> =
            listOf(
                TestData(),
                TestData(
                    clientId = 123,
                    expectedPreValidationDefects = listOf(
                        DefectWithField(
                            CommonDefects.isNull(),
                            CommonStrategy.CLIENT_ID
                        )
                    )
                ),
                TestData(
                    walletId = 124,
                    expectedPreValidationDefects = listOf(
                        DefectWithField(
                            CommonDefects.isNull(),
                            CommonStrategy.WALLET_ID
                        )
                    )
                ),
                TestData(
                    cids = listOf(1, 2),
                    isPublic = false,
                    expectedPreValidationDefects = listOf(
                        DefectWithField(
                            CollectionDefects.maxCollectionSize(1),
                            CommonStrategy.CIDS
                        )
                    ),
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            StrategyDefects.linkingNonPublicStrategyToSeveralCampaigns(),
                            CommonStrategy.IS_PUBLIC
                        )
                    )
                ),
                TestData(
                    attributionModel = null,
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            CommonDefects.notNull(),
                            CommonStrategy.ATTRIBUTION_MODEL
                        )
                    )
                ),
                TestData(isDifferentPlacesEnabled = true),
                TestData(
                    name = "",
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            StringDefects.notEmptyString(),
                            CommonStrategy.NAME
                        )
                    )
                ),
                TestData(
                    name = "", attributionModel = null,
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            StringDefects.notEmptyString(),
                            CommonStrategy.NAME
                        ), DefectWithField(CommonDefects.notNull(), CommonStrategy.ATTRIBUTION_MODEL)
                    )
                ),
                TestData(cids = listOf(1)),
                TestData(cids = listOf(1, 2)),
                TestData(cids = listOf(1), isPublic = false),
                TestData(
                    strategyType = AUTOBUDGET_AVG_CLICK,
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            StrategyDefects.inconsistentStrategyToStrategyType(),
                            null
                        )
                    )
                ),
                TestData(
                    cids = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L),
                    expectedValidationDefects = listOf(
                        DefectWithField(
                            StrategyDefects.tooMuchCampaignsLinkedToStrategy(10),
                            CommonStrategy.CIDS
                        )
                    )
                ),
            ).map { listOf(it) }
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testData")
    fun testPreValidate(testData: TestData) {
        val strategy = autobudgetAvgCpa()
            .withClientId(testData.clientId)
            .withName(testData.name)
            .withWalletId(testData.walletId)
            .withIsPublic(testData.isPublic)
            .withCids(testData.cids)
            .withAttributionModel(testData.attributionModel)
        val container = createContainer(testData)
        val validator = CommonStrategyValidatorProvider.createAddStrategyPreValidator(container)

        val vr = validator.apply(strategy)
        if (testData.expectedPreValidationDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedPreValidationDefects.forEach {
                vr.check(matcher(it))
            }
        }
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testData")
    fun testValidate(testData: TestData) {
        val strategy = autobudgetAvgCpa()
            .withType(testData.strategyType)
            .withClientId(testData.clientId)
            .withName(testData.name)
            .withWalletId(testData.walletId)
            .withAttributionModel(testData.attributionModel)
            .withCids(testData.cids)
            .withIsPublic(testData.isPublic)
        val container = createContainer(testData)
        val validator = CommonStrategyValidatorProvider.createAddStrategyValidator(container)

        val vr = validator.apply(strategy)
        if (testData.expectedValidationDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedValidationDefects.forEach {
                vr.check(matcher(it))
            }
        }
    }

    private fun matcher(defect: DefectWithField) = Matchers.hasDefectDefinitionWith<CommonStrategy>(
        Matchers.validationError(
            defect.field?.let { PathHelper.path(PathHelper.field(it)) } ?: PathHelper.emptyPath(),
            defect.defect
        )
    )

    private fun createContainer(testData: TestData): StrategyAddOperationContainer {
        val container = Mockito.mock(StrategyAddOperationContainer::class.java)
        whenever(container.campaignType(anyOrNull())).thenReturn(testData.campaignType)
        whenever(container.wallet).thenReturn(Campaign())
        whenever(container.campaigns(anyOrNull())).thenReturn(testData.cids?.map {
            TextCampaign().withType(testData.campaignType).withId(it)
        })
        whenever(container.options).thenReturn(StrategyOperationOptions(maxNumberOfCids = 10))
        return container
    }
}

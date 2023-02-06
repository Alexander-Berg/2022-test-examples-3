package ru.yandex.direct.core.entity.strategy.type.common

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.container.StrategyUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class CommonStrategyValidatorProviderUpdateTest {

    companion object {

        private const val strategyValidName = "simpleName"
        private const val strategyId = 1L

        data class TestData(
            val isDifferentPlacesEnabled: Boolean?,
            val strategyType: StrategyName = AUTOBUDGET_AVG_CPA,
            val name: String? = strategyValidName,
            val clientId: Long? = null,
            val campaignType: CampaignType = TEXT,
            val walletId: Long? = null,
            val attributionModel: StrategyAttributionModel? = LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE,
            val cids: List<Long>? = listOf(),
            val isPublic: Boolean = false,
            val currentIsPublicValue: Boolean = false,
            val statusArchived: Boolean = false,
            val expectedPreValidationDefects: List<DefectWithField> = listOf(),
            val expectedBeforeApplyValidationDefects: List<DefectWithField> = listOf(),
            val expectedValidationDefects: List<DefectWithField> = listOf()
        )

        data class DefectWithField(val defect: Defect<*>, val field: ModelProperty<*, *>?)

        @JvmStatic
        fun testData(): List<List<TestData>> =
            listOf(
                listOf(
                    TestData(false),
                    TestData(
                        false,
                        clientId = 123L,
                        expectedPreValidationDefects = listOf(
                            DefectWithField(
                                forbiddenToChange(),
                                CommonStrategy.CLIENT_ID
                            )
                        )
                    ),
                    TestData(
                        false,
                        walletId = 123L,
                        expectedPreValidationDefects = listOf(
                            DefectWithField(
                                forbiddenToChange(),
                                CommonStrategy.WALLET_ID
                            )
                        )
                    ),
                    TestData(
                        false,
                        attributionModel = null,
                        expectedValidationDefects = listOf(
                            DefectWithField(
                                CommonDefects.notNull(),
                                CommonStrategy.ATTRIBUTION_MODEL
                            )
                        )
                    ),
                    TestData(true),
                    TestData(
                        true,
                        isPublic = false,
                        currentIsPublicValue = false,
                        cids = listOf(1, 2),
                        expectedValidationDefects = listOf(
                            DefectWithField(
                                CollectionDefects.maxCollectionSize(1),
                                CommonStrategy.CIDS
                            )
                        ),
                        expectedBeforeApplyValidationDefects = listOf(
                            DefectWithField(
                                StrategyDefects.linkingNonPublicStrategyToSeveralCampaigns(),
                                CommonStrategy.IS_PUBLIC
                            )
                        )
                    ),

                    TestData(
                        true,
                        isPublic = false,
                        currentIsPublicValue = true,
                        cids = listOf(1, 2),
                        expectedValidationDefects = listOf(
                            DefectWithField(
                                CollectionDefects.maxCollectionSize(1),
                                CommonStrategy.CIDS
                            )
                        ),
                        expectedBeforeApplyValidationDefects = listOf(
                            DefectWithField(
                                StrategyDefects.linkingNonPublicStrategyToSeveralCampaigns(),
                                CommonStrategy.IS_PUBLIC
                            )
                        )
                    ),
                    TestData(
                        false,
                        isPublic = true,
                        cids = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L),
                        expectedValidationDefects = listOf(
                            DefectWithField(
                                StrategyDefects.tooMuchCampaignsLinkedToStrategy(10),
                                CommonStrategy.CIDS
                            )
                        )
                    ),
                ),
                listOfBeforeApplyValidationTestData()
            )
                .flatten()
                .map { listOf(it) }

        @JvmStatic
        fun listOfBeforeApplyValidationTestData(): List<TestData> =
            listOf(
                beforeApplyTestData(listOf(1), true, false, listOf()),
                beforeApplyTestData(listOf(1), true, true, listOf()),
                beforeApplyTestData(listOf(1, 2), true, false, listOf()),
                beforeApplyTestData(listOf(1, 2), true, true, listOf()),
                beforeApplyTestData(listOf(1), false, false, listOf()),
                beforeApplyTestData(
                    listOf(1), false, true, listOf(
                        DefectWithField(StrategyDefects.changePublicStrategyToPrivate(), CommonStrategy.IS_PUBLIC)
                    )
                ),
            )

        private fun beforeApplyTestData(
            cids: List<Long>?,
            isPublic: Boolean,
            currentIsPublicValue: Boolean,
            expectedBeforeApplyValidationDefects: List<DefectWithField>
        ): TestData =
            TestData(
                true,
                cids = cids,
                isPublic = isPublic,
                currentIsPublicValue = currentIsPublicValue,
                expectedBeforeApplyValidationDefects = expectedBeforeApplyValidationDefects
            )
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testData")
    fun testPreValidate(testData: TestData) {
        val modelChanges = ModelChanges(strategyId, CommonStrategy::class.java)
            .processNotNull(testData.clientId, CommonStrategy.CLIENT_ID)
            .processNotNull(testData.walletId, CommonStrategy.WALLET_ID)
            .process(testData.name, CommonStrategy.NAME)
            .process(testData.attributionModel, CommonStrategy.ATTRIBUTION_MODEL)

        val container = createContainer(testData)
        val validator = CommonStrategyValidatorProvider.createUpdateStrategyPreValidator(container)

        val vr = validator.apply(modelChanges)
        if (testData.expectedPreValidationDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedPreValidationDefects.forEach {
                vr.check(preValidateMatcher(it))
            }
        }
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testData")
    fun testValidateBeforeApply(testData: TestData) {
        val strategy = autobudgetAvgCpa()
            .withIsPublic(testData.currentIsPublicValue)

        val modelChanges = ModelChanges(strategyId, CommonStrategy::class.java)
            .processNotNull(testData.clientId, CommonStrategy.CLIENT_ID)
            .process(testData.name, CommonStrategy.NAME)
            .processNotNull(testData.walletId, CommonStrategy.WALLET_ID)
            .process(testData.attributionModel, CommonStrategy.ATTRIBUTION_MODEL)
            .process(testData.cids, CommonStrategy.CIDS)
            .process(testData.isPublic, CommonStrategy.IS_PUBLIC)

        val container = createContainer(testData)
        val validator = CommonStrategyValidatorProvider.createUpdateStrategyBeforeApplyValidator(
            container,
            CommonStrategyUpdateOperationContainer(mapOf(strategyId to strategy))
        )

        val vr = validator.apply(modelChanges)
        if (testData.expectedBeforeApplyValidationDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedBeforeApplyValidationDefects.forEach {
                vr.check(validateBeforeApplyMatcher(it))
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
            .withStatusArchived(testData.statusArchived)
            .withIsPublic(testData.isPublic)
            .withCids(testData.cids)
        val container = createContainer(testData)
        val validator = CommonStrategyValidatorProvider.createUpdateStrategyValidator(container)

        val vr = validator.apply(strategy)
        if (testData.expectedValidationDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedValidationDefects.forEach {
                vr.check(validateMatcher(it))
            }
        }
    }

    private fun validateMatcher(defect: DefectWithField) = Matchers.hasDefectDefinitionWith<CommonStrategy>(
        Matchers.validationError(
            PathHelper.path(PathHelper.field(defect.field)),
            defect.defect
        )
    )

    private fun preValidateMatcher(defect: DefectWithField) =
        Matchers.hasDefectDefinitionWith<ModelChanges<CommonStrategy>>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(defect.field)),
                defect.defect
            )
        )

    private fun validateBeforeApplyMatcher(defect: DefectWithField) =
        Matchers.hasDefectDefinitionWith<ModelChanges<CommonStrategy>>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(defect.field)),
                defect.defect
            )
        )

    private fun createContainer(testData: TestData): StrategyUpdateOperationContainer {
        val container = Mockito.mock(StrategyUpdateOperationContainer::class.java)
        whenever(container.campaignType(anyOrNull())).thenReturn(testData.campaignType)
        whenever(container.campaignsById(anyOrNull())).thenReturn(testData.cids?.map {
            TextCampaign().withType(testData.campaignType).withId(it)
        })
        whenever(container.options).thenReturn(StrategyOperationOptions(maxNumberOfCids = 10))
        whenever(container.campaignTypeById(anyOrNull())).thenReturn(testData.campaignType)
        whenever(container.campaigns(any())).thenReturn(testData.cids?.map {
            TextCampaign().withType(testData.campaignType).withId(it)
        } ?: emptyList())
        return container
    }
}

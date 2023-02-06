package ru.yandex.direct.core.entity.strategy.type.defaultmanualstrategy

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.container.StrategyUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class DefaultManualStrategyValidatorProviderTest {

    fun validationTestData(): List<List<Any?>> = listOf(
        listOf(CampaignType.TEXT, RbacRole.CLIENT, true, null),
        listOf(
            CampaignType.TEXT,
            RbacRole.CLIENT,
            null,
            DefectWithField(notNull(), DefaultManualStrategy.ENABLE_CPC_HOLD)
        )
    )

    fun preValidationTestData(): List<List<Any?>> = listOf(
        listOf(CampaignType.TEXT, RbacRole.CLIENT, true, false, null),
        listOf(CampaignType.TEXT, RbacRole.CLIENT, null, false, null),
        listOf(
            CampaignType.CPM_BANNER,
            RbacRole.CLIENT,
            false,
            false,
            DefectWithField(isNull(), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(CampaignType.TEXT, RbacRole.CLIENT, null, false, null),
    )

    fun enableCpcHoldChangesPreValidationTestData(): List<List<Any?>> = listOf(
        listOf(null, null, null, null),
        listOf(CampaignType.TEXT, null, null, null),
        listOf(CampaignType.TEXT, null, false, null),
        listOf(CampaignType.TEXT, null, true, null),
        listOf(CampaignType.TEXT, false, false, null),
        listOf(CampaignType.TEXT, false, true, null),
        listOf(CampaignType.TEXT, true, false, null),
        listOf(CampaignType.TEXT, true, true, null),
        listOf(CampaignType.PERFORMANCE, null, null, null),
        listOf(
            CampaignType.PERFORMANCE,
            false,
            false,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(
            CampaignType.PERFORMANCE,
            null,
            true,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(
            CampaignType.PERFORMANCE,
            null,
            true,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(
            CampaignType.PERFORMANCE,
            false,
            true,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(
            CampaignType.PERFORMANCE,
            true,
            false,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
        listOf(
            CampaignType.PERFORMANCE,
            true,
            true,
            DefectWithField(Defect(DefectIds.FORBIDDEN_TO_CHANGE), DefaultManualStrategy.ENABLE_CPC_HOLD)
        ),
    )

    @Test
    @Parameters(method = "validationTestData")
    @TestCaseName("campaignType={0},rbacRole={2},platform={3},cpcHoldEnabled={4}")
    fun `validation is correct`(
        campaignType: CampaignType?,
        rbacRole: RbacRole,
        isCpcHoldEnabled: Boolean?,
        defect: DefectWithField?
    ) {
        val container = mockAddContainer(campaignType, rbacRole)
        val validator = DefaultManualStrategyValidatorProvider.createAddStrategyValidator(container)
        val strategy = clientDefaultManualStrategy()
            .withEnableCpcHold(isCpcHoldEnabled)

        val validationResult = validator.apply(strategy)

        val matcher: Matcher<ValidationResult<DefaultManualStrategy, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<DefaultManualStrategy>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.field(defect.field)),
                    defect.defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }

    @Test
    @Parameters(method = "preValidationTestData")
    @TestCaseName("campaignType={0},rbacRole={2},platform={3},cpcHoldEnabled={4}")
    fun `preValidation is correct`(
        campaignType: CampaignType?,
        rbacRole: RbacRole,
        isCpcHoldEnabled: Boolean?,
        isCopy: Boolean,
        defect: DefectWithField?
    ) {
        val container = mockAddContainer(campaignType, rbacRole, isCopy)
        val validator = DefaultManualStrategyValidatorProvider.createAddStrategyPreValidator(container)
        val strategy = clientDefaultManualStrategy()
            .withEnableCpcHold(isCpcHoldEnabled)

        val validationResult = validator.apply(strategy)

        val matcher: Matcher<ValidationResult<DefaultManualStrategy, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<DefaultManualStrategy>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.field(defect.field)),
                    defect.defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }


    @Test
    @Parameters(method = "enableCpcHoldChangesPreValidationTestData")
    @TestCaseName("campaignType={0},initialEnableCpcHold={1},enableCpcHold={2}")
    fun `enable cpc hold changes preValidation is correct`(
        campaignType: CampaignType?,
        initialEnableCpcHold: Boolean?,
        enableCpcHold: Boolean?,
        defect: DefectWithField?
    ) {
        val container = mock<StrategyUpdateOperationContainer>()
        whenever(container.campaignTypeById(anyOrNull())).thenReturn(campaignType)
        val validator = DefaultManualStrategyValidatorProvider.createUpdateStrategyPreValidator(container)
        val strategy = clientDefaultManualStrategy()
            .withEnableCpcHold(initialEnableCpcHold)
            .withId(1L)

        val changes = ModelChanges(strategy.id, DefaultManualStrategy::class.java)
            .process(enableCpcHold, DefaultManualStrategy.ENABLE_CPC_HOLD)

        val validationResult = validator.apply(changes)

        val matcher: Matcher<ValidationResult<ModelChanges<DefaultManualStrategy>, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<ModelChanges<DefaultManualStrategy>>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.field(defect.field)),
                    defect.defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }

    data class DefectWithField(val defect: Defect<*>, val field: ModelProperty<*, *>)

    private fun mockAddContainer(
        campaignType: CampaignType?,
        rbacRole: RbacRole,
        isCopy: Boolean = false
    ): StrategyAddOperationContainer {
        val m = mock<StrategyAddOperationContainer>()
        whenever(m.campaignType(anyOrNull())).thenReturn(campaignType)
        whenever(m.operatorRole).thenReturn(rbacRole)
        whenever(m.options).thenReturn(StrategyOperationOptions(isCopy = isCopy))

        return m
    }
}

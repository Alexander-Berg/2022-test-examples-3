package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.core.param.model.BooleanParamValue
import ru.yandex.market.core.param.model.ParamType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class ApiDropshipBySellerStepCalculatorFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testNone() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockPrepayRequest(PartnerApplicationStatus.NEW)
        mockContractOptions()
        mockPartnerParams(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_PI_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.API_PI_SETTINGS).status(WizardStepStatus.NONE)),
            result.steps
        )
    }

    @Test
    fun testEmpty() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(listOf())

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_PI_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.API_PI_SETTINGS).status(WizardStepStatus.EMPTY)),
            result.steps
        )
    }

    @Test
    fun testFull() {
        mockPartnerInfo(PartnerPlacementType.DBS)
        mockPrepayRequest(PartnerApplicationStatus.COMPLETED)
        mockContractOptions()
        mockPartnerParams(
            listOf(
                BooleanParamValue(ParamType.PARTNER_SETTINGS_INPUT_CONFIRM, PARTNER_ID, true),
                BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PARTNER_ID, true)
            )
        )

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.API_PI_SETTINGS)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(WizardStepInfo().step(WizardStepType.API_PI_SETTINGS).status(WizardStepStatus.FULL)),
            result.steps
        )
    }
}

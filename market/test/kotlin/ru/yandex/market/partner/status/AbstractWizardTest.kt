package ru.yandex.market.partner.status

import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.partner.status.wizard.model.WizardStepInfo
import ru.yandex.market.partner.status.wizard.model.WizardStepStatus
import ru.yandex.market.partner.status.wizard.model.WizardStepType
import ru.yandex.market.partner.status.wizard.service.WizardService

abstract class AbstractWizardTest : AbstractFunctionalTest() {

    @Autowired
    protected lateinit var wizardService: WizardService

    protected fun assertWizardStatus(
        result: List<WizardStepInfo>,
        neededStepType: WizardStepType,
        neededStatus: WizardStepStatus
    ) {
        val neededStep = result.find { it.step == neededStepType }

        assertThat(neededStep).isNotNull

        if (neededStep?.status == WizardStepStatus.INTERNAL_ERROR) {
            println("Error on step $neededStepType: ${neededStep.details?.get("error")}")
        }

        assertThat(neededStep?.status).isEqualTo(neededStatus)
    }
}

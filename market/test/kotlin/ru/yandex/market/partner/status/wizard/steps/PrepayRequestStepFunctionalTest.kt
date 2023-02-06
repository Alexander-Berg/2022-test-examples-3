package ru.yandex.market.partner.status.wizard.steps

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType

class PrepayRequestStepFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testPrepayRequestDetails() {
        mockPartnerInfo(PartnerPlacementType.FBS)
        mockPrepayRequest(PartnerApplicationStatus.IN_PROGRESS)
        mockContractOptions(true)
        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(listOf(1L)))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(1L)
                        .isDefaultCurrentMonthFrequency(false)
                        .currentMonthFrequency(PayoutFrequencyDTO.DAILY)
                        .nextMonthFrequency(PayoutFrequencyDTO.DAILY)
                )
            )
        Mockito.`when`(mbiApiClient.getPartnerSuperAdmin(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                BusinessOwnerDTO(10, 100, "login", setOf("a@ya.ru"))
            )
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.PAYMENTS_REQUEST)
            .schedule()
            .join()
        Assertions.assertNotNull(result)

        val step = result.steps?.get(0)!!

        assertThat(step.status).isEqualTo(WizardStepStatus.ENABLING)

        assertThat(step.details).isEqualTo(
            mapOf(
                "currentContractId" to 10000.0,
                "isClickAndCollect" to false,
                "isCpaPartnerInterface" to true,
                "partnerPlacementType" to "FBS",
                "contracts" to listOf(
                    mapOf(
                        "contractId" to 10000.0,
                        "contractEid" to "Contract1",
                        "currentMonthFrequency" to "DAILY",
                        "defaultCurrentMonthFrequency" to false,
                        "jurName" to "Jur name",
                        "nextMonthFrequency" to null,
                        "organizationType" to "OOO",
                        "partners" to listOf(
                            mapOf(
                                "name" to "Partner 1",
                                "partnerId" to 1.0
                            )
                        )
                    )
                ),
                "partnerApplication" to mapOf(
                    "name" to "Partner 1",
                    "contactInfo" to mapOf(
                        "firstName" to "First Name",
                        "secondName" to "Middle Name",
                        "lastName" to "Last Name",
                        "name" to "Contact person",
                        "phoneNumber" to "+7 987 987 87 87",
                        "email" to "contact@email.ru",
                        "shopAddress" to "Shop address",
                        "shopPhoneNumber" to "+7 098 098 98 98"
                    ),
                    "organizationInfo" to mapOf(
                        "accountNumber" to "AccountNumber",
                        "autoFilled" to false,
                        "bankName" to "Bank 1",
                        "bik" to "456789",
                        "corrAccountNumber" to "CorrAccountNumber",
                        "factAddress" to "factAddress",
                        "inn" to "123456",
                        "juridicalAddress" to "juridicalAddress",
                        "kpp" to "kpp",
                        "licenseDate" to "2020-01-01",
                        "licenseNumber" to "123",
                        "name" to "Organization name",
                        "ogrn" to "12345",
                        "postcode" to "123456",
                        "type" to "OOO",
                        "workSchedule" to "Work schedule"
                    ),
                    "returnContacts" to listOf(
                        mapOf(
                            "firstName" to "Return First Name",
                            "secondName" to "Return Second Name",
                            "lastName" to "Return Last Name",
                            "email" to "return@email.ru",
                            "address" to "return address",
                            "type" to "PERSON",
                            "phoneNumber" to "+7 123 123 23 23",
                            "jobPosition" to "Job position",
                            "comments" to "Return comments",
                            "datasourceId" to 1.0,
                            "enabled" to true
                        )
                    ),
                    "shopVat" to mapOf(
                        "vatRate" to "VAT_18",
                        "deliveryVatRate" to "VAT_10",
                        "vatSource" to "WEB",
                        "taxSystem" to "ENVD",
                        "datasourceId" to 1.0
                    )
                ),
                "payoutFrequencyEnabled" to true
            )
        )
    }
}

package ru.yandex.market.partner.status.wizard.check

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO
import ru.yandex.market.mbi.open.api.client.model.OrganizationType
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOption
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOptionsResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerIdName
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.wizard.model.check.payout_frequency.PayoutFrequencyType
import ru.yandex.market.partner.status.wizard.model.partner.OrderProcessingType
import ru.yandex.market.partner.status.wizard.model.partner.PartnerInfo
import ru.yandex.market.partner.status.wizard.model.partner.PartnerPlacementType

class PartnerContractOptionsCheckTest : AbstractFunctionalTest() {

    @Autowired
    protected lateinit var partnerContractOptionsCheck: PartnerContractOptionsCheck

    @BeforeEach
    fun init() {
        Mockito.clearInvocations(mbiBillingClient)
    }

    @Test
    fun testCorrectCheck() {
        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(Mockito.eq(1L)))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .currentContractId(1)
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .contractEid("eid1")
                                .jurName("Jur Name 1")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(1)
                                            .partnerName("Partner 1"),
                                        PartnerIdName()
                                            .partnerId(2)
                                            .partnerName("Partner 2"),
                                    )
                                )
                                .organizationType(OrganizationType.OOO),
                            PartnerContractOption()
                                .contractId(2)
                                .contractEid("eid2")
                                .jurName("Jur Name 2")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(3)
                                            .partnerName("Partner 3"),
                                        PartnerIdName()
                                            .partnerId(4)
                                            .partnerName("Partner 4"),
                                    )
                                )
                                .organizationType(OrganizationType.OOO),
                            PartnerContractOption()
                                .contractId(3)
                                .contractEid("eid3")
                                .jurName("Jur Name 3")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(5)
                                            .partnerName("Partner 5"),
                                        PartnerIdName()
                                            .partnerId(6)
                                            .partnerName("Partner 6"),
                                    )
                                )
                                .organizationType(OrganizationType.OOO)
                        ),
                    ),
            )

        Mockito.`when`(
            mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(
                Mockito.eq(
                    listOf(
                        1, 2, 3
                    )
                )
            )
        ).thenReturn(
            listOf(
                CurrentAndNextMonthPayoutFrequencyDTO()
                    .contractId(1)
                    .currentMonthFrequency(PayoutFrequencyDTO.DAILY)
                    .nextMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                    .isDefaultCurrentMonthFrequency(false),
                CurrentAndNextMonthPayoutFrequencyDTO()
                    .contractId(2)
                    .currentMonthFrequency(PayoutFrequencyDTO.WEEKLY)
                    .nextMonthFrequency(PayoutFrequencyDTO.BIWEEKLY)
                    .isDefaultCurrentMonthFrequency(true),
                CurrentAndNextMonthPayoutFrequencyDTO()
                    .contractId(3)
                    .currentMonthFrequency(PayoutFrequencyDTO.BIWEEKLY)
                    .nextMonthFrequency(PayoutFrequencyDTO.MONTHLY)
                    .isDefaultCurrentMonthFrequency(true)
            )
        )

        val checkResult = partnerContractOptionsCheck.check(
            PartnerInfo(
                partnerId = 1,
                orderProcessingType = OrderProcessingType.PI,
                partnerPlacementType = PartnerPlacementType.FBS,
                businessId = 2,
                partnerFFLinks = emptyList()
            )
        ).result!!

        assertThat(checkResult.currentContractId).isEqualTo(1)

        val contractOptions = checkResult.contractOptions

        val option1 = contractOptions[0]
        assertThat(option1.contractId).isEqualTo(1)
        assertThat(option1.contractEid).isEqualTo("eid1")
        assertThat(option1.jurName).isEqualTo("Jur Name 1")
        assertThat(option1.currentMonthFrequency).isEqualTo(PayoutFrequencyType.DAILY)
        assertThat(option1.nextMonthFrequency).isEqualTo(PayoutFrequencyType.WEEKLY)
        assertThat(option1.isDefaultCurrentMonthFrequency).isEqualTo(false)
        assertThat(option1.partnerIdNames[0].partnerId).isEqualTo(1)
        assertThat(option1.partnerIdNames[0].partnerName).isEqualTo("Partner 1")
        assertThat(option1.partnerIdNames[1].partnerId).isEqualTo(2)
        assertThat(option1.partnerIdNames[1].partnerName).isEqualTo("Partner 2")

        val option2 = contractOptions[1]
        assertThat(option2.contractId).isEqualTo(2)
        assertThat(option2.contractEid).isEqualTo("eid2")
        assertThat(option2.jurName).isEqualTo("Jur Name 2")
        assertThat(option2.currentMonthFrequency).isEqualTo(PayoutFrequencyType.WEEKLY)
        assertThat(option2.nextMonthFrequency).isEqualTo(PayoutFrequencyType.BI_WEEKLY)
        assertThat(option2.isDefaultCurrentMonthFrequency).isEqualTo(true)
        assertThat(option2.partnerIdNames[0].partnerId).isEqualTo(3)
        assertThat(option2.partnerIdNames[0].partnerName).isEqualTo("Partner 3")
        assertThat(option2.partnerIdNames[1].partnerId).isEqualTo(4)
        assertThat(option2.partnerIdNames[1].partnerName).isEqualTo("Partner 4")

        val option3 = contractOptions[2]
        assertThat(option3.contractId).isEqualTo(3)
        assertThat(option3.contractEid).isEqualTo("eid3")
        assertThat(option3.jurName).isEqualTo("Jur Name 3")
        assertThat(option3.currentMonthFrequency).isEqualTo(PayoutFrequencyType.BI_WEEKLY)
        assertThat(option3.nextMonthFrequency).isEqualTo(PayoutFrequencyType.MONTHLY)
        assertThat(option3.isDefaultCurrentMonthFrequency).isEqualTo(true)
        assertThat(option3.partnerIdNames[0].partnerId).isEqualTo(5)
        assertThat(option3.partnerIdNames[0].partnerName).isEqualTo("Partner 5")
        assertThat(option3.partnerIdNames[1].partnerId).isEqualTo(6)
        assertThat(option3.partnerIdNames[1].partnerName).isEqualTo("Partner 6")
    }

    @Test
    fun testFrequenciesNotFound() {
        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(Mockito.eq(1L)))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .currentContractId(1)
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(1)
                                .contractEid("eid1")
                                .jurName("Jur Name 1")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(1)
                                            .partnerName("Partner 1"),
                                        PartnerIdName()
                                            .partnerId(2)
                                            .partnerName("Partner 2"),
                                    )
                                )
                                .organizationType(OrganizationType.OOO),
                            PartnerContractOption()
                                .contractId(2)
                                .contractEid("eid2")
                                .jurName("Jur Name 2")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(3)
                                            .partnerName("Partner 3"),
                                        PartnerIdName()
                                            .partnerId(4)
                                            .partnerName("Partner 4"),
                                    )
                                )
                                .organizationType(OrganizationType.OOO)
                        ),
                    ),
            )

        Mockito.`when`(
            mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(
                Mockito.eq(
                    listOf(
                        1, 2
                    )
                )
            )
        ).thenReturn(listOf())

        val checkResult = partnerContractOptionsCheck.check(
            PartnerInfo(
                partnerId = 1,
                orderProcessingType = OrderProcessingType.PI,
                partnerPlacementType = PartnerPlacementType.FBS,
                businessId = 2,
                partnerFFLinks = emptyList()
            )
        ).result!!

        assertThat(checkResult.currentContractId).isEqualTo(1)

        val contractOptions = checkResult.contractOptions

        val option1 = contractOptions[0]
        assertThat(option1.contractId).isEqualTo(1)
        assertThat(option1.contractEid).isEqualTo("eid1")
        assertThat(option1.jurName).isEqualTo("Jur Name 1")
        assertThat(option1.currentMonthFrequency).isNull()
        assertThat(option1.nextMonthFrequency).isNull()
        assertThat(option1.isDefaultCurrentMonthFrequency).isNull()
        assertThat(option1.partnerIdNames[0].partnerId).isEqualTo(1)
        assertThat(option1.partnerIdNames[0].partnerName).isEqualTo("Partner 1")
        assertThat(option1.partnerIdNames[1].partnerId).isEqualTo(2)
        assertThat(option1.partnerIdNames[1].partnerName).isEqualTo("Partner 2")

        val option2 = contractOptions[1]
        assertThat(option2.contractId).isEqualTo(2)
        assertThat(option2.contractEid).isEqualTo("eid2")
        assertThat(option2.jurName).isEqualTo("Jur Name 2")
        assertThat(option2.currentMonthFrequency).isNull()
        assertThat(option2.nextMonthFrequency).isNull()
        assertThat(option2.isDefaultCurrentMonthFrequency).isNull()
        assertThat(option2.partnerIdNames[0].partnerId).isEqualTo(3)
        assertThat(option2.partnerIdNames[0].partnerName).isEqualTo("Partner 3")
        assertThat(option2.partnerIdNames[1].partnerId).isEqualTo(4)
        assertThat(option2.partnerIdNames[1].partnerName).isEqualTo("Partner 4")
    }

    @Test
    fun testBillingNotCalledOnEmptyMbiResponse() {
        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(Mockito.eq(1L)))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .currentContractId(null)
                    .contractOptions(listOf())
            )

        val checkResult = partnerContractOptionsCheck.check(
            PartnerInfo(
                partnerId = 1,
                orderProcessingType = OrderProcessingType.PI,
                partnerPlacementType = PartnerPlacementType.FBS,
                businessId = 2,
                partnerFFLinks = emptyList()
            )
        ).result!!

        Mockito.verify(mbiBillingClient, never()).getCurrentAndNextMonthPayoutFrequencies(Mockito.any())

        assertThat(checkResult.currentContractId).isNull()
        assertThat(checkResult.contractOptions).isEmpty()
    }
}

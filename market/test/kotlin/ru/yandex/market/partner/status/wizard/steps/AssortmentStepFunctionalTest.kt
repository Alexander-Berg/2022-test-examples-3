package ru.yandex.market.partner.status.wizard.steps

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.partner.status.wizard.model.check.feed.LastUploadedFeedInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepInfo
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepType
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class AssortmentStepFunctionalTest : WizardFunctionalTest() {

    @Test
    fun testEmpty() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockValidOfferWithStock(0)
        mockWarehouse(false)
        mockFeedInfo(null)
        mockTotalPartnerOffers(0)
        mockTotalBusinessOffers(1)

        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.ASSORTMENT)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.ASSORTMENT)
                    .status(WizardStepStatus.EMPTY)
                    .details(
                        mapOf(
                            "numberOfUnitedOffersPartner" to 0.0,
                            "numberOfUnitedOffersAvailable" to 1.0,
                            "hasValidOffer" to false,
                            "warehouseSet" to false
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testFilled() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockValidOfferWithStock(0)
        mockWarehouse(false)
        mockFeedInfo(
            LastUploadedFeedInfo(
                "file.xlsx",
                "ya.ru",
                OffsetDateTime.of(LocalDateTime.of(2022, 2, 15, 17, 39, 0), ZoneOffset.UTC), null
            )
        )
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.ASSORTMENT)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.ASSORTMENT)
                    .status(WizardStepStatus.FILLED)
                    .details(
                        mapOf(
                            "numberOfUnitedOffersPartner" to 1.0,
                            "numberOfUnitedOffersAvailable" to 0.0,
                            "hasValidOffer" to false,
                            "warehouseSet" to false,
                            "fileName" to "file.xlsx",
                            "fileUrl" to "ya.ru",
                            "fileDate" to "2022-02-15T17:39:00Z"
                        )
                    )
            ),
            result.steps
        )
    }

    @Test
    fun testFull() {
        mockPartnerInfo(
            PartnerPlacementType.FBS,
            partnerFulfillmentLinks = listOf(
                PartnerFulfillmentLinkDTO()
                    .serviceId(WAREHOUSE_ID)
                    .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
            )
        )
        mockValidOfferWithStock(1)
        mockFeedInfo(
            LastUploadedFeedInfo(
                "file.xlsx",
                "ya.ru",
                OffsetDateTime.of(LocalDateTime.of(2022, 2, 15, 17, 39, 0), ZoneOffset.UTC), null
            )
        )
        mockTotalPartnerOffers(1)
        mockTotalBusinessOffers(1)
        mockWarehouse(true)
        val result = partnerPlacementWizardApiClient.getPartnerStep(PARTNER_ID, WizardStepType.ASSORTMENT)
            .schedule()
            .join()
        Assertions.assertEquals(
            listOf(
                WizardStepInfo()
                    .step(WizardStepType.ASSORTMENT)
                    .status(WizardStepStatus.FULL)
                    .details(
                        mapOf(
                            "numberOfUnitedOffersPartner" to 1.0,
                            "numberOfUnitedOffersAvailable" to 0.0,
                            "hasValidOffer" to true,
                            "warehouseSet" to true,
                            "fileName" to "file.xlsx",
                            "fileUrl" to "ya.ru",
                            "fileDate" to "2022-02-15T17:39:00Z"
                        )
                    )
            ),
            result.steps
        )
    }
}

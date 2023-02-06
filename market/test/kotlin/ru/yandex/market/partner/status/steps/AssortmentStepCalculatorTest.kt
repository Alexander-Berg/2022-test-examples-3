package ru.yandex.market.partner.status.steps

import Market.DataCamp.DataCampContentStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import ru.yandex.market.mbi.datacamp.DataCampCommonConversions
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult
import ru.yandex.market.mbi.open.api.client.model.LastUploadedFeedInfoDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerLastUploadedFeedInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingInfoResponse
import ru.yandex.market.partner.status.AbstractWizardTest
import ru.yandex.market.partner.status.wizard.model.WizardStepStatus
import ru.yandex.market.partner.status.wizard.model.WizardStepType
import java.time.OffsetDateTime

class AssortmentStepCalculatorTest : AbstractWizardTest() {

    private val PARTNER_ID = 1L
    private val BUSINESS_ID = 10L

    @Test
    @DisplayName("Нет офферов вообще - шаг в EMPTY")
    fun testEmptyStep() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(mockFbsInfo())
        Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastUploadedFeedInfoResponse()
                    .lastUploadedFeedInfo(
                        LastUploadedFeedInfoDTO()
                            .id(123)
                            .feedTemplate("templ")
                            .name("name")
                            .url("url")
                            .uploadDateTime(OffsetDateTime.now())
                    )
            )

        Mockito.`when`(saasService.searchBusinessOffers(Mockito.any()))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(listOf())
                    .setTotalCount(0)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        val result = wizardService.getPartnerSteps(PARTNER_ID, WizardStepType.ASSORTMENT)
        assertWizardStatus(result, WizardStepType.ASSORTMENT, WizardStepStatus.EMPTY)
    }

    @Test
    @DisplayName("FBS, нет оффера со стоками - шаг в FILLED")
    fun testFbsWithoutCorrectOffer() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(mockFbsInfo())
        Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastUploadedFeedInfoResponse()
                    .lastUploadedFeedInfo(
                        LastUploadedFeedInfoDTO()
                            .id(123)
                            .feedTemplate("templ")
                            .name("name")
                            .url("url")
                            .uploadDateTime(OffsetDateTime.now())
                    )
            )

        // По общей статистике есть оффер
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(TotalOfferCountMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        // По запросу со стоками нет офферов
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(OfferWithStocksFilterMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(listOf())
                    .setTotalCount(0)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        val result = wizardService.getPartnerSteps(PARTNER_ID, WizardStepType.ASSORTMENT)
        assertWizardStatus(result, WizardStepType.ASSORTMENT, WizardStepStatus.FILLED)
    }

    @Test
    @DisplayName("FBY, нет оффера с ценой - шаг в FILLED")
    fun testFbyWithoutCorrectOffer() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(mockFbyInfo())
        Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastUploadedFeedInfoResponse()
                    .lastUploadedFeedInfo(
                        LastUploadedFeedInfoDTO()
                            .id(123)
                            .feedTemplate("templ")
                            .name("name")
                            .url("url")
                            .uploadDateTime(OffsetDateTime.now())
                    )
            )

        // По общей статистике есть оффер
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(TotalOfferCountMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        // По запросу со стоками нет офферов
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(OfferWithoutStocksFilterMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(listOf())
                    .setTotalCount(0)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        val result = wizardService.getPartnerSteps(PARTNER_ID, WizardStepType.ASSORTMENT)
        assertWizardStatus(result, WizardStepType.ASSORTMENT, WizardStepStatus.FILLED)
    }

    @Test
    @DisplayName("FBY, есть оффер с ценой - шаг в FULL")
    fun testFbyWithCorrectOffer() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(mockFbyInfo())
        Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastUploadedFeedInfoResponse()
                    .lastUploadedFeedInfo(
                        LastUploadedFeedInfoDTO()
                            .id(123)
                            .feedTemplate("templ")
                            .name("name")
                            .url("url")
                            .uploadDateTime(OffsetDateTime.now())
                    )
            )

        // По общей статистике есть оффер
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(TotalOfferCountMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        // По запросу со стоками нет офферов
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(OfferWithoutStocksFilterMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        val result = wizardService.getPartnerSteps(PARTNER_ID, WizardStepType.ASSORTMENT)
        assertWizardStatus(result, WizardStepType.ASSORTMENT, WizardStepStatus.FULL)
    }

    @Test
    @DisplayName("FBS, есть корректный оффер со стоками - шаг в FULL")
    fun testFbsWithCorrectOffer() {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(mockFbsInfo())
        Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastUploadedFeedInfoResponse()
                    .lastUploadedFeedInfo(
                        LastUploadedFeedInfoDTO()
                            .id(123)
                            .feedTemplate("templ")
                            .name("name")
                            .url("url")
                            .uploadDateTime(OffsetDateTime.now())
                    )
            )

        // По общей статистике есть оффер
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(TotalOfferCountMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        // По запросу со стоками нет офферов
        Mockito.`when`(saasService.searchBusinessOffers(Mockito.argThat(OfferWithStocksFilterMatcher())))
            .thenReturn(
                SaasSearchResult.builder()
                    .setOffers(
                        listOf(
                            SaasOfferInfo.newBuilder()
                                .setName("Offer 1")
                                .setUnitedCatalog(true)
                                .addContentStatusesCPA(
                                    listOf(
                                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY
                                    )
                                )
                                .build()
                        )
                    )
                    .setTotalCount(1)
                    .setNextPageNumber(1)
                    .setPreviousPageNumber(1)
                    .build()
            )

        val result = wizardService.getPartnerSteps(PARTNER_ID, WizardStepType.ASSORTMENT)
        assertWizardStatus(result, WizardStepType.ASSORTMENT, WizardStepStatus.FULL)
    }

    private fun mockFbsInfo(): PartnerOnboardingInfoResponse? {
        return PartnerOnboardingInfoResponse()
            .partnerId(PARTNER_ID)
            .businessId(BUSINESS_ID)
            .orderProcessingType(ru.yandex.market.mbi.open.api.client.model.OrderProcessingType.PI)
            .partnerPlacementType(ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType.FBS)
    }

    private fun mockFbyInfo(): PartnerOnboardingInfoResponse? {
        return PartnerOnboardingInfoResponse()
            .partnerId(PARTNER_ID)
            .businessId(BUSINESS_ID)
            .orderProcessingType(ru.yandex.market.mbi.open.api.client.model.OrderProcessingType.PI)
            .partnerPlacementType(ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType.FBY)
    }

    class OfferWithStocksFilterMatcher : ArgumentMatcher<SaasOfferFilter> {
        override fun matches(argument: SaasOfferFilter?): Boolean {
            argument ?: return false

            val neededStatuses = listOf(
                ResultOfferStatus.PUBLISHED,
                ResultOfferStatus.PUBLISHED_AND_CHECKING,
                ResultOfferStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                ResultOfferStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED
            ).map { DataCampCommonConversions.fromResultOfferStatus(it) }
            val statusesInArg = argument.resultOfferStatuses

            return statusesInArg.containsAll(neededStatuses) && statusesInArg.size == neededStatuses.size
        }
    }

    class OfferWithoutStocksFilterMatcher : ArgumentMatcher<SaasOfferFilter> {
        override fun matches(argument: SaasOfferFilter?): Boolean {
            argument ?: return false

            val neededStatuses = listOf(
                ResultOfferStatus.PUBLISHED,
                ResultOfferStatus.PUBLISHED_AND_CHECKING,
                ResultOfferStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                ResultOfferStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED,
                ResultOfferStatus.NOT_PUBLISHED_NO_STOCKS
            ).map { DataCampCommonConversions.fromResultOfferStatus(it) }
            val statusesInArg = argument.resultOfferStatuses

            return statusesInArg.containsAll(neededStatuses) && statusesInArg.size == neededStatuses.size
        }
    }

    class TotalOfferCountMatcher : ArgumentMatcher<SaasOfferFilter> {
        override fun matches(argument: SaasOfferFilter?): Boolean {
            return argument != null &&
                argument.resultContentStatuses.isEmpty() &&
                argument.resultOfferStatuses.isEmpty()
        }
    }
}

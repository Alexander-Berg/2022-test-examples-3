package ru.yandex.market.partner.status.wizard.steps

import Market.DataCamp.DataCampOfferStatus
import org.junit.jupiter.api.AfterEach
import org.mockito.Mockito
import retrofit2.Call
import retrofit2.Response
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod
import ru.yandex.market.abo.api.entity.checkorder.PlacementType
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO
import ru.yandex.market.core.feature.model.FeatureType
import ru.yandex.market.core.feature.model.ShopFeatureListItem
import ru.yandex.market.core.param.model.ParamCheckStatus
import ru.yandex.market.core.param.model.ParamValue
import ru.yandex.market.ff4shops.api.model.DebugStatus
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopDeliveryStateDto
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopsDeliveryStateRequestDto
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopsDeliveryStateResponseDto
import ru.yandex.market.mbi.api.billing.client.model.CurrentAndNextMonthPayoutFrequencyDTO
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO
import ru.yandex.market.mbi.api.client.entity.params.ShopParams
import ru.yandex.market.mbi.api.client.entity.params.ShopsWithParams
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse
import ru.yandex.market.mbi.open.api.client.model.LastUploadedFeedInfoDTO
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType
import ru.yandex.market.mbi.open.api.client.model.OrganizationType
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOption
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOptionsResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerIdName
import ru.yandex.market.mbi.open.api.client.model.PartnerLastDataFeedsInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerLastPrepayRequestDTO
import ru.yandex.market.mbi.open.api.client.model.PartnerLastUploadedFeedInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationResult
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationResultRequest
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationResultResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingHasOutletsResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingInfoResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingLegalDataResponse
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType
import ru.yandex.market.mbi.open.api.client.model.RequestType
import ru.yandex.market.mbi.open.api.client.model.ReturnContactDTO
import ru.yandex.market.mbi.open.api.client.model.ReturnContactTypeDTO
import ru.yandex.market.mbi.open.api.client.model.ShopVatDTO
import ru.yandex.market.mbi.open.api.client.model.TaxSystemDTO
import ru.yandex.market.mbi.open.api.client.model.TestingStatusDTO
import ru.yandex.market.mbi.open.api.client.model.TestingTypeDTO
import ru.yandex.market.mbi.open.api.client.model.VatRateDTO
import ru.yandex.market.mbi.open.api.client.model.VatSourceDTO
import ru.yandex.market.mbi.web.paging.SeekSliceRequest
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.wizard.model.check.apilog.LogStat
import ru.yandex.market.partner.status.wizard.model.check.feed.LastUploadedFeedInfo
import java.time.OffsetDateTime
import java.util.Optional

abstract class WizardFunctionalTest : AbstractFunctionalTest() {

    companion object {
        internal const val PARTNER_ID = 1L
        internal const val BUSINESS_ID = 10L
        internal const val WAREHOUSE_ID = 100L
        internal const val FEED_ID = 1000L
        internal const val CONTRACT_ID = 10000L
    }

    @AfterEach
    fun clear() {
        Mockito.reset(mbiApiClient)
        Mockito.reset(mbiOpenApiClient)
        Mockito.reset(mbiBillingClient)
        Mockito.reset(saasService)
        Mockito.reset(lmsClient)
        Mockito.reset(mbiLogProcessorClient)
        Mockito.reset(ff4shopsClient)
        Mockito.reset(aboApi)
        Mockito.reset(shopDeliveryStateApi)
    }

    fun mockPartnerInfo(
        placementType: PartnerPlacementType,
        orderProcessingType: OrderProcessingType = OrderProcessingType.PI,
        partnerFulfillmentLinks: List<PartnerFulfillmentLinkDTO> = emptyList()
    ) {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingInfoResponse()
                    .partnerId(PARTNER_ID)
                    .orderProcessingType(orderProcessingType)
                    .partnerPlacementType(placementType)
                    .businessId(BUSINESS_ID)
                    .fulfillmentLinks(partnerFulfillmentLinks)
            )
        Mockito.`when`(mbiApiClient.getPartnerSuperAdmin(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                BusinessOwnerDTO(1L, 123L, "login", setOf("ya@ru"))
            )
    }

    fun mockPrepayRequest(partnerApplicationStatus: PartnerApplicationStatus) {
        Mockito.`when`(mbiOpenApiClient.getPartnerOnboardingLegalData(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerOnboardingLegalDataResponse()
                    .lastPrepayRequest(
                        PartnerLastPrepayRequestDTO()
                            .id(1L)
                            .partnerId(PARTNER_ID)
                            .datasourceId(PARTNER_ID)
                            .requestType(RequestType.SUPPLIER)
                            .contactPersonFirstName("First Name")
                            .contactPersonMiddleName("Middle Name")
                            .contactPersonLastName("Last Name")
                            .contactPerson("Contact person")
                            .phoneNumber("+7 987 987 87 87")
                            .email("contact@email.ru")
                            .organizationName("Organization name")
                            .organizationType(OrganizationType.OOO)
                            .ogrn("12345")
                            .inn("123456")
                            .kpp("kpp")
                            .postcode("123456")
                            .factAddress("factAddress")
                            .jurAddress("juridicalAddress")
                            .accountNumber("AccountNumber")
                            .corrAccountNumber("CorrAccountNumber")
                            .bik("456789")
                            .bankName("Bank 1")
                            .status(partnerApplicationStatus)
                            .workSchedule("Work schedule")
                            .licenseNum("123")
                            .licenseDate(OffsetDateTime.parse("2020-01-01T16:00:00+03:00"))
                            .shopContactAddress("Shop address")
                            .shopPhoneNumber("+7 098 098 98 98")

                    )
                    .shopVat(
                        ShopVatDTO()
                            .vatRate(VatRateDTO.VAT_18)
                            .deliveryVatRate(VatRateDTO.VAT_10)
                            .vatSource(VatSourceDTO.WEB)
                            .datasourceId(PARTNER_ID)
                            .taxSystem(TaxSystemDTO.ENVD)
                    )
                    .returnContacts(
                        listOf(
                            ReturnContactDTO()
                                .datasourceId(PARTNER_ID)
                                .firstName("Return First Name")
                                .secondName("Return Second Name")
                                .comments("Return comments")
                                .isEnabled(true)
                                .lastName("Return Last Name")
                                .email("return@email.ru")
                                .address("return address")
                                .type(ReturnContactTypeDTO.PERSON)
                                .phoneNumber("+7 123 123 23 23")
                                .jobPosition("Job position")
                        )
                    )
                    .partnerName("Partner 1")
            )
    }

    fun mockContractOptions(hasCurrentContract: Boolean? = null) {
        Mockito.`when`(mbiOpenApiClient.getPartnerContractOptions(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerContractOptionsResponse()
                    .currentContractId(if (hasCurrentContract == true) CONTRACT_ID else null)
                    .contractOptions(
                        listOf(
                            PartnerContractOption()
                                .contractId(CONTRACT_ID)
                                .contractEid("Contract1")
                                .jurName("Jur name")
                                .partnerIdNames(
                                    listOf(
                                        PartnerIdName()
                                            .partnerId(PARTNER_ID)
                                            .partnerName("Partner 1")
                                    )
                                )
                                .organizationType(OrganizationType.OOO)
                        )
                    )
            )
        mockPayoutFrequencies()
    }

    fun mockPayoutFrequencies() {
        Mockito.`when`(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(Mockito.eq(listOf(CONTRACT_ID))))
            .thenReturn(
                listOf(
                    CurrentAndNextMonthPayoutFrequencyDTO()
                        .contractId(CONTRACT_ID)
                        .currentMonthFrequency(PayoutFrequencyDTO.DAILY)
                        .isDefaultCurrentMonthFrequency(false)
                )
            )
    }

    fun mockTotalPartnerOffers(count: Int) {
        Mockito.`when`(
            saasService.searchBusinessOffers(
                Mockito.eq(
                    SaasOfferFilter.newBuilder()
                        .setPrefix(BUSINESS_ID)
                        .setBusinessId(BUSINESS_ID)
                        .addShopId(PARTNER_ID)
                        .setDocType(SaasDocType.OFFER)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .build()
                )
            )
        ).thenReturn(SaasSearchResult.builder().setTotalCount(count).build())
    }

    fun mockTotalBusinessOffers(count: Int) {
        Mockito.`when`(
            saasService.searchBusinessOffers(
                Mockito.eq(
                    SaasOfferFilter.newBuilder()
                        .setPrefix(BUSINESS_ID)
                        .setBusinessId(BUSINESS_ID)
                        .setDocType(SaasDocType.OFFER)
                        .setUnitedCatalog(true)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .build()
                )
            )
        ).thenReturn(SaasSearchResult.builder().setTotalCount(count).build())
    }

    fun mockValidOfferWithStock(count: Int) {
        Mockito.`when`(
            saasService.searchBusinessOffers(
                Mockito.eq(
                    SaasOfferFilter.newBuilder()
                        .setPrefix(BUSINESS_ID)
                        .setBusinessId(BUSINESS_ID)
                        .addShopId(PARTNER_ID)
                        .setDocType(SaasDocType.OFFER)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .addResultOfferStatuses(
                            PARTNER_ID,
                            listOf(
                                DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED,
                                DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED_AND_CHECKING,
                                DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                                DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED
                            )
                        )
                        .build()
                )
            )
        ).thenReturn(SaasSearchResult.builder().setTotalCount(count).build())
    }

    fun mockValidOfferWithoutStock(count: Int) {
        Mockito.`when`(
            saasService.searchBusinessOffers(
                Mockito.eq(
                    SaasOfferFilter.newBuilder()
                        .setPrefix(BUSINESS_ID)
                        .setBusinessId(BUSINESS_ID)
                        .addShopId(PARTNER_ID)
                        .setDocType(SaasDocType.OFFER)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .addResultOfferStatuses(
                            PARTNER_ID,
                            listOf(
                                DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED,
                                DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED_AND_CHECKING,
                                DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                                DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED,
                                DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_NO_STOCKS
                            )
                        )
                        .build()
                )
            )
        ).thenReturn(SaasSearchResult.builder().setTotalCount(count).build())
    }

    fun mockWarehouse(hasActive: Boolean) {
        Mockito.`when`(
            lmsClient.searchPartnerRelation(
                Mockito.eq(
                    PartnerRelationFilter.newBuilder()
                        .enabled(true)
                        .fromPartnerId(WAREHOUSE_ID)
                        .build()
                )
            )
        ).thenReturn(if (hasActive) listOf(PartnerRelationEntityDto.newBuilder().build()) else listOf())
    }

    fun mockFeedInfo(feedInfo: LastUploadedFeedInfo?) {
        if (feedInfo == null) {
            Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
                .thenReturn(PartnerLastUploadedFeedInfoResponse())
        } else {
            Mockito.`when`(mbiOpenApiClient.getPartnerLastUploadedFeedInfo(Mockito.eq(PARTNER_ID)))
                .thenReturn(
                    PartnerLastUploadedFeedInfoResponse()
                        .lastUploadedFeedInfo(
                            LastUploadedFeedInfoDTO()
                                .id(1234)
                                .name(feedInfo.name)
                                .url(feedInfo.url)
                                .size(1000)
                                .feedTemplate("template")
                                .uploadDateTime(feedInfo.date)
                        )
                )
        }
    }

    fun mockPartnerParams(partnerParams: List<ParamValue<*>>) {
        Mockito.`when`(mbiApiClient.getShopCheckedParams(Mockito.eq(listOf(PARTNER_ID))))
            .thenReturn(ShopsWithParams(listOf(ShopParams.of(PARTNER_ID, partnerParams))))
    }

    fun mockApiLogStat(logStat: LogStat) {
        Mockito.`when`(
            mbiLogProcessorClient.getLogStats(
                Mockito.eq(PARTNER_ID),
                Mockito.any(),
                Mockito.any()
            )
        ).thenReturn(
            PushApiLogStatsResponse()
                .count(logStat.count)
                .errorCount(logStat.errorCount)
                .maxEventTime(logStat.maxEventTime.epochSecond)
                .minEventTime(logStat.minEventTime.epochSecond)
                .successCount(logStat.successCount)
        )
    }

    fun mockStocksDebugStatus(debugStatus: DebugStatus) {
        Mockito.`when`(ff4shopsClient.getDebugStockStatus(Mockito.eq(PARTNER_ID))).thenReturn(debugStatus)
    }

    fun mockLmsPartner(partnerResponse: PartnerResponse?) {
        Mockito.`when`(lmsClient.getPartner(Mockito.eq(WAREHOUSE_ID))).thenReturn(Optional.ofNullable(partnerResponse))
    }

    fun mockAboSelfCheck(selfCheck: List<SelfCheckDTO>) {
        Mockito.`when`(
            aboApi.getSelfCheckScenarios(
                Mockito.eq(PARTNER_ID),
                Mockito.eq(PlacementType.DSBB),
                Mockito.eq(OrderProcessMethod.API)
            )
        ).thenReturn(selfCheck)
    }

    fun mockFeature(featureType: FeatureType, featureStatus: ParamCheckStatus, cpaIsPartnerInterface: Boolean = true) {
        Mockito.`when`(
            mbiApiClient.getShopWithFeature(Mockito.eq(PARTNER_ID), Mockito.eq(featureType.id))
        ).thenReturn(ShopFeatureListItem(PARTNER_ID, featureType, featureStatus, cpaIsPartnerInterface))
    }

    fun mockShopFeedCheck(uploadId: Long? = null) {
        Mockito.`when`(mbiOpenApiClient.getDataFeedsInfo(Mockito.eq(PARTNER_ID)))
            .thenReturn(
                PartnerLastDataFeedsInfoResponse()
                    .addLastDataFeedsInfoItem(
                        LastUploadedFeedInfoDTO()
                            .id(FEED_ID)
                            .name("superFeed")
                            .uploadDateTime(OffsetDateTime.now())
                            .url("url.ru")
                            .uploadId(uploadId)
                    )
            )
    }

    fun mockShopOutlets(hasOutlets: Boolean) {
        Mockito.`when`(mbiOpenApiClient.hasOutlets(Mockito.eq(PARTNER_ID)))
            .thenReturn(PartnerOnboardingHasOutletsResponse().hasOutlets(hasOutlets))
    }

    fun mockHasConfiguredDelivery(hasConfiguredDelivery: Boolean) {
        val result = ShopsDeliveryStateResponseDto()
        if (hasConfiguredDelivery) {
            result.addDeliveryStatesItem(ShopDeliveryStateDto())
        } else {
            result.deliveryStates(emptyList())
        }

        val responseCall = Mockito.mock(Call::class.java)
        Mockito.`when`(responseCall.execute()).thenReturn(Response.success(result))
        Mockito.`when`(
            shopDeliveryStateApi.getDeliveryState(
                Mockito.eq(ShopsDeliveryStateRequestDto().addShopIdsItem(PARTNER_ID))
            )
        ).thenReturn(responseCall as Call<ShopsDeliveryStateResponseDto>?)
    }

    fun mockSelfCheckState(testingStatusDTO: TestingStatusDTO?) {
        val response = PartnerModerationResultResponse()
        if (testingStatusDTO != null) {
            response.result(PartnerModerationResult().testingStatus(testingStatusDTO).isCancelled(false))
        }
        Mockito.`when`(
            mbiOpenApiClient.getModerationResult(
                Mockito.eq(PARTNER_ID),
                Mockito.eq(PartnerModerationResultRequest().testingType(TestingTypeDTO.SELF_CHECK))
            )
        ).thenReturn(response)
    }

    fun mockModeration(testingStatusDTO: TestingStatusDTO?, isCancelled: Boolean? = null) {
        val response = PartnerModerationResultResponse()
        if (testingStatusDTO != null) {
            response.result(PartnerModerationResult().testingStatus(testingStatusDTO).isCancelled(isCancelled))
        }
        Mockito.`when`(
            mbiOpenApiClient.getModerationResult(
                Mockito.eq(PARTNER_ID),
                Mockito.eq(PartnerModerationResultRequest().testingType(TestingTypeDTO.CPA))
            )
        ).thenReturn(response)
    }
}

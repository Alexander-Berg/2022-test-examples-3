package ru.yandex.market.markup3.mocks

import ru.yandex.market.markup3.skuconflicts.BusinessSkuKey
import ru.yandex.market.mboc.http.MboMappings
import ru.yandex.market.mboc.http.MboMappingsService
import ru.yandex.market.mboc.http.SupplierOffer

open class MboMappingsServiceMock : MboMappingsService {
    var businessSkuKeyToOfferId: Map<BusinessSkuKey, Int> = mapOf();

    override fun addProductInfo(request: MboMappings.ProviderProductInfoRequest?): MboMappings.ProviderProductInfoResponse {
        TODO("Not yet implemented")
    }

    override fun updateMappings(request: MboMappings.UpdateMappingsRequest?): MboMappings.ProviderProductInfoResponse {
        TODO("Not yet implemented")
    }

    override fun updateAvailability(request: MboMappings.UpdateAvailabilityRequest?): MboMappings.UpdateAvailabilityResponse {
        TODO("Not yet implemented")
    }

    override fun updateAntiMappings(request: MboMappings.UpdateAntiMappingsRequest?): MboMappings.UpdateAntiMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchVendorsByShopId(request: MboMappings.SearchVendorsBySupplierIdRequest?): MboMappings.SearchVendorsResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingsByShopId(request: MboMappings.SearchMappingsBySupplierIdRequest?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingsByMarketSkuId(request: MboMappings.SearchMappingsByMarketSkuIdRequest?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchBaseOfferMappingsByMarketSkuId(request: MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingsByKeys(request: MboMappings.SearchMappingsByKeysRequest?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingsByBusinessKeys(request: MboMappings.SearchMappingsByBusinessKeysRequest): MboMappings.SearchMappingsResponse {
        return MboMappings.SearchMappingsResponse.newBuilder().apply {
            addAllOffers(request.keysList.mapNotNull {
                val key = BusinessSkuKey(it.businessId.toLong(), it.offerId)

                if (!businessSkuKeyToOfferId.containsKey(key))
                    return@mapNotNull null

                SupplierOffer.Offer.newBuilder().apply {
                    internalOfferId = businessSkuKeyToOfferId[key]!!.toLong()
                    supplierId = key.businessId
                    shopSkuId = key.shopSku
                }.build()
            })
        }.build()
    }

    override fun searchMappingsForContentLab(request: MboMappings.SearchMappingsForContentLab?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchProductInfoByYtStamp(request: MboMappings.SearchProductInfoByYtStampRequest?): MboMappings.SearchProductInfoByYtStampResponse {
        TODO("Not yet implemented")
    }

    override fun searchProductInfoLiteByYtStamp(request: MboMappings.SearchProductInfoByYtStampRequest?): MboMappings.SearchProductInfoLiteByYtStampResponse {
        TODO("Not yet implemented")
    }

    override fun searchProductInfoLiteByYtStampNotDataCamp(request: MboMappings.SearchProductInfoByYtStampRequest?): MboMappings.SearchProductInfoLiteByYtStampResponse {
        TODO("Not yet implemented")
    }

    override fun searchAntiMappingInfoByStamp(request: MboMappings.SearchProductInfoByYtStampRequest?): MboMappings.SearchAntiMappingInfoByStampResponse {
        TODO("Not yet implemented")
    }

    override fun searchAntiMappingsByMarketSkuId(request: MboMappings.SearchAntiMappingsByMarketSkuIdRequest?): MboMappings.SearchOfferAntiMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingCategoriesByShopId(request: MboMappings.SearchMappingCategoriesRequest?): MboMappings.SearchMappingCategoriesResponse {
        TODO("Not yet implemented")
    }

    override fun searchOfferProcessingStatusesByShopId(request: MboMappings.SearchOfferProcessingStatusesRequest?): MboMappings.SearchOfferProcessingStatusesResponse {
        TODO("Not yet implemented")
    }

    override fun searchApprovedMappingsByMarketSkuId(request: MboMappings.SearchApprovedMappingsRequest?): MboMappings.SearchApprovedMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchApprovedMappingsByKeys(request: MboMappings.SearchMappingsByKeysRequest?): MboMappings.SearchApprovedMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchApprovedMappingsByBusinessKeys(request: MboMappings.SearchMappingsByBusinessKeysRequest?): MboMappings.SearchApprovedMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchLiteApprovedMappingsByKeys(request: MboMappings.SearchLiteMappingsByKeysRequest?): MboMappings.SearchLiteMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun searchLiteApprovedMappingsByMarketSkuId(request: MboMappings.SearchLiteMappingsByMarketSkuIdRequest?): MboMappings.SearchLiteMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun uploadExcelFile(request: MboMappings.OfferExcelUpload.Request?): MboMappings.OfferExcelUpload.Response {
        TODO("Not yet implemented")
    }

    override fun uploadExcelFileStreamed(request: MboMappings.OfferExcelUpload.Request?): MboMappings.OfferExcelUpload.Response {
        TODO("Not yet implemented")
    }

    override fun getUploadStatus(request: MboMappings.OfferExcelUpload.ProcessingRequest?): MboMappings.OfferExcelUpload.Response {
        TODO("Not yet implemented")
    }

    override fun downloadUploadResult(request: MboMappings.OfferExcelUpload.ResultRequest?): MboMappings.OfferExcelUpload.Result {
        TODO("Not yet implemented")
    }

    override fun downloadUploadStreamedResult(request: MboMappings.OfferExcelUpload.ResultRequest?): MboMappings.OfferExcelUpload.StreamedResult {
        TODO("Not yet implemented")
    }

    override fun updateOfferProcessingStatus(request: MboMappings.UpdateOfferProcessingStatusRequest?): MboMappings.UpdateOfferProcessingStatusResponse {
        TODO("Not yet implemented")
    }

    override fun updateContentProcessingTasks(request: MboMappings.UpdateContentProcessingTasksRequest?): MboMappings.UpdateContentProcessingTasksResponse {
        TODO("Not yet implemented")
    }

    override fun reprocessOffersClassification(request: MboMappings.ReprocessOffersClassificationRequest?): MboMappings.ReprocessOffersClassificationResponse {
        TODO("Not yet implemented")
    }

    override fun countBusinessOffers(request: MboMappings.CountBusinessOffersRequest?): MboMappings.CountBusinessOffersResponse {
        TODO("Not yet implemented")
    }

    override fun getShopSKU(request: MboMappings.ShopSKURequest?): MboMappings.SKUMappingResponse {
        TODO("Not yet implemented")
    }

    override fun addOfferToContentProcessing(request: MboMappings.AddToContentProcessingRequest?): MboMappings.AddToContentProcessingResponse {
        TODO("Not yet implemented")
    }

    override fun getAssortmentChildSskus(request: MboMappings.GetAssortmentChildSskusRequest?): MboMappings.GetAssortmentChildSskusResponse {
        TODO("Not yet implemented")
    }

    override fun sendToRecheck(request: MboMappings.SendToRecheckMappingRequest?): MboMappings.SendToRecheckMappingResponse {
        TODO("Not yet implemented")
    }

    override fun resetMapping(request: MboMappings.ResetMappingRequest?): MboMappings.ResetMappingResponse {
        TODO("Not yet implemented")
    }
}

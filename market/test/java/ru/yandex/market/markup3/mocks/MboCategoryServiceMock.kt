package ru.yandex.market.markup3.mocks

import ru.yandex.market.http.MonitoringResult
import ru.yandex.market.markup3.mboc.OfferId
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.MboCategory.SaveTaskMappingsRequest
import ru.yandex.market.mboc.http.MboCategory.UpdateSupplierOfferCategoryRequest
import ru.yandex.market.mboc.http.MboCategory.UpdateSupplierOfferCategoryResponse
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.MboMappings
import ru.yandex.market.mboc.http.SupplierOffer
import java.util.Collections

/**
 * @author shadoff
 * created on 7/29/21
 */
open class MboCategoryServiceMock : MboCategoryService {

    val taskOffersMap: HashMap<OfferId, MboCategory.GetTaskOffersResponse.TaskOffer.Builder> = HashMap()

    val savedMappingModerationIds: MutableList<String> = Collections.synchronizedList(ArrayList())!!
    var lastSavedMappingModerationRequest: MboCategory.SaveMappingsModerationRequest? = null

    var saveResponse: MboCategory.SaveMappingModerationResponse =
        MboCategory.SaveMappingModerationResponse.newBuilder().setResult(
            SupplierOffer.OperationResult.newBuilder().setStatus(SupplierOffer.OperationStatus.SUCCESS)
        ).build()

    val saveTaskMappingsRequests = mutableListOf<SaveTaskMappingsRequest>()
    var saveTaskMappingsResponse: MboCategory.SaveTaskMappingsResponse? = null

    val updateSupplierOfferCategoryRequests = mutableListOf<UpdateSupplierOfferCategoryRequest>()
    var updateSupplierOfferCategoryResponse: UpdateSupplierOfferCategoryResponse? = null

    var groupsResponse = defaultCategoryGroupsResponse()

    override fun getTicketPriorities(p0: MboCategory.GetTicketPrioritiesRequest?): MboCategory.GetTicketPrioritiesResponse {
        TODO("Not yet implemented")
    }

    fun addTaskOffers(vararg offers: MboCategory.GetTaskOffersResponse.TaskOffer.Builder) {
        taskOffersMap.putAll(offers.associateBy { it.offerId })
    }

    fun addTaskOffers(offers: Collection<MboCategory.GetTaskOffersResponse.TaskOffer.Builder>) {
        taskOffersMap.putAll(offers.associateBy { it.offerId })
    }

    override fun getTaskOffers(request: MboCategory.GetTaskOffersRequest?): MboCategory.GetTaskOffersResponse {
        return MboCategory.GetTaskOffersResponse.newBuilder().apply {
            addAllOffers(taskOffersMap.values.map { it.build() })
        }.build()
    }

    override fun saveMappingsModeration(r: MboCategory.SaveMappingsModerationRequest): MboCategory.SaveMappingModerationResponse {
        r.resultsList.forEach { savedMappingModerationIds.add(it.offerId) }
        lastSavedMappingModerationRequest = r
        return saveResponse
    }

    fun setDefaultSaveResponse() {
        saveResponse = MboCategory.SaveMappingModerationResponse.newBuilder().setResult(
            SupplierOffer.OperationResult.newBuilder().setStatus(SupplierOffer.OperationStatus.SUCCESS)
        ).build()
    }

    override fun ping(): MonitoringResult {
        TODO("Not yet implemented")
    }

    override fun monitoring(): MonitoringResult {
        TODO("Not yet implemented")
    }

    override fun updateSupplierOfferMappings(p0: SaveTaskMappingsRequest?): MboCategory.SaveTaskMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun saveTaskMappings(p0: SaveTaskMappingsRequest?): MboCategory.SaveTaskMappingsResponse {
        saveTaskMappingsRequests.add(p0!!)
        return saveTaskMappingsResponse ?: throw error("Mock not configured")
    }

    override fun searchMappingsByInternalOfferId(p0: MboCategory.SearchMappingsByInternalOfferIdRequest?): MboCategory.SearchMappingsByInternalOfferIdResponse {
        TODO("Not yet implemented")
    }

    override fun searchMappingsBulkByOfferId(p0: MboCategory.SearchMappingsBulkAMRequest?): MboMappings.SearchMappingsResponse {
        TODO("Not yet implemented")
    }

    override fun loadCategorySupplierOffersBindings(p0: MboCategory.CategorySupplierOffersBindingsRequest?): MboCategory.CategorySupplierOffersBindingsResponse {
        TODO("Not yet implemented")
    }

    override fun getOffersPriorities(p0: MboCategory.GetOffersPrioritiesRequest?): MboCategory.GetOffersPrioritiesResponse {
        TODO("Not yet implemented")
    }

    override fun saveOfferMarkupStatuses(p0: MboCategory.SaveOfferMarkupStatusesRequest?): MboCategory.SaveOfferMarkupStatusesResponse {
        TODO("Not yet implemented")
    }

    override fun updateContentLabStates(p0: MboCategory.UpdateContentLabStatesRequest?): MboCategory.UpdateContentLabStatesResponse {
        TODO("Not yet implemented")
    }

    override fun processFillSkuFromClabTaskResult(p0: MboCategory.ProcessTaskFillSkuFromClabRequest?): MboCategory.ProcessTaskFillSkuFromClabResponse {
        TODO("Not yet implemented")
    }

    override fun getProcessedSkuFromClab(p0: MboCategory.GetProcessedSkuFromClabRequest?): MboCategory.GetProcessedSkuFromClabResponse {
        TODO("Not yet implemented")
    }

    override fun getContentCommentTypes(p0: MboCategory.ContentCommentTypes.Request?): MboCategory.ContentCommentTypes.Response {
        TODO("Not yet implemented")
    }

    override fun updateSupplierOfferCategory(p0: UpdateSupplierOfferCategoryRequest?): UpdateSupplierOfferCategoryResponse {
        updateSupplierOfferCategoryRequests.add(p0!!)
        return updateSupplierOfferCategoryResponse ?: throw error("Mock not configured")
    }

    override fun forceOfferCategory(p0: MboCategory.ForceOfferCategoryRequest?): MboCategory.ForceOfferCategoryResponse {
        TODO("Not yet implemented")
    }

    override fun forceOfferReclassification(p0: MboCategory.ForceOfferReclassificationRequest?): MboCategory.ForceOfferReclassificationResponse {
        TODO("Not yet implemented")
    }

    override fun getShortOfferInfos(p0: MboCategory.GetShortOfferInfosRequest?): MboCategory.GetShortOfferInfosResponse {
        TODO("Not yet implemented")
    }

    override fun getShortOfferInfoById(p0: MboCategory.GetShortOfferInfoByIdRequest?): MboCategory.GetShortOfferInfoByIdResponse {
        TODO("Not yet implemented")
    }

    override fun getShortSupplierInfos(p0: MboCategory.GetShortSupplierInfosRequest?): MboCategory.GetShortSupplierInfosResponse {
        TODO("Not yet implemented")
    }

    override fun getTicketStatuses(p0: MboCategory.GetTicketStatusesRequest?): MboCategory.GetTicketStatusesResponse {
        TODO("Not yet implemented")
    }

    override fun removeMappingBySkuIds(request: MboCategory.RemoveMappingBySkuIdsRequest?): MboCategory.RemoveMappingBySkuIdsResponse {
        TODO("Not yet implemented")
    }

    override fun getCategoryGroups(request: MboCategory.GetCategoryGroupsRequest?): MboCategory.GetCategoryGroupsResponse {
        return groupsResponse
    }

    fun defaultCategoryGroupsResponse(): MboCategory.GetCategoryGroupsResponse {
        return MboCategory.GetCategoryGroupsResponse.newBuilder()
            .addCategoryGroups(MboCategory.GetCategoryGroupsResponse.CategoryGroup.newBuilder()
                .addCategories(11L)
                .addCategories(12L)
                .setId(1L)
            )
            .addCategoryGroups(MboCategory.GetCategoryGroupsResponse.CategoryGroup.newBuilder()
                .addCategories(21L)
                .addCategories(22L)
                .addCategories(23L)
                .setId(2L)
            )
            .addCategoryGroups(MboCategory.GetCategoryGroupsResponse.CategoryGroup.newBuilder()
                .addCategories(31L)
                .setId(3L)
            )
            .build()
    }
}

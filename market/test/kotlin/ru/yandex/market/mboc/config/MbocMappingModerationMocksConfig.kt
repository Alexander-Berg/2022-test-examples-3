package ru.yandex.market.mboc.config

import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import ru.yandex.market.mboc.common.services.offers.UpdateSupplierOfferCategoryService
import ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService
import ru.yandex.market.mboc.common.services.offers.mapping.SaveTaskMappingsService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.http.SupplierOffer.OperationStatus
import ru.yandex.market.mboc.processing.moderation.processor.TolokaMappingModerationResultProcessor

@Profile("test")
@Configuration
open class MbocMappingModerationMocksConfig {

    @Bean
    open fun tolokaMappingModerationResultProcessor(
        saveMappingModerationService: SaveMappingModerationService,
    ): TolokaMappingModerationResultProcessor {
        return mock(TolokaMappingModerationResultProcessor::class.java)
    }

    @Bean("succeedSave")
    open fun succeedSaveMappingModerationService() = createSpecialMMService(listOf(OperationStatus.SUCCESS))

    @Primary
    @Bean("failedSave")
    open fun failedSaveMappingModerationService() = createSpecialMMService(listOf(OperationStatus.ERROR))

    @Bean("succeedSaveTaskMapping")
    open fun succeedSaveTaskMappingsService(): SaveTaskMappingsService =
        createSpecialTMService(listOf(OperationStatus.SUCCESS))

    @Bean("failedSaveTaskMapping")
    @Primary
    open fun failedSaveTaskMappingsService(): SaveTaskMappingsService =
        createSpecialTMService(listOf(OperationStatus.ERROR))

    @Bean("succeedUpdateSupplierOfferCategoryService")
    open fun succeedUpdateSupplierOfferCategoryService(): UpdateSupplierOfferCategoryService =
        createSpecialUpdateSupplierService(listOf(OperationStatus.SUCCESS))

    @Bean("failedUpdateSupplierOfferCategoryService")
    @Primary
    open fun failedUpdateSupplierOfferCategoryService(): UpdateSupplierOfferCategoryService =
        createSpecialUpdateSupplierService(listOf(OperationStatus.ERROR))

    private fun createSpecialMMService(statusList: List<OperationStatus>): SaveMappingModerationService {
        val mockedService = mock(SaveMappingModerationService::class.java)

        val operationStatus =
            if (statusList.contains(OperationStatus.ERROR)) OperationStatus.ERROR
            else OperationStatus.SUCCESS
        val possibleOfferStatuses =
            if (operationStatus == OperationStatus.SUCCESS)
                listOf(OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)
            else
                listOf(OperationStatus.ERROR, OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)

        doAnswer { invocation ->
            val request = invocation.arguments[0] as MboCategory.SaveMappingsModerationRequest
            request.resultsList
            return@doAnswer MboCategory.SaveMappingModerationResponse.newBuilder().apply {
                result = buildResult(request.resultsList, possibleOfferStatuses, operationStatus)
            }.build()
        }.`when`(mockedService).saveMappingsModeration(Mockito.any())
        return mockedService
    }

    private fun buildResult(requestList: List<SupplierOffer.MappingModerationTaskResult>,
        offerStatuses: List<OperationStatus>,
        status: OperationStatus,
    ) = SupplierOffer.OperationResult.newBuilder().apply {
        val resultList = requestList.mapIndexed{ idx, requestOffer ->
            SupplierOffer.OfferStatus.newBuilder().apply {
                offerId = requestOffer.offerId
                this.status = offerStatuses[idx % offerStatuses.size]
            } .build()
        }
        addAllOfferStatuses(resultList)
        this.status = status
    }.build()

    private fun createSpecialTMService(statusList: List<OperationStatus>): SaveTaskMappingsService {
        val mockedService = mock(SaveTaskMappingsService::class.java)

        val operationStatus =
            if (statusList.contains(OperationStatus.ERROR)) OperationStatus.ERROR
            else OperationStatus.SUCCESS
        val possibleOfferStatuses =
            if (operationStatus == OperationStatus.SUCCESS)
                listOf(OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)
            else
                listOf(OperationStatus.ERROR, OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)

        doAnswer { invocation ->
            val request = invocation.arguments[0] as MboCategory.SaveTaskMappingsRequest
            return@doAnswer MboCategory.SaveTaskMappingsResponse.newBuilder().apply {
                result = buildTaskSaveResult(request.mappingList, possibleOfferStatuses, operationStatus)
            }.build()
        }.`when`(mockedService).saveTaskMappings(Mockito.any())
        return mockedService
    }

    private fun buildTaskSaveResult(requestList: List<SupplierOffer.ContentTaskResult>,
        offerStatuses: List<OperationStatus>,
        status: OperationStatus,
    ) = SupplierOffer.OperationResult.newBuilder().apply {
        val resultList = requestList.mapIndexed{ idx, requestOffer ->
            SupplierOffer.OfferStatus.newBuilder().apply {
                offerId = requestOffer.offerId
                this.status = offerStatuses[idx % offerStatuses.size]
            } .build()
        }
        addAllOfferStatuses(resultList)
        this.status = status
    }.build()

    private fun createSpecialUpdateSupplierService(statusList: List<OperationStatus>)
    : UpdateSupplierOfferCategoryService {
        val mockedService = mock(UpdateSupplierOfferCategoryService::class.java)

        val operationStatus =
            if (statusList.contains(OperationStatus.ERROR)) OperationStatus.ERROR
            else OperationStatus.SUCCESS
        val possibleOfferStatuses =
            if (operationStatus == OperationStatus.SUCCESS)
                listOf(OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)
            else
                listOf(OperationStatus.ERROR, OperationStatus.SUCCESS, OperationStatus.NOOP, OperationStatus.REPROCESS)

        doAnswer { invocation ->
            val request = invocation.arguments[0] as MboCategory.UpdateSupplierOfferCategoryRequest
            return@doAnswer MboCategory.UpdateSupplierOfferCategoryResponse.newBuilder().apply {
                result = buildUpdateSupplierCategorySaveResult(request.resultList, possibleOfferStatuses, operationStatus)
            }.build()
        }.`when`(mockedService).updateSupplierOfferCategory(Mockito.any())
        return mockedService
    }

    private fun buildUpdateSupplierCategorySaveResult(requestList: List<SupplierOffer.ClassificationTaskResult>,
        offerStatuses: List<OperationStatus>,
        status: OperationStatus,
    ) = SupplierOffer.OperationResult.newBuilder().apply {
        val resultList = requestList.mapIndexed{ idx, requestOffer ->
            SupplierOffer.OfferStatus.newBuilder().apply {
                offerId = requestOffer.offerId
                this.status = offerStatuses[idx % offerStatuses.size]
            } .build()
        }
        addAllOfferStatuses(resultList)
        this.status = status
    }.build()
}

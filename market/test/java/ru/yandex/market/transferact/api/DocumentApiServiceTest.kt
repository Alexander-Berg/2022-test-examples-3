package ru.yandex.market.transferact.api

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.transferact.AbstractWebTest
import ru.yandex.market.transferact.entity.document.DocumentSaveService
import ru.yandex.market.transferact.entity.document.DocumentType
import ru.yandex.market.transferact.entity.item.transfer.log.ItemTransferLogRepository
import ru.yandex.market.transferact.entity.item.transfer.log.ItemTransferLogViewRepository
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.transfer.TransferMapper
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import java.time.Instant

class DocumentApiServiceTest : AbstractWebTest() {

    @Autowired
    lateinit var documentSaveService: DocumentSaveService

    @Autowired
    lateinit var itemTransferLogViewRepository: ItemTransferLogViewRepository

    @Autowired
    lateinit var itemTransferLogRepository: ItemTransferLogRepository

    @Test
    fun `when get document with non existing itemExternalId then return 404`() {
        mockMvc.perform(
            get("/document")
                .param("itemExternalId", "123")
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    fun `when get document then return document`() {
        val transfer = testOperationHelper.createTransfer(TransferStatus.CLOSED, closedAt = Instant.now())

        val actorCourier =
            testOperationHelper.createActor(name = "Кошелев Владимир Александрович", companyName = "ООО \"Воробушек\"")
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124", "ООО \"Интер\"")

        val operationReceive = testOperationHelper.createOperation(
            transfer = transfer, actor = actorCourier
        )

        val operationProvide = testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf()
        )

        testOperationHelper.createSignature(
            "test",
            "test",
            "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
            operationReceive
        )

        testOperationHelper.createSignature(
            "test",
            "test",
            "8d23cf6c86e834a7aa6eded54c26ce2bb2e74903538c61bdd5d2197997ab2f72",
            operationProvide
        )

        val transferProjection = TransferMapper.mapToProjection(transfer, operationProvide, operationReceive)

        val document = documentSaveService.saveDocument(transferProjection, DocumentType.TRANSFER_ACT)
        itemTransferLogRepository.saveAll(
            itemTransferLogViewRepository.findAllByTransferId(transfer.id)
                .map { it.mapToItemTransferLogEntity() }
        )

        doNothing().`when`(documentUploadService).uploadDocument(transferProjection, document)

        documentUploadService.uploadDocument(transferProjection, document)

        Mockito.`when`(documentUploadService.downloadDocument(anyString())).thenReturn(ByteArray(100))

        mockMvc.perform(
            get("/document")
                .param("itemExternalId", "item2")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().contentType("application/zip"))
            .andReturn().response
    }

    @Test
    fun `when get document by transferId with non existing transferId then return 404`() {
        mockMvc.perform(get("/document/transfer/123"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `when get document by transferId then return document`() {
        val transfer = testOperationHelper.createTransfer(TransferStatus.CLOSED, closedAt = Instant.now())

        val actorCourier = testOperationHelper.createActor(
            name = "Кошелев Владимир Александрович",
            companyName = "ООО \"Вкусно — и точка\""
        )
        val actorPickupPoint = testOperationHelper.createActor(
            externalId = "1000847789", actorTypeName = "MARKET_PVZ",
            apiKey = "124", name = "ООО \"ПВЗ на Арбате\""
        )

        val operationReceive = testOperationHelper.createOperation(transfer = transfer, actor = actorCourier)
        val operationProvide = testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorPickupPoint,
            items = mapOf()
        )
        testOperationHelper.createSignature(
            "test",
            "test",
            "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
            operationReceive
        )
        testOperationHelper.createSignature(
            "test",
            "test",
            "8d23cf6c86e834a7aa6eded54c26ce2bb2e74903538c61bdd5d2197997ab2f72",
            operationProvide
        )

        val transferProjection = TransferMapper.mapToProjection(transfer, operationProvide, operationReceive)

        val document = documentSaveService.saveDocument(transferProjection, DocumentType.TRANSFER_ACT)
        itemTransferLogRepository.saveAll(
            itemTransferLogViewRepository.findAllByTransferId(transfer.id)
                .map { it.mapToItemTransferLogEntity() })

        doNothing().`when`(documentUploadService).uploadDocument(transferProjection, document)

        documentUploadService.uploadDocument(transferProjection, document)

        Mockito.`when`(documentUploadService.downloadDocument(anyString())).thenReturn(ByteArray(100))

        mockMvc.perform(get("/document/transfer/{}}", transfer.id.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().response
    }
}

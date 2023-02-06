package ru.yandex.market.transferact.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil
import ru.yandex.market.transferact.AbstractWebTest
import ru.yandex.market.transferact.common.serialization.ObjectMappers
import ru.yandex.market.transferact.dbqueue.QueueType
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.transfer.TransferRepository
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.mj.generated.server.model.TransferDto

class TransferApiServiceTest : AbstractWebTest() {

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var dbQueueTestUtil: DbQueueTestUtil

    @BeforeEach
    fun init() {
        dbQueueTestUtil.clear(QueueType.TRANSFER_CALLBACK)
        dbQueueTestUtil.clear(QueueType.DOCUMENT_CREATE)
        testOperationHelper.createActorType("MARKET_SC", "scApiKey", "sc-callback-url")
        testOperationHelper.createActorType("MARKET_COURIER", "courierApiKey", "courier-callback-url", 123)
        testOperationHelper.createActorType("MARKET_PVZ", "pvzApiKey", "pvz-callback-url")
        testOperationHelper.createActorType("MARKET_MAGISTRAL", "magistralApiKey", null)
        testOperationHelper.createActorType("MARKET_SHOP", "shopApiKey", null)
    }

    @Test
    fun `put registry happy path`() {
        mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("X-Ya-Service-Ticket", "123")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
    }

    @Test
    fun `when send two request with same idempotency key then return same result`() {
        mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        assertThat(transferRepository.findAll()).hasSize(1)
    }

    @Test
    fun `when put registry without api key then return 403`() {
        mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `get transfer happy path`() {
        val createTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val transferDto = ObjectMappers.mapper.readValue(createTransferResponse, TransferDto::class.java)
        val transferId = transferDto.id

        mockMvc.perform(
            get("/transfer/$transferId")
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
    }

    @Test
    fun `when get transfer with wrong apiKey then return 404`() {
        val createTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val transferDto = ObjectMappers.mapper.readValue(createTransferResponse, TransferDto::class.java)
        val transferId = transferDto.id

        mockMvc.perform(
            get("/transfer/$transferId")
                .header("apiKey", "pvzApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `get pending transfer happy path`() {
        mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString

        mockMvc.perform(
            get("/pendingTransfers")
                .param("actorExternalId", "courierActor")
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/pendingTransfer.json")))

        mockMvc.perform(
            get("/pendingTransfers")
                .param("actorExternalId", "scActor")
                .header("apiKey", "scApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/pendingTransfer.json")))
    }

    @Test
    fun `get pending transfer when there is no such actor returns empty list`() {
        mockMvc.perform(
            get("/pendingTransfers")
                .param("actorExternalId", "courierActor")
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json("[]"))
    }

    @Test
    fun `cancel transfer happy path`() {
        val createTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val transferDto = ObjectMappers.mapper.readValue(createTransferResponse, TransferDto::class.java)
        val transferId = transferDto.id

        mockMvc.perform(
            delete("/transfer/$transferId")
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/deleteTransfer.json")))
    }

    @Test
    fun `put signature happy path`() {
        val createTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransfer.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val transferDto = ObjectMappers.mapper.readValue(createTransferResponse, TransferDto::class.java)
        val transferId = transferDto.id

        var transfer = transferRepository.getByIdOrThrow(transferId.toLong())
        assertThat(transfer.status).isEqualTo(TransferStatus.CREATED)

        mockMvc.perform(
            put("/transfer/$transferId/signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putSignature.json"))
                .header("apiKey", "scApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        transfer = transferRepository.getByIdOrThrow(transferId.toLong())
        assertThat(transfer.status).isEqualTo(TransferStatus.CLOSED)

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_CALLBACK, 4)
        dbQueueTestUtil.assertQueueHasSize(QueueType.DOCUMENT_CREATE, 1)
    }

    @Test
    fun `create two transfers for same transportationId happy path`() {
        val createOutboundTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putOutboundTransfer.json"))
                .header("apiKey", "scApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putOutboundTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val outboundTransferDto = ObjectMappers.mapper.readValue(createOutboundTransferResponse, TransferDto::class.java)
        val outboundTransferId = outboundTransferDto.id

        val outboundTransfer = transferRepository.getByIdOrThrow(outboundTransferId.toLong())
        assertThat(outboundTransfer.status).isEqualTo(TransferStatus.CLOSED)

        val receiveSignatureData = outboundTransfer.operations.find {
            it.type == OperationType.RECEIVE
        }!!.signatures.first().signatureData;
        assertThat(receiveSignatureData).isEqualTo("AUTO_SIGNATURE")

        val createInboundTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putInboundTransfer.json"))
                .header("apiKey", "scApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putInboundTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val inboundTransferDto = ObjectMappers.mapper.readValue(createInboundTransferResponse, TransferDto::class.java)
        val inboundTransferId = inboundTransferDto.id

        val inboundTransfer = transferRepository.getByIdOrThrow(inboundTransferId.toLong())
        assertThat(inboundTransfer.status).isEqualTo(TransferStatus.CLOSED)

        val provideSignatureData = inboundTransfer.operations.find {
            it.type == OperationType.PROVIDE
        }!!.signatures.first().signatureData;
        assertThat(provideSignatureData).isEqualTo("AUTO_SIGNATURE")

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_CALLBACK, 4)
        dbQueueTestUtil.assertQueueHasSize(QueueType.DOCUMENT_CREATE, 3)
    }

    @Test
    fun `do not send TRANSFER_ACT twice for withdraw partner`() {
        val createOutboundTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putOutboundTransferShopShop.json"))
                .header("apiKey", "shopApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putOutboundTransferShopShop.json")))
            .andReturn()
            .response
            .contentAsString
        val outboundTransferDto = ObjectMappers.mapper.readValue(createOutboundTransferResponse, TransferDto::class.java)
        val outboundTransferId = outboundTransferDto.id

        val outboundTransfer = transferRepository.getByIdOrThrow(outboundTransferId.toLong())
        assertThat(outboundTransfer.status).isEqualTo(TransferStatus.CLOSED)

        val createInboundTransferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putInboundTransferShopSC.json"))
                .header("apiKey", "scApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putInboundTransferShopSC.json")))
            .andReturn()
            .response
            .contentAsString
        val inboundTransferDto = ObjectMappers.mapper.readValue(createInboundTransferResponse, TransferDto::class.java)
        val inboundTransferId = inboundTransferDto.id

        val inboundTransfer = transferRepository.getByIdOrThrow(inboundTransferId.toLong())
        assertThat(inboundTransfer.status).isEqualTo(TransferStatus.CLOSED)

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_CALLBACK, 2)
        dbQueueTestUtil.assertQueueHasSize(QueueType.DOCUMENT_CREATE, 2)
    }

    @Test
    fun `empty transfer signature`() {
        val transferResponse = mockMvc.perform(
            put("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFileContent("request/putTransferEmptySignature.json"))
                .header("apiKey", "courierApiKey")
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(content().json(readFileContent("response/putTransfer.json")))
            .andReturn()
            .response
            .contentAsString
        val transferDto = ObjectMappers.mapper.readValue(transferResponse, TransferDto::class.java)
        val outboundTransferId = transferDto.id
        val outboundTransfer = transferRepository.getByIdOrThrow(outboundTransferId.toLong())
        assertThat(outboundTransfer.status).isEqualTo(TransferStatus.CLOSED)
    }

}

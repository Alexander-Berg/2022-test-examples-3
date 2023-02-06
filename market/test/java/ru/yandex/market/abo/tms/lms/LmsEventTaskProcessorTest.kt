package ru.yandex.market.abo.tms.lms

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.EmptyTestWithTransactionTemplate
import ru.yandex.market.abo.core.assortment.SupplierAssortmentService
import ru.yandex.market.abo.core.business.BusinessService
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.ADDRESS
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.PHONE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.SCHEDULE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.SHIPMENT_TYPE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseModerationService
import ru.yandex.market.abo.core.lms.LmsEventTask
import ru.yandex.market.abo.core.lms.LmsEventTaskService
import ru.yandex.market.abo.core.lms.LmsEventTaskType.EXPRESS_WAREHOUSE_CHANGED
import ru.yandex.market.abo.core.lms.LmsEventTaskType.WAREHOUSE_CREATED
import ru.yandex.market.abo.core.storage.json.express.moderation.JsonExpressModerationInfoService
import ru.yandex.market.abo.cpa.MbiApiService
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason.NEWBIE
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType.SUPPLIER_ASSORTMENT
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType.WAREHOUSE_MODERATION
import ru.yandex.market.abo.util.FakeUsers
import ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER
import ru.yandex.market.core.feature.model.FeatureType.DROPSHIP
import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto
import ru.yandex.market.logistics.management.entity.logbroker.EventDto
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO

/**
 * @author zilzilok
 */
class LmsEventTaskProcessorTest : EmptyTestWithTransactionTemplate() {
    private val partnerInfoDTO: PartnerInfoDTO = mock {
        on { type } doReturn SUPPLIER
    }
    private val mbiApiService: MbiApiService = mock {
        on { getPartnerInfo(SHOP_ID) } doReturn partnerInfoDTO
        on { isFeatureSuccess(SHOP_ID, DROPSHIP) } doReturn true
    }
    private val mbiApiClient: MbiApiClient = mock {
        on { getPartnerLinks(listOf(SERVICE_ID)) } doReturn
            PartnerFulfillmentLinksDTO(
                listOf(PartnerFulfillmentLinkDTO(SHOP_ID, SERVICE_ID, null, null))
            )
    }
    private val cpaOrderLimitService: CpaOrderLimitService = mock()
    private val recheckTicketManager: RecheckTicketManager = mock()
    private val businessService: BusinessService = mock()
    private val jsonExpressModerationInfoService: JsonExpressModerationInfoService = mock()
    private val lmsEventTaskService = LmsEventTaskService(mock(), mock())
    private val lmsEventTaskProcessor = LmsEventTaskProcessor(
        SupplierAssortmentService(
            mbiApiService,
            recheckTicketManager,
            jsonExpressModerationInfoService,
            transactionTemplate
        ),
        WarehouseModerationService(
            mbiApiService,
            recheckTicketManager,
            jsonExpressModerationInfoService,
            transactionTemplate
        ),
        mbiApiClient,
        cpaOrderLimitService,
        lmsEventTaskService,
        transactionTemplate,
        businessService
    )

    @Test
    fun `process task warehouse change with active SUPPLIER_ASSORTMENT ticket`() {
        val supplierAssortmentTicket =
            RecheckTicket(SUPPLIER_ASSORTMENT_TICKET_ID, SHOP_ID, SUPPLIER_ASSORTMENT, SYNOPSIS)
        whenever(recheckTicketManager.findTickets(any())).thenReturn(listOf(supplierAssortmentTicket))

        lmsEventTaskProcessor.processTasks(SERVICE_ID, getChangeTask())
        verifyUpdate(supplierAssortmentTicket)
    }

    @Test
    fun `do not process warehouse change if supplier assortment check not finished yet`() {
        whenever(recheckTicketManager.findTickets(any())).thenReturn(listOf())

        assertThrows<java.lang.IllegalStateException> {
            lmsEventTaskProcessor.processTasks(SERVICE_ID, getChangeTask())
        }
        verify(recheckTicketManager, never()).addTicketAndFireListeners(any<RecheckTicket>())
        verify(jsonExpressModerationInfoService, never()).save(any(), any())
        verify(recheckTicketManager, never()).updateTicket(any())
    }

    @Test
    fun `process warehouse change with active WAREHOUSE_MODERATION ticket`() {
        val supplierAssortmentTicket =
            RecheckTicket(SUPPLIER_ASSORTMENT_TICKET_ID, SHOP_ID, SUPPLIER_ASSORTMENT, SYNOPSIS).apply {
                status = RecheckTicketStatus.PASS
            }
        whenever(recheckTicketManager.findTickets(any())).thenReturn(listOf(supplierAssortmentTicket))

        val ticket = RecheckTicket(TICKET_ID, SHOP_ID, WAREHOUSE_MODERATION, SYNOPSIS)
        whenever(recheckTicketManager.findTickets(any())).thenReturn(listOf(ticket))

        lmsEventTaskProcessor.processTasks(SERVICE_ID, getChangeTask())
        verifyUpdate(ticket)
    }

    @Test
    fun `process warehouse change without active ticket`() {
        val supplierAssortmentTicket =
            RecheckTicket(SUPPLIER_ASSORTMENT_TICKET_ID, SHOP_ID, SUPPLIER_ASSORTMENT, SYNOPSIS).apply {
                status = RecheckTicketStatus.PASS
            }
        whenever(recheckTicketManager.findTickets(any())).thenReturn(listOf(supplierAssortmentTicket))

        val ticket = RecheckTicket(TICKET_ID, SHOP_ID, WAREHOUSE_MODERATION, SYNOPSIS)
        whenever(recheckTicketManager.addTicketAndFireListeners(any<RecheckTicket>())).thenReturn(ticket)

        lmsEventTaskProcessor.processTasks(SERVICE_ID, getChangeTask())
        verify(recheckTicketManager, times(1)).addTicketAndFireListeners(any<RecheckTicket>())
        verifyUpdate(ticket)
    }

    @Test
    fun `process warehouse creation`() {
        whenever(cpaOrderLimitService.existsLimit(SHOP_ID, DSBB, NEWBIE)).thenReturn(false)

        lmsEventTaskProcessor.processTasks(SERVICE_ID, getCreationTask())
        verify(cpaOrderLimitService, times(1)).addTemporaryLimitForNewbieIfNotExceptional(
            SHOP_ID, DSBB,
            FakeUsers.WAREHOUSE_ORDER_LIMIT_CREATOR.id
        )
    }

    private fun verifyUpdate(ticket: RecheckTicket) {
        verify(jsonExpressModerationInfoService, times(1)).save(any(), any())
        verify(recheckTicketManager, times(1)).updateTicket(ticket)
    }

    private fun getCreationTask(): List<LmsEventTask> {
        return listOf(LmsEventTask(1L, 1L, SERVICE_ID, DSBB, WAREHOUSE_CREATED))
    }

    private fun getChangeTask(): List<LmsEventTask> {
        val event =
            MAPPER.readValue(javaClass.getResourceAsStream("/logistic/update_warehouse.json"), EventDto::class.java)
        val snapshot = MAPPER.convertValue(event.entitySnapshot, BusinessWarehouseSnapshotDto::class.java)

        val body: String = lmsEventTaskService.createTaskBody(DEFAULT_CHANGES, snapshot)
        return listOf(LmsEventTask(1L, 1L, SERVICE_ID, DSBB, EXPRESS_WAREHOUSE_CHANGED, body))
    }

    companion object {
        private val MAPPER = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModules(ParameterNamesModule(), JavaTimeModule())
        private val DEFAULT_CHANGES = listOf(ADDRESS, PHONE, SCHEDULE, SHIPMENT_TYPE)
        private const val SHOP_ID = 1L
        private const val SERVICE_ID = 23L
        private const val TICKET_ID = -1L
        private const val SUPPLIER_ASSORTMENT_TICKET_ID = -2L
        private const val SYNOPSIS = "qwerty123"
    }
}

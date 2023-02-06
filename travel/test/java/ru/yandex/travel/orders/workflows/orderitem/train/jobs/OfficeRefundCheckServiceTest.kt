package ru.yandex.travel.orders.workflows.orderitem.train.jobs

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows // TODO update junit to >=4.13 and switch to its method
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import ru.yandex.travel.orders.entities.GenericOrder
import ru.yandex.travel.orders.entities.TrainOrderItem
import ru.yandex.travel.orders.management.StarTrekService
import ru.yandex.travel.orders.repository.TrainOrderItemRepository
import ru.yandex.travel.orders.repository.TrainRefundedOperationRepository
import ru.yandex.travel.orders.services.OperationTypes
import ru.yandex.travel.orders.services.train.GenericOfficeRefundStartService
import ru.yandex.travel.orders.workflows.orderitem.train.TrainWorkflowProperties
import ru.yandex.travel.train.model.TrainPassenger
import ru.yandex.travel.train.model.TrainReservation
import ru.yandex.travel.train.model.TrainTicket
import ru.yandex.travel.train.partners.im.ImClient
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderInfo
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderItem
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderItemType
import ru.yandex.travel.train.partners.im.model.orderlist.OrderListResponse
import ru.yandex.travel.workflow.WorkflowProcessService
import ru.yandex.travel.workflow.single_operation.SingleOperationService
import java.time.Clock
import java.time.Duration
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OfficeRefundCheckServiceTest {
    private val imClient: ImClient = mock()
    private val trainOrderItemRepository: TrainOrderItemRepository = mock()
    private val trainRefundedOperationRepository: TrainRefundedOperationRepository = mock()
    private val trainWorkflowProperties = TrainWorkflowProperties().also { properties ->
        properties.officeRefund = TrainWorkflowProperties.OfficeRefundProperties().also { officeRefundProperties ->
            officeRefundProperties.daysToCheck = 1
            officeRefundProperties.refundDelay = Duration.ofMinutes(20)
            officeRefundProperties.refundExtraDelay = Duration.ofMinutes(10)
        }
    }
    private val starTrekService: StarTrekService = mock()
    private val singleOperationService: SingleOperationService = mock()
    private val workflowProcessService: WorkflowProcessService = mock()

    private val officeRefundCheckService: OfficeRefundCheckService = OfficeRefundCheckService(
        { imClient },
        trainOrderItemRepository,
        trainRefundedOperationRepository,
        trainWorkflowProperties,
        starTrekService,
        singleOperationService,
        workflowProcessService,
        Clock.systemUTC(),
    )

    private val dummyOrderId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val dummyOrderItemId = UUID.fromString("1111111-1111-1111-1111-111111111111")
    private val imOrderId = 100
    private val imBuyOperationId = 200
    private val imRefundOperationId = 300

    private val dummyImOrderListResponse = OrderListResponse().also { response ->
        response.orders = listOf(
            ImShortOrderInfo().also { imOrder ->
                imOrder.orderId = imOrderId
                imOrder.orderItems = listOf(
                    ImShortOrderItem()
                        .also { imOrderItem ->
                            imOrderItem.type = ImShortOrderItemType.RAILWAY
                            imOrderItem.orderItemId = imRefundOperationId
                            imOrderItem.isExternallyLoaded = true
                            imOrderItem.previousOrderItemId = imBuyOperationId
                        },
                )
            }
        )
    }
    private val dummyOrder = GenericOrder().also { order ->
        order.id = dummyOrderId
    }
    private val dummyOrderItems = listOf(
        TrainOrderItem().also { orderItem ->
            orderItem.id = dummyOrderItemId
            orderItem.order = dummyOrder
            orderItem.reservation = TrainReservation().also { reservation ->
                reservation.passengers = listOf(
                    TrainPassenger().also { passenger ->
                        passenger.ticket = TrainTicket().also { ticket ->
                            ticket.partnerBuyOperationId = imBuyOperationId
                        }
                    },
                    TrainPassenger().also { passenger ->
                        passenger.ticket = TrainTicket().also { ticket ->
                            ticket.partnerBuyOperationId = imBuyOperationId
                        }
                    },
                )
            }
        }
    )

    @Test
    fun `start refund`() {
        whenever(imClient.orderList(any())).thenReturn(dummyImOrderListResponse)
        whenever(trainRefundedOperationRepository.excludeRefundedOperationIds(any())).thenAnswer {
            it.getArgument(0)
        }
        whenever(trainOrderItemRepository.findAllByProviderId(imOrderId.toString())).thenReturn(dummyOrderItems)

        officeRefundCheckService.sendOfficeRefundMessages("unused")

        verify(singleOperationService).scheduleOperation(
            any(),
            eq(OperationTypes.GENERIC_TRAINS_OFFICE_REFUND.value),
            eq(
                GenericOfficeRefundStartService.StartOfficeRefundData(
                    dummyOrderId,
                    listOf(
                        GenericOfficeRefundStartService.Service(
                            dummyOrderItemId,
                            setOf(imRefundOperationId),
                            null,
                        )
                    )
                )
            ),
            any()
        )
        verify(trainRefundedOperationRepository).save(argThat { operationId == imRefundOperationId })
    }

    @Test
    fun `no office refunds`() {
        whenever(imClient.orderList(any())).thenReturn(
            OrderListResponse().also { response ->
                response.orders = listOf(
                    ImShortOrderInfo().also { imOrder ->
                        imOrder.orderId = imOrderId
                        imOrder.orderItems = listOf(
                            // following cases are invalid OrderList response items and should be ignored:
                            ImShortOrderItem()
                                .also { imOrderItem ->
                                    imOrderItem.type = ImShortOrderItemType.RAILWAY
                                    imOrderItem.orderItemId = 301
                                    imOrderItem.isExternallyLoaded = false
                                },
                            ImShortOrderItem()
                                .also { imOrderItem ->
                                    imOrderItem.type = ImShortOrderItemType.INSURANCE
                                    imOrderItem.orderItemId = 302
                                    imOrderItem.isExternallyLoaded = true
                                },
                        )
                    }
                )
            }
        )

        officeRefundCheckService.sendOfficeRefundMessages("unused")

        verifyZeroInteractions(singleOperationService)
        verify(trainRefundedOperationRepository, never()).save(any())
    }

    @Test
    fun `refunded office refunds`() {
        whenever(imClient.orderList(any())).thenReturn(dummyImOrderListResponse)
        whenever(trainRefundedOperationRepository.excludeRefundedOperationIds(any())).thenReturn(emptySet())

        officeRefundCheckService.sendOfficeRefundMessages("unused")

        verifyZeroInteractions(singleOperationService)
        verify(trainRefundedOperationRepository, never()).save(any())
    }

    @Test
    fun `office refunds of unknown orders`() {
        whenever(imClient.orderList(any())).thenReturn(dummyImOrderListResponse)
        whenever(trainRefundedOperationRepository.excludeRefundedOperationIds(any())).thenAnswer {
            it.getArgument(0)
        }
        whenever(trainOrderItemRepository.findAllByProviderId(imOrderId.toString())).thenReturn(emptyList())

        val exception = assertThrows(IllegalStateException::class.java) {
            officeRefundCheckService.sendOfficeRefundMessages("unused")
        }
        assertEquals("TrainOrderItems for IM orderID 100 not found", exception.message)
        verifyZeroInteractions(singleOperationService)
        verify(trainRefundedOperationRepository, never()).save(any())
    }

    @Test
    fun `office refunds of unknown operations`() {
        whenever(imClient.orderList(any())).thenReturn(dummyImOrderListResponse)
        whenever(trainRefundedOperationRepository.excludeRefundedOperationIds(any())).thenAnswer {
            it.getArgument(0)
        }
        whenever(trainOrderItemRepository.findAllByProviderId(imOrderId.toString())).thenReturn(listOf(
            TrainOrderItem().also { orderItem ->
                orderItem.id = dummyOrderItemId
                orderItem.order = dummyOrder
                orderItem.reservation = TrainReservation().also { reservation ->
                    reservation.passengers = listOf(
                        TrainPassenger().also { passenger ->
                            passenger.ticket = TrainTicket().also { ticket ->
                                ticket.partnerBuyOperationId = 999
                            }
                        }
                    )
                }
            }
        ))

        val exception = assertThrows(IllegalStateException::class.java) {
            officeRefundCheckService.sendOfficeRefundMessages("unused")
        }
        assertEquals("TrainOrderItems for IM orderID 100 and refund operations [300] not found", exception.message)
        verifyZeroInteractions(singleOperationService)
        verify(trainRefundedOperationRepository, never()).save(any())
    }
}

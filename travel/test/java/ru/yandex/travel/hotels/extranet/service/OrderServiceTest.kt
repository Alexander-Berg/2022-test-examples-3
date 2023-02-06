package ru.yandex.travel.hotels.extranet.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.commons.proto.ProtoUtils
import ru.yandex.travel.hotels.extranet.AuthorizedHotel
import ru.yandex.travel.hotels.extranet.TDateIntervalFilter
import ru.yandex.travel.hotels.extranet.TGetOrdersReq
import ru.yandex.travel.hotels.extranet.TGetOrdersRsp
import ru.yandex.travel.hotels.extranet.entities.HotelAgreement
import ru.yandex.travel.hotels.extranet.entities.HotelIdentifier
import ru.yandex.travel.hotels.extranet.entities.User
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrder
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrderDetail
import ru.yandex.travel.hotels.extranet.entities.orders.BillingTransaction
import ru.yandex.travel.hotels.extranet.entities.orders.BoYOrder
import ru.yandex.travel.hotels.extranet.entities.orders.FinancialEvent
import ru.yandex.travel.hotels.extranet.repository.BankOrdersRepository
import ru.yandex.travel.hotels.extranet.repository.OrdersRepository
import ru.yandex.travel.hotels.extranet.service.hotels.HotelInfoService
import ru.yandex.travel.hotels.extranet.service.invitations.InvitationServiceImpl
import ru.yandex.travel.hotels.extranet.service.orders.OrdersService
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleService
import ru.yandex.travel.hotels.proto.EPartnerId
import ru.yandex.travel.hotels.proto.THotelId
import ru.yandex.travel.orders.commons.proto.TPage
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
open class OrderServiceTest {
    private val HOTEL_ID = "hotel1"

    @Autowired
    lateinit var repository: OrdersRepository

    @Autowired
    lateinit var bankOrdersRepository: BankOrdersRepository

    @MockBean
    lateinit var userRoleService: UserRoleService

    @Autowired
    lateinit var service: OrdersService

    @MockBean
    lateinit var hotelInfoService: HotelInfoService

    lateinit var order1: String
    lateinit var order2: String
    lateinit var orderCancelled: String

    val ordersIdsList: String
        get() = "order1=${order1}, order2=${order2}, orderCancelled=${orderCancelled}"

    @BeforeEach
    @Transactional
    open fun setUp() {
        repository.deleteAll()
        repository.flush()
        bankOrdersRepository.deleteAll()
        bankOrdersRepository.flush()

        order1 = UUID.randomUUID().toString()
        order2 = UUID.randomUUID().toString()
        orderCancelled = UUID.randomUUID().toString()

        val hotelAgreement = HotelAgreement(0, HotelIdentifier(EPartnerId.PI_BNOVO, HOTEL_ID))
        repository.save(
            BoYOrder(
                id = order1,
                prettyId = "YA-0000-0000-0000",
                orderCreatedAt = Instant.parse("2000-01-01T01:01:01Z"),
                checkInDate = LocalDate.parse("2000-01-01"),
                checkOutDate = LocalDate.parse("2000-01-02"),
                hotelAgreement = hotelAgreement,
            )
        )
        repository.save(
            BoYOrder(
                id = order2,
                prettyId = "YA-0000-0000-0001",
                orderCreatedAt = Instant.parse("2001-01-01T01:01:01Z"),
                checkInDate = LocalDate.parse("2001-01-01"),
                checkOutDate = LocalDate.parse("2002-01-02"),
                hotelAgreement = hotelAgreement,
            )
        )
        repository.save(
            BoYOrder(
                id = orderCancelled,
                prettyId = "YA-0000-0000-0002",
                orderCreatedAt = Instant.parse("2000-02-01T01:01:01Z"),
                orderCancelledAt = Instant.parse("2000-02-02T01:01:01Z"),
                checkInDate = LocalDate.parse("2000-02-01"),
                checkOutDate = LocalDate.parse("2000-02-02"),
                hotelAgreement = hotelAgreement,
            )
        )
        given(userRoleService.checkPermission(any(), any(), any(), any())).willReturn(User(0))
        given(hotelInfoService.getHotelInfo(any())).willReturn(AuthorizedHotel.newBuilder()
            .setHotelId(HotelIdentifier(EPartnerId.PI_BNOVO, HOTEL_ID).toProto())
            .build())
    }

    @Test
    fun `By default all results are returned sorted in descending order by some date, I guess`() {
        val orders = query()
        assertEquals(3, orders.totalRecords)

        assertEquals(order2, orders.resultList[0].id) { ordersIdsList }
        assertEquals(orderCancelled, orders.resultList[1].id) { ordersIdsList }
        assertEquals(order1, orders.resultList[2].id) { ordersIdsList }
    }

    @Test
    fun `Filter by check in date`() {
        val orders = query() {
            it.checkInDate = dateFilter("2000-10-10")
            it
        }
        assertEquals(1, orders.totalRecords)

        assertEquals(order2, orders.resultList[0].id) { ordersIdsList }
    }

    @Test
    fun `Filter by check out date`() {
        val orders = query() {
            it.checkOutDate = dateFilter("2000-01-01", "2000-10-10")
            it
        }
        assertEquals(2, orders.totalRecords)

        assertEquals(orderCancelled, orders.resultList[0].id) { ordersIdsList }
        assertEquals(order1, orders.resultList[1].id) { ordersIdsList }
    }

    @Test
    fun `Filter by ordered at date`() {
        val orders = query() {
            it.orderedAt = dateFilter("2000-01-31", "2000-03-01")
            it
        }
        assertEquals(1, orders.totalRecords)

        assertEquals(orderCancelled, orders.resultList[0].id) { ordersIdsList }
    }

    @Test
    fun `Filter by cancelled at date`() {
        val orders = query() {
            it.cancelledAt = dateFilter("2000-01-01", "2010-03-01")
            it
        }
        assertEquals(1, orders.totalRecords)

        assertEquals(orderCancelled, orders.resultList[0].id) { ordersIdsList }
    }

    @Test
    fun `Filter by cancelled at date and ordered at date`() {
        val orders = query() {
            it.orderedAt = dateFilter("2000-01-31", "2000-03-01")
            it.cancelledAt = dateFilter("2000-01-01", "2010-03-01")
            it
        }
        assertEquals(1, orders.totalRecords)

        assertEquals(orderCancelled, orders.resultList[0].id) { ordersIdsList }
    }

    @Test
    fun `Filter by ordered at so no orders is found`() {
        val orders = query() {
            it.orderedAt = dateFilter("1980-01-31", "1999-03-01")
            it
        }
        assertEquals(0, orders.totalRecords)
    }

    @Test
    @Transactional
    open fun `Check that the response is enriched with bank info if the information is present`() {
        // prepare
        val ytId = 100L
        val billingContractId = 10L
        val order = repository.getOne(order1)
        val fe = FinancialEvent(
            order = order,
            billingContractId = billingContractId,
        )
        val bt = BillingTransaction(
            financialEvent = fe,
            ytId = ytId,
        )
        fe.billingTransactions.add(bt)
        order.financialEvents.add(fe)
        repository.saveAndFlush(order)

        val bankOrderId = "1234"
        val bankOrder = BankOrder(
            bankOrderId = bankOrderId,
        )
        bankOrder.bankOrderDetails.add(
            BankOrderDetail(
                bankOrder = bankOrder,
                ytId = ytId,
            )
        )
        bankOrdersRepository.saveAndFlush(bankOrder)

        // query
        val rsp = query().resultList.find { it.id == order1 }!!

        // assert
        assertEquals(1, rsp.bankOrderInfoCount)
        assertEquals(bankOrderId, rsp.bankOrderInfoList[0].bankOrderId)
    }

    private fun query(fn: (req: TGetOrdersReq.Builder) -> TGetOrdersReq.Builder = { it }): TGetOrdersRsp {
        return service.getOrders(
            fn(
                TGetOrdersReq.newBuilder()
                    .setPage(TPage.newBuilder().setNum(0).setSize(10).build())
                    .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BNOVO).setOriginalId(HOTEL_ID).build())
            )
                .build()
        )
    }
}

fun dateFilter(from: String, till: String? = null): TDateIntervalFilter {
    val builder = TDateIntervalFilter.newBuilder()
    builder.from = ProtoUtils.toTDate(LocalDate.parse(from))
    till?.let { builder.till = ProtoUtils.toTDate(LocalDate.parse(till)) }

    return builder.build()
}

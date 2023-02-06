package ru.yandex.travel.hotels.extranet.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.hotels.extranet.TGetBankOrderInfoReq
import ru.yandex.travel.hotels.extranet.TGetBankOrderInfoRsp
import ru.yandex.travel.hotels.extranet.entities.Hotel
import ru.yandex.travel.hotels.extranet.entities.HotelAgreement
import ru.yandex.travel.hotels.extranet.entities.HotelIdentifier
import ru.yandex.travel.hotels.extranet.entities.HotelManagementSource
import ru.yandex.travel.hotels.extranet.entities.Organization
import ru.yandex.travel.hotels.extranet.entities.User
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrder
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrderDetail
import ru.yandex.travel.hotels.extranet.entities.orders.FETCHED_STATUS
import ru.yandex.travel.hotels.extranet.repository.BankOrdersRepository
import ru.yandex.travel.hotels.extranet.repository.HotelAgreementRepository
import ru.yandex.travel.hotels.extranet.repository.HotelRepository
import ru.yandex.travel.hotels.extranet.service.bankorders.BankOrdersServiceImpl
import ru.yandex.travel.hotels.extranet.service.roles.UserRoleService
import ru.yandex.travel.hotels.proto.EPartnerId
import ru.yandex.travel.hotels.proto.THotelId
import ru.yandex.travel.orders.commons.proto.TPage
import java.math.BigDecimal
import java.time.LocalDate
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
open class BankOrderServiceTest {

    private val HOTEL_ID = "hotel_bank_test"

    private var hotelAgreement: HotelAgreement? = null

    @Autowired
    lateinit var repository: BankOrdersRepository

    @Autowired
    lateinit var hotelAgreementRepository: HotelAgreementRepository

    @MockBean
    lateinit var userRoleService: UserRoleService

    @MockBean
    lateinit var hotelRepository: HotelRepository

    @Autowired
    lateinit var service: BankOrdersServiceImpl

    @BeforeEach
    @Transactional
    open fun setUp() {
        repository.deleteAll()

        var bankOrder = BankOrder(
            paymentBatchId = "1",
            bankOrderId = "1",
            sum = BigDecimal("100"),
            eventTime = LocalDate.parse("2000-01-01"),
            bankOrderDetails = mutableListOf(
                BankOrderDetail(
                    id = 1,
                    contractId = 1,
                )
            ),
            fetchStatus = FETCHED_STATUS,
        )
        bankOrder.bankOrderDetails[0].bankOrder = bankOrder
        repository.save(
            bankOrder
        )
        bankOrder = BankOrder(
            paymentBatchId = "2",
            bankOrderId = "2",
            sum = BigDecimal("100"),
            eventTime = LocalDate.parse("2001-01-01"),
            bankOrderDetails = mutableListOf(
                BankOrderDetail(
                    id = 2,
                    contractId = 1
                )
            ),
            fetchStatus = FETCHED_STATUS,
        )
        bankOrder.bankOrderDetails[0].bankOrder = bankOrder
        repository.save(bankOrder)

        hotelAgreement = hotelAgreementRepository.save(
            HotelAgreement(
                financialContractId = 1,
                hotelIdentifier = HotelIdentifier(EPartnerId.PI_BNOVO, HOTEL_ID)
            )
        )
        given(userRoleService.checkPermission(any(), any(), any(), any())).willReturn(User(0))
        given(hotelRepository.findHotelByPartnerHotelId(any())).willReturn(
            Hotel(
                Organization("foo"), "bar", managedBy = HotelManagementSource.BNOVO,
                partnerHotelId = HotelIdentifier(EPartnerId.PI_BNOVO, HOTEL_ID)
            )
        )
    }

    @AfterEach
    @Transactional
    open fun cleanUp() {
        hotelAgreementRepository.delete(hotelAgreement!!)
    }

    @Test
    fun `No extra parameters returns all results`() {
        val orders = query()
        assertEquals(2, orders.totalRecords)

        assertEquals("2", orders.resultList[0].paymentBatchId)
        assertEquals("1", orders.resultList[1].paymentBatchId)
    }

    @Test
    fun `Can filter by event time`() {
        val orders = query() {
            it.eventTime = dateFilter("1999-12-01", "2000-12-01")
            it
        }
        assertEquals(1, orders.totalRecords)

        assertEquals("1", orders.resultList[0].paymentBatchId)
    }

    private fun query(fn: (req: TGetBankOrderInfoReq.Builder) -> TGetBankOrderInfoReq.Builder = { it }):
        TGetBankOrderInfoRsp {
        return service.getBankOrders(
            fn(
                TGetBankOrderInfoReq.newBuilder()
                    .setPage(TPage.newBuilder().setNum(0).setSize(10).build())
                    .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BNOVO).setOriginalId(HOTEL_ID).build())
            )
                .build()
        )
    }
}

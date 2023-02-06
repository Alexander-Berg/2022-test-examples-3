package ru.yandex.travel.hotels.extranet.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import ru.yandex.travel.hotels.extranet.AuthorizedHotel
import ru.yandex.travel.hotels.extranet.EBankOrderTransactionTransactionType.BTTT_PAYMENT
import ru.yandex.travel.hotels.extranet.TGetBankOrderDetailsReq
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrder
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrderDetailWebView
import ru.yandex.travel.hotels.extranet.entities.orders.BankOrderDetailsWebViewIdx
import ru.yandex.travel.hotels.extranet.repository.BankOrderDetailsWebViewRepository
import ru.yandex.travel.hotels.extranet.repository.BankOrdersRepository
import ru.yandex.travel.hotels.extranet.service.bankorders.BankOrdersServiceImpl
import ru.yandex.travel.hotels.extranet.service.hotels.HotelInfoService
import ru.yandex.travel.hotels.extranet.service.hotels.HotelInfoServiceImpl
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
open class BankOrderDetailsServiceTest {

    @Autowired
    lateinit var repository: BankOrderDetailsWebViewRepository

    @Autowired
    lateinit var bankOrderRepo: BankOrdersRepository

    @Autowired
    lateinit var service: BankOrdersServiceImpl

    @MockBean
    lateinit var hotelInfoService: HotelInfoService

    @BeforeEach
    open fun setUp() {
        repository.deleteAll()
        bankOrderRepo.deleteAll()
        given(hotelInfoService.getHotelInfo(any())).willReturn(AuthorizedHotel.getDefaultInstance())
    }

    @Test
    open fun `bank order details repo returns the entity with nullable fields`() {
        val paymentBatchId = "123"
        val bankOrderId = "321"
        repository.saveAndFlush(
            BankOrderDetailWebView(
                id = BankOrderDetailsWebViewIdx(paymentBatchId, bankOrderId, "123-123-123", BTTT_PAYMENT),
                paidAmount = BigDecimal.TEN,
                prettyId = "Ya-Krevedko",
                orderCreatedAt = Instant.now(),
                checkInDate = LocalDate.now(),
                checkOutDate = LocalDate.now().plusDays(2),
            )
        )
        bankOrderRepo.saveAndFlush(BankOrder(
            paymentBatchId = paymentBatchId,
            bankOrderId = bankOrderId,
            status = "fetched",
            sum = BigDecimal.TEN,
        ))
        val bankOrderDetails = service.getBankOrderDetails(
            TGetBankOrderDetailsReq.newBuilder()
                .setPaymentBatchId(paymentBatchId)
                .build()
        )
        Assertions.assertEquals(1, bankOrderDetails.resultCount)
    }
}

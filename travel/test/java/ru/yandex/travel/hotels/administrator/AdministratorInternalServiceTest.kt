package ru.yandex.travel.hotels.administrator

import io.grpc.stub.StreamObserver
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.travel.hotels.administrator.entity.HotelConnection
import ru.yandex.travel.hotels.administrator.entity.LegalDetails
import ru.yandex.travel.hotels.administrator.entity.UnpublishedReason
import ru.yandex.travel.hotels.administrator.grpc.proto.EInternalUnpublishedReason
import ru.yandex.travel.hotels.administrator.grpc.proto.GetHotelConnectionReq
import ru.yandex.travel.hotels.administrator.grpc.proto.GetHotelConnectionRsp
import ru.yandex.travel.hotels.administrator.repository.HotelConnectionRepository
import ru.yandex.travel.hotels.administrator.repository.LegalDetailsRepository
import ru.yandex.travel.hotels.administrator.workflow.proto.EHotelConnectionState
import ru.yandex.travel.hotels.administrator.workflow.proto.ELegalDetailsState
import ru.yandex.travel.hotels.proto.EPartnerId
import ru.yandex.travel.workflow.entities.Workflow
import ru.yandex.travel.workflow.repository.WorkflowRepository
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AdministratorInternalServiceTest
{
    @Autowired lateinit var administratorInternalService: AdministratorInternalService
    @Autowired lateinit var hotelConnectionRepository: HotelConnectionRepository
    @Autowired lateinit var legalDetailsRepository: LegalDetailsRepository
    @Autowired lateinit var workflowRepository: WorkflowRepository
    @Autowired lateinit var transactionTemplate: TransactionTemplate

    @Mock lateinit var streamObserver: StreamObserver<GetHotelConnectionRsp>

    @Before
    fun init() {
        transactionTemplate.execute<Any?> {
            hotelConnectionRepository.saveAndFlush(hotelConnection())
        }
    }

    @After
    fun tearDown() {
        transactionTemplate.execute<Any?> {
            hotelConnectionRepository.deleteAll()
        }
    }

    @Test
    fun testGetHotelConnectionDetailsNotFound() {
        administratorInternalService.getHotelConnection(
            GetHotelConnectionReq.newBuilder()
                .setPartnerId(EPartnerId.PI_TRAVELLINE)
                .setHotelCode("nonexistent hotel code").build(),
            streamObserver)
        Mockito.verify(streamObserver).onNext(ArgumentMatchers.argThat { response: GetHotelConnectionRsp ->
            Assert.assertEquals(EHotelConnectionState.CS_UNKNOWN, response.connectionState)
            true
        })
    }

    @Test
    fun testGetHotelConnection() {
        administratorInternalService.getHotelConnection(
            GetHotelConnectionReq.newBuilder()
                .setPartnerId(EPartnerId.PI_TRAVELLINE)
                .setHotelCode(HOTEL_CODE).build(),
            streamObserver)
        Mockito.verify(streamObserver).onNext(ArgumentMatchers.argThat { resp: GetHotelConnectionRsp ->
            Assert.assertEquals(resp.connectionState, EHotelConnectionState.CS_NEW)
            Assert.assertEquals(resp.permalink, PERMALINK)
            Assert.assertEquals(resp.hotelName, HOTEL_NAME)
            Assert.assertEquals(resp.stTicket, ST_TICKET)
            Assert.assertEquals(resp.paperAgreement, false)
            Assert.assertEquals(resp.unpublishedReason, EInternalUnpublishedReason.IUR_AGENCY)
            Assert.assertEquals(resp.accountantEmailsList, listOf("email1@yandex.ru", "email2@gmail.com"))
            Assert.assertEquals(resp.contractPersonEmailsList, listOf("email42@yandex.ru"))
            Assert.assertEquals(resp.billingClientId, 42)
            Assert.assertEquals(resp.billingContractId, 4343)
            true
        })
    }

    private fun hotelConnection(): HotelConnection {
        val legalDetails = LegalDetails()
        legalDetails.id = UUID.randomUUID()
        legalDetails.balanceClientId = 42
        legalDetails.balanceContractId = 4343
        legalDetails.inn = "random42"
        legalDetails.bic = "random42"
        legalDetails.paymentAccount = "random42"
        legalDetails.paymentAccount = "random42"
        legalDetails.state = ELegalDetailsState.DS_REGISTERED
        legalDetails.partnerId = EPartnerId.PI_TRAVELLINE
        legalDetailsRepository.save(legalDetails)

        val hotelConn = HotelConnection()
        hotelConn.id = UUID.randomUUID()
        hotelConn.hotelCode = HOTEL_CODE
        hotelConn.permalink = PERMALINK
        hotelConn.hotelName = HOTEL_NAME
        hotelConn.partnerId = EPartnerId.PI_TRAVELLINE
        hotelConn.state = EHotelConnectionState.CS_NEW
        hotelConn.stTicket = ST_TICKET
        hotelConn.paperAgreement = false
        hotelConn.unpublishedReason = UnpublishedReason.AGENCY
        hotelConn.accountantEmail = "email1@yandex.ru;email2@gmail.com"
        hotelConn.contractPersonEmail = "email42@yandex.ru"

        hotelConn.legalDetails = legalDetails

        workflowRepository.save(Workflow.createWorkflowForEntity(hotelConn))
        return hotelConn
    }

    companion object {
        private const val HOTEL_CODE = "123"
        private const val PERMALINK = 1234L
        private const val HOTEL_NAME = "HotelName"
        private const val ST_TICKET = "ST-TICKET"
    }
}

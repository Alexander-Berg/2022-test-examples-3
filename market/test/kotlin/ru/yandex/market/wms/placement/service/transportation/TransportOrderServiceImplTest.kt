package ru.yandex.market.wms.placement.service.transportation

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.dto.CancellationTransportOrderConsumerNotificationDto
import ru.yandex.market.wms.core.base.response.GetParentContainerResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.placement.exception.UserAlreadyHasActiveOrderException
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.placement.service.transportation.async.CancelTransportationOrdersProducer
import ru.yandex.market.wms.placement.service.transportation.impl.ConveyorIdServiceImpl
import ru.yandex.market.wms.placement.service.transportation.impl.TransportOrderServiceImpl
import ru.yandex.market.wms.receiving.client.ReceivingClient
import ru.yandex.market.wms.transportation.client.TransportationClient
import ru.yandex.market.wms.transportation.core.model.response.GetTransportOrdersResponse
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent

internal class TransportOrderServiceImplTest {

    @Mock
    private lateinit var coreService: CoreService

    @Mock
    private lateinit var coreClient: CoreClient

    @Mock
    private lateinit var transportationClient: TransportationClient

    @Mock
    private lateinit var receivingClient: ReceivingClient

    @Mock
    private lateinit var defaultJmsTemplate: JmsTemplate

    @Mock
    private lateinit var dbConfigService: DbConfigService

    @Mock
    private lateinit var userProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider

    @InjectMocks
    private lateinit var cancellationCommandProducer: CancelTransportationOrdersProducer

    private lateinit var conveyorIdService: ConveyorIdService
    private lateinit var transportOrderService: TransportOrderService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        conveyorIdService = ConveyorIdServiceImpl()
        transportOrderService =
            TransportOrderServiceImpl(
                conveyorIdService,
                cancellationCommandProducer,
                transportationClient,
                coreClient,
                receivingClient,
                coreService,
                userProvider
            )
        `when`(userProvider.user).thenReturn("test_user")
    }

    @Test
    fun sendCancellationCommandWhenIdIsConveyor() {
        `when`(dbConfigService.getConfig(anyString())).thenReturn("")

        val conveyorIdList = listOf("VS001", "TM002", "BL003", "BM004")
        val notConveyorIdList = listOf("001VS", "CART003", null, "VM333", "PLT1")

        transportOrderService.cancelTransportOrderByContainerId(conveyorIdList + notConveyorIdList)

        verify(defaultJmsTemplate, times(conveyorIdList.size))
            .convertAndSend(
                anyString(),
                any(CancellationTransportOrderConsumerNotificationDto::class.java)
            )
    }

    @Test
    fun createTransportOrderThrowsExceptionWhenUserBusy() {
        val fakeId = "fakeId"
        val fakeResponseContent = TransportOrderResourceContent.builder().id(fakeId).build()
        val fakeResponse = GetTransportOrdersResponse(listOf(fakeResponseContent))

        `when`(coreService.isIdExists(anyString())).thenReturn(true)
        `when`(coreClient.getParentContainer(anyString())).thenReturn(GetParentContainerResponse(null))
        `when`(transportationClient.getIncompleteTransportOrdersByUser(userProvider.user)).thenReturn(fakeResponse)

        assertThatThrownBy { transportOrderService.createTransportOrderById(fakeId, false) }
            .isInstanceOf(UserAlreadyHasActiveOrderException::class.java)
    }

    @Test
    fun createTransportOrderThrowsExceptionWhenContainerNested() {
        val fakeId = "fakeId"
        val fakeParentId = "fakeParentId"
        val fakeResponse = GetTransportOrdersResponse(listOf())

        `when`(coreService.isIdExists(anyString())).thenReturn(true)
        `when`(coreClient.getParentContainer(anyString())).thenReturn(GetParentContainerResponse(fakeParentId))
        `when`(transportationClient.getIncompleteTransportOrdersByUser(userProvider.user)).thenReturn(fakeResponse)

        assertThatThrownBy { transportOrderService.createTransportOrderById(fakeId, false) }
            .hasMessage("400 BAD_REQUEST \"Тара $fakeId вложена в $fakeParentId, сканируйте ее\"")
    }
}

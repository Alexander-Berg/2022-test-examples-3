package ru.yandex.market.wms.core.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.SortationStationDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDao
import ru.yandex.market.wms.common.spring.service.LotIdService
import ru.yandex.market.wms.common.spring.service.PickDetailService
import ru.yandex.market.wms.common.spring.service.SerialInventoryService
import ru.yandex.market.wms.core.async.lost.RemoveFromWaveProducer
import ru.yandex.market.wms.core.base.dto.ServiceType
import ru.yandex.market.wms.core.base.request.MoveToLostData
import ru.yandex.market.wms.core.base.request.MoveToLostRequest
import ru.yandex.market.wms.core.base.request.RemoveFromWaveRequest
import ru.yandex.market.wms.core.base.request.ReserveWaveRequest
import ru.yandex.market.wms.core.logging.ShortageLogger
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.time.Clock

class MoveToLostPackingServiceTest(
    @Autowired private val serialInventoryService: SerialInventoryService,
    @Autowired private val pickDetailDao: PickDetailDao,
    @Autowired private val userActivityDao: UserActivityDao,
    @Autowired private val taskDetailDao: TaskDetailDao,
    @Autowired private val pickDetailService: PickDetailService,
    @Autowired private val orderDetailDao: OrderDetailDao,
    @Autowired private val orderDao: OrderDao,
    @Autowired private val clock: Clock,
    @Autowired private val lotIdService: LotIdService,
    @Autowired private val sortationStationDetailDao: SortationStationDetailDao,
    @Autowired private val lotLocIdDao: LotLocIdDao,
    @Autowired private val waveDao: WaveDao,
    @Autowired private val shortageLogger: ShortageLogger
) : IntegrationTest() {
    private val mockJmsTemplate = Mockito.mock(JmsTemplate::class.java)
    private val moveToLostService = MoveToLostPackingService(
        serialInventoryService,
        pickDetailDao,
        userActivityDao,
        taskDetailDao,
        clock,
        lotIdService,
        orderDao,
        pickDetailService,
        orderDetailDao,
        RemoveFromWaveProducer(mockJmsTemplate),
        sortationStationDetailDao,
        lotLocIdDao,
        waveDao,
        shortageLogger
    )

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/packing/short-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/packing/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short items on packing`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PACKING,
                data = MoveToLostData(null, null, null, null, null, user, setOf("100001", "100003"))
            ), clock.millis()
        )
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.REMOVE_FROM_WAVE,
            RemoveFromWaveRequest("WAVE0001", setOf("ORD001", "ORD002"), user)
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/packing/short-after.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/packing/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short item on packing duplicated message came`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PACKING,
                data = MoveToLostData(null, null, null, null, null, user, setOf("100001", "100003"))
            ), clock.millis()
        )
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.REMOVE_FROM_WAVE,
            RemoveFromWaveRequest::class.java
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/packing/no-serials-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/packing/no-serials-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short items on packing when serials not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PACKING,
                data = MoveToLostData(null, null, null, null, null, user, setOf("111111", "222222"))
            ), clock.millis()
        )
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.REMOVE_FROM_WAVE,
            RemoveFromWaveRequest::class.java
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/packing/no-pick-details-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/packing/no-pick-details-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short items on packing when pick details not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PACKING,
                data = MoveToLostData(null, null, null, null, null, user, setOf("100001", "100003"))
            ), clock.millis()
        )
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest::class.java
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/packing/short-big-withdrawal-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/packing/short-big-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short items in big withdrawal without removing it from wave`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PACKING,
                data = MoveToLostData(null, null, null, null, null, user, setOf("100001"))
            ), clock.millis()
        )
        mockJmsTemplate.verifyNothingSent(QueueNameConstants.REMOVE_FROM_WAVE, RemoveFromWaveRequest::class.java)
    }
}

package ru.yandex.market.wms.core.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao
import ru.yandex.market.wms.common.spring.service.LotIdService
import ru.yandex.market.wms.common.spring.service.SerialInventoryService
import ru.yandex.market.wms.core.async.lost.ReserveWaveTaskProducer
import ru.yandex.market.wms.core.async.lost.ShortInventoryTaskProducer
import ru.yandex.market.wms.core.base.dto.ServiceType
import ru.yandex.market.wms.core.base.request.MoveToLostData
import ru.yandex.market.wms.core.base.request.MoveToLostRequest
import ru.yandex.market.wms.core.base.request.ReserveWaveRequest
import ru.yandex.market.wms.core.logging.ShortageLogger
import ru.yandex.market.wms.core.service.ScanningOperationLog
import ru.yandex.market.wms.inventorization.core.model.ShortInventoryTaskRequest
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.time.Clock

class MoveToLostPickingServiceTest(
    @Autowired private val serialInventoryService: SerialInventoryService,
    @Autowired private val pickDetailDao: PickDetailDao,
    @Autowired private val userActivityDao: UserActivityDao,
    @Autowired private val taskDetailDao: TaskDetailDao,
    @Autowired private val lotDao: LotDao,
    @Autowired private val orderDetailDao: OrderDetailDao,
    @Autowired private val orderDao: OrderDao,
    @Autowired private val clock: Clock,
    @Autowired private val lotIdService: LotIdService,
    @Autowired private val shortageLogger: ShortageLogger,
    @Autowired private val scanningOperationLog: ScanningOperationLog
) : IntegrationTest() {
    private val mockJmsTemplate = Mockito.mock(JmsTemplate::class.java)
    private val moveToLostService = MoveToLostPickingService(
        serialInventoryService,
        pickDetailDao,
        userActivityDao,
        taskDetailDao,
        clock,
        lotIdService,
        orderDao,
        scanningOperationLog,
        lotDao,
        orderDetailDao,
        ReserveWaveTaskProducer(mockJmsTemplate),
        ShortInventoryTaskProducer(mockJmsTemplate),
        shortageLogger
    )

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/picking/short-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/picking/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short item on picking`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
            service = ServiceType.PICKING,
            data = MoveToLostData(sku, storerKey, lot, picking_loc, picking_id, user, null)
        ), clock.millis())
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest("WAVE0001", "ASGN01", user)
        )
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.SHORT_INVENTORY_TASK,
            ShortInventoryTaskRequest("TDK1", picking_loc, sku, lot, 2, user, clock.instant())
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/picking/short-after.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/picking/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short item on picking duplicated message came`() {
        moveToLostService.moveToLost(MoveToLostRequest(
            service = ServiceType.PICKING,
            data = MoveToLostData(sku, storerKey, lot, picking_loc, picking_id, user, null)
        ), clock.millis())
        mockJmsTemplate.verifyNothingSent(QueueNameConstants.RESERVE_WAVE, ReserveWaveRequest::class.java)
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.SHORT_INVENTORY_TASK,
            ShortInventoryTaskRequest::class.java
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/picking/no-serials-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/picking/no-serials-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short item on picking when serials not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PICKING,
                data = MoveToLostData(sku, storerKey, lot, picking_loc, picking_id, user, null)
            ), clock.millis())
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest("WAVE0001", "ASGN01", user)
        )
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.SHORT_INVENTORY_TASK,
            ShortInventoryTaskRequest::class.java
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/picking/no-pick-details-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/picking/no-pick-details-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short item on picking when pick details not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PICKING,
                data = MoveToLostData(sku, storerKey, lot, picking_loc, picking_id, user, null)
            ), clock.millis())
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest::class.java
        )
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.SHORT_INVENTORY_TASK,
            ShortInventoryTaskRequest("", picking_loc, sku, lot, 2, user, clock.instant())
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/picking/short-with-id-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/picking/short-with-id-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short item with id on picking`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.PICKING,
                data = MoveToLostData(sku, storerKey, lot, picking_loc, picking_id01, user, null)
            ), clock.millis())
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest("WAVE0001", "ASGN01", user)
        )
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.SHORT_INVENTORY_TASK,
            ShortInventoryTaskRequest("TDK1", picking_loc, sku, lot, 2, user, clock.instant())
        )
    }
}

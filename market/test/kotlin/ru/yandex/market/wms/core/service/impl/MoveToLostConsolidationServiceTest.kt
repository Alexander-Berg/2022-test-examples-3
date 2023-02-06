package ru.yandex.market.wms.core.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao
import ru.yandex.market.wms.common.spring.service.LotIdService
import ru.yandex.market.wms.common.spring.service.SerialInventoryService
import ru.yandex.market.wms.core.async.lost.ReserveWaveTaskProducer
import ru.yandex.market.wms.core.base.dto.ServiceType
import ru.yandex.market.wms.core.base.request.MoveToLostData
import ru.yandex.market.wms.core.base.request.MoveToLostRequest
import ru.yandex.market.wms.core.base.request.ReserveWaveRequest
import ru.yandex.market.wms.core.logging.ShortageLogger
import ru.yandex.market.wms.core.service.ScanningOperationLog
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.time.Clock

class MoveToLostConsolidationServiceTest(
    @Autowired private val serialInventoryService: SerialInventoryService,
    @Autowired private val pickDetailDao: PickDetailDao,
    @Autowired private val userActivityDao: UserActivityDao,
    @Autowired private val taskDetailDao: TaskDetailDao,
    @Autowired private val orderDetailDao: OrderDetailDao,
    @Autowired private val orderDao: OrderDao,
    @Autowired private val clock: Clock,
    @Autowired private val lotIdService: LotIdService,
    @Autowired private val shortageLogger: ShortageLogger,
    @Autowired private val scanningOperationLog: ScanningOperationLog
) : IntegrationTest() {
    private val mockJmsTemplate = Mockito.mock(JmsTemplate::class.java)
    private val moveToLostService = MoveToLostConsolidationService(
        serialInventoryService,
        pickDetailDao,
        userActivityDao,
        taskDetailDao,
        clock,
        lotIdService,
        orderDao,
        scanningOperationLog,
        orderDetailDao,
        ReserveWaveTaskProducer(mockJmsTemplate),
        shortageLogger
    )

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/consolidation/short-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/consolidation/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short cart on consolidation`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
            service = ServiceType.CONSOLIDATION,
            data = MoveToLostData(null, null, null, consolidation_loc, consolidation_id, user, null)
        ), clock.millis())
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest("WAVE0001", "ASGN01", user)
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/consolidation/short-after.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/consolidation/short-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short item on consolidation duplicated message came`() {
        moveToLostService.moveToLost(MoveToLostRequest(
            service = ServiceType.CONSOLIDATION,
            data = MoveToLostData(null, null, null, consolidation_loc, consolidation_id, user, null)
        ), clock.millis())
        mockJmsTemplate.verifyNothingSent(QueueNameConstants.RESERVE_WAVE, ReserveWaveRequest::class.java)
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/consolidation/no-serials-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/consolidation/no-serials-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `short cart on consolidation when serials not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.CONSOLIDATION,
                data = MoveToLostData(null, null, null, consolidation_loc, consolidation_id, user, null)
            ), clock.millis())
        mockJmsTemplate.verifyMessageSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest("WAVE0001", "ASGN01", user)
        )
    }

    @Test
    @DatabaseSetup(
        "/service/move-to-lost/common.xml",
        "/service/move-to-lost/consolidation/no-pick-details-before.xml"
    )
    @ExpectedDatabase(
        value = "/service/move-to-lost/consolidation/no-pick-details-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters = [ItrnKeyFilter::class]
    )
    fun `short cart on consolidation when pick details not found`() {
        moveToLostService.moveToLost(
            MoveToLostRequest(
                service = ServiceType.CONSOLIDATION,
                data = MoveToLostData(null, null, null, consolidation_loc, consolidation_id, user, null)
            ), clock.millis())
        mockJmsTemplate.verifyNothingSent(
            QueueNameConstants.RESERVE_WAVE,
            ReserveWaveRequest::class.java
        )
    }
}

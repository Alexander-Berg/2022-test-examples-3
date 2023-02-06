package ru.yandex.market.dsm.dbqueue.tracker

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.config.props.SelfemployedTrackerProperties
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.startrek.client.Transitions
import ru.yandex.startrek.client.model.IssueUpdate

class TrackerSelfemployedChangeStatusTest : AbstractTest() {

    @Autowired
    private lateinit var trackerSelfemployedChangeStatusProducer: TrackerSelfemployedChangeStatusProducer

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var selfemployedTrackerProperties: SelfemployedTrackerProperties

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var transitions: Transitions

    @AfterEach
    fun afterEach() {
        Mockito.reset(transitions)
    }

    @Test
    fun testCloseTicke() {
        Mockito.`when`(transitions.execute(Mockito.anyString(), Mockito.anyString(), Mockito.any<IssueUpdate>()))
            .thenReturn(null)

        val courierCommand = courierTestFactory.generateCreateCommand()
        courierCommand.status = CourierStatus.NEWBIE
        dsmCommandService.handle(courierCommand)
        dsmCommandService.handle(CourierBaseCommand.UpdateStatus(courierCommand.id, CourierStatus.ACTIVE))
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.TRACKER_SELFEMPLOYED_CHANGE_STATUS)

        Mockito.verify(transitions, Mockito.times(1)).execute(
            Mockito.eq(courierCommand.createTicket),
            Mockito.eq(selfemployedTrackerProperties.statusKeyClose),
            Mockito.any<IssueUpdate>()
        )
    }

    @Test
    fun testChangeStatus() {
        val key = "A"
        Mockito.`when`(transitions.execute(Mockito.anyString(), Mockito.anyString())).thenReturn(null)
        trackerSelfemployedChangeStatusProducer.produceSingle(
            "A",
            TrackerSelfemployedChangeStatusPayload.TicketStatus.CREATED
        )
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.TRACKER_SELFEMPLOYED_CHANGE_STATUS)
        Mockito.verify(transitions, Mockito.times(1)).execute(
            key,
            selfemployedTrackerProperties.statusKeyCreated
        )
    }
}


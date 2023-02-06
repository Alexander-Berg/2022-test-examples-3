package ru.yandex.market.dsm.dbqueue.tracker

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.config.props.SelfemployedTrackerProperties
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.startrek.client.Issues

class TrackerSelfemployedAddCourierLinkTest : AbstractTest() {
    @Autowired
    private lateinit var issues: Issues

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var trackerSelfemployedAddCourierLinkProducer: TrackerSelfemployedAddCourierLinkProducer

    @Autowired
    private lateinit var selfemployedTrackerProperties: SelfemployedTrackerProperties

    @Test
    fun testAddLink() {
        val ticketKey = "a"
        val uid = "a"
        Mockito.`when`(
            issues.update(
                Mockito.eq(ticketKey), Mockito.any()
            )
        ).thenReturn(null)
        trackerSelfemployedAddCourierLinkProducer.produceSingle(ticketKey, uid)
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.TRACKER_SELFEMPLOYED_ADD_COURIER_LINK)
        Mockito.verify(issues, Mockito.times(1)).update(Mockito.eq(ticketKey), Mockito.any())
    }
}

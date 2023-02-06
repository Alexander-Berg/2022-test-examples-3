package ru.yandex.market.dsm.dbqueue.courier.selfemployed

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.config.props.SelfemployedTrackerProperties
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.core.test.ClockUtil
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.model.CourierRegistrationStatus
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.startrek.client.Transitions
import java.time.Clock
import java.time.LocalDateTime

class SelfemployedStatusCheckByFnsTest : AbstractTest() {

    @Autowired
    private lateinit var selfemployedStatusCheckByFnsProducer: SelfemployedStatusCheckByFnsProducer

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Autowired
    private lateinit var configurationPropertiesService: ConfigurationPropertiesService

    @Autowired
    private lateinit var selfemployedTrackerProperties: SelfemployedTrackerProperties

    @Autowired
    private lateinit var transitions: Transitions

    @BeforeEach
    fun before() {
        configurationPropertiesService.mergeValue(
            ConfigurationName.FNS_SYNCHRONIZATION_INTERVAL_OF_ATTEMPTS_IN_HOURS,
            3
        )
        configurationPropertiesService.mergeValue(
            ConfigurationName.FNS_SYNCHRONIZATION_INTERVAL_BETWEEN_ATTEMPTS_IN_MINUTES,
            1
        )
    }

    @Test
    fun goodCheck() {
        Mockito.`when`(transitions.execute(Mockito.anyString(), Mockito.anyString())).thenReturn(null)

        val courier = getCourierId()

        selfemployedStatusCheckByFnsProducer.produceSingle(courier.id, LocalDateTime.now(clock), true)
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.SELFEMPLOYED_STATUS_CHECK_BY_FNS)

        val updateCourier = courierQueryService.getById(courier.id)
        Assertions.assertThat(updateCourier.courierRegistrationStatus).isEqualTo(CourierRegistrationStatus.REGISTERED)

        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.TRACKER_SELFEMPLOYED_CHANGE_STATUS)
        Mockito.verify(transitions, Mockito.times(1)).execute(
            courier.createTicket,
            selfemployedTrackerProperties.statusKeyReadyToGo
        )
    }

    @Test
    fun badCheck() {
        val courierId = getCourierId().id

        selfemployedStatusCheckByFnsProducer.produceSingle(courierId, LocalDateTime.now(clock), false)

        ClockUtil.initFixed(clock, LocalDateTime.now(clock).plusHours(4))
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.SELFEMPLOYED_STATUS_CHECK_BY_FNS)

        val updateCourier = courierQueryService.getById(courierId)
        Assertions.assertThat(updateCourier.courierRegistrationStatus)
            .isEqualTo(CourierRegistrationStatus.READY_TO_BE_SELF_EMPLOYED)
    }

    @Test
    fun reenqueue() {
        val courierId = getCourierId().id

        selfemployedStatusCheckByFnsProducer.produceSingle(courierId, LocalDateTime.now(clock), false)

        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.SELFEMPLOYED_STATUS_CHECK_BY_FNS)

        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.SELFEMPLOYED_STATUS_CHECK_BY_FNS, 1)
        val updateCourier = courierQueryService.getById(courierId)
        Assertions.assertThat(updateCourier.courierRegistrationStatus)
            .isEqualTo(CourierRegistrationStatus.SELF_EMPLOYED_REGISTRATION_AWAITING)
    }

    private fun getCourierId(): CourierBaseCommand.Create {
        val courier = courierTestFactory.generateCreateCommand()
        courier.courierRegistrationStatus = CourierRegistrationStatus.SELF_EMPLOYED_REGISTRATION_AWAITING
        dsmCommandService.handle(courier)
        return courier
    }
}

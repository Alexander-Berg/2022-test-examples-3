package ru.yandex.market.dsm.dbqueue.courier.pro

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory

class CourierRegistrationInProByListenerTest: AbstractTest() {

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var propertyConfigurationService: ConfigurationPropertiesService

    @BeforeEach
    fun before() {
        propertyConfigurationService.mergeValue(ConfigurationName.REGISTRATION_IN_PRO_PARTNER_COURIER_ENABLED, true)
        propertyConfigurationService.mergeValue(ConfigurationName.REGISTRATION_IN_PRO_SELFEMPLOYED_COURIER_ENABLED, true)
    }

    @AfterEach
    fun after() {
        propertyConfigurationService.mergeValue(ConfigurationName.REGISTRATION_IN_PRO_PARTNER_COURIER_ENABLED, false)
        propertyConfigurationService.mergeValue(ConfigurationName.REGISTRATION_IN_PRO_SELFEMPLOYED_COURIER_ENABLED, false)
        dbQueueTestUtil.clear(DsmDbQueue.COURIER_REGISTRATION_IN_PRO)
    }

    @Test
    fun registrationPartnerCourierInProByCreate() {
        val courier = courierTestFactory.generateCreateCommand()
        courier.status = CourierStatus.ACTIVE
        courier.courierType = CourierType.PARTNER
        courier.yaProId = null
        dsmCommandService.handle(courier)

        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 1)
    }

    @Test
    fun registrationPartnerCourierInProByUpdate() {
        val courier = courierTestFactory.generateCreateCommand()
        courier.status = CourierStatus.NOT_ACTIVE
        courier.courierType = CourierType.PARTNER
        courier.yaProId = null
        dsmCommandService.handle(courier)

        //Не создается в неверном статусе
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 0)

        //Создается при верном статусе
        dsmCommandService.handle(CourierBaseCommand.UpdateStatus(courier.id, CourierStatus.NEWBIE))
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 1)
    }


    @Test
    fun registrationSelfemployedCourierInProByCreate() {
        val courier = courierTestFactory.generateCreateCommand()
        courier.status = CourierStatus.ACTIVE
        courier.courierType = CourierType.SELF_EMPLOYED
        courier.yaProId = null
        dsmCommandService.handle(courier)

        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 1)
    }

    @Test
    fun registrationSelfemployedCourierInProByUpdate() {
        val courier = courierTestFactory.generateCreateCommand()
        courier.status = CourierStatus.REVIEW
        courier.courierType = CourierType.SELF_EMPLOYED
        courier.yaProId = null
        dsmCommandService.handle(courier)

        //Не создается в неверном статусе
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 0)

        //Создается при верном статусе
        dsmCommandService.handle(CourierBaseCommand.UpdateStatus(courier.id, CourierStatus.INTERNSHIP))
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.COURIER_REGISTRATION_IN_PRO, 1)
    }
}

package ru.yandex.market.dsm.quartz

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.service.SelfemployedService
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.external.CheckSelfemployedStatusFakeClient
import java.security.InvalidParameterException

class SelfemployedStatusCheckExecutorTest : AbstractTest() {

    @Autowired
    private lateinit var selfemployedService: SelfemployedService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Autowired
    private lateinit var configurationPropertiesService: ConfigurationPropertiesService

    @Autowired
    private lateinit var checkSelfemployedStatusFakeClient: CheckSelfemployedStatusFakeClient

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @BeforeEach
    fun before() {
        configurationPropertiesService.mergeValue(ConfigurationName.CHECK_SELFEMPLOYED_STATUS_ENABLED, true)
    }

    @Test
    fun checkSelfemployedStatusTest() {
        //selfemployed courier
        val courierSelfemployed = getCourierYaProId()
        Mockito.doReturn(true).`when`(checkSelfemployedStatusFakeClient).checkSelfemployedStatus(courierSelfemployed)

        //selfemployed courier, but throw exception
        val courierException = getCourierYaProId()
        val exception = InvalidParameterException()
        Mockito.doThrow(exception).`when`(checkSelfemployedStatusFakeClient).checkSelfemployedStatus(courierException)

        //not found courier
        val courierNotFound = courierTestFactory.generateCreateCommand()
        courierNotFound.courierType = CourierType.SELF_EMPLOYED
        courierNotFound.status = CourierStatus.ACTIVE
        courierNotFound.yaProId = null
        dsmCommandService.handle(courierNotFound)

        //not selfemployed courier
        val courierNotSelfemployed = getCourierYaProId()
        Mockito.doReturn(false).`when`(checkSelfemployedStatusFakeClient)
            .checkSelfemployedStatus(courierNotSelfemployed)


        assertThrows<RuntimeException> {
            selfemployedService.checkSelfemployedStatus()
        }
        Mockito.verify(checkSelfemployedStatusFakeClient, Mockito.times(1))
            .checkSelfemployedStatus(courierNotSelfemployed)

        val courierSelfemployedUpdate = courierQueryService.getByYaProId(courierSelfemployed)
        Assertions.assertThat(courierSelfemployedUpdate.lastSelfemployedStatusSuccessCheckDate).isNotNull

        val courierExceptionUpdate = courierQueryService.getByYaProId(courierException)
        Assertions.assertThat(courierExceptionUpdate.lastSelfemployedStatusSuccessCheckDate).isNull()

        val courierNotFoundUpdate = courierQueryService.getById(courierNotFound.id)
        Assertions.assertThat(courierNotFoundUpdate.lastSelfemployedStatusSuccessCheckDate).isNull()

        val courierNotSelfemployedUpdate = courierQueryService.getByYaProId(courierNotSelfemployed)
        Assertions.assertThat(courierNotSelfemployedUpdate.lastSelfemployedStatusSuccessCheckDate).isNull()
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.CREATE_SELFEMPLOYED_STATUS_LOSS_TICKET, 1)
    }

    private fun getCourierYaProId(): String {
        val courier = courierTestFactory.generateCreateCommand()
        courier.courierType = CourierType.SELF_EMPLOYED
        courier.status = CourierStatus.ACTIVE
        dsmCommandService.handle(courier)

        return courier.yaProId!!
    }
}

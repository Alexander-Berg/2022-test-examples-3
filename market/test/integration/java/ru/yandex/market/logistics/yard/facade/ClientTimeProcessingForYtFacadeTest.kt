package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.ClientTimeProcessingForYtEntity
import ru.yandex.market.logistics.yard_v2.facade.ClientTimeProcessingForYtFacade
import java.time.LocalDateTime
import java.time.LocalTime

class ClientTimeProcessingForYtFacadeTest(
    @Autowired val clientTimeProcessingForYtFacade: ClientTimeProcessingForYtFacade
) : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/client-time-processing-for-yt/1/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/client-time-processing-for-yt/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        val entity = ClientTimeProcessingForYtEntity(
            serviceId = 1 ,
            serviceName = "1",
            zoneId = 2,
            zoneName = "2",
            initialProcessType = "1",
            processType = "3",
            statusFrom = "3",
            status = "4",
            clientId = 5,
            clientName = "login",
            ticketCode = "P123",
            takeAwayReturns = "true",
            autoCode = "auto",
            autoRamp = "need_ramp",
            windowNum = "1",
            workerLogin = "ricnorr",
            duration = LocalTime.of(1, 0),
            clientPhone = "+7928",
            creationTime = LocalDateTime.of(2020, 1, 1,1,1,1),
        )
        clientTimeProcessingForYtFacade.persist(entity)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/client-time-processing-for-yt/2/before.xml"])
    fun findByIdGreater() {
        val entity = ClientTimeProcessingForYtEntity(
            id = 2,
            serviceId = 1 ,
            serviceName = "1",
            zoneId = 2,
            zoneName = "2",
            processType = "3",
            status = "4",
            clientId = 5,
            clientName = "login",
            ticketCode = "P123",
            takeAwayReturns = "true",
            autoCode = "auto",
            autoRamp = "need_ramp",
            windowNum = "1",
            clientPhone = "+7928",
            workerLogin = "ricnorr",
            duration = LocalTime.of(1, 0),
            creationTime = LocalDateTime.of(2020, 1, 1,1,1,1),
        )
        softly.assertThat(clientTimeProcessingForYtFacade.findByIdGreaterThan(1, 2).size).isEqualTo(1)
        val foundEntity = clientTimeProcessingForYtFacade.findByIdGreaterThan(1, 2)[0]
        softly.assertThat(foundEntity).isEqualTo(entity)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/client-time-processing-for-yt/3/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/client-time-processing-for-yt/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteByIdLess() {
        clientTimeProcessingForYtFacade.deleteByIdLessThanEqual(2)
    }
}

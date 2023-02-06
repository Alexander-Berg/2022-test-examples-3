package ru.yandex.market.dsm.dbqueue.balance.selfemployed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.courier.test.CourierBalanceDataTestFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.courier.balance.service.CourierBalanceQueryService
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

class SelfEmployedRegistrationInBalanceTest : AbstractTest() {
    @Autowired
    private lateinit var selfEmployedRegistrationInBalanceProducer: SelfEmployedRegistrationInBalanceProducer

    @Autowired
    private lateinit var courierBalanceDataTestFactory: CourierBalanceDataTestFactory

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var employerService: EmployerService

    @Autowired
    private lateinit var courierBalanceQueryService: CourierBalanceQueryService

    @Test
    fun produceTask() {
        val balanceContractId = 111L
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.createCommand(upsertEmployer.id, "12137567546",
            "test3359385677556465")
        courierFirst.balanceClientId = null
        courierFirst.balancePersonId = null
        courierFirst.balanceContractId = null
        dsmCommandService.handle(courierFirst)

        val candidate = courierBalanceDataTestFactory.createCommand()
        candidate.balanceRegistrationStatus = CourierBalanceRegistrationStatus.CREATE_CLIENT
        candidate.courierId = courierFirst.id
        dsmCommandService.handle(candidate)

        selfEmployedRegistrationInBalanceProducer.produceSingle(candidate.id)

        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.SELF_EMPLOYED_REGISTRATION_IN_BALANCE)

        val updateCandidate = courierBalanceQueryService.getById(candidate.id)
        assertThat(updateCandidate.balanceRegistrationStatus).isEqualTo(
            CourierBalanceRegistrationStatus.REGISTERED
        )
        assertThat(updateCandidate.balanceClientId).isNotNull
        assertThat(updateCandidate.balancePersonId).isNotNull
        assertThat(updateCandidate.balanceContractId).isNotNull

        dbQueueTestUtil.clear(DsmDbQueue.SELF_EMPLOYED_REGISTRATION_IN_BALANCE)
    }
}


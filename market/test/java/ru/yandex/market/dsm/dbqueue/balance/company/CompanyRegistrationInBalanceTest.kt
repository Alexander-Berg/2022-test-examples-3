package ru.yandex.market.dsm.dbqueue.balance.company

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.model.EmployerBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService

class CompanyRegistrationInBalanceTest : AbstractTest() {
    @Autowired
    private lateinit var companyRegistrationInBalanceProducer: CompanyRegistrationInBalanceProducer

    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var employerQueryService: EmployerQueryService

    @Test
    fun produceTask() {
        val company = employersTestFactory.createAndSave()

        companyRegistrationInBalanceProducer.produceSingle(company.id)
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.COMPANY_REGISTRATION_IN_BALANCE)

        val updateCompany = employerQueryService.findById(company.id)
        Assertions.assertThat(updateCompany).isNotNull
        Assertions.assertThat(updateCompany!!.balanceRegistrationStatus)
            .isEqualTo(EmployerBalanceRegistrationStatus.REGISTERED)
        Assertions.assertThat(updateCompany.balanceClientId).isNotNull
        Assertions.assertThat(updateCompany.balancePersonId).isNotNull
        Assertions.assertThat(updateCompany.balanceContractId).isNotNull

        dbQueueTestUtil.clear(DsmDbQueue.COMPANY_REGISTRATION_IN_BALANCE)
    }
}

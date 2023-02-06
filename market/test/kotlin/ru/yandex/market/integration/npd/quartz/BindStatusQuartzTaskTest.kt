package ru.yandex.market.integration.npd.quartz

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.integration.npd.AbstractFunctionalTest
import ru.yandex.market.integration.npd.repository.PartnerAppNpdRepository
import ru.yandex.market.integration.npd.service.RegistrationService

class BindStatusQuartzTaskTest : AbstractFunctionalTest() {

    @Autowired
    lateinit var partnerAppNpdRepository: PartnerAppNpdRepository

    @Test
    @DbUnitDataSet(
        before = ["QuartzTask.bindStatus.before.csv"]
    )
    @DisplayName("Выбираем только активные статусы для джобы")
    fun testCompletedBindStatus() {
        val registrationServiceMock = Mockito.mock(RegistrationService::class.java)
        val task = BindStatusQuartzTask(registrationServiceMock, partnerAppNpdRepository, 1)
        task.doJob(Mockito.mock(JobExecutionContext::class.java))
        Mockito.verify(registrationServiceMock, times(2)).actualizeApplication(anyLong())
    }
}

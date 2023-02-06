package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import java.time.Instant

@DisplayName("Тест удаление ненужных заказов из ЛОМа")
class DeleteUnnecessaryLomOrdersEntitiesExecutorTest: AbstractContextualTest() {
    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var transactionTemplate: TransactionOperations

    @Autowired
    private lateinit var lomOrderService: LomOrderService

    private lateinit var executor: DeleteUnnecessaryLomOrdersEntitiesExecutor

    private val time = Instant.parse("2021-09-09T18:00:00Z")

    @BeforeEach
    fun setup() {
        clock.setFixed(time, DateTimeUtils.MOSCOW_ZONE)
        executor = DeleteUnnecessaryLomOrdersEntitiesExecutor(
            transactionTemplate,
            lomOrderService,
            clock,
        )
    }

    @Test
    @DatabaseSetup("/tms/processDeleteUnnecessaryLomOrdersEntitiesExecutor/before/success.xml")
    @ExpectedDatabase(
        value = "/tms/processDeleteUnnecessaryLomOrdersEntitiesExecutor/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление заказов")
    fun successTest() {
        executor.doJob(jobContext)
    }
}

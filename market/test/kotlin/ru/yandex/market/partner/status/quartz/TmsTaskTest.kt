package ru.yandex.market.partner.status.quartz

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.tms.quartz2.model.Executor

class TmsTaskTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var tmsLogsCleanupExecutor: Executor

    @Test
    @DbUnitDataSet(
        before = ["TmsTaskTest/oldLogs.before.csv"],
        after = ["TmsTaskTest/oldLogs.after.csv"]
    )
    fun testTask() {
        tmsLogsCleanupExecutor.doJob(null)
    }
}

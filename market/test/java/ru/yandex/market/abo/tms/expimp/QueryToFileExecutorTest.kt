package ru.yandex.market.abo.tms.expimp;

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.dynamic.model.ExpImpTask
import ru.yandex.EmptyTest
import ru.yandex.market.abo.tms.storage.QueryToFileExecutor

class QueryToFileExecutorTest @Autowired constructor(val executor: QueryToFileExecutor) : EmptyTest() {

    @Test
    fun name() {
        var task = ExpImpTask()
        task.setFileName("v_premod_report.csv")
        task.setQuery("select * from v_premod_report")
        executor.doJob(null, task)
    }
}

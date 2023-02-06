package ru.yandex.market.abo.tms.expimp;

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.dynamic.model.ExpImpTask;
import ru.yandex.EmptyTest;
import ru.yandex.market.abo.tms.storage.FileToTableExecutor

/**
 * @author frenki on 15.05.2017.
 */
@Disabled
class FileToTableExecutorTest @Autowired constructor(val mbiFileToTableExecutor: FileToTableExecutor) : EmptyTest() {

    @Test
    fun testImport() {
        var task = ExpImpTask()
        task.setFileName("V_MISSED_CLICKS_STAT.csv")
        task.setQuery("ext_missed_clicks_stat")
        task.setAppendable(true)
        mbiFileToTableExecutor.doJob(null, task)
    }
}

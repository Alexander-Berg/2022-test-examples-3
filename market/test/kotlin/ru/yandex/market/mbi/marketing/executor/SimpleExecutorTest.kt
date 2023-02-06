package ru.yandex.market.mbi.marketing.executor

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.marketing.FunctionalTest


class SimpleExecutorTest : FunctionalTest() {

    @Autowired
    private lateinit var simpleExecutor: SimpleExecutor

    @Test
    @Throws(Exception::class)
    @DbUnitDataSet(
        after = ["/ru/yandex/market/mbi/marketing/executor/SimpleExecutorTest/after.csv"]
    )
    fun testJob() {
        simpleExecutor.doJob(null)
    }


}

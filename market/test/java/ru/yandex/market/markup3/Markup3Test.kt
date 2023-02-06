package ru.yandex.market.markup3

import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.testutils.BaseAppTest

class Markup3Test : BaseAppTest() {
    @Autowired
    lateinit var taskDbService: TaskDbService

    @Test
    fun test() {
        taskDbService.findAll() shouldHaveSize 0
    }
}

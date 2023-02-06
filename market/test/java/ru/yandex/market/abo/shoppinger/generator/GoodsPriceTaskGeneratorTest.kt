package ru.yandex.market.abo.shoppinger.generator

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class GoodsPriceTaskGeneratorTest @Autowired constructor(val generator: GoodsPriceTaskGenerator) : EmptyTest() {

    @Test
    fun `test sql query`() {
        generator.addNewTasks()
    }
}

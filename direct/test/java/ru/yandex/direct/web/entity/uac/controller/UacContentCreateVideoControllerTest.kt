package ru.yandex.direct.web.entity.uac.controller

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacContentCreateVideoControllerTest : UacContentCreateVideoControllerTestBase() {

    override fun checkDbContentExists(id: String) {
        contentRepository.getContents(listOf(id)).checkSize(1)
    }
}

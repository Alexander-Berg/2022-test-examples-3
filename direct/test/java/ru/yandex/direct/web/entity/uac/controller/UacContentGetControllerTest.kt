package ru.yandex.direct.web.entity.uac.controller

import org.junit.Ignore
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.UacContentService
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
@Ignore("YDB не используется + тест не готов к тому, что в глубине контроллера вызывается грут")
class UacContentGetControllerTest : UacContentGetControllerTestBase() {

    @Autowired
    override lateinit var uacContentService: UacContentService

    @Autowired
    private lateinit var uacContentRepository: UacYdbContentRepository

    override fun saveContent(content: UacYdbContent) {
        uacContentRepository.saveContents(listOf(content))
    }
}

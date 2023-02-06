package ru.yandex.direct.web.entity.uac.controller

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacContentDeleteControllerTest : UacContentDeleteControllerTestBase() {

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    override fun saveContent(userInfo: UserInfo): String {
        val newAccount = UacYdbAccount(uid = userInfo.uid, directClientId = userInfo.clientId.asLong())
        uacYdbAccountRepository.saveAccount(newAccount)

        val content = createDefaultImageContent(accountId = newAccount.id)
        uacYdbContentRepository.saveContents(listOf(content))

        return content.id
    }

    override fun contentExists(id: String): Boolean {
        return uacYdbContentRepository.getContents(listOf(id)).isNotEmpty()
    }
}

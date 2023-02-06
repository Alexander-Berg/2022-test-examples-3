package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUserRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbUser
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.web.configuration.DirectWebTest
import java.time.Duration
import java.time.LocalDateTime

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbUserRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    private lateinit var uacYdbUserRepository: UacYdbUserRepository

    private lateinit var user: UacYdbUser

    @Before
    fun before() {
        val uid = randomPositiveLong()
        user = UacYdbUser(
            uid = uid,
            createdAt = LocalDateTime.now().withNano(0),
        )
        uacYdbUserRepository.saveUser(user)
    }

    @Test
    fun testGetUserByUidNonExisting() {
        val nonExistentId = randomPositiveLong()
        assertThat(uacYdbUserRepository.getUserByUid(nonExistentId)).isNull()
    }

    @Test
    fun testGetUserByUid() {
        val actualUser = uacYdbUserRepository.getUserByUid(user.uid)
        assertThat(actualUser).isEqualTo(user)
    }

    @Test
    @TestCaseName("testAddUser({0})")
    @Parameters(source = UacIdsProvider::class)
    fun testAddUser(caseName: String, id: String) {
        val uid = randomPositiveLong()
        val user = UacYdbUser(
            id = id,
            uid = uid,
            createdAt = LocalDateTime.now().withNano(0) - Duration.ofHours(2),
        )

        uacYdbUserRepository.saveUser(user)

        val actualUser = uacYdbUserRepository.getUserByUid(uid)
        assertThat(actualUser).isEqualTo(user)
    }
}

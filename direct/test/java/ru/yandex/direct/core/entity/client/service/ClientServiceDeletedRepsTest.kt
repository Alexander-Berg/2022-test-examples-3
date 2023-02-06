package ru.yandex.direct.core.entity.client.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.user.model.DeletedClientRepresentative
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientServiceDeletedRepsTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientService: ClientService

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun getDeletedReps_NormalJson() {
        steps.clientSteps().setClientProperty(
            clientInfo,
            Client.DELETED_REPS,
            "[{\"uid\":\"1\",\"login\":\"login1\",\"email\":\"login1@ya.ru\",\"phone\":\"1234567\",\"fio\":\"fio1\"}]"
        )

        val expectedResult = setOf(
            DeletedClientRepresentative().apply {
                uid = 1
                login  = "login1"
                email = "login1@ya.ru"
                phone = "1234567"
                fio = "fio1"
            }
        )

        val result = clientService.getClientDeletedReps(clientInfo.clientId!!)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun getDeletedReps_Null() {
        steps.clientSteps().setClientProperty(
            clientInfo,
            Client.DELETED_REPS,
            null
        )

        val result = clientService.getClientDeletedReps(clientInfo.clientId!!)
        assertThat(result).isEqualTo(setOf<DeletedClientRepresentative>())
    }

    @Test
    fun getDeletedReps_EmptyString() {
        steps.clientSteps().setClientProperty(
            clientInfo,
            Client.DELETED_REPS,
            ""
        )

        val result = clientService.getClientDeletedReps(clientInfo.clientId!!)
        assertThat(result).isEqualTo(setOf<DeletedClientRepresentative>())
    }

    @Test
    fun getDeletedReps_BlankString() {
        steps.clientSteps().setClientProperty(
            clientInfo,
            Client.DELETED_REPS,
            "  \t \r \n"
        )

        val result = clientService.getClientDeletedReps(clientInfo.clientId!!)
        assertThat(result).isEqualTo(setOf<DeletedClientRepresentative>())
    }
}

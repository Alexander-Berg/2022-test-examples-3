package ru.yandex.direct.core.entity.client.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.client.model.ClientAutoOverdraftInfo
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(SpringRunner::class)
class ClientRepositoryAutoOverdraftInfoTest {
    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var steps: Steps

    @Test
    fun getOverdraftInfoSuccessPath() {
        val client = steps.clientSteps().createClient(
            TestClients.defaultClient()
                .withDebt("1.00".toBigDecimal())
                .withOverdraftLimit("2.00".toBigDecimal())
                .withAutoOverdraftLimit("3.00".toBigDecimal())
                .withStatusBalanceBanned(true)
        )

        val ret = clientRepository.getClientsAutoOverdraftInfo(
            client.shard,
            listOf(client.clientId)
        )

        assertThat(ret).hasSize(1)
        assertThat(ret[0]).isEqualTo(
            ClientAutoOverdraftInfo()
                .withClientId(client.clientId!!.asLong())
                .withDebt("1.00".toBigDecimal())
                .withOverdraftLimit("2.00".toBigDecimal())
                .withAutoOverdraftLimit("3.00".toBigDecimal())
                .withStatusBalanceBanned(true)
        )
    }
}

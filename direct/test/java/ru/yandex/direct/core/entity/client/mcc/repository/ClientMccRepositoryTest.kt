package ru.yandex.direct.core.entity.client.mcc.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.mcc.ClientMccRepository
import ru.yandex.direct.core.entity.client.mcc.ClientMccRequest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.utils.TestUtils.assumeThat

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientMccRepositoryTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var clientMccRepository: ClientMccRepository

    private lateinit var controlClient1Info: ClientInfo
    private lateinit var controlClient2Info: ClientInfo
    private lateinit var controlClient3Info: ClientInfo
    private lateinit var controlClient4Info: ClientInfo
    private lateinit var controlClientForHasRequestInfo: ClientInfo
    private lateinit var managedClient1Info: ClientInfo
    private lateinit var managedClient2Info: ClientInfo
    private lateinit var managedClient3Info: ClientInfo
    private lateinit var managedClient4Info: ClientInfo
    private lateinit var managedClient5Info: ClientInfo
    private lateinit var managedClientForGetInfo: ClientInfo
    private lateinit var managedClientForNonexistentInfo: ClientInfo

    @Before
    fun before() {
        controlClient1Info = steps.clientSteps().createDefaultClient()
        controlClient2Info = steps.clientSteps().createDefaultClient()
        controlClient3Info = steps.clientSteps().createDefaultClient()
        controlClient4Info = steps.clientSteps().createDefaultClient()
        controlClientForHasRequestInfo = steps.clientSteps().createDefaultClient()
        managedClient1Info = steps.clientSteps().createDefaultClient()
        managedClient2Info = steps.clientSteps().createDefaultClientAnotherShard()
        managedClient3Info = steps.clientSteps().createDefaultClientAnotherShard()
        managedClient4Info = steps.clientSteps().createDefaultClient()
        managedClient5Info = steps.clientSteps().createDefaultClient()
        managedClientForGetInfo = steps.clientSteps().createDefaultClient()
        managedClientForNonexistentInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun addRequestTest() {
        assumeThat { sa ->
            sa.assertThat(getRequestIdsFromDb(controlClient2Info, managedClient4Info).size).isEqualTo(0)
        }
        clientMccRepository.addRequest(
            controlClient2Info.shard,
            controlClient2Info.clientId!!,
            managedClient4Info.clientId!!
        )
        assertThat(getRequestIdsFromDb(controlClient2Info, managedClient4Info).size).isEqualTo(1)
    }

    private fun getRequestIdsFromDb(controlClientInfo: ClientInfo, managedClientInfo: ClientInfo): List<Long> {
        return dslContextProvider.ppc(controlClientInfo.shard)
            .select(Tables.CLIENT_MCC_REQUESTS.REQUEST_ID)
            .from(Tables.CLIENT_MCC_REQUESTS)
            .where(Tables.CLIENT_MCC_REQUESTS.CLIENT_ID_FROM.eq(controlClientInfo.clientId!!.asLong()))
            .and(Tables.CLIENT_MCC_REQUESTS.CLIENT_ID_TO.eq(managedClientInfo.clientId!!.asLong()))
            .fetch(Tables.CLIENT_MCC_REQUESTS.REQUEST_ID)
    }

    @Test
    fun getRequestsByControlClientIdTest() {
        val managedClients = setOf(managedClient1Info, managedClient2Info, managedClient3Info)
        val expectedRequests = managedClients.map {
            ClientMccRequest(null, controlClient1Info.clientId!!, it.clientId!!, null)
        }.toSet()

        managedClients.forEach {
            clientMccRepository.addRequest(controlClient1Info.shard, controlClient1Info.clientId!!, it.clientId!!)
        }

        val requests =
            clientMccRepository.getRequestsByControlClientId(controlClient1Info.shard, controlClient1Info.clientId!!)

        assertThat(requests)
            .usingRecursiveComparison()
            .ignoringFields(ClientMccRequest::requestId.name, ClientMccRequest::createTime.name)
            .isEqualTo(expectedRequests)
    }

    @Test
    fun deleteRequestByIdTest() {
        clientMccRepository.addRequest(
            controlClient3Info.shard,
            controlClient3Info.clientId!!,
            managedClient5Info.clientId!!
        )
        val requestIds = getRequestIdsFromDb(controlClient3Info, managedClient5Info)
        assumeThat { sa -> sa.assertThat(requestIds.size).isEqualTo(1) }
        clientMccRepository.deleteRequest(controlClient3Info.shard, requestIds[0])
        assertThat(getRequestIdsFromDb(controlClient3Info, managedClient5Info).size).isEqualTo(0)
    }

    @Test
    fun deleteRequestByClientIdsTest() {
        clientMccRepository.addRequest(
            controlClient4Info.shard,
            controlClient4Info.clientId!!,
            managedClient5Info.clientId!!
        )
        val requestIds = getRequestIdsFromDb(controlClient4Info, managedClient5Info)
        assumeThat { sa -> sa.assertThat(requestIds.size).isEqualTo(1) }
        clientMccRepository.deleteRequest(controlClient4Info.shard, controlClient4Info.clientId!!,  managedClient5Info.clientId!!)
        assertThat(getRequestIdsFromDb(controlClient4Info, managedClient5Info)).isEmpty()
    }

    @Test
    fun hasRequestsByControlClientIdTest() {
        setOf(managedClient1Info, managedClient2Info, managedClient3Info).forEach {
            clientMccRepository.addRequest(controlClientForHasRequestInfo.shard,
                controlClientForHasRequestInfo.clientId!!, it.clientId!!)
        }

        assertThat(clientMccRepository.hasRequestsByControlClientId(controlClientForHasRequestInfo.shard,
            controlClientForHasRequestInfo.clientId!!)).isTrue
    }

    @Test
    fun hasRequestsByControlClientIdNegativeTest() {
        assertThat(clientMccRepository.hasRequestsByControlClientId(managedClientForNonexistentInfo.shard,
            managedClientForNonexistentInfo.clientId!!)).isFalse
    }

    @Test
    fun getRequestsByManagedClientIdTest() {
        val controlClients = setOf(controlClient1Info, controlClient2Info, controlClient3Info)
        val expectedRequests = controlClients.map {
            ClientMccRequest(null, it.clientId!!, managedClientForGetInfo.clientId!!, null)
        }.toSet()

        controlClients.forEach {
            clientMccRepository.addRequest(it.shard, it.clientId!!, managedClientForGetInfo.clientId!!)
        }

        val requests =
            clientMccRepository.getRequestsByManagedClientId(managedClientForGetInfo.shard, managedClientForGetInfo.clientId!!)

        assertThat(requests)
            .usingRecursiveComparison()
            .ignoringFields(ClientMccRequest::requestId.name, ClientMccRequest::createTime.name)
            .isEqualTo(expectedRequests)
    }
}

package ru.yandex.direct.core.entity.client.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.mcc.ClientMccRepository
import ru.yandex.direct.core.entity.client.mcc.ClientMccService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.rbac.RbacClientsRelations
import ru.yandex.direct.test.utils.TestUtils

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientMccServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientMccService: ClientMccService

    @Autowired
    private lateinit var clientMccRepository: ClientMccRepository

    @Autowired
    private lateinit var rbacClientsRelations: RbacClientsRelations

    private lateinit var controlClient1Info: ClientInfo
    private lateinit var controlClient2Info: ClientInfo

    private lateinit var managedClient1Info: ClientInfo
    private lateinit var managedClientNonexistentInfo: ClientInfo
    private lateinit var managedClientForGetInfo: ClientInfo

    @Before
    fun before() {
        controlClient1Info = steps.clientSteps().createDefaultClient()
        controlClient2Info = steps.clientSteps().createDefaultClient()
        managedClient1Info = steps.clientSteps().createDefaultClient()
        managedClientNonexistentInfo = steps.clientSteps().createDefaultClient()
        managedClientForGetInfo = steps.clientSteps().createDefaultClient()

        setOf(controlClient1Info, controlClient2Info).map {
            steps.clientMccSteps().createClientMccLink(it.clientId!!, managedClientForGetInfo.clientId!!)
        }
    }

    @Test
    fun approveRequestTest() {
        steps.clientMccSteps().addMccRequest(controlClient1Info.clientId!!, managedClient1Info.clientId!!)
        TestUtils.assumeThat { sa ->
            sa.assertThat(hasMccRequest(controlClient1Info, managedClient1Info.clientId!!)).isTrue
        }
        val result = clientMccService.approveRequest(controlClient1Info.clientId!!, managedClient1Info.clientId!!)
        assertSoftly { softly ->
            softly.assertThat(result).isTrue()
            softly.assertThat(hasMccRequest(controlClient1Info, managedClient1Info.clientId!!)).isFalse
            softly.assertThat(hasMccLink(controlClient1Info.clientId!!, managedClient1Info.clientId!!)).isTrue
        }
    }

    @Test
    fun approveNonexistentRequestTest() {
        steps.clientMccSteps().addMccRequest(controlClient1Info.clientId!!, managedClientNonexistentInfo.clientId!!)
        val result = clientMccService.approveRequest(controlClient1Info.clientId!!, managedClient1Info.clientId!!)
        assertThat(result).isFalse()
    }

    @Test
    fun getControlClientsTest() {
        val result = clientMccService.getControlClients(managedClientForGetInfo.clientId!!)
        assertThat(result).containsExactlyInAnyOrder(controlClient1Info.clientId, controlClient2Info.clientId)
    }

    @Test
    fun getControlClientsEmptyListTest() {
        val result = clientMccService.getControlClients(controlClient2Info.clientId!!)
        assertThat(result).isEmpty()
    }

    private fun hasMccRequest(controlClientInfo: ClientInfo, managedClientId: ClientId): Boolean {
        val request = clientMccRepository.getRequestByClientIdPair(controlClientInfo.shard, controlClientInfo.clientId!!, managedClientId)
        return request != null
    }

    private fun hasMccLink(controlClientId: ClientId, managedClientId: ClientId): Boolean {
        val link = rbacClientsRelations.getClientRelation(managedClientId, controlClientId).relationId
        return link != null
    }
}

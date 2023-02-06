package ru.yandex.direct.core.entity.agency.repository


import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.user.model.AgencyLimRep
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestAgencyRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.rbac.RbacAgencyLimRepType
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.util.stream.Stream

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AgencyRepositoryTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var agencyRepository: AgencyRepository

    @Autowired
    private lateinit var testAgencyRepository: TestAgencyRepository

    private lateinit var agency: ClientInfo
    private lateinit var client1: ClientInfo
    private lateinit var client2: ClientInfo

    private lateinit var agencyLimRep1: AgencyLimRep
    private lateinit var agencyLimRep2: AgencyLimRep
    private lateinit var agencyLimRep3: AgencyLimRep

    @Before
    fun before() {
        agency = steps.clientSteps().createDefaultAgency()
        client1 = steps.clientSteps().createClientUnderAgency(agency)
        client2 = steps.clientSteps().createClientUnderAgency(agency, ClientInfo().withShard(client1.shard))

        agencyLimRep1 = AgencyLimRep().withRepType(RbacAgencyLimRepType.LEGACY)
        agencyLimRep2 = AgencyLimRep().withRepType(RbacAgencyLimRepType.CHIEF).withGroupId(RandomNumberUtils.nextPositiveLong())
        agencyLimRep3 = AgencyLimRep().withRepType(RbacAgencyLimRepType.MAIN).withGroupId(agencyLimRep2.groupId)

        Stream.of(agencyLimRep1, agencyLimRep2, agencyLimRep3).forEach { o ->
            steps.userSteps().createAgencyLimRep(agency, o)
        }

        testAgencyRepository.linkLimRepToClient(client1.shard,
            listOf(agencyLimRep1.uid, agencyLimRep2.uid, agencyLimRep3.uid), client1.clientId!!.asLong())
        testAgencyRepository.linkLimRepToClient(client2.shard, listOf(agencyLimRep2.uid), client2.clientId!!.asLong())
    }

    @Test
    fun getAgencyLimRepsTest() {
        val agencyLimReps = agencyRepository.getAgencyLimReps(agency.shard,
            listOf<Long>(agencyLimRep1.uid, agencyLimRep2.uid, agencyLimRep3.uid))
        assertThat(agencyLimReps).`is`(matchedBy(beanDiffer(
            mapOf<Long, AgencyLimRep>(
                agencyLimRep1.uid to agencyLimRep1,
                agencyLimRep2.uid to agencyLimRep2,
                agencyLimRep3.uid to agencyLimRep3
            )
        )))
    }

    @Test
    fun getAgencyLimRepUidsByClientsTest() {
        val clientsAgencyLimReps = agencyRepository.getAgencyLimRepUidsByClients(client1.shard,
            listOf(client1.clientId!!.asLong(), client2.clientId!!.asLong()))
        assertThat(clientsAgencyLimReps).`is`(matchedBy(beanDiffer(
            mapOf(
                client1.clientId!!.asLong() to setOf<Long>(agencyLimRep1.uid, agencyLimRep2.uid, agencyLimRep3.uid),
                client2.clientId!!.asLong() to setOf<Long>(agencyLimRep2.uid),
            )
        )))
    }
}

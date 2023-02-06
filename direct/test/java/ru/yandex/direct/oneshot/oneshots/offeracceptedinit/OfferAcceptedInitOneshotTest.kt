package ru.yandex.direct.oneshot.oneshots.offeracceptedinit

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.ClientSteps.Companion.DEFAULT_SHARD
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.USERS_OPTIONS
import ru.yandex.direct.dbschema.ppc.enums.UsersOptionsIsOfferAccepted.Yes
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.rbac.RbacRepType
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors

@OneshotTest
@RunWith(SpringRunner::class)
class OfferAcceptedInitOneshotTest {

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dsl: DslContextProvider

    @Autowired
    lateinit var oneshot: OfferAcceptedInitOneshot

    private lateinit var firstClient: ClientInfo
    private lateinit var agency: ClientInfo
    private lateinit var clientOfAgency: ClientInfo
    private lateinit var severalRepClient: ClientInfo
    private lateinit var clientWithDeletedRep: ClientInfo
    private lateinit var secondClient: ClientInfo

    @Before
    fun setUp() {
        firstClient = steps.clientSteps().createDefaultClient()

        agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
        clientOfAgency = steps.clientSteps().createDefaultClientUnderAgency(agency)

        severalRepClient = steps.clientSteps().createDefaultClient()
        steps.userSteps().createRepresentative(severalRepClient, RbacRepType.MAIN)

        clientWithDeletedRep = steps.clientSteps().createDefaultClient()
        steps.userSteps().createDeletedUser(clientWithDeletedRep)

        secondClient = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun execute_whenFirstIteration_success() {
        val state = oneshot.execute(null, null, DEFAULT_SHARD)!!
        assertThat(state.lastClientId.asLong())
                .`as`("lastClientId").isGreaterThanOrEqualTo(1L)
    }

    @Test
    fun execute_whenNextLoop_success() {
        val clientId = firstClient.clientId!!.asLong()
        val prevState = State(ClientId.fromLong(clientId - 1L))

        val state = oneshot.execute(null, prevState, DEFAULT_SHARD)!!

        val actualFirstClientFlag = getIsOfferAccepted(firstClient.uid)
        val actualAgencyFlag = getIsOfferAccepted(agency.uid)
        val actualClientOfAgencyFlag = getIsOfferAccepted(clientOfAgency.uid)
        val actualSeveralRepClientFlag = getIsOfferAccepted(severalRepClient.uid)
        val actualWithDeletedRepFlag = getIsOfferAccepted(clientWithDeletedRep.uid)
        val actualSecondClientFlag = getIsOfferAccepted(secondClient.uid)
        assertSoftly {
            it.assertThat(actualFirstClientFlag).`as`("actualFirstClientFlag").isTrue
            it.assertThat(actualAgencyFlag).`as`("actualAgencyFlag").isFalse
            it.assertThat(actualClientOfAgencyFlag).`as`("actualClientOfAgencyFlag").isFalse
            it.assertThat(actualSeveralRepClientFlag).`as`("actualSeveralRepClientFlag").isFalse
            it.assertThat(actualSecondClientFlag).`as`("actualSecondClientFlag").isTrue
            it.assertThat(actualWithDeletedRepFlag).`as`("actualWithDeletedRepFlag").isFalse
            it.assertThat(state.lastClientId.asLong())
                    .`as`("lastClientId").isEqualTo(secondClient.clientId!!.asLong())
        }
    }

    @Test
    fun execute_whenLastIteration_failure() {
        val prevState = State(secondClient.clientId!!)

        val state = oneshot.execute(null, prevState, DEFAULT_SHARD)

        assertThat(state).`as`("state").isNull()
    }

    @Test
    fun validate_successful() {
        val validationResult = oneshot.validate(null)
        assertThat(validationResult).`as`("validationResult").`is`(matchedBy(hasNoErrors<Any>()))
    }

    private fun getIsOfferAccepted(uid: Long): Boolean =
            dsl.ppc(DEFAULT_SHARD)
                    .select(USERS_OPTIONS.IS_OFFER_ACCEPTED)
                    .from(USERS_OPTIONS)
                    .where(USERS_OPTIONS.UID.eq(uid))
                    .fetchOne(USERS_OPTIONS.IS_OFFER_ACCEPTED)
                    .equals(Yes)

}

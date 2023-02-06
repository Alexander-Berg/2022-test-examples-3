package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.turbolandings.client.TurboLandingsClient
import ru.yandex.direct.turbolandings.client.TurboLandingsClientException
import ru.yandex.direct.turbolandings.client.model.DcTurboLanding

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class ClientDataServiceClientInfoTurboLandingCreationFeatureTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var turboLandingsClient: TurboLandingsClient

    @Autowired
    private lateinit var clientDataService: ClientDataService

    fun parameters() = listOf(
        listOf(false, true, true),
        listOf(false, false, true),
        listOf(true, true, true),
        listOf(true, false, false)
    )

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("When creation disabled is {0} and turbolandings exist is {1}, then flag is {2}")
    fun `test flag when turbo landing client throws exception`(
        creationDisabled: Boolean,
        hasTurbolandings: Boolean,
        result: Boolean
    ) {
        val clientInfo = steps.clientSteps().createDefaultClient()
        steps.featureSteps().addClientFeature(
            clientInfo.clientId,
            FeatureName.TURBO_LANDINGS_CREATION_DISABLED,
            creationDisabled
        )
        if (hasTurbolandings) {
            steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.clientId)
        }
        doThrow(TurboLandingsClientException::class)
            .whenever(turboLandingsClient).getTurboLandings(any(), any())

        val gdClientInfo = clientDataService.getClientInfo(
            clientInfo.chiefUserInfo!!.user!!,
            listOf(clientInfo.clientId!!.asLong())
        )[0]

        val shouldCallTurboLandingsClient = creationDisabled && hasTurbolandings
        verify(turboLandingsClient, times(if (shouldCallTurboLandingsClient) 1 else 0))
            .getTurboLandings(any(), any())
        Assertions.assertThat(gdClientInfo.isTurboLandingCreationEnabled).isEqualTo(result)
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("When creation disabled is {0} and turbolandings exist is {1}, then flag is {2}")
    fun `test flag`(creationDisabled: Boolean, hasTurbolandings: Boolean, result: Boolean) {
        val clientInfo = steps.clientSteps().createDefaultClient()
        steps.featureSteps().addClientFeature(
            clientInfo.clientId,
            FeatureName.TURBO_LANDINGS_CREATION_DISABLED,
            creationDisabled
        )
        steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.clientId)
        doReturn(if (hasTurbolandings) listOf(DcTurboLanding()) else emptyList())
            .whenever(turboLandingsClient).getTurboLandings(any(), any())

        val gdClientInfo = clientDataService.getClientInfo(
            clientInfo.chiefUserInfo!!.user!!,
            listOf(clientInfo.clientId!!.asLong())
        )[0]

        val shouldCallTurboLandingsClient = creationDisabled
        verify(turboLandingsClient, times(if (shouldCallTurboLandingsClient) 1 else 0))
            .getTurboLandings(any(), any())
        Assertions.assertThat(gdClientInfo.isTurboLandingCreationEnabled).isEqualTo(result)
    }
}

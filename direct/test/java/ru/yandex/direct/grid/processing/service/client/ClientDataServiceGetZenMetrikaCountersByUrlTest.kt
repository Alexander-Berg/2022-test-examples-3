package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.bangenproxy.client.zenmeta.ZenMetaInfoClient
import ru.yandex.direct.bangenproxy.client.zenmeta.model.ZenMetaInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdInaccessibleMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounterSource
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientDataServiceGetZenMetrikaCountersByUrlTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientDataService: ClientDataService

    @Autowired
    private lateinit var zenMetaInfoClient: ZenMetaInfoClient

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    private lateinit var client: ClientInfo
    private val zenUrl = "https://zen.yandex.ru/super-blog"

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @After
    fun after() {
        Mockito.reset(zenMetaInfoClient, metrikaClient)
        metrikaClient.clearUnavailableCounters()
    }

    @Test
    fun noCounterTest() {
        val zenMetaInfo = ZenMetaInfo()
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)
        val result = clientDataService.getMetrikaCountersByUrl(client.uid, client.chiefUserInfo?.user, zenUrl, false)
        assertThat(result).`as`("Not null result").isNotNull
         SoftAssertions.assertSoftly {
            it.assertThat(result.isMetrikaAvailable).`as`("MetrikaAvailable is false").isTrue
            it.assertThat(result.accessibleCountersByDomain).`as`("Empty accessibleCountersIds").isEmpty()
            it.assertThat(result.inaccessibleCountersByDomain).`as`("Empty inaccessibleCountersIds").isEmpty()
        }
    }

    @Test
    fun suggestedInaccessibleCounterTest() {
        val counterId = 123
        val fakeCounterId = 124

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(CounterInfoDirect().withCounterPermission("view").withId(fakeCounterId))
        )
        metrikaClient.addUnavailableCounter(counterId.toLong(), true)

        val result = clientDataService.getMetrikaCountersByUrl(client.uid, client.chiefUserInfo?.user, zenUrl, false)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.accessibleCountersByDomain).`as`("Empty accessibleCountersIds").isEmpty()
            it.assertThat(result.inaccessibleCountersByDomain).`as`("counters").containsExactlyInAnyOrder(
                GdInaccessibleMetrikaCounter()
                    .withId(counterId.toLong())
                    .withAccessRequested(false)
            )
        }
    }

    @Test
    fun notSuggestedInaccessibleCounterTest() {
        val counterId = 125
        val fakeCounterId = 126

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(CounterInfoDirect().withCounterPermission("view").withId(fakeCounterId))
        )
        metrikaClient.addUnavailableCounter(counterId.toLong(), false)

        val result = clientDataService.getMetrikaCountersByUrl(client.uid, client.chiefUserInfo?.user, zenUrl, false)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.isMetrikaAvailable).`as`("MetrikaAvailable is false").isTrue
            it.assertThat(result.accessibleCountersByDomain).`as`("Empty accessibleCountersIds").isEmpty()
            it.assertThat(result.inaccessibleCountersByDomain).`as`("Empty inaccessibleCountersIds").isEmpty()
        }
    }

    @Test
    fun suggestedAccessibleCounterTest() {
        val counterId = 127

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(
                CounterInfoDirect()
                    .withCounterPermission("view").withId(counterId).withCounterSource("system")
            )
        )

        val result = clientDataService.getMetrikaCountersByUrl(client.uid, client.chiefUserInfo?.user, zenUrl, false)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.accessibleCountersByDomain).`as`("accessibleCountersIds").containsExactlyInAnyOrder(
                GdClientMetrikaCounter()
                    .withId(counterId.toLong())
                    .withIsEditableByOperator(false)
                    .withSource(GdMetrikaCounterSource.SYSTEM)
                    .withName("")
                    .withDomain("")
                    .withHasEcommerce(false)
            )
            it.assertThat(result.inaccessibleCountersByDomain).`as`("Empty inaccessibleCountersIds").isEmpty()
        }
    }
}

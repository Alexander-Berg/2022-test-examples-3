package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.any
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
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounterForSuggest
import ru.yandex.direct.grid.processing.model.client.GdSuggestDataByUrl
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientDataServiceZenSuggestCounterTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientDataService: ClientDataService

    @Autowired
    private lateinit var zenMetaInfoClient: ZenMetaInfoClient

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    private lateinit var client: ClientInfo
    private lateinit var context: GridGraphQLContext
    private val zenUrl = "https://zen.yandex.ru/super-blog"
    private val input: GdSuggestDataByUrl = GdSuggestDataByUrl().withUrl(zenUrl)

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        context = GridGraphQLContext(client.chiefUserInfo!!.user)
    }

    @After
    fun after() {
        Mockito.reset(zenMetaInfoClient, metrikaClient, metrikaGoalsService)
        metrikaClient.clearUnavailableCounters()
    }

    @Test
    fun noCounterTest() {
        val zenMetaInfo = ZenMetaInfo()
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)
        val result = clientDataService.getSuggestDataByUrl(context, input)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.counterIds).`as`("Empty countersIds").isEmpty()
            it.assertThat(result.zenMeta).`as`("Not null zenMeta").isNotNull
            it.assertThat(result.goals).`as`("Empty goals").isEmpty()
        }
    }

    @Test
    fun notSuggestedExistentCounterTest() {
        val counterId = 123
        val fakeCounterId = 124

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(CounterInfoDirect().withCounterPermission("view").withId(fakeCounterId))
        )
        metrikaClient.addUnavailableCounter(counterId.toLong(), true)

        val result = clientDataService.getSuggestDataByUrl(context, input)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.counters).`as`("counters").containsExactlyInAnyOrder(
                GdClientMetrikaCounterForSuggest()
                    .withId(counterId.toLong())
                    .withIsAccessible(false)
                    .withIsEditableByOperator(false)
                    .withIsCalltrackingOnSiteCompatible(false)
            )
            it.assertThat(result.zenMeta).`as`("Not null zenMeta").isNotNull
            it.assertThat(result.goals).`as`("Empty goals").isEmpty()
        }
    }

    @Test
    fun notSuggestedNotExistentCounterTest() {
        val counterId = 125
        val fakeCounterId = 126

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(CounterInfoDirect().withCounterPermission("view").withId(fakeCounterId))
        )
        metrikaClient.addUnavailableCounter(counterId.toLong(), false)

        val result = clientDataService.getSuggestDataByUrl(context, input)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.counters).`as`("Empty counters").isEmpty()
            it.assertThat(result.zenMeta).`as`("Not null zenMeta").isNotNull
            it.assertThat(result.goals).`as`("Empty goals").isEmpty()
        }
    }

    @Test
    fun suggestedCounterTest() {
        val counterId = 127

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addUserCounters(
            client.uid,
            listOf(
                CounterInfoDirect()
                    .withCounterPermission("view").withId(counterId)
            )
        )

        val result = clientDataService.getSuggestDataByUrl(context, input)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(result.counters).`as`("counters").containsExactlyInAnyOrder(
                GdClientMetrikaCounterForSuggest()
                    .withId(counterId.toLong())
                    .withIsAccessible(true)
                    .withIsEditableByOperator(false)
                    .withIsCalltrackingOnSiteCompatible(true)
            )
            it.assertThat(result.zenMeta).`as`("Not null zenMeta").isNotNull
            it.assertThat(result.goals).`as`("Empty goals").isEmpty()
        }
    }

    @Test
    fun suggestedCounterWithGoalsTest() {
        val counterId = 128

        val zenMetaInfo = ZenMetaInfo().withPublisherVacuumCounterId(counterId.toLong())
        whenever(zenMetaInfoClient.getZenMetaInfoByUrl(zenUrl)).thenReturn(zenMetaInfo)

        metrikaClient.addCounterGoal(counterId, 456)
        metrikaClient.addCounterGoal(counterId, 789)
        metrikaClient.addUserCounters(
            client.uid,
            listOf(CounterInfoDirect().withCounterPermission("view").withId(counterId))
        )

        val result = clientDataService.getSuggestDataByUrl(context, input)
        assertThat(result).`as`("Not null result").isNotNull
        SoftAssertions.assertSoftly { it: SoftAssertions ->
            it.assertThat(result.counters).`as`("counters").containsExactlyInAnyOrder(
                    GdClientMetrikaCounterForSuggest()
                        .withId(counterId.toLong())
                        .withIsAccessible(true)
                        .withIsEditableByOperator(false)
                        .withIsCalltrackingOnSiteCompatible(true)
            )
            it.assertThat(result.zenMeta).`as`("Not null zenMeta").isNotNull
            it.assertThat(result.goals.map { it.id }.toSet()).`as`("goals")
                .containsExactlyInAnyOrderElementsOf(setOf(456L, 789L))
        }
    }
}

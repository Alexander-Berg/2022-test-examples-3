package ru.yandex.direct.core.entity.metrika.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Select
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.ytcomponents.service.AllCountersByDomainDynContextProvider
import ru.yandex.direct.ytcomponents.service.CounterByDomainDynContextProvider
import ru.yandex.direct.ytcomponents.service.CountersByDomainHitLogDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext

private const val DOMAIN = "ozon.ru"

@RunWith(JUnitParamsRunner::class)
class MetrikaCounterByDomainRepositoryTest {
    @Mock
    private lateinit var ytDynamicContext: YtDynamicContext
    @Mock
    private lateinit var counterByDomainDynContextProvider: CounterByDomainDynContextProvider
    @Mock
    private lateinit var allCountersByDomainDynContextProvider: AllCountersByDomainDynContextProvider
    @Mock
    private lateinit var countersByDomainHitLogDynContextProvider: CountersByDomainHitLogDynContextProvider
    @InjectMocks
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository

    private val queryCaptor = argumentCaptor<Select<*>>()

    fun restrictedCountersByDomainTestData() = listOf(
        listOf(false, COUNTERS_BY_DOMAIN_QUERY),
        listOf(true, COUNTERS_BY_DOMAIN_HIT_LOG_QUERY),
    )

    fun allCountersByDomainTestData() = listOf(
        listOf(false, ALL_COUNTERS_BY_DOMAIN_QUERY),
        listOf(true, ALL_COUNTERS_BY_DOMAIN_HIT_LOG_QUERY),
    )

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        whenever(ytDynamicContext.executeTimeoutSafeSelect(any())).thenReturn(mock())
        whenever(counterByDomainDynContextProvider.context).thenReturn(ytDynamicContext)
        whenever(allCountersByDomainDynContextProvider.context).thenReturn(ytDynamicContext)
        whenever(countersByDomainHitLogDynContextProvider.context).thenReturn(ytDynamicContext)
    }

    @Test
    @Parameters(method = "restrictedCountersByDomainTestData")
    fun restrictedCountersByDomain_QueryTest(useHitLogTable: Boolean, expectedQuery: String) {
        metrikaCounterByDomainRepository.getRestrictedCountersByDomain(DOMAIN, useHitLogTable)
        assertQuery(expectedQuery)
    }

    @Test
    @Parameters(method = "allCountersByDomainTestData")
    fun allCountersByDomain_QueryTest(useHitLogTable: Boolean, expectedQuery: String) {
        metrikaCounterByDomainRepository.getAllCountersByDomain(DOMAIN, useHitLogTable)
        assertQuery(expectedQuery)
    }

    private fun assertQuery(expectedQuery: String) {
        verify(ytDynamicContext).executeTimeoutSafeSelect(queryCaptor.capture())
        assertThat(queryCaptor.firstValue.toString()).isEqualTo(expectedQuery)
    }

    private companion object {
        private val COUNTERS_BY_DOMAIN_QUERY = """
            SELECT 
              C.counter_id AS counter_id, 
              C.owner_uid AS owner_uid, 
              C.timestamp AS timestamp
            FROM yt.counter_by_domain AS C
            WHERE C.domain = '$DOMAIN'
            """.trimIndent()

        private val COUNTERS_BY_DOMAIN_HIT_LOG_QUERY = """
            SELECT 
              C.counter_id AS counter_id, 
              C.owner_uid AS owner_uid, 
              C.timestamp AS timestamp
            FROM yt.counters_by_domain_hit_log AS C
            WHERE (
              C.domain = '$DOMAIN'
              AND C.num_domain_users >= 10
              AND C.num_domain_users <= (C.num_counter_users * 10)
            )""".trimIndent()

        private val ALL_COUNTERS_BY_DOMAIN_QUERY = """
            SELECT 
              C.counter_id AS counter_id, 
              C.owner_uid AS owner_uid, 
              C.timestamp AS timestamp
            FROM yt.all_counters_by_domain AS C
            WHERE C.domain = '$DOMAIN'
            """.trimIndent()

        private val ALL_COUNTERS_BY_DOMAIN_HIT_LOG_QUERY = """
            SELECT 
              C.counter_id AS counter_id, 
              C.owner_uid AS owner_uid, 
              C.timestamp AS timestamp
            FROM yt.counters_by_domain_hit_log AS C
            WHERE (
              C.domain = '$DOMAIN'
              AND C.num_domain_users >= 1
            )""".trimIndent()
    }
}

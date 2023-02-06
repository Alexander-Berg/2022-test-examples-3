package ru.yandex.market.logistics.mqm.service.tms.statistics

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.bolts.collection.impl.UnmodifiableDefaultCollectionF
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.SearchRequest

class StartrekQueuesStatisticsExecutorTest: AbstractContextualTest() {
    @Autowired
    lateinit var executor: StartrekQueuesStatisticsExecutor

    @Autowired
    lateinit var startrekSession: Session

    @Test
    @DisplayName("Успешное создание метрик")
    fun executeJob() {
        val issues = Mockito.mock(Issues::class.java)
        whenever(startrekSession.issues()).thenReturn(issues)
        val issuesForStatistics = UnmodifiableDefaultCollectionF.wrap(
            listOf(
                Issue(null, null, "K-1", null, 1, EmptyMap(), null)
            )
        ).iterator()
        whenever(issues.find(ArgumentMatchers.argThat<SearchRequest> {
            it.queue.equals("MQMCRT") && it.filterId.equals("0")
        }
        )).thenReturn(issuesForStatistics)

        executor.run()

        val captor = ArgumentCaptor.forClass(SearchRequest::class.java)
        verify(issues).find(captor.capture())
    }
}

package ru.yandex.market.dsm.quartz

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.bolts.collection.impl.DefaultIteratorF
import ru.yandex.bolts.collection.impl.DefaultListF
import ru.yandex.bolts.collection.impl.DefaultMapF
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationPropertiesDboRepository
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import ru.yandex.market.dsm.external.tracker.selfemployed.SelfemployedSyncTrackerService
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.SearchRequest

class SelfemployedSyncTrackerServiceTest : AbstractTest() {

    @Autowired
    private lateinit var selfemployedSyncTrackerService: SelfemployedSyncTrackerService

    @Autowired
    private lateinit var configurationPropertiesService: ConfigurationPropertiesService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var configurationPropertiesDboRepository: ConfigurationPropertiesDboRepository

    @Autowired
    private lateinit var trackerSession: Session

    @Autowired
    private lateinit var trackerIssues: Issues

    @BeforeEach
    fun beforeEach() {
        configurationPropertiesService.mergeValue(ConfigurationName.SELFEMPLOYED_SYNC_TRACKER_ENABLED, true)
        clearInvocations(trackerIssues)
        clearInvocations(trackerSession)
    }

    @Test
    fun `sync - success`() {
        val issue1 = Mockito.spy(getIssue("TEST-01"))
        val issue2 = Mockito.spy(getIssue("TEST-02"))
        Mockito.doReturn(null).`when`(issue1).status
        Mockito.doReturn(null).`when`(issue2).status
        val issues = listOf(
            issue1,
            issue2,
        )

        Mockito.`when`(trackerIssues.get(issue1.id)).thenReturn(issue1)
        Mockito.`when`(trackerIssues.get(issue2.id)).thenReturn(issue2)

        val issuesIterator = DefaultIteratorF.wrap(issues.iterator())
        Mockito.`when`(trackerIssues.find(Mockito.any<SearchRequest>(), Mockito.any())).thenReturn(issuesIterator)

        selfemployedSyncTrackerService.sync()
    }

    @Test
    fun `sync - persists last update time`() {
        deleteValue()

        val issuesIterator = DefaultIteratorF.wrap(listOf<Issue>().iterator())

        val searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest::class.java)
        Mockito.`when`(trackerIssues.find(searchRequestCaptor.capture(), Mockito.eq(
            DefaultListF.wrap(listOf("summary")))))
            .thenReturn(issuesIterator)

        selfemployedSyncTrackerService.sync()
        Assertions.assertThat(searchRequestCaptor.value.query.get())
            .isEqualTo(getTicketsSearchRequestQuery("1970-01-01 00:00"))

        selfemployedSyncTrackerService.sync()
        Assertions.assertThat(searchRequestCaptor.value.query.get())
            .isNotEqualTo(getTicketsSearchRequestQuery("1970-01-01 00:00"))
    }

    @Test
    fun `sync - no updates`() {
        val issuesIterator = DefaultIteratorF.wrap(listOf<Issue>().iterator())

        Mockito.`when`(trackerIssues.find(Mockito.any<SearchRequest>(), Mockito.any())).thenReturn(issuesIterator)

        selfemployedSyncTrackerService.sync()
    }

    @Test
    fun `sync - disabled`() {
        configurationPropertiesService.mergeValue(ConfigurationName.SELFEMPLOYED_SYNC_TRACKER_ENABLED, false)

        selfemployedSyncTrackerService.sync()

        Mockito.verify(trackerSession, Mockito.times(0)).issues()
    }

    private fun getTicketsSearchRequestQuery(updateDateTime: String) =
        "Queue: NONE and Updated: >= \"$updateDateTime\""

    private fun getIssue(key: String) = Issue(
        "$key-id",
        null,
        key,
        "summary",
        1,
        DefaultMapF.wrap(mutableMapOf()),
        trackerSession,
    )

    private fun deleteValue() {
        transactionTemplate.execute {
            val property = configurationPropertiesDboRepository.findByKey(
                ConfigurationName.SELFEMPLOYED_SYNC_TRACKER_LAST_SUCCESSFUL_ATTEMPT_DATETIME.name
            )
            if (property != null) {
                configurationPropertiesDboRepository.delete(property)
            }
        }
    }
}


package ru.yandex.market.tpl.tms.service.surcharge

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.ListF
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.DefaultIteratorF
import ru.yandex.bolts.collection.impl.DefaultListF
import ru.yandex.bolts.collection.impl.DefaultMapF
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties
import ru.yandex.market.tpl.core.domain.surcharge.entity.Surcharge
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeType
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeValidationStatus
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeRepository
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeTypeRepository
import ru.yandex.market.tpl.tms.config.props.SurchargeTrackerProperties
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest
import ru.yandex.startrek.client.Events
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.model.Event
import ru.yandex.startrek.client.model.FieldRef
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.SearchRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class SurchargeSyncServiceTest : TplTmsAbstractTest() {

    @Autowired
    private lateinit var surchargeSyncService: SurchargeSyncService
    @Autowired
    private lateinit var surchargeRepository: SurchargeRepository
    @Autowired
    private lateinit var surchargeTypeRepository: SurchargeTypeRepository
    @Autowired
    private lateinit var configurationServiceAdapter: ConfigurationServiceAdapter
    @Autowired
    private lateinit var surchargeTrackerSessionFactory: SurchargeTrackerSessionFactory
    @Autowired
    private lateinit var surchargeTrackerProperties: SurchargeTrackerProperties
    @Autowired
    private lateinit var trackerSession: Session
    @Autowired
    private lateinit var trackerIssues: Issues
    @Autowired
    private lateinit var trackerEvents: Events

    private lateinit var surchargeType: SurchargeType

    @BeforeEach
    fun beforeEach() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SURCHARGES_SYNC_EXECUTOR_ENABLED, true)
        surchargeRepository.deleteAll()
        surchargeTypeRepository.deleteAll()

        surchargeType = surchargeTypeRepository.save(
            SurchargeType(
                id = UUID.randomUUID().toString(),
                code = "test-code",
                name = "test",
                type = SurchargeType.Type.PENALTY,
                description = "test",
                userShiftIsRequired = false,
                deleted = false,
            )
        )

        clearInvocations(trackerIssues)
        clearInvocations(trackerEvents)
        clearInvocations(trackerSession)
        `when`(surchargeTrackerSessionFactory.create()).thenReturn(trackerSession)
        `when`(trackerSession.issues()).thenReturn(trackerIssues)
        `when`(trackerSession.events()).thenReturn(trackerEvents)
    }

    @Test
    fun `sync - successfully processes multiple issues`() {
        val issue1 = getIssue("issue-1")
        `when`(trackerIssues.get(issue1.id)).thenReturn(issue1)
        val issue1Events = listOf(
            getEvent(
                "1-event-1",
                1,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
            getEvent(
                "1-event-2",
                2,
                DefaultListF.wrap(listOf(getFieldChange("other", "some value"))),
            ),
        )
        `when`(trackerEvents.getAll(issue1.id)).thenReturn(DefaultListF.wrap(issue1Events).iterator())

        val issue2 = getIssue("issue-2")
        `when`(trackerIssues.get(issue2.id)).thenReturn(issue2)
        val issue2Events = listOf(
            getEvent(
                "2-event-1",
                1,
                DefaultListF.wrap(listOf(getFieldChange("other", "some value"))),
            ),
        )
        `when`(trackerEvents.getAll(issue2.id)).thenReturn(DefaultListF.wrap(issue2Events).iterator())

        val issue3 = getIssue("issue-3")
        `when`(trackerIssues.get(issue3.id)).thenReturn(issue3)
        `when`(trackerEvents.getAll(issue3.id)).thenReturn(DefaultListF.wrap(listOf<Event>()).iterator())

        mockIssuesFind(listOf(issue1, issue2, issue3))

        surchargeSyncService.sync()

        assertThat(surchargeRepository.findAll().size).isEqualTo(1)
    }

    @Test
    fun `sync - skips resolution changes before first fixed resolution`() {
        val issue = getIssue("issue-1")
        `when`(trackerIssues.get(issue.id)).thenReturn(issue)
        val issueEvents = listOf(
            getEvent(
                "1-event-1",
                1,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value"))),
            ),
            getEvent(
                "1-event-2",
                2,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value-2"))),
            ),
            getEvent(
                "1-event-3",
                3,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
        )
        `when`(trackerEvents.getAll(issue.id)).thenReturn(DefaultListF.wrap(issueEvents).iterator())

        mockIssuesFind(listOf(issue))

        surchargeSyncService.sync()

        val persistedSurcharges = surchargeRepository.findAll()

        assertThat(persistedSurcharges.size).isEqualTo(1)
        assertThat(persistedSurcharges.first().resolution).isEqualTo(SurchargeResolution.COMMIT)
    }

    @Test
    fun `sync - persists resolution changes without duplicates`() {
        val issue = getIssue("issue-1")
        `when`(trackerIssues.get(issue.id)).thenReturn(issue)
        val changelogId1 = "1-event-1"
        val changelogId2 = "1-event-2"
        val changelogId4 = "1-event-4"
        val issueEvents = listOf(
            getEvent(
                changelogId1,
                1,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
            getEvent(
                changelogId2,
                2,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value"))),
            ),
            getEvent(
                "1-event-3",
                3,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value-2"))),
            ),
            getEvent(
                changelogId4,
                4,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
            getEvent(
                "1-event-5",
                5,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
        )
        `when`(trackerEvents.getAll(issue.id)).thenReturn(DefaultListF.wrap(issueEvents).iterator())

        mockIssuesFind(listOf(issue))

        surchargeSyncService.sync()

        val persistedSurcharges = surchargeRepository.findAll()

        assertThat(persistedSurcharges.size).isEqualTo(3)
        assertThat(persistedSurcharges.find { it.trackerChangelogId == changelogId1 }!!.resolution)
            .isEqualTo(SurchargeResolution.COMMIT)
        assertThat(persistedSurcharges.find { it.trackerChangelogId == changelogId2 }!!.resolution)
            .isEqualTo(SurchargeResolution.ROLLBACK)
        assertThat(persistedSurcharges.find { it.trackerChangelogId == changelogId4 }!!.resolution)
            .isEqualTo(SurchargeResolution.COMMIT)
    }

    @Test
    fun `sync - skips persisted events`() {
        val issue = getIssue("issue-1")
        `when`(trackerIssues.get(issue.id)).thenReturn(issue)
        val changelogId1 = "1-event-1"
        val issueEvents = listOf(
            getEvent(
                changelogId1,
                1,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
            getEvent(
                "1-event-2",
                2,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value"))),
            ),
        )
        `when`(trackerEvents.getAll(issue.id)).thenReturn(DefaultListF.wrap(issueEvents).iterator())

        mockIssuesFind(listOf(issue))

        createSurcharge(issue, changelogId1, null)

        surchargeSyncService.sync()

        val persistedSurcharges = surchargeRepository.findAll()

        assertThat(persistedSurcharges.size).isEqualTo(2)
    }

    @Test
    fun `sync - uses amount from db for rollback surcharge`() {
        val issue = getIssue("issue-1")
        `when`(trackerIssues.get(issue.id)).thenReturn(issue)
        val changelogId1 = "1-event-1"
        val changelogId2 = "1-event-2"
        val issueEvents = listOf(
            getEvent(
                changelogId1,
                1,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "fixed"))),
            ),
            getEvent(
                changelogId2,
                2,
                DefaultListF.wrap(listOf(getFieldChange("resolution", "some-value"))),
            ),
        )
        `when`(trackerEvents.getAll(issue.id)).thenReturn(DefaultListF.wrap(issueEvents).iterator())

        mockIssuesFind(listOf(issue))

        val amount = 4769.0
        createSurcharge(issue, changelogId1, amount)

        surchargeSyncService.sync()

        val persistedSurcharges = surchargeRepository.findAll()

        assertThat(persistedSurcharges.size).isEqualTo(2)
        assertThat(persistedSurcharges.find { it.trackerChangelogId == changelogId2 }!!.amount)
            .isEqualTo(BigDecimal.valueOf(amount))
    }

    @Test
    fun `sync - persists last update time`() {
        configurationServiceAdapter.deleteValue(ConfigurationProperties.SURCHARGES_SYNC_LAST_SUCCESSFUL_ATTEMPT_DATETIME)

        val issuesIterator = DefaultIteratorF.wrap(listOf<Issue>().iterator())
        val searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest::class.java)
        `when`(trackerIssues.find(
            searchRequestCaptor.capture(),
            eq(DefaultListF.wrap(listOf("summary")))
        )).thenReturn(issuesIterator)

        surchargeSyncService.sync()
        assertThat(searchRequestCaptor.value.query.get())
            .isEqualTo(getTicketsSearchRequestQuery("1970-01-01 00:00"))

        surchargeSyncService.sync()
        assertThat(searchRequestCaptor.value.query.get())
            .isEqualTo(getTicketsSearchRequestQuery("1990-01-01 00:00"))
    }

    @Test
    fun `sync - no updates`() {
        mockIssuesFind(listOf())

        surchargeSyncService.sync()

        assertThat(surchargeRepository.findAll().size).isEqualTo(0)
    }

    @Test
    fun `sync - disabled`() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SURCHARGES_SYNC_EXECUTOR_ENABLED, false)

        surchargeSyncService.sync()

        verify(trackerSession, times(0)).issues()
    }

    private fun mockIssuesFind(issues: List<Issue>) {
        val issuesIterator = DefaultIteratorF.wrap(issues.iterator())
        `when`(trackerIssues.find(any<SearchRequest>(), any())).thenReturn(issuesIterator)
    }

    private fun getTicketsSearchRequestQuery(updateDateTime: String) =
        "Queue: TPLCOMPLAINSTST and Updated: >= \"$updateDateTime\""

    private fun getIssue(key: String) = Issue(
        "$key-id",
        null,
        key,
        "summary",
        1,
        DefaultMapF.wrap(
            mutableMapOf(
                surchargeTrackerProperties.issueFields.surchargeEventDate.key
                    to Option.of(org.joda.time.LocalDate("2020-01-01")),
                surchargeTrackerProperties.issueFields.surchargeType.key
                    to Option.of(surchargeType.code),
                surchargeTrackerProperties.issueFields.surchargeCargoType.key
                    to Option.of("test cargo type"),
                surchargeTrackerProperties.issueFields.surchargeCompanyId.key
                    to Option.of(1L),
                surchargeTrackerProperties.issueFields.surchargeCourierId.key
                    to Option.of(2L),
                surchargeTrackerProperties.issueFields.surchargeScId.key
                    to Option.of(3L),
                surchargeTrackerProperties.issueFields.surchargeMultiplier.key
                    to Option.of(5L),
                surchargeTrackerProperties.issueFields.surchargeAmount.key
                    to Option.of(10.0),
            ) as Map<String, Any>
        ),
        trackerSession,
    )

    private fun createSurcharge(
        issue: Issue,
        changelogId: String,
        amount: Double?,
    ) {
        surchargeRepository.save(
            Surcharge(
                id = UUID.randomUUID().toString(),
                validationStatus = SurchargeValidationStatus.VALID,
                validationErrors = null,
                resolution = SurchargeResolution.COMMIT,
                type = "type",
                cargoType = "cargo-type",
                eventDate = LocalDate.now(),
                companyId = 1L,
                scId = 2L,
                userId = null,
                userShiftId = null,
                amount = if (amount == null) null else BigDecimal.valueOf(amount),
                multiplier = 1,
                trackerTicket = issue.key,
                trackerChangelogId = changelogId,
            )
        )
    }

    private fun getEvent(
        id: String,
        updatedAt: Long,
        fieldChanges: ListF<Event.FieldChange>,
    ) = Event(
        id,
        null,
        Instant(updatedAt),
        null,
        null,
        null,
        null,
        fieldChanges,
        null,
        null,
        null,
        null,
        null,
        null,
    )

    private fun getFieldChange(
        fieldId: String,
        toKeyValue: String,
    ) = Event.FieldChange(
        mock(FieldRef::class.java).apply {
            `when`(this.id).thenReturn(fieldId)
        },
        Option.empty<Map<Any, Any>>(),
        Option.of(mutableMapOf<String, Any>()).apply {
            this.get()["key"] = toKeyValue
        },
    )
}

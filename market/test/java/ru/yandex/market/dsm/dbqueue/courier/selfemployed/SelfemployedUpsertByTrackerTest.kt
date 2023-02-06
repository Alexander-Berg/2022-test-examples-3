package ru.yandex.market.dsm.dbqueue.courier.selfemployed

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.model.FiredReason
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.external.tracker.selfemployed.model.SelfemployedTrackerTicketInfo
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient
import ru.yandex.startrek.client.Transitions
import java.security.InvalidParameterException

class SelfemployedUpsertByTrackerTest : AbstractTest() {
    @Autowired
    private lateinit var selfemployedUpsertByTrackerProducer: SelfemployedUpsertByTrackerProducer

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var blackboxClient: BlackboxClient

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var employerTestFactory: EmployersTestFactory

    @Autowired
    private lateinit var configurationPropertiesService: ConfigurationPropertiesService

    @Autowired
    private lateinit var selfemployedUpsertByTrackerService: SelfemployedUpsertByTrackerService

    @Autowired
    private lateinit var transitions: Transitions

    @Test
    fun testCreateNotValidSelfemployedFromTicket() {
        Mockito.`when`(transitions.execute(Mockito.anyString(), Mockito.anyString())).thenReturn(null)
        var ticket = courierTestFactory.createFromTicket()
        ticket = ticket.copy(phone = null, status = SelfemployedTrackerTicketInfo.Status.OPEN)
        execute(ticket)
        val courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier).isNull()
        verify(transitions, times(1)).execute(ticket.key, "invalid")
    }

    @Test
    fun testIgnoreSelfemployedFromTicketWithIgnoreStatuses() {
        var ticket = courierTestFactory.createFromTicket()

        val courierCommand = courierTestFactory.generateCreateCommand()
        val status = CourierStatus.NEWBIE
        courierCommand.status = status
        courierCommand.createTicket = ticket.key
        dsmCommandService.handle(courierCommand)


        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.CREATED)
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        var courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.status).isEqualTo(status)

        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.UNKNOWN)
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.status).isEqualTo(status)

        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.SYNCHRONIZATION_ERROR)
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.status).isEqualTo(status)

        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.UNKNOWN
        )
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.status).isEqualTo(status)

        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.FIXED
        )
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.status).isEqualTo(status)

        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.REJECTED_BY_SECURITY
        )
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.firedReason).isEqualTo(FiredReason.FAILED_SECURITY_CHECK)
        dsmCommandService.handle(CourierBaseCommand.UpdateStatus(
            courier.id,
            CourierStatus.NEWBIE
        ))

        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.INAPPROPRIATE
        )
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.firedReason).isEqualTo(FiredReason.FAILED_INTERNSHIP)
        dsmCommandService.handle(CourierBaseCommand.UpdateStatus(
            courier.id,
            CourierStatus.NEWBIE
        ))

        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.REFUSAL
        )
        selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier!!.firedReason).isEqualTo(FiredReason.FAILED_REGISTRATION)
    }

    @Test
    fun cantCreateCourierInNotOpenTicketStatus() {
        var ticket = courierTestFactory.createFromTicket()
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.CHECK)
        assertThrows<InvalidParameterException> {
            selfemployedUpsertByTrackerService.processPayload(selfemployedUpsertByTrackerProducer.map(ticket))
        }
    }

    @Test
    fun testCreateSelfemployedFromTicket() {
        val fakeCompanyId = "djlkgdjgdl3934e34"
        employerTestFactory.createAndSave(
            id = fakeCompanyId,
            "sgfkafdkajfg",
            "dsfcxvhsfhaks",
            null,
            null,
            true
        )
        configurationPropertiesService.mergeValue(ConfigurationName.SELFEMPLOYED_FAKE_COMPANY_ID, fakeCompanyId)

        val uid: Long = 257967252542L
        Mockito.doReturn(uid).`when`(blackboxClient).getUidForLogin(Mockito.any())

        var ticket = courierTestFactory.createFromTicket()
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.OPEN)
        execute(ticket)

        val courier = courierQueryService.getByCreateTicket(ticket.key)
        Assertions.assertThat(courier).isNotNull
        Assertions.assertThat(courier!!.status).isEqualTo(CourierStatus.REVIEW)
        Assertions.assertThat(courier.personalData.email).isEqualTo(ticket.email)
        Assertions.assertThat(courier.personalData.passportData.patronymicName).isEqualTo(ticket.patronymicName)
        Assertions.assertThat(courier.personalData.passportData.lastName).isEqualTo(ticket.lastName)
        Assertions.assertThat(courier.personalData.passportData.firstName).isEqualTo(ticket.firstName)
        Assertions.assertThat(courier.personalData.passportData.birthday).isEqualTo(ticket.birthday)
        Assertions.assertThat(courier.personalData.phone).isEqualTo(ticket.phone)
        Assertions.assertThat(courier.personalData.passportData.nationality).isEqualTo(ticket.nationality)
        Assertions.assertThat(courier.createTicket).isEqualTo(ticket.key)
        Assertions.assertThat(courier.personalData.telegramLogin).isEqualTo(ticket.telegram)
        Assertions.assertThat(courier.employerId).isEqualTo(fakeCompanyId)
    }

    @Test
    fun testUpdateStatus() {
        val courierCommand = courierTestFactory.generateCreateCommand()
        courierCommand.status = CourierStatus.REVIEW
        courierCommand.uid = "34765356385"
        dsmCommandService.handle(courierCommand)
        Mockito.doReturn(courierCommand.uid.toLong()).`when`(blackboxClient).getUidForLogin(Mockito.any())
        var ticket = courierTestFactory.createFromTicket()
        ticket = ticket.copy(key = courierCommand.createTicket!!)

        //Проверка статуса OPEN
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.OPEN)
        execute(ticket)
        var courier = courierQueryService.getById(courierCommand.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.REVIEW)

        //Проверка статуса CHECK
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.CHECK)
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.REVIEW)

        //Проверка статуса INTERNSHIP
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.INTERNSHIP)
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.INTERNSHIP)

        //Проверка статуса REGISTRATION
        ticket = ticket.copy(status = SelfemployedTrackerTicketInfo.Status.REGISTRATION)
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.NEWBIE)

        //Проверка статуса CLOSED и резолюции REJECTED_BY_SECURITY
        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.REJECTED_BY_SECURITY
        )
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.FIRED)
        CourierBaseCommand.UpdateStatus(
            courier.id,
            CourierStatus.NEWBIE
        )

        //Проверка статуса CLOSED и резолюции INAPPROPRIATE
        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.INAPPROPRIATE
        )
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.FIRED)
        CourierBaseCommand.UpdateStatus(
            courier.id,
            CourierStatus.NEWBIE
        )

        //Проверка статуса CLOSED и резолюции REFUSAL
        ticket = ticket.copy(
            status = SelfemployedTrackerTicketInfo.Status.CLOSED,
            resolution = SelfemployedTrackerTicketInfo.Resolution.REFUSAL
        )
        execute(ticket)
        courier = courierQueryService.getById(courier.id)
        Assertions.assertThat(courier.status).isEqualTo(CourierStatus.FIRED)
    }

    @Test
    fun `ticket validation - blackbox error`() {
        reset(transitions, blackboxClient)

        doThrow(RuntimeException("some message"))
            .`when`(blackboxClient).getUidForLogin(Mockito.any())
        val ticket = courierTestFactory.createFromTicket()
            .copy(
                status = SelfemployedTrackerTicketInfo.Status.OPEN
            )

        execute(ticket)
        verify(transitions, times(1)).execute(ticket.key, "invalid")
    }

    @Test
    fun `ticket validation - uid duplicate`() {
        reset(transitions, blackboxClient)

        val courierCommand = courierTestFactory.generateCreateCommand().apply {
            this.status = CourierStatus.REVIEW
            this.uid = "34765356385"
            this.createTicket = "1"
        }
        dsmCommandService.handle(courierCommand)

        doReturn(courierCommand.uid.toLong())
            .`when`(blackboxClient).getUidForLogin(Mockito.any())
        val ticket = courierTestFactory.createFromTicket()
            .copy(
                key = "2",
                status = SelfemployedTrackerTicketInfo.Status.OPEN
            )

        execute(ticket)
        verify(transitions, times(1)).execute(ticket.key, "invalid")
    }

    private fun execute(ticketInfo: SelfemployedTrackerTicketInfo) {
        selfemployedUpsertByTrackerProducer.produceSingle(ticketInfo)
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.SELFEMPLOYED_UPSERT_BY_TRACKER)
    }
}


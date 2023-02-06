package ru.yandex.market.pricingmgmt.approver_strategy

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.bolts.collection.Option
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.error.StartrekInternalClientError
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.ResolutionRef
import ru.yandex.startrek.client.model.StatusRef
import java.io.IOException

object TrackerTestUtil {
    fun createTicket(session: Session, ticket: String, status: String? = null, resolution: String? = null) {
        val issue = Mockito.mock(Issue::class.java)
        Mockito.`when`(issue.key).thenReturn(ticket)

        val statusRef = Mockito.mock(StatusRef::class.java)
        Mockito.`when`(statusRef.key).thenReturn(status)
        Mockito.`when`(issue.status).thenReturn(statusRef)

        val resolutionRef = Mockito.mock(ResolutionRef::class.java)
        Mockito.`when`(resolutionRef.key).thenReturn(resolution)
        Mockito.`when`(issue.resolution).thenReturn(Option.of(resolutionRef))

        val issues = Mockito.mock(Issues::class.java)
        Mockito.`when`(session.issues()).thenReturn(issues)
        Mockito.`when`(session.issues().get(ticket)).thenReturn(issue)
        Mockito.`when`(session.issues().create(ArgumentMatchers.any())).thenReturn(issue)
    }

    fun createTicketRaisedException(session: Session) {
        val ioEx = IOException("test IO exception")
        val issues = Mockito.mock(Issues::class.java)

        Mockito.`when`(session.issues()).thenReturn(issues)
        Mockito.`when`(session.issues().create(ArgumentMatchers.any()))
            .thenThrow(StartrekInternalClientError(ioEx))
    }
}

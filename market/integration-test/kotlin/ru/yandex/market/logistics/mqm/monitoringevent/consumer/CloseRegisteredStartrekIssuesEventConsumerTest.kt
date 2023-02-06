package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.CloseRegisteredStartrekIssuesEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CloseRegisteredStartrekIssuesPayload
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.Transition

class CloseRegisteredStartrekIssuesEventConsumerTest: StartrekProcessorTest() {
    @Autowired
    private lateinit var consumer: CloseRegisteredStartrekIssuesEventConsumer

    @Test
    @DisplayName("Проверка закрытия")
    @DatabaseSetup("/monitoringevent/consumer/before/close_all_issues.xml")
    @ExpectedDatabase(
        value = "/monitoringevent/consumer/after/close_all_issues.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssuesTest() {
        issue(ISSUE_ID)
        issue(ISSUE_2_ID)
        consumer.processPayload(
            CloseRegisteredStartrekIssuesPayload(
                setOf(
                    CloseRegisteredStartrekIssuesPayload.Entity(ENTITY_ID, IssueLinkEntityType.ORDER),
                    CloseRegisteredStartrekIssuesPayload.Entity(ENTITY_2_ID, IssueLinkEntityType.ORDER),
                )
            ),
            null
        )
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
        verify(transitions).execute(
            eq(ISSUE_2_ID),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
    }

    private fun issue(issueId: String): Issue {
        val issue = Mockito.mock(Issue::class.java)
        whenever(issue.key).thenReturn(issueId)
        val transition = Mockito.mock(Transition::class.java)
        whenever(issue.transitions).thenReturn(ArrayListF(listOf(transition)))
        whenever(transition.id).thenReturn(TRANSACTION)
        whenever(issues[issueId]).thenReturn(issue)
        return issue
    }

    companion object {
        private const val ENTITY_ID = "2053"
        private const val ENTITY_2_ID = "2054"
        private const val TRANSACTION = "close"
        private const val ISSUE_ID = "Q-1"
        private const val ISSUE_2_ID = "Q-2"
    }
}

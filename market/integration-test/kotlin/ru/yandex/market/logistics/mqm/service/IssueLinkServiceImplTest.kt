package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.MapF
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkReason
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Transition
import ru.yandex.startrek.client.model.Update

@DisplayName("Тест сервиса связок")
class IssueLinkServiceImplTest : StartrekProcessorTest() {
    @Autowired
    private lateinit var issueLinkService: IssueLinkService

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Проверка закрытия задачи")
    @DatabaseSetup("/service/issue_link_service/before/close.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/close.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeTest(useReason: Boolean) {
        issue(ISSUE_ID)
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
        assertResolutionAndComment(captor)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Проверка закрытия с несколькими сущностями")
    @DatabaseSetup("/service/issue_link_service/before/close_all.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/close_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllTest(useReason: Boolean) {
        issue(ISSUE_ID)
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
        assertResolutionAndComment(captor)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Проверка закрытия с несколькими сущностями и несколькими задачами")
    @DatabaseSetup("/service/issue_link_service/before/close_all_issues.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/close_all_issues.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllIssuesTest(useReason: Boolean) {
        issue(ISSUE_ID)
        issue(ISSUE_2_ID)
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        val captorFirst = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            captorFirst.capture()
        )
        assertResolutionAndComment(captorFirst)
        val captorSecond = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_2_ID),
            ArgumentMatchers.any(String::class.java),
            captorSecond.capture()
        )
        assertResolutionAndComment(captorSecond)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Проверка частичного закрытия с несколькими сущностями и несколькими задачами")
    @DatabaseSetup("/service/issue_link_service/before/close_all_issues_partly.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/close_all_issues_partly.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllIssuesAndSeveralIssuesTest(useReason: Boolean) {
        issue(ISSUE_ID)
        issue(ISSUE_2_ID)
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        val captorFirst = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            captorFirst.capture()
        )
        assertResolutionAndComment(captorFirst)
        verify(transitions, Mockito.never()).execute(
            eq(ISSUE_2_ID),
            ArgumentMatchers.any(String::class.java),
            ArgumentMatchers.any(IssueUpdate::class.java)
        )
    }

    @Test
    @DisplayName(
        "Проверка частичного закрытия с несколькими сущностями и несколькими задачами, не для всех есть причина"
    )
    @DatabaseSetup("/service/issue_link_service/before/close_all_issues_partly_without_reason.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/close_all_issues_partly_without_reason.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeAllIssuesAndSeveralIssuesWithReasonTest() {
        issue(ISSUE_ID)
        issue(ISSUE_3_ID)
        issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        val captorFirst = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq(ISSUE_3_ID),
            ArgumentMatchers.any(String::class.java),
            captorFirst.capture()
        )
        assertResolutionAndComment(captorFirst)
        verify(transitions, Mockito.never()).execute(
            eq(ISSUE_2_ID),
            ArgumentMatchers.any(String::class.java),
            ArgumentMatchers.any(IssueUpdate::class.java)
        );
        verify(transitions, Mockito.never()).execute(
            eq(ISSUE_ID),
            ArgumentMatchers.any(String::class.java),
            ArgumentMatchers.any(IssueUpdate::class.java)
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Закрытие не происходит, если нет данных")
    fun noCloseBecauseNoDataTest(useReason: Boolean) {
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        verifyNoMoreInteractions(transitions)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @DisplayName("Закрытие не происходит, если есть другие свзяки")
    @DatabaseSetup("/service/issue_link_service/before/no_close_some_processing.xml")
    @ExpectedDatabase(
        value = "/service/issue_link_service/after/no_close_some_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun noCloseBecauseSomeProcessingTest(useReason: Boolean) {
        issue(ISSUE_ID)
        if (useReason) {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
        } else {
            issueLinkService.close(ENTITY_ID, IssueLinkEntityType.ORDER)
        }
        verifyNoMoreInteractions(transitions)
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

    private fun assertResolutionAndComment(captor: ArgumentCaptor<IssueUpdate>) {
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values
        assertSoftly {
            commentString shouldBe "Задача автоматически была закрыта из-за изменений в заказе $ENTITY_ID"
            values.getAsScalarUpdate("resolution") shouldBe "fixed"
        }
    }

    private fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()

    companion object {
        private const val ENTITY_ID = "2053"
        private const val ISSUE_ID = "Q-1"
        private const val ISSUE_2_ID = "Q-2"
        private const val ISSUE_3_ID = "Q-3"
        private const val TRANSACTION = "close"
    }
}

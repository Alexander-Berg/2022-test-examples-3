package ru.yandex.market.pers.qa.tms.startrek;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.exception.QaRuntimeException;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.startrek.QaTicketState;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.service.common.startrek.StartrekService.CLOSED_TRANSITIONS;

/**
 * @author grigor-vlad
 * 13.04.2022
 */
public class AddCtmVendorAnswerToTicketExecutorTest extends PersQaTmsTest {
    private static final String QUESTION_TEXT = "Это хороший текст для вопроса?";
    private static final String DEFAULT_ANSWER_TEXT = "Просто ответ на вопрос";
    private static final long DEFAULT_MODEL_ID = 1;
    private static final Issue ISSUE = new Issue(
        "1", null, "issue-key", null, 1, new EmptyMap<>(), null
    );

    @Autowired
    private StartrekService startrekService;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    @Qualifier("addCtmVendorAnswerToTicketExecutor")
    private AddCtmVendorAnswerToTicketExecutor executor;

    @Value("${pers.startrek.model.ctm.vendorId}")
    private long mainVendorId;


    @Test
    public void testAddAnswerExecutor() throws Exception {
        //create 3 tickets and 3 answers for them (third from another vendor)
        //first ticket should close successfully the second one should fail and third shouldn't be commented
        String ticket1 = "QASTMTEST-1";
        String answerText1 = "Неплохой ответ с id = 1";
        Pair<Long, Long> questionAndAnswer1 = createQuestionAndAnswer(1L, answerText1, mainVendorId);
        addQuestionTicket(ticket1, questionAndAnswer1.getKey());

        String ticket2 = "QASTMTEST-2";
        Pair<Long, Long> questionAndAnswer2 = createQuestionAndAnswer(2L, DEFAULT_ANSWER_TEXT, mainVendorId);
        addQuestionTicket(ticket2, questionAndAnswer2.getKey());

        String ticket3 = "QASTMTEST-3";
        Pair<Long, Long> questionAndAnswer3 = createQuestionAndAnswer(3L, DEFAULT_ANSWER_TEXT);
        addQuestionTicket(ticket3, questionAndAnswer3.getKey());

        when(startrekService.getTicket(ticket1)).thenReturn(ISSUE);
        when(startrekService.getTicket(ticket2)).thenThrow(new RuntimeException("No ticket"));
        when(startrekService.getPossibleTransition(eq(ISSUE), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.of(mock(Transition.class)));

        try {
            executor.addVendorAnswerToTicketCtm();
            Assertions.fail();
        } catch (QaRuntimeException qaEx) {
            assertEquals("1 exception(s) found. First exception: No ticket", qaEx.getMessage());
        }

        verify(startrekService, times(1)).createComment(eq(ISSUE), (String) argThat((text) -> {
            return (text != null) && ((String) text).endsWith(answerText1);
        }));
        verify(startrekService, times(1)).closeTicket(eq(ISSUE));
        assertEquals(QaTicketState.CLOSE, getQaTicketState(ticket1));
        assertEquals(QaTicketState.OPEN, getQaTicketState(ticket2));
        assertEquals(QaTicketState.OPEN, getQaTicketState(ticket3));
    }

    @Test
    public void testDifferentAnswersFromOneVendor() throws Exception {
        String ticketKey = "QASTMTEST-1";
        String answerText1 = "Неплохой ответ с id = 1";
        Pair<Long, Long> questionAndAnswer1 = createQuestionAndAnswer(1L, answerText1, mainVendorId);
        addQuestionTicket(ticketKey, questionAndAnswer1.getKey());

        String answerText2 = "Неплохой ответ с id = 2";
        createAnswer(2L, answerText2, questionAndAnswer1.getKey(), mainVendorId);

        String answerText3 = "Неплохой ответ с id = 3";
        createAnswer(3L, answerText3, questionAndAnswer1.getKey(), mainVendorId);

        when(startrekService.getTicket(ticketKey)).thenReturn(ISSUE);
        when(startrekService.getPossibleTransition(eq(ISSUE), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.of(mock(Transition.class)));

        executor.addVendorAnswerToTicketCtm();

        verify(startrekService, times(1)).createComment(eq(ISSUE), (String) argThat((text) -> {
            return (text != null) && ((String) text).endsWith(answerText1);
        }));
        verify(startrekService, times(1)).closeTicket(eq(ISSUE));
        assertEquals(QaTicketState.CLOSE, getQaTicketState(ticketKey));
    }

    @Test
    public void testCloseTicketAlreadyClosedManually() throws Exception {
        String ticketKey = "QASTMTEST-1";
        String answerText1 = "Неплохой ответ для вопроса";
        Pair<Long, Long> questionAndAnswer1 = createQuestionAndAnswer(1L, answerText1, mainVendorId);
        addQuestionTicket(ticketKey, questionAndAnswer1.getKey());

        when(startrekService.getTicket(ticketKey)).thenReturn(ISSUE);
        when(startrekService.getPossibleTransition(eq(ISSUE), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.empty());

        executor.addVendorAnswerToTicketCtm();

        verify(startrekService, times(1)).createComment(eq(ISSUE), anyString());
        verify(startrekService, times(0)).closeTicket(eq(ISSUE));
        assertEquals(QaTicketState.CLOSE, getQaTicketState(ticketKey));
    }


    private Pair<Long, Long> createQuestionAndAnswer(long userId, String answerText) {
        final Question question = questionService.createModelQuestion(userId, QUESTION_TEXT, DEFAULT_MODEL_ID);
        long answerId = createAnswer(userId, answerText, question.getId());
        return new Pair<>(question.getId(), answerId);
    }

    private Pair<Long, Long> createQuestionAndAnswer(long userId, String answerText, long vendorId) {
        final Question question = questionService.createModelQuestion(userId, QUESTION_TEXT, DEFAULT_MODEL_ID);
        long answerId = createAnswer(userId, answerText, question.getId(), vendorId);
        return new Pair<>(question.getId(), answerId);
    }

    private long createAnswer(long userId, String answerText, long questionId) {
        final Answer answer = answerService.createAnswer(userId, answerText, questionId);
        answerService.forceUpdateModState(answer.getId(), ModState.AUTO_FILTER_PASSED);
        return answer.getId();
    }

    private long createAnswer(long userId, String answerText, long questionId, long vendorId) {
        final Answer answer = answerService.createVendorAnswer(userId, answerText, questionId, vendorId);
        answerService.forceUpdateModState(answer.getId(), ModState.AUTO_FILTER_PASSED);
        return answer.getId();
    }


    private void addQuestionTicket(String ticketKey, long questionId) {
        pgJdbcTemplate.update(
            "insert into qa.ticket (ticket_key, qa_entity_id, qa_entity_type) values (?, ?, ?)",
            ticketKey, questionId, QaEntityType.QUESTION.getValue()
        );
    }

    private QaTicketState getQaTicketState(String ticketKey) {
        return pgJdbcTemplate.query(
            "select state from qa.ticket where ticket_key = ?",
            (rs, rowNum) -> QaTicketState.byValue(rs.getInt("state")),
            ticketKey
        ).stream().findFirst().orElse(null);
    }

}

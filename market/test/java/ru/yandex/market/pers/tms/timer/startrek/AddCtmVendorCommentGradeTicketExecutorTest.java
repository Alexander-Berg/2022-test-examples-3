package ru.yandex.market.pers.tms.timer.startrek;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.comments.model.CommentModState;
import ru.yandex.market.pers.grade.core.db.GradeCommentDao;
import ru.yandex.market.pers.grade.core.model.Comment;
import ru.yandex.market.pers.grade.core.model.core.TicketState;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Transition;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
 * 01.04.2022
 */
public class AddCtmVendorCommentGradeTicketExecutorTest extends MockedPersTmsTest {
    private static final String COMMENT_TEXT = "Обычный текст для комментария c id=";
    private static final Long GRADE_ID1 = 1L;
    private static final Long GRADE_ID2 = 2L;
    private static final Long COMMENT_ID1 = 1L;
    private static final Long COMMENT_ID2 = 2L;
    private static final String TICKET_KEY1 = "QASTMTEST-1";
    private static final String TICKET_KEY2 = "QASTMTEST-2";
    private static final String DEFAULT_TIMESTAMP = "2022-04-04 03:00:00.0";
    private static final Date DEFAULT_DATE = new Date();
    private static final Issue ISSUE = new Issue(
        "1", null, TICKET_KEY1, null, 1, new EmptyMap<>(), null
    );
    private static final String COMMEND_ID_TEMPLATE = "child-0-%d";

    @Value("${pers.startrek.model.ctm.vendorId}")
    private Long vendorId;

    @Autowired
    @Qualifier("addCtmVendorCommentGradeTicketExecutor")
    private AddCtmVendorCommentGradeTicketExecutor executor;

    @Autowired
    private StartrekService startrekService;

    @Autowired
    private GradeCommentDao gradeCommentDao;

    @Autowired
    private QaClient qaClient;

    @Test
    public void testLeaveCommentAndCloseTicket() throws Exception {
        //Добавляем один тикет и один комментарий, связанный с отзывом id = 1L
        addGradeTicket(TICKET_KEY1, GRADE_ID1);
        addGradeComment(COMMENT_ID1, GRADE_ID1);

        when(qaClient.getCommentBulk(eq(CommentProject.GRADE), eq(List.of(COMMENT_ID1))))
            .thenReturn(List.of(buildComment(COMMENT_ID1, GRADE_ID1)));
        when(startrekService.getTicket(anyString())).thenReturn(ISSUE);
        when(startrekService.getPossibleTransition(eq(ISSUE), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.of(mock(Transition.class)));

        executor.runAddVendorCommentAndCLoseIssue();

        String commentText = formCommentText(COMMENT_ID1);
        verify(startrekService, times(1)).createComment(eq(ISSUE), eq(commentText));
        verify(startrekService, times(1)).closeTicket(eq(ISSUE));
        assertEquals(TicketState.CLOSE, getTicketState(TICKET_KEY1));
    }

    @Test
    public void testLeaveCommentWithExceptions() throws Exception {
        //Добавляем 2 тикета и 2 отзыва
        //У первого всё проходит хорошо, а при обработке второго вдруг сломался startrekService
        addGradeTicket(TICKET_KEY1, GRADE_ID1);
        addGradeComment(COMMENT_ID1, GRADE_ID1);
        addGradeTicket(TICKET_KEY2, GRADE_ID2);
        addGradeComment(COMMENT_ID2, GRADE_ID2);

        when(qaClient.getCommentBulk(eq(CommentProject.GRADE), eq(List.of(COMMENT_ID1, COMMENT_ID2))))
            .thenReturn(List.of(
                buildComment(COMMENT_ID1, GRADE_ID1),
                buildComment(COMMENT_ID2, GRADE_ID2)
            ));
        when(startrekService.getTicket(argThat(TICKET_KEY1::equals))).thenReturn(ISSUE);
        when(startrekService.getTicket(argThat(TICKET_KEY2::equals)))
            .thenThrow(new RuntimeException("Unable get ticket"));
        when(startrekService.getPossibleTransition(any(), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.of(mock(Transition.class)));

        try {
            executor.runAddVendorCommentAndCLoseIssue();
            Assert.fail();
        } catch (Exception ex) {
            assertEquals(
                "1 exception(s) found. First exception: Unable get ticket",
                ex.getMessage()
            );
        }

        //Здесь нет необходимости проверять используемые аргументы
        verify(startrekService, times(1)).createComment(any(), anyString());
        verify(startrekService, times(1)).closeTicket(any());
        assertEquals(TicketState.CLOSE, getTicketState(TICKET_KEY1));
        assertEquals(TicketState.OPEN, getTicketState(TICKET_KEY2));
    }

    @Test
    public void testAddOnlyFirstVendorComment() throws Exception {
        //Добавляем 1 тикет по отзыву и 3 комментария к этому отзыву
        // 2 от вендора, с разным временем создания и 1 от другого пользователя с более ранним временем
        addGradeTicket(TICKET_KEY1, GRADE_ID1);
        addGradeComment(COMMENT_ID1, GRADE_ID1, null, "2022-04-04 02:55:00.0");
        addGradeComment(COMMENT_ID2, GRADE_ID1, vendorId, "2022-04-04 03:00:00.0");
        addGradeComment(COMMENT_ID2 + 1, GRADE_ID1, vendorId, "2022-04-04 03:05:00.0");

        when(qaClient.getCommentBulk(eq(CommentProject.GRADE), eq(List.of(COMMENT_ID2))))
            .thenReturn(List.of(buildComment(COMMENT_ID2, GRADE_ID1)));
        when(startrekService.getTicket(anyString())).thenReturn(ISSUE);
        when(startrekService.getPossibleTransition(any(), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.of(mock(Transition.class)));

        executor.runAddVendorCommentAndCLoseIssue();

        //В результате должен быть добавлен комментарий с COMMENT_ID2 (он был раньше и от нужного вендора)
        String commentText = formCommentText(COMMENT_ID2);
        verify(startrekService, times(1)).createComment(eq(ISSUE), eq(commentText));
        verify(startrekService, times(1)).closeTicket(eq(ISSUE));
        assertEquals(TicketState.CLOSE, getTicketState(TICKET_KEY1));
    }

    @Test
    public void testCloseTicketAlreadyClosedManually() throws Exception {
        addGradeTicket(TICKET_KEY1, GRADE_ID1);
        addGradeComment(COMMENT_ID1, GRADE_ID1);

        when(qaClient.getCommentBulk(eq(CommentProject.GRADE), eq(List.of(COMMENT_ID1))))
            .thenReturn(List.of(buildComment(COMMENT_ID1, GRADE_ID1)));
        when(startrekService.getTicket(anyString())).thenReturn(ISSUE);
        //возвращаем пустой optional, значит нет возможности закрыть тикет
        when(startrekService.getPossibleTransition(eq(ISSUE), eq(CLOSED_TRANSITIONS)))
            .thenReturn(Optional.empty());

        executor.runAddVendorCommentAndCLoseIssue();

        String commentText = formCommentText(COMMENT_ID1);
        verify(startrekService, times(1)).createComment(eq(ISSUE), eq(commentText));
        verify(startrekService, times(0)).closeTicket(any());
        assertEquals(TicketState.CLOSE, getTicketState(TICKET_KEY1));
    }


    private void addGradeTicket(String ticketKey, Long gradeFixId) {
        pgJdbcTemplate.update(
            "insert into grade_ticket (ticket_key, grade_fix_id) values (?, ?)",
            ticketKey, gradeFixId
        );
    }

    private void addGradeComment(Long commentId, Long gradeId) {
        addGradeComment(commentId, gradeId, vendorId, DEFAULT_TIMESTAMP);
    }

    private void addGradeComment(Long commentId, Long gradeId, Long vendorId, String time) {
        Comment comment = new Comment(
            String.format(COMMEND_ID_TEMPLATE, commentId),
            gradeId,
            1L,
            vendorId,
            CommentModState.UNMODERATED,
            false,
            Timestamp.valueOf(time),
            Timestamp.valueOf(time)
        );
        gradeCommentDao.save(comment);
    }

    private CommentDto buildComment(long commentId, long gradeId) {
        CommentDto result = new CommentDto();
        result.setText(COMMENT_TEXT + commentId);
        result.setId(commentId);
        result.setEntityId(gradeId);
        result.setProjectId(CommentProject.GRADE.getId());
        result.setStateEnum(CommentState.NEW);
        result.setUser(new AuthorIdDto(UserType.UID, "123"));
        result.setAuthor(new AuthorIdDto(UserType.UID, "123"));
        result.setCreateTime(DEFAULT_DATE);
        result.setUpdateTime(DEFAULT_DATE);
        return result;
    }

    private String formCommentText(Long commentId) {
        return String.format(
            AddCtmVendorCommentGradeTicketExecutor.COMMENT_TEXT_TEMPLATE,
            DateUtil.getFullFormatString(DEFAULT_DATE), COMMENT_TEXT + commentId
        );
    }

    private TicketState getTicketState(String ticketKey) {
        return pgJdbcTemplate.query(
            "select state from grade_ticket where ticket_key = ?",
            (rs, rowNum) -> TicketState.byValue(rs.getInt("state")),
            ticketKey
        ).stream().findFirst().orElse(null);
    }

}

package ru.yandex.market.pers.grade.admin.controller.complaint;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.comments.model.Comment;
import ru.yandex.market.comments.model.CommentModState;
import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintState;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintType;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentParamDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.request.ChangeCommentRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dinyat
 *         15/05/2017
 */
public class GradeCommentComplaintControllerTest extends BaseGradeAdminDbTest {

    private final long entityId = 123456;
    private final long projectd = CommentProject.GRADE.getProjectId();
    private final long commentId = 123;
    private final Long complaintId = 12L;

    @Autowired
    private QaClient qaClient;

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testModerateCommentComplaintWithApprovedState() throws Exception {
        changeComplaintStateByModerator(ComplaintState.APPROVED_BY_MODERATOR);

        verify(qaClient, times(1)).deleteCommentById(eq(commentId));
        final ArgumentMatcher<ChangeCommentRequest> changeCommentRequestMatcher = getChangeCommentRequestMatcher(
            CommentModState.REJECTED_BY_MANAGER);
        verify(qaClient, times(1)).changeCommentProperties(eq(commentId), argThat(changeCommentRequestMatcher));
    }

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testModerateCommentComplaintWithRejectedState() throws Exception {
        changeComplaintStateByModerator(ComplaintState.REJECTED_BY_MODERATOR);

        verify(qaClient, times(0)).deleteCommentById(anyLong());
        verify(qaClient, times(0)).changeCommentProperties(anyLong(), any());
    }

    private void changeComplaintStateByModerator(ComplaintState complaintState) throws Exception {
        createCommentComplaint();
        mvc.perform(MockMvcRequestBuilders.post("/api/comment/complaint/moderate")
            .param("complaintId", String.valueOf(complaintId))
            .param("projectId", String.valueOf(projectd))
            .param("rootId", String.valueOf(entityId))
            .param("commentId", String.valueOf(commentId))
            .param("complaintState", complaintState.name()))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        final ComplaintState newState = ComplaintState.byValue(
            pgJdbcTemplate.queryForObject("select state from grade_complaint where id = ?",
                Long.class, complaintId).intValue());
        assertEquals(newState, complaintState);
    }

    @Test
    public void testGetComplaintsForModeration() throws Exception {
        long commentAuthorId = 100500;
        createCommentComplaint();

        CommentDto comment = new CommentDto();
        comment.setText("text");
        comment.setId(commentId);
        comment.setEntity(String.valueOf(entityId));
        comment.setAuthor(new AuthorIdDto(AuthorIdDto.USER, commentAuthorId));
        comment.setUser(new AuthorIdDto(AuthorIdDto.USER, commentAuthorId));
        when(qaClient.getCommentById(commentId)).thenReturn(comment);

        String response = mvc.perform(get("/api/comment/complaint/for-moderation")
            .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();
    }

    private void createCommentComplaint() {
        createCommentComplaint(1234);
    }

    private void createCommentComplaint(long authorId) {
        pgJdbcTemplate.update("insert into grade_complaint(id, source_id, reason_id, author_id, cr_time, state, type) " +
                "values(?, ?, ?, ?, now(), ?, ?)",
            complaintId, "child-0-" + commentId, 8, authorId, ComplaintState.NEW.value(), ComplaintType.COMMENT_COMPLAINT.value());
    }

    private ArgumentMatcher<ChangeCommentRequest> getChangeCommentRequestMatcher(CommentModState commentModState) {
        return new ArgumentMatcher<ChangeCommentRequest>() {
            @Override
            public boolean matches(ChangeCommentRequest o) {
                if (o instanceof ChangeCommentRequest) {
                    CommentParamDto[] paramDtos = ((ChangeCommentRequest) o).getProperties();
                    boolean result = paramDtos.length == 1;
                    result = result && Comment.STATUS_PARAM_NAME.equals(paramDtos[0].name);
                    result = result && commentModState.toString().equals(paramDtos[0].value);
                    return result;
                }
                return false;
            }
        };
    }

    private void mergeComplaints(long fromUid, long toUid) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/comment/complaint/merge")
                .param("fromUserId", String.valueOf(complaintId))
                .param("toUserId", String.valueOf(projectd)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }
}

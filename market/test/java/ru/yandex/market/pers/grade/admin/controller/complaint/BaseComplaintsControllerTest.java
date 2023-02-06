package ru.yandex.market.pers.grade.admin.controller.complaint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.qa.client.QaClient;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.client.dto.complaint.ArticleCommentComplaintDto;
import ru.yandex.market.pers.qa.client.dto.complaint.QaCommentComplaintDto;
import ru.yandex.market.pers.qa.client.model.ComplaintType;
import ru.yandex.market.pers.qa.client.model.QuestionType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author varvara
 * 27.11.2018
 */
abstract class BaseComplaintsControllerTest extends BaseGradeAdminDbTest {

    @Autowired
    protected QaClient qaClient;

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected String getContent(String resourcePath) throws IOException {
        return StreamUtils.copyToString(getClass().getClassLoader().getResourceAsStream(resourcePath), Charset.forName("UTF-8"));
    }

    @NotNull
    protected List<CommentComplaintModerationLogObject> getComplaints() {
        List<CommentComplaintModerationLogObject> list = new ArrayList<>();
        pgJdbcTemplate.query("select complaint_id, moderator_id, mod_state, type from COMMENT_COMPLAINT_MODERATION",
            rs -> { list.add(CommentComplaintModerationLogObject.valueOf(rs)); });
        return list;
    }

    protected void checkComplaint(long moderatorId, Optional<CommentComplaintModerationLogObject> complaint, long modState, ComplaintType complaintType) {
        assertTrue(complaint.isPresent());
        assertEquals(complaint.get().moderatorId, moderatorId);
        assertEquals(complaint.get().type, complaintType.getValue());
        assertEquals(complaint.get().modState, modState);
    }

    protected static class CommentComplaintModerationLogObject {
        long complaintId;
        long moderatorId;
        long modState;
        long type;

        public CommentComplaintModerationLogObject(long complaintId, long moderatorId, long modState, long type) {
            this.complaintId = complaintId;
            this.moderatorId = moderatorId;
            this.modState = modState;
            this.type = type;
        }

        public static CommentComplaintModerationLogObject valueOf(ResultSet rs) throws SQLException {
            return new CommentComplaintModerationLogObject(
                rs.getLong("complaint_id"),
                rs.getLong("moderator_id"),
                rs.getLong("mod_state"),
                rs.getLong("type"));
        }
    }

    protected void mockQaClient() {
        when(qaClient.getArticleComplaints(anyLong())).thenReturn(mockGetArticleComplaintsQaClient(1, 2));
        when(qaClient.getQaComplaints(anyLong())).thenReturn(mockGetQaModelComplaintsQaClient(1, 2));
    }

    private QAPager<ArticleCommentComplaintDto> mockGetArticleComplaintsQaClient(long... compaintIds) {
        List<ArticleCommentComplaintDto> list = new ArrayList<>(compaintIds.length);
        for (long compaintId : compaintIds) {
            ArticleCommentComplaintDto dto = new ArticleCommentComplaintDto();
            dto.setId(compaintId);
            dto.setParentCommentText(UUID.randomUUID().toString());
            dto.setCommentText(UUID.randomUUID().toString());
            dto.setComplaintText(UUID.randomUUID().toString());
            dto.setAuthor(new AuthorIdDto(AuthorIdDto.USER, 1));
            list.add(dto);
        }
        return new QAPager<>(list, new QAPager.Pager(compaintIds.length, 1, 10));
    }

    private QAPager<QaCommentComplaintDto> mockGetQaModelComplaintsQaClient(long... compaintIds) {
        List<QaCommentComplaintDto> list = new ArrayList<>(compaintIds.length);
        for (long compaintId : compaintIds) {
            QaCommentComplaintDto dto = new QaCommentComplaintDto();
            dto.setId(compaintId);
            dto.setCommentText(UUID.randomUUID().toString());
            dto.setComplaintText(UUID.randomUUID().toString());
            dto.setAuthor(new AuthorIdDto(AuthorIdDto.USER, 1));
            dto.setQuestionEntityId("1234");
            dto.setQuestionType(QuestionType.MODEL.getValue());
            dto.setQuestionText(UUID.randomUUID().toString());
            list.add(dto);
        }
        return new QAPager<>(list, new QAPager.Pager(compaintIds.length, 1, 10));
    }

}

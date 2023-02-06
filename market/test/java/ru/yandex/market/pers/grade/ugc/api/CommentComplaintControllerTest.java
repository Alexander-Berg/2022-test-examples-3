package ru.yandex.market.pers.grade.ugc.api;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.ugc.model.Complaint;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintState;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintType;
import ru.yandex.market.pers.grade.core.ugc.model.GradeComplaintReason;
import ru.yandex.market.pers.grade.ugc.api.dto.CommentComplaintRequest;
import ru.yandex.market.pers.grade.ugc.api.dto.CommentComplaintResponse;
import ru.yandex.market.pers.grade.ugc.api.dto.IdObject;
import ru.yandex.market.pers.grade.ugc.api.dto.UidObject;
import ru.yandex.market.pers.qa.client.QaClient;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.COMMENT_COMPLAINT_REASON_DICTIONARY_KEY;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.USER_COMPLAINTS_KEY_AUTH_PREFIX;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.USER_COMPLAINTS_KEY_NO_AUTH_PREFIX;

/**
 * @author dinyat
 *         27/03/2017
 */
public class CommentComplaintControllerTest extends MockedPersGradeTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GradeComplaintReason reason = new GradeComplaintReason(6, "Bad comment", ComplaintType.COMMENT_COMPLAINT);
    private final String COMPLAINT_TEXT = "Complaint about comment";
    private final long UID = 11;
    private final String YANDEX_UID = "yandex-uid";
    private final CommentComplaintRequest yandexUidComplaint = new CommentComplaintRequest(new UidObject(YANDEX_UID), new IdObject("2"), new IdObject("child-0-123"), reason, COMPLAINT_TEXT);
    private final CommentComplaintRequest uidComplaint = new CommentComplaintRequest(new UidObject(String.valueOf(UID)), new IdObject("2"), new IdObject("child-0-123"), reason, COMPLAINT_TEXT);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemCachedAgent memCachedAgent;
    @Autowired
    private QaClient qaClient;

    @Test
    @Ignore
    public void testLeaveTooManyComplaintsByUid() throws Exception {
        complaintService.setMaxCntComplaintsADay(TEST_MAX_COMPLAINTS_CNT_A_DAY);
        for (int i = 0; i < TEST_MAX_COMPLAINTS_CNT_A_DAY; i++) {
            uidComplaint.comment.id = String.valueOf(i);
            testLeaveComplaint("/api/comment/complaint/UID/" + uidComplaint.user.uid, uidComplaint);
        }
        uidComplaint.comment.id = String.valueOf(5);
        testLeaveTooManyComplaints("/api/comment/complaint/UID/" + uidComplaint.user.uid, uidComplaint);
    }

    @Test
    @Ignore
    public void testLeaveTooManyComplaintsByYandexUid() throws Exception {
        complaintService.setMaxCntComplaintsADay(TEST_MAX_COMPLAINTS_CNT_A_DAY);
        for (int i = 0; i < TEST_MAX_COMPLAINTS_CNT_A_DAY; i++) {
            yandexUidComplaint.comment.id = String.valueOf(i);
            testLeaveComplaint("/api/comment/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, yandexUidComplaint);
        }
        yandexUidComplaint.comment.id = String.valueOf(5);
        testLeaveTooManyComplaints("/api/comment/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, yandexUidComplaint);
    }

    @Test
    public void testLeaveCommentComplaintByUID() throws Exception {
        testLeaveComplaint("/api/comment/complaint/UID/" + uidComplaint.user.uid, uidComplaint);
        verify(qaClient, times(1)).createGradeCommentComplaintByUid(
            eq(UID),
            eq(uidComplaint.comment.id),
            eq(COMPLAINT_TEXT),
            anyInt()
        );
    }

    @Test
    public void testLeaveCommentComplaintByYandexUID() throws Exception {
        testLeaveComplaint("/api/comment/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, yandexUidComplaint);
        verify(qaClient, times(1)).createGradeCommentComplaintByYandexUid(
            eq(YANDEX_UID),
            eq(yandexUidComplaint.comment.id),
            eq(COMPLAINT_TEXT),
            anyInt()
        );
    }

    @Test
    public void testMappingGradeComplaintReasonsToQUid() throws Exception {
        testMappingGradeComplaintReasonsToQaUid(6, 3);
        testMappingGradeComplaintReasonsToQaUid(7, 2);
        testMappingGradeComplaintReasonsToQaUid(8, 1);
    }

    private void testMappingGradeComplaintReasonsToQaUid(int gradeId, int qaId) throws Exception {
        uidComplaint.reason.setId(gradeId);
        testLeaveComplaint("/api/comment/complaint/UID/" + uidComplaint.user.uid, uidComplaint);
        verify(qaClient, times(1)).createGradeCommentComplaintByUid(
            eq(UID),
            eq(uidComplaint.comment.id),
            eq(COMPLAINT_TEXT),
            eq(qaId)
        );
    }

    @Test
    public void testMappingGradeComplaintReasonsToQaYandexUid() throws Exception {
        testMappingGradeComplaintReasonsToQaYandexUid(6, 3);
        testMappingGradeComplaintReasonsToQaYandexUid(7, 2);
        testMappingGradeComplaintReasonsToQaYandexUid(8, 1);
    }

    private void testMappingGradeComplaintReasonsToQaYandexUid(int gradeId, int qaId) throws Exception {
        yandexUidComplaint.reason.setId(gradeId);
        testLeaveComplaint("/api/comment/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, yandexUidComplaint);
        verify(qaClient, times(1)).createGradeCommentComplaintByYandexUid(
            eq(YANDEX_UID),
            eq(yandexUidComplaint.comment.id),
            eq(COMPLAINT_TEXT),
            eq(qaId)
        );
    }

    @Test
    public void testGetUserCommentComplaintsByYandexUid() throws Exception {
        String targetYandexUid = yandexUidComplaint.user.uid;
        Complaint complaint = new Complaint(48L,
            yandexUidComplaint.comment.id,
            null,
            targetYandexUid,
            (int) yandexUidComplaint.reason.getId(),
            yandexUidComplaint.text,
            ComplaintType.COMMENT_COMPLAINT,
            ComplaintState.NEW);

        memCachedAgent.putInCache(USER_COMPLAINTS_KEY_NO_AUTH_PREFIX + targetYandexUid, singletonList(complaint));
        memCachedAgent.putInCache(COMMENT_COMPLAINT_REASON_DICTIONARY_KEY, singletonList(reason));

        testGetUserComplaints("/api/comment/complaint/YANDEXUID/" + targetYandexUid, yandexUidComplaint);
    }

    @Test
    public void testGetUserCommentComplaintsByUid() throws Exception {
        String targetUid = uidComplaint.user.uid;
        Complaint complaint = new Complaint(48L,
            uidComplaint.comment.id,
            null,
            targetUid,
            (int) uidComplaint.reason.getId(),
            uidComplaint.text,
            ComplaintType.COMMENT_COMPLAINT,
            ComplaintState.NEW);

        memCachedAgent.putInCache(USER_COMPLAINTS_KEY_AUTH_PREFIX + targetUid, singletonList(complaint));
        memCachedAgent.putInCache(COMMENT_COMPLAINT_REASON_DICTIONARY_KEY, singletonList(reason));

        testGetUserComplaints("/api/comment/complaint/UID/" + targetUid, uidComplaint);
    }

    private void testGetUserComplaints(String path, CommentComplaintRequest complaint) throws Exception {
        String jsonResponse = mockMvc.perform(get(path))
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, CommentComplaintResponse.class);
        CommentComplaintResponse response = mapper.<List<CommentComplaintResponse>>readValue(jsonResponse, type).get(0);

        Assert.assertNotNull(response.id);
        Assert.assertEquals(complaint.comment, response.comment);
        Assert.assertEquals(complaint.text, response.text);
        Assert.assertEquals(complaint.reason.getId(), response.reason.getId());
        Assert.assertEquals(complaint.reason.getName(), response.reason.getName());
        Assert.assertNull(response.reason.getType());
        Assert.assertEquals(complaint.user, response.user);
    }

    private void testLeaveComplaint(String path, CommentComplaintRequest request) throws Exception {
        String jsonResponse = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        CommentComplaintResponse response = mapper.readValue(jsonResponse, CommentComplaintResponse.class);

        Assert.assertNotNull(response.id);
        Assert.assertEquals(request.comment, response.comment);
        Assert.assertEquals(request.text, response.text);
        Assert.assertEquals(request.reason.getId(), response.reason.getId());
        Assert.assertEquals(request.reason.getName(), response.reason.getName());
        Assert.assertNull(response.reason.getType());
        Assert.assertEquals(request.user, response.user);
    }

    private void testLeaveTooManyComplaints(String path, CommentComplaintRequest request) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
            .andDo(print())
            .andReturn().getResponse();

        Assert.assertEquals(429, response.getStatus());
        Assert.assertEquals(GradeComplaintController.TOO_MANY_COMPLAINTS_ERROR, response.getErrorMessage());
    }

}

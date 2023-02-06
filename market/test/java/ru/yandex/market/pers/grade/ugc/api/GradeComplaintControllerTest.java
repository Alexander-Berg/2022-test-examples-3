package ru.yandex.market.pers.grade.ugc.api;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.db.RecheckQueueService;
import ru.yandex.market.pers.grade.core.ugc.model.Complaint;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintState;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintType;
import ru.yandex.market.pers.grade.core.ugc.model.GradeComplaintReason;
import ru.yandex.market.pers.grade.ugc.api.dto.GradeComplaintRequest;
import ru.yandex.market.pers.grade.ugc.api.dto.GradeComplaintResponse;
import ru.yandex.market.pers.grade.ugc.api.dto.IdObject;
import ru.yandex.market.pers.grade.ugc.api.dto.UidObject;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.GRADE_COMPLAINT_REASON_DICTIONARY_KEY;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.USER_COMPLAINTS_KEY_AUTH_PREFIX;
import static ru.yandex.market.pers.grade.ugc.CachedComplaintService.USER_COMPLAINTS_KEY_NO_AUTH_PREFIX;

/**
 * @author dinyat
 *         24/03/2017
 */
public class GradeComplaintControllerTest extends MockedPersGradeTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GradeComplaintReason reason = new GradeComplaintReason(1, "Bad comment", ComplaintType.GRADE_COMPLAINT);
    private final String text = "Complaint about comment";
    private final GradeComplaintRequest yandexUidComplaint = new GradeComplaintRequest(new UidObject("yandex-uid"), new IdObject("123"), reason, text);
    private final GradeComplaintRequest uidComplaint = new GradeComplaintRequest(new UidObject("11"), new IdObject("123"), reason, text);


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemCachedAgent memCachedAgent;
    @Autowired
    private RecheckQueueService recheckQueueService;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Test
    public void testLeaveTooManyComplaintsByUid() throws Exception {
        complaintService.setMaxCntComplaintsADay(TEST_MAX_COMPLAINTS_CNT_A_DAY);
        UidObject uid = new UidObject("123");
        for (int i = 0; i < TEST_MAX_COMPLAINTS_CNT_A_DAY; i++) {
            long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 720 + i);
            GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(gradeId)), reason, text);
            testLeaveComplaint("/api/grade/complaint/UID/" + uidComplaint.user.uid, shopComp);
        }
        long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 719);
        GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(gradeId)), reason, text);
        testLeaveTooManyComplaints("/api/grade/complaint/UID/" + uidComplaint.user.uid, shopComp);
    }

    @Test
    public void testLeaveTooManyComplaintsByYandexUid() throws Exception {
        complaintService.setMaxCntComplaintsADay(TEST_MAX_COMPLAINTS_CNT_A_DAY);
        UidObject uid = new UidObject("123");
        for (int i = 0; i < TEST_MAX_COMPLAINTS_CNT_A_DAY; i++) {
            long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 720 + i);
            GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(gradeId)), reason, text);
            testLeaveComplaint("/api/grade/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, shopComp);
        }
        long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 719);
        GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(gradeId)), reason, text);
        testLeaveTooManyComplaints("/api/grade/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, shopComp);
    }

    @Test
    public void testLeaveGradeComplaintByUID() throws Exception {
        testLeaveComplaints4xx("/api/grade/complaint/UID/" + uidComplaint.user.uid, uidComplaint);
        testLeaveGradeComplaint("UID", uidComplaint.user.uid);
    }

    @Test
    public void testLeaveGradeComplaintByYandexUID() throws Exception {
        testLeaveComplaints4xx("/api/grade/complaint/YANDEXUID/" + yandexUidComplaint.user.uid, yandexUidComplaint);
        testLeaveGradeComplaint("YANDEXUID", yandexUidComplaint.user.uid);
    }

    @Test
    public void testGetReasonDictionary() throws Exception {
        mockMvc.perform(get("/api/grade/complaint/dictionary")
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testGetUserGradeComplaintsByYandexUid() throws Exception {
        String targetYandexUid = yandexUidComplaint.user.uid;
        Complaint complaint = new Complaint(48L,
            yandexUidComplaint.grade.id,
            null,
            targetYandexUid,
            (int) yandexUidComplaint.reason.getId(),
            yandexUidComplaint.text,
            ComplaintType.GRADE_COMPLAINT,
            ComplaintState.NEW);

        memCachedAgent.putInCache(USER_COMPLAINTS_KEY_NO_AUTH_PREFIX + targetYandexUid, singletonList(complaint));
        memCachedAgent.putInCache(GRADE_COMPLAINT_REASON_DICTIONARY_KEY, singletonList(reason));

        testGetUserComplaints("/api/grade/complaint/YANDEXUID/" + targetYandexUid, yandexUidComplaint);
    }

    @Test
    public void testGetUserGradeComplaintsByUid() throws Exception {
        long targetUid = Long.parseLong(uidComplaint.user.uid);
        Complaint complaint = new Complaint(48L,
            uidComplaint.grade.id,
            targetUid,
            targetUid + "yanexuid",
            (int) uidComplaint.reason.getId(),
            uidComplaint.text,
            ComplaintType.GRADE_COMPLAINT,
            ComplaintState.NEW);

        memCachedAgent.putInCache(USER_COMPLAINTS_KEY_AUTH_PREFIX + targetUid, singletonList(complaint));
        memCachedAgent.putInCache(GRADE_COMPLAINT_REASON_DICTIONARY_KEY, singletonList(reason));

        testGetUserComplaints("/api/grade/complaint/UID/" + targetUid, uidComplaint);
    }

    @Test
    public void testComplaintIsInRecheckQueue() throws Exception {
        UidObject uid = new UidObject("123");
        // чтобы увидеть магазинный отзыв из фидбэка в речек кью -
        // - необходимо сперва промодерировать его автомодератором
        long shopGradeId = gradeCreator.createFeedbackGrade(720L, Long.parseLong(uid.uid), "720");
        gradeAdminService.moderateGradeReplies(List.of(shopGradeId), DbGradeAdminService.FAKE_MODERATOR, ModState.APPROVED);

        long modelGradeId = gradeCreator.createModelGrade(720L, Long.parseLong(uid.uid));
        long modelGradeId2 = gradeCreator.createModelGrade(721L, Long.parseLong(uid.uid));
        GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(shopGradeId)), reason, text);
        GradeComplaintRequest modelComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(modelGradeId)), reason, text);
        GradeComplaintRequest modelComp2 = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(modelGradeId2)), reason, text);

        testLeaveComplaint("/api/grade/complaint/UID/" + shopComp.user.uid, shopComp);
        testLeaveComplaint("/api/grade/complaint/UID/" + modelComp.user.uid, modelComp);
        testLeaveComplaint("/api/grade/complaint/UID/" + modelComp2.user.uid, modelComp2);
        Assert.assertEquals(1L, recheckQueueService.getRecheckShopGradesCount().longValue());
        Assert.assertEquals(1, recheckQueueService.getRecheckShopGrades(Long.parseLong(shopComp.user.uid), 100).size());
        Assert.assertEquals(2L, recheckQueueService.getRecheckModelGradesCount().longValue());
        Assert.assertEquals(2, recheckQueueService.getRecheckModelGrades(Long.parseLong(modelComp.user.uid), 100).size());
    }

    @Test
    public void testComplaintForNonExistingGrades() throws Exception {
        UidObject uid = new UidObject("123");
        long shopGradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 720);
        long modelGradeId = gradeCreator.createModelGrade(720L, Long.parseLong(uid.uid));
        GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(shopGradeId)), reason, text);
        GradeComplaintRequest modelComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(modelGradeId)), reason, text);
        GradeComplaintRequest modelComp2 = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(modelGradeId + 100)), reason, text);

        testLeaveComplaint("/api/grade/complaint/UID/" + shopComp.user.uid, shopComp);
        testLeaveComplaint("/api/grade/complaint/UID/" + modelComp.user.uid, modelComp);
        testLeaveComplaints4xx("/api/grade/complaint/UID/" + modelComp2.user.uid, modelComp2);
        testLeaveComplaints4xx("/api/grade/complaint/YANDEXUID/" + modelComp2.user.uid, modelComp2);
    }

    private void testLeaveGradeComplaint(String uidPath, String uidStr) throws Exception {
        UidObject uid = new UidObject("123");
        long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 720);
        GradeComplaintRequest shopComp = new GradeComplaintRequest(new UidObject("123"), new IdObject(String.valueOf(gradeId)), reason, text);
        testLeaveComplaint("/api/grade/complaint/" + uidPath + "/" + uidStr, shopComp);
    }

    private void testGetUserComplaints(String path, GradeComplaintRequest complaint) throws Exception {
        String jsonResponse = mockMvc.perform(get(path))
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, GradeComplaintResponse.class);
        GradeComplaintResponse response = mapper.<List<GradeComplaintResponse>>readValue(jsonResponse, type).get(0);

        Assert.assertNotNull(response.id);
        Assert.assertEquals(complaint.grade, response.grade);
        Assert.assertEquals(complaint.text, response.text);
        Assert.assertEquals(complaint.reason.getId(), response.reason.getId());
        Assert.assertEquals(complaint.reason.getName(), response.reason.getName());
        Assert.assertNull(response.reason.getType());
        Assert.assertEquals(complaint.user, response.user);
    }

    private void testLeaveComplaints4xx(String path, GradeComplaintRequest complaint) throws Exception {
        mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(complaint)))
            .andDo(print())
            .andExpect(status().is4xxClientError());
    }

    private void testLeaveComplaint(String path, GradeComplaintRequest request) throws Exception {
        String jsonResponse = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
            .andDo(print())
            .andReturn().getResponse().getContentAsString();

        GradeComplaintResponse response = mapper.readValue(jsonResponse, GradeComplaintResponse.class);

        Assert.assertNotNull(response.id);
        Assert.assertEquals(request.grade, response.grade);
        Assert.assertEquals(request.text, response.text);
        Assert.assertEquals(request.reason.getId(), response.reason.getId());
        Assert.assertEquals(request.reason.getName(), response.reason.getName());
        Assert.assertNull(response.reason.getType());
        Assert.assertEquals(request.user, response.user);
    }

    private void testLeaveTooManyComplaints(String path, GradeComplaintRequest request) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
            .andDo(print())
            .andReturn().getResponse();

        Assert.assertEquals(429, response.getStatus());
        Assert.assertEquals(GradeComplaintController.TOO_MANY_COMPLAINTS_ERROR, response.getErrorMessage());
    }

}

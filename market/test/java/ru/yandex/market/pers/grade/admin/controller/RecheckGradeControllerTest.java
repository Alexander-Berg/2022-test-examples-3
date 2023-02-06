package ru.yandex.market.pers.grade.admin.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.admin.article.api.dto.Count;
import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.grade.admin.controller.dto.RecheckGradeDto;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.RecheckQueueService;
import ru.yandex.market.pers.grade.core.db.RecheckQueueService.GradeComplaint;
import ru.yandex.market.pers.grade.core.model.core.GradeRecheckReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ModerationType;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.startrek.client.model.IssueCreate;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.admin.controller.RecheckTicketService.TICKET_TEMPLATE;
import static ru.yandex.market.pers.grade.admin.controller.RecheckTicketService.getLinks;

/**
 * @author varvara
 * 25.04.2019
 */
public class RecheckGradeControllerTest extends BaseGradeAdminDbTest {

    public static final String GRADE_TEXT = "Best you can afford";
    public static final String GRADE_PRO = "Fast, supports deep class III warp diving";
    public static final String GRADE_CONTRA = "Price is huge. Required experienced engineers to operate/repair";
    public static final long REASON_ID = 131;
    public static final String REASON_TEXT = "Потому что всё плохо";

    @Autowired
    private DbGradeService dbGradeService;
    @Autowired
    private RecheckQueueService recheckQueueService;
    @Autowired
    private StartrekService startrekService;
    @Autowired
    private DbGradeAdminService gradeAdminService;
    @Autowired
    private GradeCreator gradeCreator;

    @Value("${pers.startrek.model.recheck.queue.name}")
    private String queue;
    @Value("${pers.startrek.model.recheck.component.id}")
    private long component;
    @Value("${pers.grade.admin.url}")
    private String adminUrl;

    @Before
    public void prepare() {
        pgJdbcTemplate.update(
            "insert into mod_rejection_reason(id, type, name, recomendation, active, suggest_correction) " +
                "values(?,1,?,?,1,1)",
            REASON_ID, REASON_TEXT, REASON_TEXT
        );
    }

    @Test
    public void testModerateRecheckGrades() throws Exception {
        long grade1 = createModelGrade();
        addGradesToRecheckQueue(Collections.singletonList(grade1), GradeRecheckReason.DISLIKED_GRADE);

        long grade2 = createModelGrade();
        addGradesToRecheckQueue(Collections.singletonList(grade2), GradeRecheckReason.COMPLAINT);

        checkModState(grade1, ModState.APPROVED);
        checkModState(grade2, ModState.APPROVED);

        // correct moderation with APPROVED mod state
        moderate(grade2, ModState.APPROVED, null);
        checkGradeAfterRecheckModeration(grade2, ModState.APPROVED); // now grade2 in APPROVED
        checkModState(grade1, ModState.APPROVED); // grade1 still in APPROVED

        // not correct moderation with REJECTED mod state and without reason
        moderate(grade1, ModState.REJECTED, null);
        checkModState(grade1, ModState.APPROVED); // grade1 still in APPROVED

        // correct moderation with REJECTED mod state and with reason
        moderate(grade1, ModState.REJECTED, 1L);
        checkGradeAfterRecheckModeration(grade1, ModState.REJECTED); // now grade1 in REJECTED
    }

    @Test
    public void testInboxRecheckGrades() throws Exception {
        Long grade1 = createModelGrade();
        addGradesToRecheckQueue(Collections.singletonList(grade1), GradeRecheckReason.DISLIKED_GRADE);
        checkRecheckGrades(1, grade1);

        Long grade2 = createModelGrade();
        addGradesToRecheckQueue(Collections.singletonList(grade2), GradeRecheckReason.COMPLAINT);

        Long grade3 = createModelGrade();
        addGradesToRecheckQueue(Collections.singletonList(grade3), GradeRecheckReason.DISLIKED_GRADE);
        checkRecheckGrades(2, grade2, grade3); // grade1 still in inbox
    }

    @Test
    public void testInboxRecheckComplaint() throws Exception {
        Long grade1 = createModelGrade();
        Long cid = createComplaintGetId(grade1);

        recheckQueueService.addGradesWithComplaintToRecheckQueue(List.of(new GradeComplaint(grade1, cid)));

        List<RecheckGradeDto> recheckGrades = getRecheckModelGrades();
        assertEquals("Жалоба. Это реклама", recheckGrades.get(0).getReason());
    }

    @Test
    public void testInboxRecheckComplaintBanned() throws Exception {
        Long grade1 = createModelGrade();
        Long cid = createComplaintGetId(grade1);

        recheckQueueService.addGradesWithComplaintToRecheckQueue(List.of(new GradeComplaint(grade1, cid)));

        // ban
        gradeAdminService.moderate(List.of(Object4Moderation.moderated(grade1, ModState.REJECTED, 1L)), 1L);

        List<RecheckGradeDto> recheckGrades = getRecheckModelGrades();
        assertEquals(0, recheckGrades.size());
    }

    @Test
    public void testInboxRecheckComplaintDuplicates() throws Exception {

        Long grade1 = createModelGrade();
        addGradesToRecheckQueue(List.of(grade1, grade1), GradeRecheckReason.COMPLAINT);
        List<RecheckGradeDto> recheckGrades = getRecheckModelGrades();
        assertEquals(1, recheckGrades.size());

        Long grade2 = createModelGrade();
        Long grade3 = createModelGrade();
        addGradesToRecheckQueue(List.of(grade2, grade3), GradeRecheckReason.COMPLAINT);

        recheckGrades = getRecheckModelGrades();
        assertEquals(2, recheckGrades.size());
    }

    @Test
    public void testInboxRecheckComplaintDuplicateRestrictions() throws Exception {
        // Not more than 3 complaints per user
        Long grade1 = createModelGrade();
        Long grade2 = createModelGrade();
        Long grade3 = createModelGrade();
        Long grade4 = createModelGrade();
        addGradesToRecheckQueue(List.of(grade1, grade2, grade3, grade4), GradeRecheckReason.COMPLAINT);

        List<RecheckGradeDto> recheckGrades = getRecheckModelGrades();
        assertEquals(3, recheckGrades.size());

        // 3 new, 1 moderated today
        Long grade5 = createModelGrade();
        Long grade6 = createModelGrade();
        Long grade7 = createModelGrade();
        Long grade8 = createModelGrade();
        addGradesToRecheckQueue(List.of(grade5, grade6, grade7), GradeRecheckReason.COMPLAINT);
        pgJdbcTemplate.update("INSERT INTO RECHECK_GRADE_QUEUE (grade_id, cr_time, mod_time, recheck_reason) " +
                "values (?, now(), now(), ?)",
            grade8,
            GradeRecheckReason.COMPLAINT.getValue());

        recheckGrades = getRecheckModelGrades();
        assertEquals(2, recheckGrades.size());
    }

    @Test
    public void testModerateRecheckCreatesTicket() throws Exception {
        long dislikApproved = createModelGrade();
        long complaintApproved = createModelGrade();
        long dislikeRejected = createModelGrade();
        long complaintRejected = createModelGrade();

        moderate(dislikApproved, ModState.APPROVED, null);
        moderate(complaintApproved, ModState.APPROVED, null);
        moderate(dislikeRejected, ModState.APPROVED, null);
        moderate(complaintRejected, ModState.APPROVED, null);


        addGradesToRecheckQueue(Collections.singletonList(dislikApproved), GradeRecheckReason.DISLIKED_GRADE);
        addGradesToRecheckQueue(Collections.singletonList(complaintApproved), GradeRecheckReason.COMPLAINT);
        addGradesToRecheckQueue(Collections.singletonList(dislikeRejected), GradeRecheckReason.DISLIKED_GRADE);
        addGradesToRecheckQueue(Collections.singletonList(complaintRejected), GradeRecheckReason.COMPLAINT);


        // correct moderation with APPROVED mod state
        moderate(complaintApproved, ModState.APPROVED, null);
        moderate(dislikApproved, ModState.APPROVED, null);
        checkGradeAfterRecheckModeration(complaintApproved, ModState.APPROVED);
        checkGradeAfterRecheckModeration(dislikApproved, ModState.APPROVED);
        checkModState(complaintApproved, ModState.APPROVED);
        checkModState(dislikApproved, ModState.APPROVED);

        verify(startrekService, never()).createTicket(Mockito.any(IssueCreate.class));

        // correct moderation with REJECTED mod state and with reason
        moderate(dislikeRejected, ModState.REJECTED, REASON_ID);
        moderate(complaintRejected, ModState.REJECTED, REASON_ID);
        checkGradeAfterRecheckModeration(dislikeRejected, ModState.REJECTED);
        checkGradeAfterRecheckModeration(complaintRejected, ModState.REJECTED);
        checkModState(dislikeRejected, ModState.REJECTED);
        checkModState(complaintRejected, ModState.REJECTED);

        ArgumentCaptor<IssueCreate> argumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService, times(2)).createTicket(argumentCaptor.capture());

        List<IssueCreate> issues = argumentCaptor.getAllValues();
        assertEquals(2, issues.size());
        checkIssue(issues.get(0), dislikeRejected, "По дизлайкам");
        checkIssue(issues.get(1), complaintRejected, "Это реклама");
    }

    @Test
    public void testMultipleGradesModerateRecheckCreatesTicket() throws Exception {
        long dislikeRejected = createModelGrade();
        long complaintRejected = createModelGrade();

        moderate(dislikeRejected, ModState.APPROVED, null);
        moderate(complaintRejected, ModState.APPROVED, null);

        addGradesToRecheckQueue(Collections.singletonList(dislikeRejected), GradeRecheckReason.DISLIKED_GRADE);
        addGradesToRecheckQueue(Collections.singletonList(complaintRejected), GradeRecheckReason.COMPLAINT);

        moderateList(List.of(dislikeRejected, complaintRejected), ModState.REJECTED, REASON_ID);
        checkGradeAfterRecheckModeration(dislikeRejected, ModState.REJECTED);
        checkGradeAfterRecheckModeration(complaintRejected, ModState.REJECTED);
        checkModState(dislikeRejected, ModState.REJECTED);
        checkModState(complaintRejected, ModState.REJECTED);

        ArgumentCaptor<IssueCreate> argumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService, times(2)).createTicket(argumentCaptor.capture());

        List<IssueCreate> issues = argumentCaptor.getAllValues();
        assertEquals(2, issues.size());
        checkIssue(issues.get(0), dislikeRejected, "По дизлайкам");
        checkIssue(issues.get(1), complaintRejected, "Это реклама");
    }

    @Test
    public void testRecheckShopGradeController() throws Exception {

        long fbGrade = gradeCreator.createFeedbackGradeRnd();
        long shopGrade = createShopGrade();
        long modelGrade = createModelGrade();

        gradeAdminService.moderateGradeReplies(List.of(fbGrade), DbGradeAdminService.FAKE_MODERATOR, ModState.APPROVED);
        addGradesToRecheckQueue(List.of(fbGrade, shopGrade, modelGrade), GradeRecheckReason.COMPLAINT);

        assertEquals(1, getFeedbackGradesCount());
        List<RecheckGradeDto> result = getRecheckFeedbackGrades();
        assertEquals(1, result.size());
        assertEquals(GradeType.SHOP_GRADE, dbGradeService.getGrade(result.get(0).getId()).getType());

        assertEquals(1, getModelGradesCount());
        result = getRecheckModelGrades();
        assertEquals(1, result.size());
        assertEquals(GradeType.MODEL_GRADE, dbGradeService.getGrade(result.get(0).getId()).getType());
    }

    private void checkRecheckGrades(long count, Long... gradeIds) throws Exception {
        assertEquals(count, getModelGradesCount());
        final List<Long> recheckGradeIds =
            getRecheckModelGrades().stream().map(RecheckGradeDto::getId).collect(Collectors.toList());
        final List<Long> expectedIds = Arrays.asList(gradeIds);
        assertEquals(expectedIds.size(), recheckGradeIds.size());
        assertTrue(recheckGradeIds.containsAll(expectedIds));
    }

    private long createModelGrade() {
        ModelGrade modelGrade = GradeCreator.constructModelGradeRnd();
        modelGrade.setText(GRADE_TEXT);
        modelGrade.setPro(GRADE_PRO);
        modelGrade.setContra(GRADE_CONTRA);
        return gradeCreator.createGrade(modelGrade);
    }

    private long createShopGrade() {
        ShopGrade shopGrade = GradeCreator.constructShopGradeRnd();
        shopGrade.setText(GRADE_TEXT);
        shopGrade.setPro(GRADE_PRO);
        shopGrade.setContra(GRADE_CONTRA);
        return gradeCreator.createGrade(shopGrade);
    }

    private void checkIssue(IssueCreate issues, long gradeId, String reason) {
        long modelId = dbGradeService.getGrade(gradeId).getResourceId();
        assertEquals(component, ((long[]) issues.getValues().getTs("components"))[0]);
        assertEquals(queue, issues.getValues().getTs("queue"));
        assertEquals(format("Перемодерация отзыва ID %s", gradeId), issues.getValues().getTs("summary"));
        // huge expectations, I know
        String expect = (format(TICKET_TEMPLATE,
            gradeId,
            GRADE_PRO, GRADE_CONTRA, GRADE_TEXT,
            adminUrl, gradeId,
            reason,
            gradeId, "anyDate", "", FAKE_MODERATOR_ID,
            REASON_TEXT, "anyDate", "", FAKE_MODERATOR_ID,
            getLinks(gradeId, modelId, adminUrl))
            .replaceAll("\\*", "\\\\*")
            .replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")
            .replaceAll("\\.", "\\\\.").replaceAll("\\?", "\\\\?"))
            .replaceAll("anyDate", ".*");
        assertTrue(issues.getValues().getTs("description").toString().matches(expect));
    }

    private long getFeedbackGradesCount() throws Exception {
        return getCount("shop");
    }

    private long getModelGradesCount() throws Exception {
        return getCount("model");
    }

    private long getCount(String uri) throws Exception {
        Count count = FormatUtils.fromJson(
            mvc.perform(get(format("/api/recheck/grade/%s/count", uri)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<Count>() {
            });

        return count.getCount();
    }

    private void checkGradeAfterRecheckModeration(long gradeId, ModState expectedModState) {
        checkModState(gradeId, expectedModState);

        final Long moderatorId = pgJdbcTemplate.queryForObject(
            "select moderator_id from mod_grade_last where grade_id = ?",
            Long.class, gradeId);
        assertEquals(FAKE_MODERATOR_ID, moderatorId.longValue());
        final Integer moderationType = pgJdbcTemplate.queryForObject(
            "select moderation_type from mod_grade where grade_id = ? order by mod_time desc limit 1",
            Integer.class, gradeId);
        assertEquals(ModerationType.RECHECK.value(), moderationType.intValue());
    }

    private void checkModState(long gradeId, ModState expectedModState) {
        final Integer modState = pgJdbcTemplate.queryForObject("select mod_state from grade where id = ?",
            Integer.class,
            gradeId);
        assertEquals(expectedModState.value(), modState.intValue());
    }

    private List<RecheckGradeDto> getRecheckModelGrades() throws Exception {
        return getRecheckGrades("/model");
    }

    private List<RecheckGradeDto> getRecheckFeedbackGrades() throws Exception {
        return getRecheckGrades("/shop");
    }

    private List<RecheckGradeDto> getRecheckGrades(String uri) throws Exception {
        GradePager<RecheckGradeDto> result = FormatUtils.fromJson(
            mvc.perform(get("/api/recheck/grade" + uri))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<GradePager<RecheckGradeDto>>() {
            });

        return result.getData();
    }

    private void moderate(long id, ModState modState, Long reason) throws Exception {
        String body = format("{\"grades\":[" +
            "{\"id\":%s,\"modState\":%s,\"modReason\":" + (reason == null ? "null" : reason) + "}]" +
            "}", id, modState.value());

        mvc.perform(post("/api/recheck/grade/moderate")
            .content(body)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();
    }

    private void moderateList(List<Long> ids, ModState modState, Long reason) throws Exception {
        String body = ids.stream()
            .map(id -> String.format("{\"id\":%s,\"modState\":%s,\"modReason\":" +
                (reason == null ? "null" : reason) + "}", id, modState.value()))
            .collect(Collectors.joining(",", "{\"grades\":[", "]}"));

        mvc.perform(post("/api/recheck/grade/moderate")
            .content(body)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().is2xxSuccessful())
            .andDo(print())
            .andReturn().getResponse().getContentAsString();
    }

    private void addGradesToRecheckQueue(List<Long> ids, GradeRecheckReason recheckReason) {
        switch (recheckReason) {
            case DISLIKED_GRADE:
                recheckQueueService.addGradesWithDislikesToRecheckQueue(ids);
                break;
            case COMPLAINT:
                recheckQueueService.addGradesWithComplaintToRecheckQueue(
                    ids.stream()
                        .map(grade -> new GradeComplaint(grade, createComplaintGetId(grade)))
                        .collect(Collectors.toList()));
        }
    }

    private Long createComplaintGetId(Long grade1) {
        int reasonId = 7;

        Long existingComplaint = pgJdbcTemplate.query(
            "SELECT ID FROM GRADE_COMPLAINT WHERE SOURCE_ID = ? AND REASON_ID = ?",
            rs -> rs.next() ? rs.getLong("ID") : null, String.valueOf(grade1), reasonId);

        if (existingComplaint != null) {
            return existingComplaint;
        }

        Long cid = pgJdbcTemplate.queryForObject("SELECT nextval('s_grade_complaint')", Long.class);
        int update = pgJdbcTemplate.update("INSERT INTO GRADE_COMPLAINT (ID, REASON_ID, SOURCE_ID) VALUES (?, ?, ?)",
            cid, reasonId, String.valueOf(grade1));
        return cid;
    }

}

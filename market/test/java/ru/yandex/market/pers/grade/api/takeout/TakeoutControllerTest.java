package ru.yandex.market.pers.grade.api.takeout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintType;
import ru.yandex.market.pers.grade.core.ugc.model.GradeComplaintReason;
import ru.yandex.market.pers.grade.ugc.api.dto.CommentComplaintRequest;
import ru.yandex.market.pers.grade.ugc.api.dto.ComplaintRequest;
import ru.yandex.market.pers.grade.ugc.api.dto.GradeComplaintRequest;
import ru.yandex.market.pers.grade.ugc.api.dto.IdObject;
import ru.yandex.market.pers.grade.ugc.api.dto.UidObject;
import ru.yandex.market.pers.grade.web.grade.GradeControllerBaseTest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.api.ExperimentalGradeControllerTest.OFFER_GRADE_JSON;

public class TakeoutControllerTest extends MockedPersGradeTest {

    private static final long FAKE_USER = 12345L;
    private static final String VOTE_BODY = "{\n" +
        "  \"gradeId\": %s,\n" +
        "  \"authorId\": " + FAKE_USER + ",\n" +
        "  \"vote\": %d\n" +
        "}";

    private final GradeComplaintReason gradeReason = new GradeComplaintReason(1,
        "Bad comment ",
        ComplaintType.GRADE_COMPLAINT);
    private final GradeComplaintReason commentReason = new GradeComplaintReason(7,
        "Bad comment ",
        ComplaintType.COMMENT_COMPLAINT);
    private final String text = "Complaint about comment";
    private final CommentComplaintRequest commentUidComplaint =
        new CommentComplaintRequest(new UidObject(String.valueOf(
            FAKE_USER)), new IdObject("2"), new IdObject("123"), commentReason, text);
    private final GradeComplaintRequest gradeUidComplaint = new GradeComplaintRequest(new UidObject(String.valueOf(
        FAKE_USER)), new IdObject("321"), gradeReason, text);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GradeCreator gradeCreator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String performTakeout(String color) throws Exception {
        return invokeAndRetrieveResponse(get("/api/takeout")
            .param("uid", String.valueOf(FAKE_USER))
            .param("color", color), status().is2xxSuccessful());
    }

    private String getTakeoutStatus(long userId) throws Exception {
        return invokeAndRetrieveResponse(get("/takeout/status")
            .param("uid", String.valueOf(userId)), status().is2xxSuccessful());
    }

    private String deleteWithTakeout(long userId, String entity) throws Exception {
        return invokeAndRetrieveResponse(post("/takeout/delete")
            .param("uid", String.valueOf(userId))
            .param("types", entity), status().is2xxSuccessful());
    }

    private String addGrade(String gradeType,
                            String type,
                            String userId,
                            String body,
                            ResultMatcher expected) throws Exception {
        return invokeAndRetrieveResponse(
            post("/api/grade/user/" + type + "/" + userId + "/" + gradeType + "/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    private String addModelGrade(String type, String userId, String body, ResultMatcher expected) throws Exception {
        return addGrade("model", type, userId, body, expected);
    }

    private ModelGradeResponseDto addModelGrade(long userId) throws Exception {
        return objectMapper.readValue(
            addModelGrade("UID",
                String.valueOf(userId),
                GradeControllerBaseTest.ADD_MODEL_GRADE_BODY,
                status().is2xxSuccessful()),
            ModelGradeResponseDto.class);
    }

    private String addShopGrade(String type, String userId, String body, ResultMatcher expected) throws Exception {
        return addGrade("shop", type, userId, body, expected);
    }

    private ShopGradeResponseDto addShopGrade(long userId) throws Exception {
        return objectMapper.readValue(
            addShopGrade("UID",
                String.valueOf(userId),
                GradeControllerBaseTest.ADD_SHOP_GRADE_BODY,
                status().is2xxSuccessful()),
            ShopGradeResponseDto.class);
    }

    private String addExperimentalGrade(String gradeType,
                                        String userType,
                                        String userId,
                                        String body,
                                        ResultMatcher expected)
        throws Exception {
        return invokeAndRetrieveResponse(
            post("/api/exp_grade/user/" + userType + "/" + userId + "/" + gradeType + "/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

    private String addExperimentalGrade() throws Exception {
        return addExperimentalGrade("offer", "UID", String.valueOf(FAKE_USER), OFFER_GRADE_JSON,
            status().is2xxSuccessful());
    }

    private void addComplaints() throws Exception {
        leaveComplaint("/api/comment/complaint/UID/" + commentUidComplaint.user.uid, commentUidComplaint);
        leaveGradeComplaint();
    }

    private void leaveGradeComplaint() throws Exception {
        UidObject uid = new UidObject("123");
        long gradeId = gradeCreator.createShopGrade(Long.parseLong(uid.uid), 719);
        GradeComplaintRequest shopComp = new GradeComplaintRequest(uid,
            new IdObject(String.valueOf(gradeId)),
            new GradeComplaintReason(1, "Bad comment", ComplaintType.GRADE_COMPLAINT),
            text);
        leaveComplaint("/api/grade/complaint/UID/" + gradeUidComplaint.user.uid, shopComp);
    }

    private void leaveComplaint(String path, ComplaintRequest complaint) throws Exception {
        invokeAndRetrieveResponse(post(path).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(complaint)), status().is2xxSuccessful());
    }

    private void addVotes(long likeGrade, long dislikeGrade) throws Exception {
        invokeAndRetrieveResponse(post("/api/grade/vote")
            .content(String.format(VOTE_BODY, likeGrade, 1))
            .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful());
        invokeAndRetrieveResponse(post("/api/grade/vote")
            .content(String.format(VOTE_BODY, dislikeGrade, 0))
            .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful());
    }

    @Before
    public void prepareData() throws Exception {
        ModelGradeResponseDto modelGradeResponseDto = addModelGrade(FAKE_USER);
        ShopGradeResponseDto shopGradeResponseDto = addShopGrade(FAKE_USER);
        addExperimentalGrade();
        addVotes(modelGradeResponseDto.getId(), shopGradeResponseDto.getId());
        addComplaints();
    }


    @Test
    public void testGetBlueUserData() throws Exception {
        String takeoutDataWrapper = performTakeout("blue");
        JSONAssert.assertEquals(fileToString("/data/takeout/empty.json"), takeoutDataWrapper, true);
    }

    @Test
    public void testGetRedUserData() throws Exception {
        String takeoutDataWrapper = performTakeout("red");
        JSONAssert.assertEquals(
            fileToString("/data/takeout/empty.json"),
            takeoutDataWrapper,
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data.opinions[*].id", (o1, o2) -> true),
                new Customization("data.opinions[*].crTime", (o1, o2) -> true)));
    }

    @Test
    public void testGetWhiteUserData() throws Exception {
        String takeoutDataWrapper = performTakeout("white");
        JSONAssert.assertEquals(
            fileToString("/data/takeout/white_takeout.json"),
            takeoutDataWrapper,
            new CustomComparator(JSONCompareMode.STRICT,
                new Customization("data.opinions[*].crTime", (o1, o2) -> true),
                new Customization("data.opinions[*].id", (o1, o2) -> true),
                new Customization("data.votes[*].crTime", (o1, o2) -> true),
                new Customization("data.votes[*].gradeId", (o1, o2) -> true),
                new Customization("data.expOpinions[*].crTime", (o1, o2) -> true),
                new Customization("data.complaints[*].crTime", (o1, o2) -> true),
                new Customization("data.complaints[*].sourceId", (o1, o2) -> true)));
    }


    @Test
    public void testStatusDeleteUserData() throws Exception {
        // only first user have any prepared data
        assertHaveContent(FAKE_USER);
        assertNoContent(FAKE_USER + 1);

        // try to delete
        deleteWithTakeout(FAKE_USER + 1, TakeoutController.GRADE_STATUS_TYPE);
        assertHaveContent(FAKE_USER);

        deleteWithTakeout(FAKE_USER, TakeoutController.GRADE_STATUS_TYPE + 1);
        assertHaveContent(FAKE_USER);

        assertEquals(0, pgJdbcTemplate.queryForObject(
            "select count(*) from takeout_del_history", Integer.class).intValue());

        deleteWithTakeout(FAKE_USER, TakeoutController.GRADE_STATUS_TYPE);
        assertNoContent(FAKE_USER);

        assertEquals(2, pgJdbcTemplate.queryForObject(
            "select count(*) from takeout_del_history", Integer.class).intValue());
    }

    @Test
    public void testStatusDeleteUserDataMultiUser() throws Exception {
        // add data to other users - check delete changes only one of them
        addModelGrade(FAKE_USER + 1);
        addModelGrade(FAKE_USER + 2);

        assertHaveContent(FAKE_USER + 1);
        assertHaveContent(FAKE_USER + 1);
        assertHaveContent(FAKE_USER + 2);

        deleteWithTakeout(FAKE_USER, TakeoutController.GRADE_STATUS_TYPE);
        deleteWithTakeout(FAKE_USER + 1, TakeoutController.GRADE_STATUS_TYPE);
        assertNoContent(FAKE_USER + 1);
        assertNoContent(FAKE_USER + 1);
        assertHaveContent(FAKE_USER + 2);

        assertEquals(3, pgJdbcTemplate.queryForObject(
            "select count(*) from takeout_del_history", Integer.class).intValue());
    }

    private void assertHaveContent(long userId) throws Exception {
        assertEquals("{\"types\":[\"grade\"]}", getTakeoutStatus(userId));
    }

    private void assertNoContent(long userId) throws Exception {
        assertEquals("{\"types\":[]}", getTakeoutStatus(userId));
    }

}

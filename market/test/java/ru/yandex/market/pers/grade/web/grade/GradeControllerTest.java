package ru.yandex.market.pers.grade.web.grade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.api.GradeController;
import ru.yandex.market.pers.grade.api.model.Vote;
import ru.yandex.market.pers.grade.cache.GradeCacher;
import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.FactorCreator;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.db.model.GradeFilter;
import ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.core.UserGradeStats;
import ru.yandex.market.pers.grade.core.model.notification.NotificationEvent;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.model.vote.UserGradeVote;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.grade.core.service.NotificationQueueService;
import ru.yandex.market.pers.grade.core.ugc.model.RadioFactorValue;
import ru.yandex.market.pers.grade.core.util.MarketUtilsService;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_VOTED_PUSH;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_AGREE;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_REJECT;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.assertGradeVotes;

/**
 * @author korolyov
 * 13.03.17
 */
public class GradeControllerTest extends GradeControllerBaseTest {
    @Autowired
    private ReportService reportService;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private GradeCacher gradeCacher;
    @Autowired
    private DbGradeService dbGradeService;
    @Autowired
    private DbGradeVoteService dbGradeVoteService;
    @Autowired
    private NotificationQueueService notificationQueueService;
    @Autowired
    private GradeQueueService gradeQueueService;
    @Autowired
    private FactorCreator factorCreator;

    @Before
    public void clearNonShopFactors() {
        pgJdbcTemplate.update("delete from grade_factor where category_id is not null");
    }

    @Test
    public void testStats() throws Exception {
        assertEquals(0,
            (int) pgJdbcTemplate
                .queryForObject(
                    "select count(*) from grade where author_id= ?",
                    Integer.class,
                    FAKE_USER));

        createModelGradesInDifferentModStates();
        createShopGradesInDifferentModStates();
        createModelGradesWithoutTextInDifferentModStates();
        gradeCreator.createFeedbackGrade(SHOP_ID, FAKE_USER, "2134");
        gradeCreator.createFeedbackGrade(SHOP_ID + 1, FAKE_USER, "412314");
        gradeCacher.cleanForAuthorId(FAKE_USER);

        String result = invokeAndRetrieveResponse(
            get("/api/grade/user/UID/" + FAKE_USER + "/stats"),
            status().is2xxSuccessful());

        UserGradeStats statsDto = objectMapper.readValue(result, UserGradeStats.class);

        assertEquals(4, (int) statsDto.getGrades());
        assertEquals(18, (int) statsDto.getReviews());
        assertEquals(8, (int) statsDto.getRejectedReviews());
    }

    @Test
    public void testModelGradeUid() throws Exception {
        List<ModelGradeResponseDto> response = performGetModelGradesByUid();
        assertTrue(response.isEmpty());

        performCreateModelGradeUid();

        String existsResponse = invokeAndRetrieveResponse(
            get("/api/grade/user/model/" + MODEL_ID + "/exists?passportId=" + FAKE_USER)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        assertTrue(existsResponse.replaceAll("\\s+", "").contains("\"exist\":true"));

        final List<ModelGradeResponseDto> grades = performGetModelGradesByUid();
        assertFalse(grades.isEmpty());

        invokeAndRetrieveResponse(
            delete("/api/grade/" + grades.get(0).getId() + "?uid=" + FAKE_USER),
            status().is2xxSuccessful());

        assertTrue(performGetModelGradesByUid().isEmpty());
    }

    @Test
    public void testModelGradeUidWithInvalidRedGrades() throws Exception {
        List<ModelGradeResponseDto> response = performGetModelGradesByUid();
        assertTrue(response.isEmpty());

        performCreateModelGradeUid();

        List<ModelGradeResponseDto> grades = performGetModelGradesByUid();
        assertEquals(1, grades.size());

        Long gradeId = grades.get(0).getId();

        pgJdbcTemplate.update("update grade set type=? where id=?", GradeType.OFFER_GROUP_GRADE.value(), gradeId);
        resetCache();


        // should not fail, should not return old grade
        assertEquals(0, performGetModelGradesByUid().size());
    }

    @Test
    public void testModelGradeUidUnapprovedAfterUpdatePhoto() throws Exception {
        List<ModelGradeResponseDto> testResponse = performGetModelGradesByUid();
        assertTrue(testResponse.isEmpty());

        String jsonReponse = addModelGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO,
            status().is2xxSuccessful());
        ModelGradeResponseDto response = objectMapper.readValue(jsonReponse, ModelGradeResponseDto.class);
        assertEquals(ModState.UNMODERATED, response.getModState());

        moderateGrade(response.getId(), ModState.APPROVED);
        ModelGradeResponseDto grade = performGetModelGradesByUid().get(0);
        assertEquals(ModState.APPROVED, grade.getModState());

        jsonReponse = performCreateModelGradeUid();
        response = objectMapper.readValue(jsonReponse, ModelGradeResponseDto.class);
        assertEquals(ModState.UNMODERATED, response.getModState());
    }

    @Test
    public void testModelGradeInvalidFactors() throws Exception {
        String addModelGradeWithInvalidFactorValue =
            IOUtils.readInputStream(getClass()
                .getResourceAsStream("/data/factor/model_grade_with_invalid_factor_value.json"));

        addModelGrade("UID",
            String.valueOf(FAKE_USER),
            addModelGradeWithInvalidFactorValue,
            status().is4xxClientError());
    }

    @Test
    public void testModelGradeWithRadioFactorValue() throws Exception {
        Long radioFactorId = factorCreator.addRadioFactorAndReturnId("Вещь соответствует заявленному размеру?",
            1,
            10,
            true,
            List.of(
                new RadioFactorValue(0, 0, "Да"),
                new RadioFactorValue(1, 20, "Нет, большемерит"),
                new RadioFactorValue(2, 20, "Нет, большемерит")
            ));

        String addModelGradeWithRadioValue =
            IOUtils.readInputStream(getClass()
                .getResourceAsStream("/data/factor/model_grade_with_radio_value.json"))
            .replace("\"<FACTOR_ID>\"", radioFactorId.toString());

        String jsonResponse = addModelGrade("UID",
            String.valueOf(FAKE_USER),
            addModelGradeWithRadioValue,
            status().is2xxSuccessful());

        ModelGradeResponseDto response = objectMapper.readValue(jsonResponse, ModelGradeResponseDto.class);

        //validate factors in response
        JSONAssert.assertEquals(
            IOUtils.readInputStream(getClass()
                .getResourceAsStream("/data/factor/model_grade_with_radio_value_factors_result.json")),
            objectMapper.writeValueAsString(response.getFactors()),
            true
        );

        //validate factorsV2 in response
        JSONAssert.assertEquals(
            IOUtils.readInputStream(getClass()
                .getResourceAsStream("/data/factor/model_grade_with_radio_value_factorsV2_result.json"))
                .replace("\"<FACTOR_ID>\"", radioFactorId.toString()),
            objectMapper.writeValueAsString(response.getFactorsV2()),
            true
        );
    }

    @Test
    public void testModelGradeYandexUid() throws Exception {
        assertTrue(performGetModelGradesByYandexUid().isEmpty());
        performCreateModelGradeYandexUid();

        String existsResponse = invokeAndRetrieveResponse(
            get("/api/grade/user/model/" + MODEL_ID + "/exists?sessionId=" + FAKE_USER)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        assertTrue(existsResponse.replaceAll("\\s+", "").contains("\"exist\":true"));

        List<ModelGradeResponseDto> grades = performGetModelGradesByYandexUid();
        assertFalse(grades.isEmpty());

        invokeAndRetrieveResponse(
            delete("/api/grade/" + grades.get(0).getId() + "?yandexuid=" + FAKE_USER),
            status().is2xxSuccessful());

        assertTrue(performGetModelGradesByYandexUid().isEmpty());
    }

    @Test
    public void testShopGradeUid() throws Exception {
        assertTrue(performGetShopGradesByUid().isEmpty());

        performCreateShopGradeByUid();

        final List<ShopGradeResponseDto> grades = performGetShopGradesByUid();
        assertFalse(grades.isEmpty());

        invokeAndRetrieveResponse(
            delete("/api/grade/" + grades.get(0).getId() + "?uid=" + FAKE_USER),
            status().is2xxSuccessful());

        assertTrue(performGetShopGradesByUid().isEmpty());
    }

    @Test
    public void testShopGradeYandexUid() throws Exception {
        assertTrue(performGetShopGradesByYandexUid().isEmpty());
        addShopGrade("YANDEXUID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());

        List<ShopGradeResponseDto> grades = performGetShopGradesByYandexUid();
        assertFalse(grades.isEmpty());

        invokeAndRetrieveResponse(
            delete("/api/grade/" + grades.get(0).getId() + "?yandexuid=" + FAKE_USER),
            status().is2xxSuccessful());

        assertTrue(performGetShopGradesByYandexUid().isEmpty());
    }

    @Test
    public void testResolveShopGrade() throws Exception {
        createAndResolveShopGrade();
        assertTrue(performGetShopGradesByUid().get(0).isResolved());
    }

    @Test
    public void testResolveShopGradePrevious() throws Exception {
        addShopGrade("UID",
            String.valueOf(FAKE_USER),
            getAddShopGradeBody(PRO_SHOP_GRADE, "Что-то тут написано", null),
            status().is2xxSuccessful());
        ShopGradeResponseDto originalGrade = performGetShopGradesByUid().get(0);
        assertFalse(originalGrade.isResolved());

        addShopGrade("UID",
            String.valueOf(FAKE_USER),
            getAddShopGradeBody(PRO_SHOP_GRADE, "Другой текст", null),
            status().is2xxSuccessful());
        ShopGradeResponseDto grade = performGetShopGradesByUid().get(0);
        assertFalse(grade.isResolved());

        invokeAndRetrieveResponse(
            post("/api/grade/" + originalGrade.getId() + "/user/UID/" + FAKE_USER + "/resolve"),
            status().isNotFound()
        );

        List<Long> resolvedGrades = pgJdbcTemplate.queryForList(
            "select grade_id from GRADE_SHOP where grade_id in (?, ?) and RESOLVED = 1",
            Long.class,
            originalGrade.getId(), grade.getId());

        // can't resolve previous grade
        assertTrue(resolvedGrades.isEmpty());

        // can resolve last grade
        invokeAndRetrieveResponse(
            post("/api/grade/" + grade.getId() + "/user/UID/" + FAKE_USER + "/resolve"),
            status().is2xxSuccessful()
        );

        resolvedGrades = pgJdbcTemplate.queryForList(
            "select grade_id from GRADE_SHOP where grade_id in (?, ?) and RESOLVED = 1",
            Long.class,
            originalGrade.getId(), grade.getId());

        assertEquals(List.of(grade.getId()), resolvedGrades);
    }

    @Test
    public void testUnresolveShopGrade() throws Exception {
        createAndResolveShopGrade();
        invokeAndRetrieveResponse(
            post("/api/grade/" + performGetShopGradesByUid().get(0).getId() + "/user/UID/" + FAKE_USER + "/resolve")
                .param("resolved", "false"),
            status().is2xxSuccessful()
        );
        assertFalse(performGetShopGradesByUid().get(0).isResolved());

    }

    @Test
    public void testTryResolveMissingShopGrade() throws Exception {
        invokeAndRetrieveResponse(
            post("/api/grade/" + MISSING_GRADE + "/user/UID/" + FAKE_USER + "/resolve"),
            status().isNotFound()
        );
    }

    @Test
    public void testGetShopGradeByUidAndShopId() throws Exception {
        List<ShopGradeResponseDto> shopGrades = performGetShopGradeByUidAndShopId();
        assertTrue(shopGrades.isEmpty());

        ShopGradeResponseDto created = objectMapper.readValue(performCreateShopGradeByUid(),
            ShopGradeResponseDto.class);
        createVotesForGrade(created.getId());

        shopGrades = performGetShopGradeByUidAndShopId();

        assertEquals(1, shopGrades.size());
        assertFalse(shopGrades.get(0).isResolved());
        assertVotes(shopGrades.get(0));

        resolveShopGrade(created.getId());
        shopGrades = performGetShopGradeByUidAndShopId();

        assertTrue(shopGrades.isEmpty());
    }

    @Test
    public void testGetShopGradeByYandexUidAndShopId() throws Exception {
        List<ShopGradeResponseDto> shopGrades = performGetShopGradeByYandexUidAndShopId();
        assertTrue(shopGrades.isEmpty());

        ShopGradeResponseDto created = objectMapper.readValue(performCreateShopGradeByYandexUid(),
            ShopGradeResponseDto.class);
        createVotesForGrade(created.getId());

        shopGrades = performGetShopGradeByYandexUidAndShopId();

        assertEquals(1, shopGrades.size());
        assertVotes(shopGrades.get(0));
    }

    @Test
    public void testGetUserGradesCountByUid() throws Exception {
        createShopGradesInDifferentModStates();
        createModelGradesInDifferentModStates();
        createClusterGradesInDifferentModStates();

        GradeController.Count result = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(24, result.getCount());
    }

    @Test
    public void testGetUserGradesCountByUidWithDifferentUsers() throws Exception {
        String clusterBody = ADD_MODEL_GRADE_BODY
            .replaceAll(String.valueOf(MODEL_ID), String.valueOf(MODEL_ID + 1))
            .replaceAll("\"type\": 1,", "\"type\": 2,");

        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER), clusterBody, status().is2xxSuccessful());

        addModelGrade("UID", String.valueOf(FAKE_USER + 1), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
        addShopGrade("UID", String.valueOf(FAKE_USER + 1), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER + 1), clusterBody, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER + 1), ADD_MODEL_GRADE_BODY
            .replaceAll(String.valueOf(MODEL_ID), String.valueOf(MODEL_ID + 2)), status().is2xxSuccessful());

        GradeController.Count result = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(3, result.getCount());
    }

    @Test
    public void testGetUserGradesCountByUidWithSameModelId() throws Exception {
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());

        GradeController.Count result = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(1, result.getCount());
    }

    @Test
    public void testGetUserGradesCountByUidWithSameShopId() throws Exception {
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());

        GradeController.Count result = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(1, result.getCount());
    }

    @Test
    public void testGetUserGradesCountByUidWithSameClusterId() throws Exception {
        String clusterBody = ADD_MODEL_GRADE_BODY.replaceAll("\"type\": 1,", "\"type\": 2,");
        addModelGrade("UID", String.valueOf(FAKE_USER), clusterBody, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER), clusterBody, status().is2xxSuccessful());

        GradeController.Count result = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(1, result.getCount());
    }

    @Test
    public void testSaveGradeWithIp() throws Exception {
        invokeAndRetrieveResponse(
            post("/api/grade/user/UID/" + FAKE_USER + "/model/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_MODEL_GRADE_BODY)
                .header(SecurityData.HEADER_X_REAL_IP, "127.127.127.127")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());

        long gradeId = dbGradeService.loadGradesPg(new GradeFilter()).get(0).getId();

        assertEquals("127.127.127.127", pgJdbcTemplate.queryForObject(
            "select ip from security_data where grade_id = ?", String.class, gradeId));
        assertNull(pgJdbcTemplate.queryForObject(
            "select port from security_data where grade_id = ?", Integer.class, gradeId));
    }

    @Test
    public void testSaveGradeWithIpAndPort() throws Exception {
        invokeAndRetrieveResponse(
            post("/api/grade/user/UID/" + FAKE_USER + "/model/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_MODEL_GRADE_BODY)
                .header(SecurityData.HEADER_X_REAL_IP, "127.127.127.127")
                .header(SecurityData.HEADER_X_REAL_PORT, "4321")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());

        long gradeId = dbGradeService.loadGradesPg(new GradeFilter()).get(0).getId();

        assertEquals("127.127.127.127", pgJdbcTemplate.queryForObject(
            "select ip from security_data where grade_id = ?", String.class, gradeId));
        assertEquals(4321, pgJdbcTemplate.queryForObject(
            "select port from security_data where grade_id = ?", Integer.class, gradeId).intValue());
    }

    @Test
    public void testGetUserGradesCountByUidWithInvalidateCache() throws Exception {
        String clusterBody = ADD_MODEL_GRADE_BODY
            .replaceAll(String.valueOf(MODEL_ID), String.valueOf(MODEL_ID + 1))
            .replaceAll("\"type\": 1,", "\"type\": 2,");

        GradeController.Count before = performGetUserGradesCountByUid(FAKE_USER);
        addModelGrade("UID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());
        addModelGrade("UID", String.valueOf(FAKE_USER), clusterBody, status().is2xxSuccessful());
        GradeController.Count after = performGetUserGradesCountByUid(FAKE_USER);

        assertEquals(before.getCount() + 3, after.getCount());
    }

    @Test
    public void testGradeSpamStatus() throws Exception {
        performCreateShopGradeByUid();
        final ShopGradeResponseDto grade = performGetShopGradesByUid().get(0);
        assertFalse(grade.isSpam());
        markGradeAsSpam(grade.getId());
        assertTrue(performGetShopGradesByUid().get(0).isSpam());
    }

    @Test
    public void testCheckSourceOnAddModelGrade() throws Exception {
        ModelGradeResponseDto firstResponse = objectMapper.readValue(addModelGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY,
            status().isOk()), ModelGradeResponseDto.class);
        ModelGradeResponseDto secondResponse = objectMapper.readValue(addModelGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY.replaceAll(PRO_MODEL_GRADE, "Не очень красивая"),
            DIFFERENT_SOURCE,
            status().isOk()), ModelGradeResponseDto.class);
        assertNotEquals(firstResponse.getId(), secondResponse.getId());
        assertEquals(DEFAULT_SOURCE, firstResponse.getSource());
        assertEquals(firstResponse.getSource(), secondResponse.getSource());
    }

    @Test
    public void testCheckSkuOnAddModelGrade() throws Exception {
        ModelGradeResponseDto firstResponse = objectMapper.readValue(addModelGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY,
            status().isOk()), ModelGradeResponseDto.class);
        assertEquals(SKU, firstResponse.getSku());
    }

    @Test
    public void testCheckUpdateSkuOnModelGrade() throws Exception {
        // create grade without sku
        String skuField = "\"sku\": " + SKU + ",\n";

        String emptySkuBody = ADD_MODEL_GRADE_BODY.replace(skuField, "");
        ModelGradeResponseDto response = objectMapper.readValue(addModelGrade("UID", String.valueOf(FAKE_USER),
            emptySkuBody, status().isOk()), ModelGradeResponseDto.class);
        assertNull(response.getSku());

        // add sku with same test, check added
        response = objectMapper.readValue(addModelGrade("UID", String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY, status().isOk()), ModelGradeResponseDto.class);
        assertEquals(SKU, response.getSku());

        // add with null, check not changed
        response = objectMapper.readValue(addModelGrade("UID", String.valueOf(FAKE_USER),
            emptySkuBody, status().isOk()), ModelGradeResponseDto.class);
        assertEquals(SKU, response.getSku());
    }

    @Test
    public void testCheckSourceOnAddShopGrade() throws Exception {
        ShopGradeResponseDto firstResponse = objectMapper.readValue(addShopGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_SHOP_GRADE_BODY,
            status().isOk()), ShopGradeResponseDto.class);
        ShopGradeResponseDto secondResponse = objectMapper.readValue(addShopGrade("UID",
            String.valueOf(FAKE_USER),
            ADD_SHOP_GRADE_BODY.replaceAll(PRO_SHOP_GRADE, "Не очень"),
            DIFFERENT_SOURCE,
            status().isOk()), ShopGradeResponseDto.class);
        assertNotEquals(firstResponse.getId(), secondResponse.getId());
        assertEquals(DEFAULT_SOURCE, firstResponse.getSource());
        assertEquals(firstResponse.getSource(), secondResponse.getSource());
    }

    /**
     * MARKETPERS-3885
     */
    @Test
    public void textCreateModelAndClusterGradeOnSameResource() throws Exception {
        when(reportService.getModelById(anyLong())).thenReturn(Optional.of(PersCoreMockFactory.generateModel()),
            Optional.empty());
        performCreateModelGradeUid();
        performCreateModelGradeUid();
        List<ModelGradeResponseDto> result = performGetModelGradesByUid();
        assertEquals(1, result.size());
    }

    private void createSomeGrades() throws Exception {
        createAndResolveShopGrade();
        performCreateShopGradeByUid();
        performCreateModelGradeUid();
    }

    @Test
    public void testGetUserGradeByGradeId() throws Exception {
        createSomeGrades();
        List<GradeResponseDtoImpl> grades = performGetUserGradesByUid();
        assertEquals(3, grades.size());
        long gradeId = grades.get(0).getId();
        createVotesForGrade(gradeId);
        gradeCacher.cleanForAuthorId(FAKE_USER);

        grades = performGetUserGradesByUidAndGradeId(List.of(gradeId));

        assertEquals(1, grades.size());
        assertVotes(grades.get(0));
    }

    @Test
    public void testGetUserGradeWithGradeIdFilter() throws Exception {
        createSomeGrades();
        List<GradeResponseDtoImpl> grades = performGetUserGradesByUid();
        assertEquals(3, grades.size());
        long firstId = grades.get(0).getId();
        long secondId = grades.get(2).getId();

        grades = performGetUserGradesByUidAndGradeId(List.of(firstId, secondId));
        assertEquals(2, grades.size());
    }

    @Test
    public void testGetUserGradeWithInvalidGradeIdsSize() {
        List<Long> ids = new ArrayList<>();
        String url = "/api/grade/user?uid=" + FAKE_USER;
        for (int i = 0; i < 30; i++) {
            ids.add(i + 1L);
        }
        invokeAndRetrieveResponse(
            get(url).accept(MediaType.APPLICATION_JSON).param("gradeId", getIdsAsString(ids)),
            status().is4xxClientError());
    }

    @Test
    public void testDeleteWithModelFilter() throws Exception {
        createSomeGrades();
        List<GradeResponseDtoImpl> grades = performGetUserGradesByUid();
        assertEquals(3, grades.size());
        performDeleteGradeUIDWithModelFilter(MODEL_ID);
        grades = performGetUserGradesByUid();
        assertEquals(2, grades.size());
    }

    @Test
    public void testDeleteWithShopFilter() throws Exception {
        createSomeGrades();
        List<GradeResponseDtoImpl> grades = performGetUserGradesByUid();
        assertEquals(3, grades.size());
        performDeleteGradeUIDWithShopFilter(SHOP_ID);
        grades = performGetUserGradesByUid();
        assertEquals(1, grades.size());
    }

    @Test
    public void testGetUserGradesForModelUid() throws Exception {
        ModelGradeResponseDto responseDto = objectMapper.readValue(
            performCreateModelGradeUid(),
            ModelGradeResponseDto.class);
        long gradeId = responseDto.getId();
        createVotesForGrade(gradeId);

        List<ModelGradeResponseDto> response = performGetModelGradeByUidAndModelId();

        assertEquals(1, response.size());
        assertVotes(response.get(0));
    }

    private String performCreateModelGradeUid() throws Exception {
        return performCreateModelGrade("UID");
    }

    private String performCreateModelGrade(String type) throws Exception {
        return addModelGrade(type, String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
    }

    private String performCreateModelGradeWithText(String type, String text) throws Exception {
        return addModelGrade(type, String.valueOf(FAKE_USER),
            ADD_MODEL_GRADE_BODY.replace(STD_MODEL_TEXT, text),
            status().is2xxSuccessful());
    }

    @Test
    public void testGetUserGradesForModelYandexUid() throws Exception {
        ModelGradeResponseDto responseDto = objectMapper.readValue(
            performCreateModelGradeYandexUid(),
            ModelGradeResponseDto.class);
        long gradeId = responseDto.getId();
        createVotesForGrade(gradeId);

        List<ModelGradeResponseDto> response = performGetModelGradeByYandexUidAndModelId();

        assertEquals(1, response.size());
        assertVotes(response.get(0));
    }

    private String performCreateModelGradeYandexUid() throws Exception {
        return performCreateModelGrade("YANDEXUID");
    }

    @Test
    public void testAddUserVoteUid() throws Exception {
        assertTrue(dbGradeVoteService.getUserGradeVotes(FAKE_USER).isEmpty());

        ShopGradeResponseDto shopGrade =
            objectMapper.readValue(performCreateShopGradeByUid(), ShopGradeResponseDto.class);
        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);
        List<Long> expectedVoteGradeIds = Arrays.asList(shopGrade.getId(), modelGrade.getId());

        addVote(shopGrade.getId(), FAKE_USER, 1);
        addVote(modelGrade.getId(), FAKE_USER, 0);
        addVote(modelGrade.getId(), FAKE_USER + 1, 1);
        addVote(shopGrade.getId(), FAKE_USER + 1, 0);

        List<UserGradeVote> votes = dbGradeVoteService.getUserGradeVotes(FAKE_USER);
        List<Long> actualVoteGradeIds = votes.stream().map(vote -> vote.gradeId).collect(Collectors.toList());

        assertEquals(expectedVoteGradeIds.size(), actualVoteGradeIds.size());
        assertTrue(expectedVoteGradeIds.containsAll(actualVoteGradeIds));
    }

    @Test
    public void testAddUserVotePush() throws Exception {
        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);

        // add like - check event added
        addVote(modelGrade.getId(), FAKE_USER, 1);

        List<UserGradeVote> votes = dbGradeVoteService.getUserGradeVotes(FAKE_USER);
        assertEquals(1, votes.size());

        assertEquals(1, notificationQueueService.getNewEventsCount());

        List<NotificationEvent> events = notificationQueueService.getNewEvents(NOTIFY_GRADE_VOTED_PUSH, 10);
        assertEquals(1, events.size());

        NotificationEvent event = events.get(0);
        assertEquals(modelGrade.getId().longValue(), event.getDataLong(MarketUtilsService.KEY_GRADE_ID));
        assertEquals(votes.get(0).id.longValue(), event.getDataLong(MarketUtilsService.KEY_VOTE_ID));

        // add dislike - check no more events
        addVote(modelGrade.getId(), FAKE_USER + 1, 0);
        assertEquals(1, notificationQueueService.getNewEventsCount());
    }

    @Test
    public void testAddMultipleVotesFromSingleUid() throws Exception {
        assertTrue(dbGradeVoteService.getUserGradeVotes(FAKE_USER).isEmpty());

        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);

        addVote(modelGrade.getId(), FAKE_USER, 0);

        List<UserGradeVote> votes = dbGradeVoteService.getUserGradeVotes(FAKE_USER);
        assertEquals(1, votes.size());
        assertEquals(modelGrade.getId(), votes.get(0).gradeId);
        assertEquals(GradeVoteKind.reject, votes.get(0).gradeVoteKind);

        addVote(modelGrade.getId(), FAKE_USER, 1);

        votes = dbGradeVoteService.getUserGradeVotes(FAKE_USER);
        assertEquals(1, votes.size());
        assertEquals(modelGrade.getId(), votes.get(0).gradeId);
        assertEquals(GradeVoteKind.agree, votes.get(0).gradeVoteKind);
    }

    @Test
    public void testAddUserVoteYandexUid() throws Exception {
        assertTrue(dbGradeVoteService.getUserGradeVotes(FAKE_YANDEXUID).isEmpty());

        ShopGradeResponseDto shopGrade =
            objectMapper.readValue(performCreateShopGradeByUid(), ShopGradeResponseDto.class);
        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);

        addVoteYandexUid(shopGrade.getId(), FAKE_YANDEXUID, 1);
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 0);
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID + "1", 1);
        addVoteYandexUid(shopGrade.getId(), FAKE_YANDEXUID + "1", 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID + "1"),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to update vote
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 1);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            shopGrade.getId(), 1,
            modelGrade.getId(), 1);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID + "1"),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to update back (this was a real case!)
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID + "1"),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to remove votes
        deleteVote(modelGrade.getId(), null, FAKE_YANDEXUID);
        deleteVote(shopGrade.getId(), null, FAKE_YANDEXUID + "1");

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            shopGrade.getId(), 1);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID + "1"),
            modelGrade.getId(), 1);

        // try to recover deleted vote
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 0);
        addVoteYandexUid(shopGrade.getId(), FAKE_YANDEXUID + "1", 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID + "1"),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // check consistency
        assertEquals(0, dbGradeVoteService.getDuplicatedVotesCount());
    }

    @Test
    public void testAddMultipleVotesFromSingleYandexUid() throws Exception {
        assertTrue(dbGradeVoteService.getUserGradeVotes(FAKE_USER).isEmpty());

        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);

        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 0);

        List<UserGradeVote> votes = dbGradeVoteService.getUserGradeVotes(FAKE_YANDEXUID);
        assertEquals(1, votes.size());
        assertEquals(modelGrade.getId(), votes.get(0).gradeId);
        assertEquals(GradeVoteKind.reject, votes.get(0).gradeVoteKind);

        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 1);
        votes = dbGradeVoteService.getUserGradeVotes(FAKE_YANDEXUID);
        assertEquals(1, votes.size());
        assertEquals(modelGrade.getId(), votes.get(0).gradeId);
        assertEquals(GradeVoteKind.agree, votes.get(0).gradeVoteKind);
    }


    @Test
    public void testGetUserVotes() throws Exception {
        ShopGradeResponseDto shopGrade =
            objectMapper.readValue(performCreateShopGradeByUid(), ShopGradeResponseDto.class);
        ModelGradeResponseDto modelGrade =
            objectMapper.readValue(performCreateModelGradeUid(), ModelGradeResponseDto.class);

        addVote(modelGrade.getId(), FAKE_USER, 0);
        addVote(shopGrade.getId(), FAKE_USER, 1);
        addVote(modelGrade.getId(), FAKE_USER + 1, 1);
        addVote(shopGrade.getId(), FAKE_USER + 1, 0);
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID, 0);
        addVoteYandexUid(shopGrade.getId(), FAKE_YANDEXUID, 1);
        addVoteYandexUid(modelGrade.getId(), FAKE_YANDEXUID + "1", 1);
        addVoteYandexUid(shopGrade.getId(), FAKE_YANDEXUID + "1", 0);

        List<Vote> votesUid = getUserVotesUid();
        List<Vote> votesYandexUid = getUserVotesYandexUid();

        assertEquals(2, votesUid.size());
        assertTrue(votesUid.stream()
            .map(Vote::getGradeId)
            .collect(Collectors.toList())
            .containsAll(Arrays.asList(modelGrade.getId(), shopGrade.getId())));
        assertEquals(2, votesYandexUid.size());
        assertTrue(votesYandexUid.stream()
            .map(Vote::getGradeId)
            .collect(Collectors.toList())
            .containsAll(Arrays.asList(modelGrade.getId(), shopGrade.getId())));

        // check votes
        assertVotes(getUserVotesUid(FAKE_USER),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesUid(FAKE_USER + 1),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to update vote
        addVote(modelGrade.getId(), FAKE_USER, 1);

        assertVotes(getUserVotesUid(FAKE_USER),
            shopGrade.getId(), 1,
            modelGrade.getId(), 1);

        assertVotes(getUserVotesUid(FAKE_USER + 1),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to update vote back
        addVote(modelGrade.getId(), FAKE_USER, 0);

        assertVotes(getUserVotesUid(FAKE_USER),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesUid(FAKE_USER + 1),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // try to remove votes
        deleteVote(modelGrade.getId(), FAKE_USER, null);
        deleteVote(shopGrade.getId(), FAKE_USER + 1, null);

        assertVotes(getUserVotesUid(FAKE_USER),
            shopGrade.getId(), 1);

        assertVotes(getUserVotesUid(FAKE_USER + 1),
            modelGrade.getId(), 1);

        // try to recover deleted vote
        addVote(modelGrade.getId(), FAKE_USER, 0);
        addVote(shopGrade.getId(), FAKE_USER + 1, 0);

        assertVotes(getUserVotesUid(FAKE_USER),
            shopGrade.getId(), 1,
            modelGrade.getId(), 0);

        assertVotes(getUserVotesUid(FAKE_USER + 1),
            shopGrade.getId(), 0,
            modelGrade.getId(), 1);

        // check consistency
        assertEquals(0, dbGradeVoteService.getDuplicatedVotesCount());
    }

    @Test
    public void testGetUserVotesFixId() throws Exception {
        ShopGradeResponseDto shopGrade = objectMapper.readValue(performCreateShopGradeByUid(),
            ShopGradeResponseDto.class);
        ModelGradeResponseDto modelGradeOld = objectMapper.readValue(performCreateModelGradeWithText("UID", "text"),
            ModelGradeResponseDto.class);

        addVote(modelGradeOld.getId(), FAKE_USER, 0);
        addVoteYandexUid(modelGradeOld.getId(), FAKE_YANDEXUID, 1);

        ModelGradeResponseDto modelGradeNew = objectMapper.readValue(performCreateModelGradeWithText("UID", "text2"),
            ModelGradeResponseDto.class);

        List<Vote> votesUid = getUserVotesUid();
        List<Vote> votesYandexUid = getUserVotesYandexUid();

        // check votes
        assertVotes(getUserVotesUid(FAKE_USER),
            modelGradeOld.getId(), 0,
            modelGradeNew.getId(), 0);

        assertVotes(getUserVotesYandexUid(FAKE_YANDEXUID),
            modelGradeOld.getId(), 1,
            modelGradeNew.getId(), 1);

        // try to update vote
        addVote(modelGradeOld.getId(), FAKE_USER, 1);

        assertVotes(getUserVotesUid(FAKE_USER),
            modelGradeOld.getId(), 1,
            modelGradeNew.getId(), 1);

        // try to remove votes
        deleteVote(modelGradeNew.getId(), FAKE_USER, null);

        assertVotes(getUserVotesUid(FAKE_USER));

        // try to recover deleted vote
        addVote(modelGradeNew.getId(), FAKE_USER, 0);

        assertVotes(getUserVotesUid(FAKE_USER),
            modelGradeOld.getId(), 0,
            modelGradeNew.getId(), 0);

        // check consistency
        assertEquals(0, dbGradeVoteService.getDuplicatedVotesCount());
    }


    public void assertVotes(List<Vote> votes, long... data) {
        // data is gradeId, voteKind[, gradeId, voteKind...]
        assertEquals(0, data.length % 2);

        Map<Long, Long> expected = new HashMap<>();
        for (int i = 0; i < data.length; i += 2) {
            expected.put(data[i], data[i + 1]);
        }

        Map<Long, Vote> votesMap = ListUtils.toMap(votes, x -> x.gradeId);
        assertEquals(expected.size(), votesMap.size());
        expected.forEach((gradeId, voteKind) -> {
            assertTrue(votesMap.containsKey(gradeId));
            assertEquals(voteKind.intValue(), votesMap.get(gradeId).vote);
        });
    }

    @Test
    public void testFixIdOldNull() throws Exception {
        ModelGradeResponseDto modelGradeOld = objectMapper.readValue(performCreateModelGradeWithText("UID", "text"),
            ModelGradeResponseDto.class);

        pgJdbcTemplate.update("update grade set fix_id = null where id = ?", modelGradeOld.getId());

        ModelGradeResponseDto modelGradeNew = objectMapper.readValue(performCreateModelGradeWithText("UID", "text2"),
            ModelGradeResponseDto.class);

        // ensure grades are not same
        assertNotEquals(modelGradeOld.getId(), modelGradeNew.getId());

        // ensure grades are linked with fix_id
        assertEquals(modelGradeOld.getId(), modelGradeOld.getFixId());
        assertEquals(modelGradeOld.getFixId(), modelGradeNew.getFixId());
    }

    @Test
    public void testFixIdOld() throws Exception {
        ModelGradeResponseDto modelGradeOld = objectMapper.readValue(performCreateModelGradeWithText("UID", "text"),
            ModelGradeResponseDto.class);

        ModelGradeResponseDto modelGradeNew = objectMapper.readValue(performCreateModelGradeWithText("UID", "text2"),
            ModelGradeResponseDto.class);

        // ensure grades are not same
        assertNotEquals(modelGradeOld.getId(), modelGradeNew.getId());

        // ensure grades are linked with fix_id
        assertEquals(modelGradeOld.getId(), modelGradeOld.getFixId());
        assertEquals(modelGradeOld.getFixId(), modelGradeNew.getFixId());
    }

    /*
     * MARKETPERS-5061
     * 1. создаём отзыв от неавторизованного пользователя
     * 2. авторизуемся
     * 3. создаём ещё один отзыв от неавторизованного пользователя
     * 4. отзыв, привязанный к логину после пункта 2 не должен стать предшествующим
     *
     * Использую здесь FAKE_USER и в качестве uid и в качестве yandexUid
     * */
    @Test
    public void testCreateUnloginGradesOnSameYandexUidAfterAutorization() throws Exception {
        addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());
        bindGrades(FAKE_USER, String.valueOf(FAKE_USER));
        List<ModelGradeResponseDto> getModelGradesUidResponse = performGetModelGradeByUidAndModelId();
        assertEquals(1, getModelGradesUidResponse.size());
        addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY.replace("Модель просто супер",
            "Модели получше видели"), status().is2xxSuccessful());
        gradeCacher.cleanForAuthorId(FAKE_USER);
        getModelGradesUidResponse = performGetModelGradeByUidAndModelId();
        assertEquals(1, getModelGradesUidResponse.size());
        List<ModelGradeResponseDto> getModelGradesYandexUidResponse = performGetModelGradeByYandexUidAndModelId();
        assertEquals(2, getModelGradesYandexUidResponse.size());
    }

    @Test
    public void testDoubleShopFeedbackGradesWithDifferentOrEmptyOrder() throws Exception {
        addShopGrade(
            "UID",
            String.valueOf(FAKE_USER),
            getAddShopGradeBody(SHOP_ID, 5, "все супер!", "застрял....", "пользоваться можно", GradeSource.FEEDBACK.value()),
            status().is4xxClientError()
        );
        addShopGrade(
            "UID",
            String.valueOf(FAKE_USER),
            getAddShopGradeBody(SHOP_ID, 3, "отвратительно! унесли прямо живым", "умер по дороге", "как так можно вести дела???", GradeSource.FEEDBACK.value()),
            status().is4xxClientError()
        );
    }

    @Test
    public void testSameOrderIdGradeFromFeedbackAndDefaultSource() throws Exception {
        ShopGrade grade1 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade1.setSource(GradeSource.FEEDBACK.value());
        grade1.setPro("все супер!");
        grade1.setContra("застрял....");
        grade1.setAverageGrade(5);

        ShopGrade grade2 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade2.setSource(DEFAULT_SOURCE);
        grade2.setPro("отвратительно! унесли прямо живым");
        grade2.setContra("умер по дороге");
        grade2.setAverageGrade(3);

        long gradeId1 = gradeCreator.createGrade(grade1);
        long gradeId2 = gradeCreator.createGrade(grade2);

        AbstractGrade defaultGrade = dbGradeService.getGrade(gradeId2);
        assertPrevious(dbGradeService.getGrade(gradeId1));
        assertLast(defaultGrade);
        assertEquals(GradeSource.FEEDBACK.value(), defaultGrade.getSource());
        assertEquals(DEFAULT_SOURCE, defaultGrade.getRealSource());
    }

    @Test
    public void testFeedbackGradeAfterDefaultAfterFeedback() throws Exception {
        ShopGrade grade1 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade1.setSource(GradeSource.FEEDBACK.value());
        grade1.setPro("все супер!");
        grade1.setContra("застрял....");
        grade1.setAverageGrade(5);

        ShopGrade defGrade = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade.setSource(DEFAULT_SOURCE);
        defGrade.setPro("магазин просто бомба, готов там жить");
        defGrade.setContra("взорвался!");
        defGrade.setAverageGrade(4);
        defGrade.setOrderId("1234");

        ShopGrade grade2 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade2.setSource(GradeSource.FEEDBACK.value());
        grade2.setPro("отвратительно! унесли прямо живым");
        grade2.setContra("умер по дороге");
        grade2.setAverageGrade(3);

        long fb1 = gradeCreator.createGrade(grade1);

        long defGr = gradeCreator.createGrade(defGrade);

        long fb2 = gradeCreator.createGrade(grade2);

        assertPrevious(dbGradeService.getGrade(fb1));
        assertPrevious(dbGradeService.getGrade(defGr));
        assertLast(dbGradeService.getGrade(fb2));
    }

    @Test
    public void testFeedbackShopGradesAndDefaultAfter() throws Exception {
        ShopGrade grade1 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade1.setSource(GradeSource.FEEDBACK.value());
        grade1.setPro("все супер!");
        grade1.setContra("застрял....");
        grade1.setAverageGrade(5);

        ShopGrade grade2 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        grade2.setSource(GradeSource.FEEDBACK.value());
        grade2.setPro("все понравилось, качественные материалы!");
        grade2.setContra("оставил в магазине.");
        grade2.setOrderId("43231");
        grade2.setAverageGrade(5);

        ShopGrade defGrade1 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade1.setSource(DEFAULT_SOURCE);
        defGrade1.setPro("магазин просто бомба, готов там жить");
        defGrade1.setContra("взорвался!");
        defGrade1.setAverageGrade(4);
        defGrade1.setOrderId(null);

        ShopGrade defGrade2 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade2.setSource(DEFAULT_SOURCE);
        defGrade2.setPro("отличный товар, быстро обслужили");
        defGrade2.setContra("нет недостатков, но обслужили");
        defGrade2.setAverageGrade(4);
        defGrade2.setOrderId("123451");

        long feedbackGradeId1 = gradeCreator.createGrade(grade1);
        long feedbackGradeId2 = gradeCreator.createGrade(grade2);
        long defGradeId1 = gradeCreator.createGrade(defGrade1);
        long defGradeId2 = gradeCreator.createGrade(defGrade2);

        assertLast(dbGradeService.getGrade(feedbackGradeId1));
        assertLast(dbGradeService.getGrade(feedbackGradeId2));
        assertPrevious(dbGradeService.getGrade(defGradeId1));
        assertLast(dbGradeService.getGrade(defGradeId2));

    }

    @Test
    public void testDefaultSourceGradeWithExistingFeedbackGrades() throws Exception {
        ShopGrade defGrade1 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade1.setSource(DEFAULT_SOURCE);
        defGrade1.setPro("магазин просто бомба, готов там жить");
        defGrade1.setContra("взорвался!");
        defGrade1.setAverageGrade(4);
        defGrade1.setOrderId(null);
        long defId1 = gradeCreator.createGrade(defGrade1);

        ShopGrade defGrade2 = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade2.setSource(DEFAULT_SOURCE);
        defGrade2.setPro("отличный товар, быстро обслужили");
        defGrade2.setContra("нет недостатков, но обслужили");
        defGrade2.setAverageGrade(4);
        defGrade2.setOrderId("123451");
        long defId2 = gradeCreator.createGrade(defGrade2);

        ShopGrade fbGrade = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        fbGrade.setSource(GradeSource.FEEDBACK.value());
        fbGrade.setPro("все супер!");
        fbGrade.setContra("застрял....");
        fbGrade.setAverageGrade(5);
        long id1 = gradeCreator.createGrade(fbGrade);

        ShopGrade defGrade = GradeCreator.constructShopGrade(SHOP_ID, FAKE_USER);
        defGrade.setSource(DEFAULT_SOURCE);
        defGrade.setPro("отвратительно! унесли прямо живым");
        defGrade.setContra("умер по дороге");
        defGrade.setAverageGrade(3);
        long id2 = gradeCreator.createGrade(defGrade);

        assertPrevious(dbGradeService.getGrade(defId1));
        assertLast(dbGradeService.getGrade(defId2));

        AbstractGrade feedbackGrade = dbGradeService.getGrade(id1);
        AbstractGrade defaultGrade = dbGradeService.getGrade(id2);

        assertPrevious(feedbackGrade);
        assertEquals(GradeSource.FEEDBACK.value(), feedbackGrade.getSource());
        assertEquals(GradeSource.FEEDBACK.value(), feedbackGrade.getRealSource());

        assertLast(defaultGrade);
        assertEquals(GradeSource.FEEDBACK.value(), defaultGrade.getSource());
        assertEquals(DEFAULT_SOURCE, defaultGrade.getRealSource());

    }

    @Test
    public void countUserGradesWithFeedback() throws Exception {
        gradeCreator.createShopGrade(FAKE_USER, 123451L);
        gradeCreator.createFeedbackGrade(SHOP_ID, FAKE_USER, "order1");
        gradeCreator.createFeedbackGrade(SHOP_ID + 1, FAKE_USER, "order2");

        gradeCacher.cleanForUser(new AuthorIdAndYandexUid(FAKE_USER, null));

        assertEquals(3, performGetUserGradesCount().getCount());
        List<GradeResponseDtoImpl> response = performGetUserGradesByUid();
        assertEquals(3, response.size());
        assertTrue(response.stream().anyMatch(e -> GradeSource.FEEDBACK.value().equals(e.getSource())));
    }

    private void assertLast(AbstractGrade grade) {
        assertEquals(GradeState.LAST, grade.getState());
    }

    private void assertPrevious(AbstractGrade grade) {
        assertEquals(GradeState.PREVIOUS, grade.getState());
    }

    private List<Vote> getUserVotesYandexUid() throws Exception {
        return getUserVotesYandexUid(FAKE_YANDEXUID);
    }

    private List<Vote> getUserVotesYandexUid(String yandexUid) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(get("/api/grade/vote")
                    .param("yandexUid", yandexUid)
                , status().is2xxSuccessful()),
            new TypeReference<List<Vote>>() {
            }
        );
    }

    private List<Vote> getUserVotesUid() throws Exception {
        return getUserVotesUid(FAKE_USER);
    }

    private void mergeVotes(long fromUserId, long toUserId) throws Exception {
        invoke(post("/api/grade/vote/merge?fromUserId=" + fromUserId + "&toUserId=" + toUserId),
            status().is2xxSuccessful());
    }

    private List<Vote> getUserVotesUid(long uid) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(get("/api/grade/vote?userId=" + uid), status().is2xxSuccessful()),
            new TypeReference<List<Vote>>() {
            });
    }

    private void createVotesForGrade(long gradeId) {
        createTestVotesForGrade(gradeId, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);
    }

    private void assertVotes(GradeResponseDto grade) {
        assertGradeVotes(grade, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);
    }

    private GradeController.Count performGetUserGradesCount() throws IOException {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/api/grade/user/count?uid=" + FAKE_USER),
            status().is2xxSuccessful()
        ), new TypeReference<GradeController.Count>() {});
    }

    private List<ShopGradeResponseDto> performGetShopGradeByUidAndShopId() throws Exception {
        return objectMapper.readValue(performGetShopGrade("UID"), new TypeReference<List<ShopGradeResponseDto>>() {
        });
    }

    private List<ShopGradeResponseDto> performGetShopGradeByYandexUidAndShopId() throws Exception {
        return objectMapper.readValue(performGetShopGrade("YANDEXUID"),
            new TypeReference<List<ShopGradeResponseDto>>() {
            });
    }

    private List<ModelGradeResponseDto> performGetModelGradeByUidAndModelId() throws Exception {
        return objectMapper.readValue(performGetModelGrade("UID"), new TypeReference<List<ModelGradeResponseDto>>() {
        });
    }

    private List<ModelGradeResponseDto> performGetModelGradeByYandexUidAndModelId() throws Exception {
        return objectMapper.readValue(performGetModelGrade("YANDEXUID"),
            new TypeReference<List<ModelGradeResponseDto>>() {
            });
    }

    private void performDeleteGradeUIDWithShopFilter(Long shopId) throws Exception {
        invoke(
            delete("/api/grade/user" + "?uid=" + FAKE_USER + "&shopId=" + shopId),
            status().is2xxSuccessful()
        );
    }

    private void performDeleteGradeUIDWithModelFilter(Long modelId) throws Exception {
        invoke(
            delete("/api/grade/user" + "?uid=" + FAKE_USER + "&modelId=" + modelId),
            status().is2xxSuccessful()
        );
    }

    private String performGetShopGrade(String userType) throws Exception {
        return invokeAndRetrieveResponse(
            get("/api/grade/user/" + userType + "/" + FAKE_USER + "/shop/" + SHOP_ID),
            status().is2xxSuccessful()
        );
    }

    private String performGetModelGrade(String userType) throws Exception {
        return invokeAndRetrieveResponse(
            get("/api/grade/user/" + userType + "/" + FAKE_USER + "/model/" + MODEL_ID),
            status().is2xxSuccessful()
        );
    }

    private String performCreateShopGrade(String userType) throws Exception {
        return addShopGrade(userType, String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());
    }

    private String performCreateShopGradeByUid() throws Exception {
        return performCreateShopGrade("UID");
    }

    private String performCreateShopGradeByYandexUid() throws Exception {
        return performCreateShopGrade("YANDEXUID");
    }

    private void resolveShopGrade(long gradeId) throws Exception {
        // clean indexing queue for this grade
        gradeQueueService.deleteFromQueue(gradeId);
        assertFalse(gradeQueueService.isInQueue(gradeId));

        invokeAndRetrieveResponse(
            post("/api/grade/" + gradeId + "/user/UID/" + FAKE_USER + "/resolve"),
            status().is2xxSuccessful()
        );

        // check grade is added to indexing queue afterwards
        assertTrue(gradeQueueService.isInQueue(gradeId));
    }

    private void createAndResolveShopGrade() throws Exception {
        performCreateShopGradeByUid();
        List<ShopGradeResponseDto> grades = performGetShopGradesByUid();
        assertFalse(grades.get(0).isResolved());
        resolveShopGrade(grades.get(0).getId());
    }

    private void createModelGradesWithoutTextInDifferentModStates() throws Exception {
        //"automatically_rejected" means "approved" for grades without text
        createAndModerateModelGradeWithoutText(11, ModState.AUTOMATICALLY_REJECTED);
        createAndModerateModelGradeWithoutText(22, ModState.REJECTED);
        createAndModerateModelGradeWithoutText(33, ModState.SPAMMER);
        createAndModerateModelGradeWithoutText(44, ModState.REJECTED_BY_SHOP_CLAIM);
    }

    private void createShopGradesInDifferentModStates() throws Exception {
        createAndModerateShopGrade(23L, ModState.APPROVED);
        createAndModerateShopGrade(234L, ModState.READY);
        createAndModerateShopGrade(2345L, ModState.UNMODERATED);
        createAndModerateShopGrade(23456L, ModState.DELAYED);
        //grades below count as "rejected"
        createAndModerateShopGrade(234567L, ModState.REJECTED);
        createAndModerateShopGrade(2345678L, ModState.REJECTED_BY_SHOP_CLAIM);
        createAndModerateShopGrade(23456789L, ModState.SPAMMER);
        createAndModerateShopGrade(234567890L, ModState.AUTOMATICALLY_REJECTED);
    }

    private void createModelGradesInDifferentModStates() throws Exception {
        createAndModerateModelGrade(12L, ModState.APPROVED);
        createAndModerateModelGrade(123L, ModState.READY);
        createAndModerateModelGrade(1234L, ModState.UNMODERATED);
        createAndModerateModelGrade(12345L, ModState.DELAYED);
        //grades below count as "rejected"
        createAndModerateModelGrade(123456L, ModState.REJECTED);
        createAndModerateModelGrade(1234567L, ModState.REJECTED_BY_SHOP_CLAIM);
        createAndModerateModelGrade(12345678L, ModState.SPAMMER);
        createAndModerateModelGrade(123456789L, ModState.AUTOMATICALLY_REJECTED);
    }

    private void createClusterGradesInDifferentModStates() throws Exception {
        createAndModerateClusterGrade(34L, ModState.APPROVED);
        createAndModerateClusterGrade(345L, ModState.READY);
        createAndModerateClusterGrade(3456L, ModState.UNMODERATED);
        createAndModerateClusterGrade(34567L, ModState.DELAYED);
        //grades below count as "rejected"
        createAndModerateClusterGrade(345678L, ModState.REJECTED);
        createAndModerateClusterGrade(3456789L, ModState.REJECTED_BY_SHOP_CLAIM);
        createAndModerateClusterGrade(34567890L, ModState.SPAMMER);
        createAndModerateClusterGrade(345678901L, ModState.AUTOMATICALLY_REJECTED);
    }

    private void createAndModerateModelGradeWithoutText(long modelId, ModState modState) throws Exception {
        String body = ADD_MODEL_GRADE_WITHOUT_TEXT.replaceAll(String.valueOf(MODEL_ID), String.valueOf(modelId));
        String jsonResponse = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is2xxSuccessful());
        ModelGradeResponseDto response = objectMapper.readValue(jsonResponse, ModelGradeResponseDto.class);
        moderateGrade(response.getId(), modState);
    }

    private void createAndModerateShopGrade(long shopId, ModState modState) throws Exception {
        String body = ADD_SHOP_GRADE_BODY.replaceAll(String.valueOf(SHOP_ID), String.valueOf(shopId));
        String jsonResponse = addShopGrade("UID", String.valueOf(FAKE_USER), body, status().is2xxSuccessful());
        ShopGradeResponseDto shopResponse = objectMapper.readValue(jsonResponse, ShopGradeResponseDto.class);
        moderateGrade(shopResponse.getId(), modState);
    }

    private void createAndModerateModelGrade(long modelId, ModState modState) throws Exception {
        String body = ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO.replaceAll(String.valueOf(MODEL_ID), String.valueOf(modelId));
        String jsonResponse = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is2xxSuccessful());
        ModelGradeResponseDto response = objectMapper.readValue(jsonResponse, ModelGradeResponseDto.class);
        moderateGrade(response.getId(), modState);
    }

    private void createAndModerateClusterGrade(long modelId, ModState modState) throws Exception {
        String body = ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO
            .replaceAll(String.valueOf(MODEL_ID), String.valueOf(modelId))
            .replaceAll("\"type\": 1,", "\"type\": 2,");
        String jsonResponse = addModelGrade("UID", String.valueOf(FAKE_USER), body, status().is2xxSuccessful());
        ModelGradeResponseDto response = objectMapper.readValue(jsonResponse, ModelGradeResponseDto.class);
        moderateGrade(response.getId(), modState);
    }
}

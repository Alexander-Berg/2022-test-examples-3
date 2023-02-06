package ru.yandex.market.pers.grade.web.grade;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.api.GradeController;
import ru.yandex.market.pers.grade.cache.GradeCacher;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.dto.UserAchievementDto;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.WhiteGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.achievements.UserAchievement;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_AGREE;
import static ru.yandex.market.pers.grade.web.grade.UserGradesTest.EXPECTED_VOTES_REJECT;

public abstract class GradeControllerBaseTest extends MockedPersGradeTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    protected static String FAKE_YANDEXUID = "1234567890123";
    protected static long FAKE_USER = -123456789;
    protected static long MISSING_GRADE = -123;
    protected static String DEFAULT_SOURCE = "defaultsource";
    protected static String DIFFERENT_SOURCE = "differentsource";

    protected static long MODEL_ID = 10632786;
    protected static String STD_MODEL_TEXT = "Модель просто супер!!!";
    protected static Long SKU = 3222233L;
    static final String PRO_MODEL_GRADE = "Красивенькая";
    public static final String ADD_MODEL_GRADE_BODY = "{\n" +
        "    \"entity\": \"opinion\",\n" +
        "    \"anonymous\": 0,\n" +
        "    \"comment\": \"Модель просто супер!!!\",\n" +
        "    \"pro\": \"" + PRO_MODEL_GRADE + "\",\n" +
        "    \"contra\": \"Сломалась\",\n" +
        "    \"usage\": 1,\n" +
        "    \"recommend\": true,\n" +
        "    \"averageGrade\": 5,\n" +
        "    \"factors\": [\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 0,\n" +
        "            \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 1,\n" +
        "            \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 2,\n" +
        "            \"value\": 5\n" +
        "        }\n" +
        "    ],\n" +
        "    \"photos\": [\n" +
        "        {\n" +
        "            \"entity\": \"photo\",\n" +
        "            \"groupId\": \"abcde\",\n" +
        "            \"imageName\": \"iuirghreg\"\n" +
        "        }\n" +
        "    ],\n" +
        "    \"product\": {\n" +
        "        \"entity\": \"product\",\n" +
        "        \"id\": " + MODEL_ID + " \n" +
        "    },\n" +
        "    \"sku\": " + SKU + ",\n" +
        "    \"categoryId\": null,\n" +
        "    \"type\": 1,\n" +
        "    \"region\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 213\n" +
        "    },\n" +
        "    \"ipRegion\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 214\n" +
        "    },\n" +
        "    \"source\": \"" + DEFAULT_SOURCE + "\" \n" +
        "}";
    protected static long SHOP_ID = 720;
    static final String PRO_SHOP_GRADE = "супер";
    static final String ORDER_ID = "123456789";
    public static final String ADD_SHOP_GRADE_BODY = getAddShopGradeBody(PRO_SHOP_GRADE, "Магазин просто супер!!!", "не супер");
    static final String ADD_MODEL_GRADE_BODY_WITHOUT_PHOTO = "{\n" +
        "    \"entity\": \"opinion\",\n" +
        "    \"anonymous\": 0,\n" +
        "    \"comment\": \"Модель просто супер!!!\",\n" +
        "    \"pro\": \"Красивенькая\",\n" +
        "    \"contra\": \"Сломалась\",\n" +
        "    \"usage\": 1,\n" +
        "    \"recommend\": true,\n" +
        "    \"averageGrade\": 5,\n" +
        "    \"factors\": [\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 0,\n" +
        "            \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 1,\n" +
        "            \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "            \"entity\": \"opinionFactor\",\n" +
        "            \"id\": 2,\n" +
        "            \"value\": 5\n" +
        "        }\n" +
        "    ],\n" +
        "    \"product\": {\n" +
        "        \"entity\": \"product\",\n" +
        "        \"id\": " + MODEL_ID + " \n" +
        "    },\n" +
        "    \"categoryId\": null,\n" +
        "    \"type\": 1,\n" +
        "    \"region\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 213\n" +
        "    },\n" +
        "    \"ipRegion\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 214\n" +
        "    },\n" +
        "    \"source\": \"" + DEFAULT_SOURCE + "\" \n" +
        "}";

    static final String ADD_MODEL_GRADE_WITHOUT_TEXT = "{\n" +
        "    \"entity\": \"opinion\",\n" +
        "    \"anonymous\": 0,\n" +
        "    \"usage\": 1,\n" +
        "    \"recommend\": true,\n" +
        "    \"averageGrade\": 5,\n" +
        "    \"product\": {\n" +
        "        \"entity\": \"product\",\n" +
        "        \"id\": " + MODEL_ID + " \n" +
        "    },\n" +
        "    \"type\": 1,\n" +
        "    \"region\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 213\n" +
        "    },\n" +
        "    \"ipRegion\": {\n" +
        "        \"entity\": \"region\",\n" +
        "        \"id\": 214\n" +
        "    },\n" +
        "    \"source\": \"" + DEFAULT_SOURCE + "\" \n" +
        "}";

    private static final String VOTE_BODY = "{\n" +
        "  \"gradeId\": %s,\n" +
        "  \"authorId\": %s,\n" +
        "  \"vote\": %d\n" +
        "}";

    protected static String getAddShopGradeBody(String pro, String contra, String comment) {
        return getAddShopGradeBody(SHOP_ID, 5, pro, contra, comment, DEFAULT_SOURCE);
    }

    protected static String getAddShopGradeBody(long shopId, int avgGrade, String pro, String contra, String comment,
                                                String source) {
        return String.format(
            "{\n" +
            "    \"entity\": \"opinion\",\n" +
            "    \"anonymous\": 0,\n" +
            "    \"comment\": \"%s\",\n" +
            "    \"pro\": \"%s\",\n" +
            "    \"contra\": \"%s\",\n" +
            "    \"recommend\": true,\n" +
            "    \"orderId\": \"" + ORDER_ID + "\",\n" +
                "    \"averageGrade\": " + avgGrade + ",\n" +
            "    \"factors\": [\n" +
            "    ],\n" +
            "    \"photos\": [\n" +
            "    ],\n" +
            "    \"shop\": {\n" +
            "        \"entity\": \"shop\",\n" +
                "        \"id\": " + shopId + " \n" +
            "    },\n" +
            "    \"type\": 0,\n" +
            "    \"region\": {\n" +
            "        \"entity\": \"region\",\n" +
            "        \"id\": 213\n" +
            "    },\n" +
            "    \"ipRegion\": {\n" +
            "        \"entity\": \"region\",\n" +
            "        \"id\": 214\n" +
            "    },\n" +
            "    \"source\": \"" + source + "\" \n" +
            "}", comment, pro, contra);
    }

    @Autowired
    protected GradeCacher gradeCacher;

    @Autowired
    protected GradeModeratorModificationProxy moderatorModificationProxy;

    @Autowired
    protected VerifiedGradeService verifiedGradeService;

    @Autowired
    protected DbGradeAdminService dbGradeAdminService;

    protected String addGrade(String gradeType,
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

    protected String addModelGrade(String type, String userId, String body, ResultMatcher expected) throws Exception {
        return addGrade("model", type, userId, body, expected);
    }

    protected String addModelGrade(String type, String userId, String body, String source, ResultMatcher expected) throws Exception {
        return addGrade("model", type, userId, body.replaceAll(DEFAULT_SOURCE, source), expected);
    }


    protected String addShopGrade(String type, String userId, String body, ResultMatcher expected) throws Exception {
        return addGrade("shop", type, userId, body, expected);
    }

    protected String addShopGrade(String type, String userId, String body, String source, ResultMatcher expected) throws Exception {
        return addGrade("shop", type, userId, body.replaceAll(DEFAULT_SOURCE, source), expected);
    }

    protected void deleteGrade(long gradeId) throws Exception {
        deleteGrade(gradeId, FAKE_USER);
    }

    protected void deleteGrade(long gradeId, long uid) throws Exception {
        invokeAndRetrieveResponse(
            delete("/api/grade/" + gradeId + "?uid=" + uid),
            status().is2xxSuccessful());
    }

    protected void bindGrades(long uid, String sessionUid) throws Exception {
        invokeAndRetrieveResponse(
            patch("/api/grade?userId=" + uid + "&sessionId=" + sessionUid),
            status().is2xxSuccessful());
    }

    protected List<GradeResponseDtoImpl> performGetUserGradesByYandexUid() throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/api/grade/user?yandexuid=" + FAKE_YANDEXUID),
                status().is2xxSuccessful()
        ), new TypeReference<List<GradeResponseDtoImpl>>() {
        });
    }

    protected List<GradeResponseDtoImpl> performGetUserGradesByUid() throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/api/grade/user?uid=" + String.valueOf(FAKE_USER)),
                status().is2xxSuccessful()
        ), new TypeReference<List<GradeResponseDtoImpl>>() {
        });
    }

    protected String performGetUserGradesByYandexUidRaw() throws Exception {
        return invokeAndRetrieveResponse(
            get("/api/grade/user?yandexuid=" + FAKE_USER)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected GradeController.Count performGetUserGradesCountByUid(long uid) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
                get("/api/grade/user/count?uid=" + uid),
                status().is2xxSuccessful()
        ), new TypeReference<GradeController.Count>() {
        });
    }

    protected List<ShopGradeResponseDto> performGetShopGradesByYandexUid() throws Exception {
        return objectMapper.readValue(performGetUserGradesByYandexUidRaw(), new TypeReference<List<ShopGradeResponseDto>>() {
        });
    }

    protected List<ModelGradeResponseDto> performGetModelGradesByYandexUid() throws Exception {
        return objectMapper.readValue(performGetUserGradesByYandexUidRaw(), new TypeReference<List<ModelGradeResponseDto>>() {
        });
    }

    protected List<UserGradesTest.GradeResponseDtoImpl> performGetUserGradesByUidAndGradeId(List<Long> gradeIds) throws Exception {
        return objectMapper.readValue(performGetUserGradesByUidRaw(gradeIds), new TypeReference<List<UserGradesTest.GradeResponseDtoImpl>>() {
        });
    }

    protected void moderateGrade(long gradeId, ModState modState) {
        moderatorModificationProxy.moderateGradeReplies(
            List.of(gradeId), List.of(),
            DbGradeAdminService.FAKE_MODERATOR, modState);
        gradeCacher.cleanForAuthorId(FAKE_USER);
    }

    protected void markGradeAsSpam(long gradeId) {
        dbGradeAdminService.setGradeState(List.of(gradeId), true);
        gradeCacher.cleanForAuthorId(FAKE_USER);
    }

    protected List<UserAchievement> getUserAchievements(long userId) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(
                get("/api/achievements/UID/" + userId)
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            new TypeReference<List<UserAchievement>>() {
            });
    }

    protected List<UserAchievementDto> getAllUserAchievements(long userId) throws Exception {
        return objectMapper.readValue(
            invokeAndRetrieveResponse(
                get("/api/achievements/UID/" + userId + "/all")
                    .accept(MediaType.APPLICATION_JSON),
                status().is2xxSuccessful()),
            new TypeReference<List<UserAchievementDto>>() {
            });
    }


    protected String performGetUserGradesByUidRaw(List<Long> gradeIds) throws Exception {
        String url = "/api/grade/user?uid=" + FAKE_USER;
        if (CollectionUtils.isNonEmpty(gradeIds)) {
            return invokeAndRetrieveResponse(
                get(url).accept(MediaType.APPLICATION_JSON).param("gradeId", getIdsAsString(gradeIds)),
                status().is2xxSuccessful()
            );
        }
        return invokeAndRetrieveResponse(
            get(url).accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected GradePager<GradeResponseDtoImpl> getUserPagedGradesWithFirstReviewId(String userType,
                                                                                   String fakeUid,
                                                                                   Long firstReviewId,
                                                                                   GradeType gradeType,
                                                                                   int defaultPageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(get("/api/grade/user/pager?"
                        + (userType.equals("UID") ? "uid=" : "yandexuid=") + fakeUid
                        + (firstReviewId == null ? "" : "&firstReviewId=" + firstReviewId)
                        + (gradeType == null ? "" : "&gradeType=" + gradeType.toString())
                        + "&page_size=" + defaultPageSize),
                status().is2xxSuccessful()
        ), new TypeReference<GradePager<GradeResponseDtoImpl>>() {
        });
    }

    protected List<ShopGradeResponseDto> performGetShopGradesByUid() throws Exception {
        return objectMapper.readValue(performGetUserGradesByUidRaw(null), new TypeReference<List<ShopGradeResponseDto>>() {
        });
    }

    protected List<ModelGradeResponseDto> performGetModelGradesByUid() throws Exception {
        return objectMapper.readValue(performGetUserGradesByUidRaw(null), new TypeReference<List<ModelGradeResponseDto>>() {
        });
    }

    protected String addVote(long gradeId, long authorId, int vote) {
        return addVote(gradeId, Long.toString(authorId), vote);
    }

    protected String addVote(long gradeId, long authorId, int vote, ResultMatcher resultMatcher) throws Exception {
        return addVote(gradeId, Long.toString(authorId), vote, resultMatcher);
    }

    protected String addVote(long gradeId, String authorId, int vote) {
        return addVote(gradeId, authorId, vote, status().is2xxSuccessful());
    }

    protected String addVote(long gradeId, String authorId, int vote, ResultMatcher resultMatcher) {
        return invokeAndRetrieveResponse(post("/api/grade/vote")
            .content(String.format(VOTE_BODY, gradeId, authorId, vote))
            .contentType(MediaType.APPLICATION_JSON), resultMatcher);
    }

    protected String addVoteYandexUid(long gradeId, String yandexUid, int vote) throws Exception {
        return addVoteYandexUid(gradeId, yandexUid, vote, status().is2xxSuccessful());
    }

    protected String addVoteYandexUid(long gradeId,
                                      String yandexUid,
                                      int vote,
                                      ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(post("/api/grade/vote")
            .content(String.format(VOTE_BODY, gradeId, yandexUid, vote).replace("authorId", "yandexUid"))
            .contentType(MediaType.APPLICATION_JSON), resultMatcher);
    }

    protected void deleteVote(long gradeId, Long uid, String yandexUid) throws Exception {
        invokeAndRetrieveResponse(delete("/api/grade/vote")
            .param("gradeId", String.valueOf(gradeId))
            .param("userId", uid != null ? uid.toString() : null)
            .param("yandexUid", yandexUid)
            .contentType(MediaType.APPLICATION_JSON), status().is2xxSuccessful());
    }

    protected void createTestVotesForGrade(long gradeId, int agree, int reject) {
        Assert.assertTrue(agree >= 0);
        Assert.assertTrue(reject >= 0);

        IntStream.range(0, agree).forEach(x -> {
            addVote(gradeId, FAKE_USER + x, 1);
        });
        IntStream.range(agree, agree + reject).forEach(x -> {
            addVote(gradeId, FAKE_USER + x, 0);
        });

        initKarmaVotes(gradeId, EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT);
    }

    protected void initKarmaVotes(long gradeId, int agree, int reject) {
        // try ti find way to initialize votes in future, so they can be read from service

        // write some karma grade votes so they are different from expected votes
        int karmaAgree = agree + 42;
        int karmaReject = reject + 324232;
        pgJdbcTemplate.update(
            "insert into karma_grade_vote (grade_id, agree, reject, last_update_table)\n" +
                "values (?, ?, ?, ?)\n" +
                "on conflict (grade_id) " +
                "do update set agree = excluded.agree, " +
                "              reject = excluded.reject, " +
                "              last_update_table = excluded.last_update_table",
            gradeId, karmaAgree, karmaReject, "1"
        );
    }

    protected String[] getIdsAsString(@NotNull List<Long> ids) {
        return ids
            .stream()
            .map(String::valueOf)
            .collect(Collectors.toList())
            .toArray(new String[ids.size()]);
    }

    public static final class GradeResponseDtoImpl extends WhiteGradeResponseDto {

        public GradeResponseDtoImpl() {
        }

        @JsonProperty("usage")
        @JsonIgnore
        private int usage;

        @JsonProperty("modelId")
        @JsonIgnore
        private int modelId;

        @JsonProperty("product")
        @JsonIgnore
        private Object product;

        @JsonProperty("shop")
        @JsonIgnore
        private Object shop;

        @JsonProperty("delivery")
        @JsonIgnore
        private int delivery;

        @JsonProperty("resolved")
        @JsonIgnore
        private int resolved;

        @JsonProperty("orderId")
        @JsonIgnore
        private String orderId;
    }
}

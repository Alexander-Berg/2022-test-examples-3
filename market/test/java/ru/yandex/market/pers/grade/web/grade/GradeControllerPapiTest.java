package ru.yandex.market.pers.grade.web.grade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.grade.client.dto.DataListWithToken;
import ru.yandex.market.pers.grade.client.dto.GradeUpdatesToken;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.pers.grade.core.service.GradeChangesService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.07.2019
 */
public class GradeControllerPapiTest extends GradeControllerBaseTest {
    private static final long UID = 813901384;
    private static final long SHOP_ID = 384719247;
    public static final int PAGE_SIZE = 2;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;

    @Autowired
    private GradeChangesService gradeChangesService;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testBaseFunctionality() {
        long[] gradeIds = {
            createTestGradeOk(UID, SHOP_ID, 2),
            createTestGradeOk(UID + 1, SHOP_ID, 4),
            createTestGrade(UID + 2, SHOP_ID, 4, ModState.UNMODERATED),
            createTestGrade(UID + 3, SHOP_ID, 3, ModState.REJECTED),
            createTestGradeOk(UID + 4, SHOP_ID, 4),
            createTestGradeOk(UID + 5, SHOP_ID, 2),
        };

        // ban grade
        pgJdbcTemplate.update("update grade set grade_state = '0' where id = ?", gradeIds[5]);

        // get data
        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> shopGrades;
        shopGrades = getUpdatedGrades(SHOP_ID, null, PAGE_SIZE);

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);

        checkToken(shopGrades, gradeIds, 1);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[4], 4, UID + 4);
        assertGradeDeleted(shopGrades.getData().get(1), gradeIds[5], 2, UID + 5);

        checkToken(shopGrades, gradeIds, 5);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds, 5);
    }

    @Test
    public void testWithStrangeTime() {
        long[] gradeIds = {
            createTestGradeOk(UID, SHOP_ID, 2),
            createTestGradeOk(UID + 1, SHOP_ID, 4),
            createTestGradeOk(UID + 2, SHOP_ID, 4),
            createTestGradeOk(UID + 3, SHOP_ID, 3),
            createTestGradeOk(UID + 4, SHOP_ID, 4),
        };

        // shift grades
        setUpdateTime(5, TimeUnit.MINUTES, gradeIds[0], gradeIds[2]);
        setUpdateTime(-5, TimeUnit.MINUTES, gradeIds[1]);
        setUpdateTime(-10, TimeUnit.MINUTES, gradeIds[3], gradeIds[4]);

        // get data
        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> shopGrades;
        shopGrades = getUpdatedGrades(SHOP_ID, null, PAGE_SIZE);

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[3], 3, UID + 3);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[4], 4, UID + 4);

        checkToken(shopGrades, gradeIds, 4);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[0], 2, UID);

        checkToken(shopGrades, gradeIds, 0);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 4, UID + 2);

        checkToken(shopGrades, gradeIds, 2);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds, 2);
    }

    @Test
    public void testChangedGrade() {
        long[] gradeIds = {
            createTestGradeOk(UID, SHOP_ID, 2),
        };

        // shift all updates by 1 minute since they all are already done
        pgJdbcTemplate.update("update grade_updates set upd_time = upd_time - interval '1' minute");

        // get data
        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> shopGrades;
        shopGrades = getUpdatedGrades(SHOP_ID, null, PAGE_SIZE);

        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);

        checkToken(shopGrades, gradeIds, 0);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds, 0);

        //
        // change grage
        //

        long[] gradeIds2 = {
            createTestGrade(UID, SHOP_ID, 3, ModState.APPROVED, "fixed text", null, null),
        };

        // load with changes
        shopGrades = getUpdatedGrades(SHOP_ID, null, PAGE_SIZE);

        assertEquals(2, shopGrades.getData().size());

        assertGradeDeleted(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds2[0], 3, UID, "fixed text");

        checkToken(shopGrades, gradeIds2, 0);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds2, 0);
    }

    @Test
    public void testDeletedFunctionality() throws Exception {
        long[] gradeIds = {
            createTestGradeOk(UID, SHOP_ID, 2),
            createTestGradeOk(UID + 1, SHOP_ID, 4),
            createTestGradeOk(UID + 2, SHOP_ID, 4),
        };

        // shift all updates by 1 minute since they all are already done
        pgJdbcTemplate.update("update grade_updates set upd_time = upd_time - interval '1' minute");

        // get data
        DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> shopGrades;
        shopGrades = getUpdatedGrades(SHOP_ID, null, PAGE_SIZE);

        assertEquals(2, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[0], 2, UID);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds[1], 4, UID + 1);

        checkToken(shopGrades, gradeIds, 1);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(1, shopGrades.getData().size());

        assertGradeSimple(shopGrades.getData().get(0), gradeIds[2], 4, UID + 2);

        checkToken(shopGrades, gradeIds, 2);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds, 2);

        //
        // add more data
        //

        deleteGrade(gradeIds[1], UID + 1);

        long[] gradeIds2 = {
            createTestGradeOk(UID + 5, SHOP_ID, 5),
        };

        gradeModeratorModificationProxy
            .moderateGradeReplies(
                singletonList(gradeIds[0]),
                emptyList(),
                UID,
                ModState.REJECTED);
        shiftUpdateTime(gradeIds[0], 1, TimeUnit.MINUTES);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(2, shopGrades.getData().size());

        assertGradeDeleted(shopGrades.getData().get(0), gradeIds[1], 4, UID + 1);
        assertGradeSimple(shopGrades.getData().get(1), gradeIds2[0], 5, UID + 5);

        checkToken(shopGrades, gradeIds2, 0);

        // next page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(1, shopGrades.getData().size());

        assertGradeDeleted(shopGrades.getData().get(0), gradeIds[0], 2, UID);

        checkToken(shopGrades, gradeIds, 0);

        // last page
        shopGrades = getUpdatedGrades(SHOP_ID, shopGrades.getToken(), PAGE_SIZE);
        assertEquals(0, shopGrades.getData().size());
        checkToken(shopGrades, gradeIds, 0);
    }

    private void shiftUpdateTime(long gradeId, int timeout, TimeUnit unit) {
        pgJdbcTemplate.update(
            "update grade_updates\n" +
                "set upd_time = upd_time + ? * interval '1' second \n" +
                "where grade_id= ?",
            unit.toSeconds(timeout),
            gradeId
        );
    }

    private void setUpdateTime(int timeout, TimeUnit unit, Long... gradeIds) {
        DbUtil.queryInList(Arrays.asList(gradeIds), (sqlBindList, list) -> {
            List<Object> params = new ArrayList<>();
            params.add(unit.toSeconds(timeout));
            params.addAll(list);
            pgJdbcTemplate.update(
                "update grade_updates\n" +
                    "set upd_time = date_trunc('milliseconds', now()) + ? * interval '1' second \n" +
                    "where grade_id in (" + sqlBindList + ")",
                params.toArray()
            );
        });
    }

    private void checkToken(DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> shopGrades,
                            long[] gradeIds,
                            int i) {
        assertEquals(gradeIds[i], shopGrades.getToken().getLastGradeId().longValue());
        assertEquals(getUpdatedTime(gradeIds[i]), shopGrades.getToken().getLastTimestamp().longValue());
    }

    private long getUpdatedTime(long gradeId) {
        return pgJdbcTemplate.query(
            "select upd_time from grade_updates where grade_id = ?",
            (rs, rowNum) -> rs.getTimestamp("upd_time"),
            gradeId).get(0).getTime();
    }

    private long createTestGradeOk(long uid, long shopId, int avgGrade) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, ModState.APPROVED));
    }

    private long createTestGrade(long uid, long shopId, int avgGrade, ModState modState) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState));
    }

    private long createTestGrade(long uid,
                                 long shopId,
                                 int avgGrade,
                                 ModState modState,
                                 String text,
                                 String pro,
                                 String contra) {
        return doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState, text, pro, contra));
    }

    private long createTestGrade(long uid, long shopId, int avgGrade, ModState modState, Integer modReasonId) {
        long gradeId = doSaveGrade(buildTestGrade(uid, shopId, avgGrade, modState));
        if (modReasonId != null) {
            setModReason(gradeId, modState, modReasonId);
        }
        return gradeId;
    }

    private void setModReason(long gradeId, ModState modState, Integer reasonId) {
        final long moderator = 394617364134L;

        Object4Moderation obj = Object4Moderation.moderated(
            gradeId,
            modState,
            reasonId != null ? reasonId.longValue() : null
        );

        gradeAdminService.moderate(singletonList(obj), moderator);
    }

    @NotNull
    private ShopGrade buildTestGrade(long uid, long shopId, int avgGrade, ModState modState) {
        ShopGrade testGrade = buildTestShopGrade(uid, shopId, buildTestText(uid), avgGrade);
        testGrade.setModState(modState);
        return testGrade;
    }

    @NotNull
    private ShopGrade buildTestGrade(long uid,
                                     long shopId,
                                     int avgGrade,
                                     ModState modState,
                                     String text,
                                     String pro,
                                     String contra) {
        ShopGrade testGrade = buildTestGrade(uid, shopId, avgGrade, modState);
        testGrade.setText(text);
        testGrade.setPro(pro);
        testGrade.setContra(contra);
        return testGrade;
    }

    private ShopGrade buildTestShopGrade(long uid, long shopId, String text, int averageGrade) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, uid);
        grade.setText(text);
        grade.setAverageGrade(averageGrade);
        return grade;
    }

    private long doSaveGrade(ShopGrade testGrade) {
        long gradeId = gradeCreator.createGrade(testGrade);
        if (testGrade.getModState() == ModState.APPROVED) {
            gradeChangesService.markApproved(singletonList(gradeId));
        }

        return gradeId;
    }

    private String buildTestText(long uid) {
        return "test grade " + uid;
    }

    private void assertGradeSimple(ShopGradeResponseDto grade,
                                   long gradeId,
                                   int avgGrade,
                                   long uid) {
        assertGradeSimple(grade, gradeId, avgGrade, uid, buildTestText(uid));
    }

    private void assertGradeDeleted(ShopGradeResponseDto grade,
                                    long gradeId,
                                    int avgGrade,
                                    long uid) {
        assertGradeSimple(grade, gradeId, avgGrade, uid, buildTestText(uid));
        assertEquals(GradeState.DELETED, grade.getState());
        assertEquals(ModState.REJECTED, grade.getModState());
    }

    private void assertGradeSimple(ShopGradeResponseDto grade,
                                   long gradeId,
                                   int avgGrade,
                                   long uid,
                                   String text) {
        assertEquals(gradeId, grade.getId().longValue());
        assertEquals(uid, grade.getUser().getPassportUid().longValue());
        assertEquals(avgGrade, grade.getAverageGrade().intValue());
        assertEquals(text, grade.getText());
    }

    private DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken> getUpdatedGrades(
        long shopId,
        GradeUpdatesToken token,
        long limit
    ) {
        LinkedMultiValueMap<String, String> tokenMap = new LinkedMultiValueMap<>();
        if (token != null) {
            tokenMap.add("lastTimestamp",
                token.getLastTimestamp() != null ? String.valueOf(token.getLastTimestamp()) : null);
            tokenMap.add("lastGradeId",
                token.getLastGradeId() != null ? String.valueOf(token.getLastGradeId()) : null);
        }

        try {
            return objectMapper.readValue(
                invokeAndRetrieveResponse(
                    get("/api/grade/papi/shop/" + shopId)
                        .params(tokenMap)
                        .param("page_size", String.valueOf(limit))
                        .accept(MediaType.APPLICATION_JSON),
                    status().is2xxSuccessful()),
                new TypeReference<DataListWithToken<ShopGradeResponseDto, GradeUpdatesToken>>() {
                });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

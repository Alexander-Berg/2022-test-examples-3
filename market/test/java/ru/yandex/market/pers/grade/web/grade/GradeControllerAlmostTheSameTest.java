package ru.yandex.market.pers.grade.web.grade;

import java.util.List;

import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ShopGradeResponseDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 21.11.2018
 */
public class GradeControllerAlmostTheSameTest extends GradeControllerBaseTest {

    @Test
    public void testShopGradeUidTheSameWithEmptyPro() throws Exception {
        assertNoGrade();

        performCreateShopGrade("UID", "", "contra contra contra contra", "comment comment comment!!!!");
        assertOnlyOneGrade();

        performCreateShopGrade("UID", "", "contra contra contra contra", "comment comment comment!!!!");
        assertOnlyOneGrade();
    }

    @Test
    public void testShopGradeUidTheSameWithEmptyContra() throws Exception {
        assertNoGrade();

        performCreateShopGrade("UID", "pro pro prop pro pro pro pro", "", "comment comment comment!!!!");
        assertOnlyOneGrade();

        performCreateShopGrade("UID", "pro pro prop pro pro pro pro", "", "comment comment comment!!!!");
        assertOnlyOneGrade();
    }

    @Test
    public void testShopGradeUidTheSameWithEmptyComment() throws Exception {
        assertNoGrade();

        performCreateShopGrade("UID", "pro pro prop pro pro pro pro", "contra contra contra contra contra", "");
        assertOnlyOneGrade();

        performCreateShopGrade("UID", "pro pro prop pro pro pro pro", "contra contra contra contra contra", "");
        assertOnlyOneGrade();
    }

    @Test
    public void testModelGradeChangeRecommend() throws Exception {
        assertTrue(performGetModelGradesByYandexUid().isEmpty());
        addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY, status().is2xxSuccessful());

        String existsResponse = invokeAndRetrieveResponse(
            get("/api/grade/user/model/" + MODEL_ID + "/exists?sessionId=" + FAKE_USER)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
        assertTrue(existsResponse.replaceAll("\\s+", "").contains("\"exist\":true"));

        List<ModelGradeResponseDto> grades = performGetModelGradesByYandexUid();
        assertFalse(grades.isEmpty());

        addModelGrade("YANDEXUID", String.valueOf(FAKE_USER), ADD_MODEL_GRADE_BODY.replace("\"recommend\": true", "\"recommend\": false"), status().is2xxSuccessful());

        assertFalse(performGetModelGradesByYandexUid().get(0).getRecommend());
    }

    @Test
    public void testShopGradeChangeRecommend() throws Exception {
        assertTrue(performGetShopGradesByUid().isEmpty());

        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY, status().is2xxSuccessful());

        final List<ShopGradeResponseDto> grades = performGetShopGradesByUid();
        assertFalse(grades.isEmpty());

        addShopGrade("UID", String.valueOf(FAKE_USER), ADD_SHOP_GRADE_BODY.replace("\"recommend\": true", "\"recommend\": false"), status().is2xxSuccessful());

        assertFalse(performGetShopGradesByUid().get(0).getRecommend());
    }

    private void performCreateShopGrade(String userType, String pro, String contra, String comment) throws Exception {
        addShopGrade(userType, String.valueOf(FAKE_USER), getAddShopGradeBody(pro, contra, comment), status().is2xxSuccessful());
    }

    private void assertNoGrade() throws Exception {
        List<ShopGradeResponseDto> grades = performGetShopGradesByUid();
        assertEquals(0, grades.size());

        List<Long> ids = getGradeIds();
        assertEquals(0, ids.size());
    }

    private void assertOnlyOneGrade() throws Exception {
        List<ShopGradeResponseDto> grades = performGetShopGradesByUid();
        assertEquals(1, grades.size());

        List<Long> gradeInDb = getGradeIds();
        assertEquals(1, gradeInDb.size());
        assertEquals(grades.get(0).getId(), gradeInDb.get(0));
    }

    private List<Long> getGradeIds() {
        return pgJdbcTemplate.queryForList("select id from grade where author_id = ? and resource_id = ?",
            Long.class, FAKE_USER, SHOP_ID);
    }
}

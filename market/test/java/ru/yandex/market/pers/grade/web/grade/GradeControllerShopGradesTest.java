package ru.yandex.market.pers.grade.web.grade;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.grade.core.model.core.BusinessIdEntityType.SHOP;

public class GradeControllerShopGradesTest extends MockedPersGradeTest {

    private static final long FAKE_USER = -123456789;
    private static final long FAKE_USER_2 = -1234567890;
    private static final long FAKE_SHOP = 2;
    private static final long FAKE_SHOP_2 = 3;
    private static final long GROUP_ID = 1;

    @Autowired
    private GradeCreationHelper gradeCreationHelper;

    @Autowired
    private DbGradeService dbGradeService;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private GradeCreator gradeCreator;

    private void prepareMatView() {
        pgJdbcTemplate.update("refresh materialized view mv_partner_grade");
    }

    @Test
    public void testShopGrade1() throws Exception {
        AbstractGrade grade = createTestShopGrade1(FAKE_SHOP);
        long gradeId = gradeCreationHelper.createApprovedGrade(grade);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP),
                status().is2xxSuccessful()
        );
        String expected = String.format(file("/data/shop_grades_1.txt"), gradeId);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testShopGrade2() throws Exception {
        AbstractGrade grade = createTestShopGrade2(FAKE_SHOP);
        long gradeId = gradeCreationHelper.createApprovedGrade(grade);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP),
                status().is2xxSuccessful()
        );
        String expected = String.format(file("/data/shop_grades_2.txt"), gradeId);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSomeShopGradesWithPager() throws Exception {
        AbstractGrade grade1 = createTestShopGrade1(FAKE_SHOP);
        AbstractGrade grade2 = createTestShopGrade2(FAKE_SHOP);
        long gradeId1 = gradeCreationHelper.createApprovedGrade(grade1);
        long gradeId2 = gradeCreationHelper.createApprovedGrade(grade2);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP + "?page_num=0&page_size=2&sort_by=date&sort_desc=1"),
                status().is2xxSuccessful()
        );
        String expected = String.format(file("/data/shop_grades_3.txt"), gradeId1, FAKE_SHOP, null, gradeId2, FAKE_SHOP, null, FAKE_SHOP);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testGetOneGradeFromMany1() throws Exception {
        AbstractGrade grade1 = createTestShopGrade1(FAKE_SHOP);
        AbstractGrade grade2 = createTestShopGrade2(FAKE_SHOP);
        long gradeId1 = gradeCreationHelper.createApprovedGrade(grade1);
        long gradeId2 = gradeCreationHelper.createApprovedGrade(grade2);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP + "?page_num=0&page_size=1&sort_by=date&sort_desc=1"),
                status().is2xxSuccessful()
        );
        String expected = String.format(file("/data/shop_grades_4.txt"), gradeId1);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testGetOneGradeFromMany2() throws Exception {
        AbstractGrade grade1 = createTestShopGrade1(FAKE_SHOP);
        AbstractGrade grade2 = createTestShopGrade2(FAKE_SHOP);
        long gradeId1 = gradeCreationHelper.createApprovedGrade(grade1);
        long gradeId2 = gradeCreationHelper.createApprovedGrade(grade2);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP + "?page_num=1&page_size=1&sort_by=date&sort_desc=1"),
                status().is2xxSuccessful()
        );
        String expected = String.format(file("/data/shop_grades_5.txt"), gradeId2);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testRegionalClone() throws Exception {
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", GROUP_ID, FAKE_SHOP, SHOP.getValue());
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", GROUP_ID, FAKE_SHOP_2, SHOP.getValue());
        AbstractGrade grade1 = createTestShopGrade1(FAKE_SHOP);
        AbstractGrade grade2 = createTestShopGrade2(FAKE_SHOP_2);
        long gradeId1 = gradeCreationHelper.createApprovedGrade(grade1);
        long gradeId2 = gradeCreationHelper.createApprovedGrade(grade2);

        prepareMatView();
        String response1 = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP + "?page_num=0&page_size=2&sort_by=date&sort_desc=1"),
                status().is2xxSuccessful()
        );
        String response2 = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP_2 + "?page_num=0&page_size=2&sort_by=date&sort_desc=1"),
                status().is2xxSuccessful()
        );
        String expected1 = String.format(file("/data/shop_grades_3.txt"), gradeId1, FAKE_SHOP, GROUP_ID, gradeId2, FAKE_SHOP_2, GROUP_ID, FAKE_SHOP);
        String expected2 = String.format(file("/data/shop_grades_3.txt"), gradeId1, FAKE_SHOP, GROUP_ID, gradeId2, FAKE_SHOP_2, GROUP_ID, FAKE_SHOP_2);
        JSONAssert.assertEquals(expected1, response1, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(expected2, response2, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testRegionalCloneWithoutClones() throws Exception {
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", GROUP_ID, FAKE_SHOP, SHOP.getValue());
        pgJdbcTemplate.update("insert into ext_shop_business_id(business_id, shop_id, type) values(?, ?, ?)", GROUP_ID, FAKE_SHOP_2, SHOP.getValue());
        AbstractGrade grade1 = createTestShopGrade1(FAKE_SHOP);
        AbstractGrade grade2 = createTestShopGrade2(FAKE_SHOP_2);
        long gradeId1 = gradeCreationHelper.createApprovedGrade(grade1);
        long gradeId2 = gradeCreationHelper.createApprovedGrade(grade2);

        prepareMatView();
        String response1 = invokeAndRetrieveResponse(
            get("/api/grade/shop/" + FAKE_SHOP + "?page_num=0&page_size=2&sort_by=date&sort_desc=1&with_clones=false"),
            status().is2xxSuccessful()
        );
        String response2 = invokeAndRetrieveResponse(
            get("/api/grade/shop/" + FAKE_SHOP_2 + "?page_num=0&page_size=2&sort_by=date&sort_desc=1&with_clones=false"),
            status().is2xxSuccessful()
        );
        String expected1 = String.format(file("/data/shop_grades_7.txt"), gradeId1);
        String expected2 = String.format(file("/data/shop_grades_8.txt"), gradeId2);
        JSONAssert.assertEquals(expected1, response1, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(expected2, response2, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testShopGradeResolvedTrue() throws Exception {
        testShopGradeResolved(2, "resolved", Boolean.TRUE);
    }

    @Test
    public void testShopGradeResolvedFalse() throws Exception {
        testShopGradeResolved(2, "true", Boolean.FALSE);
    }

    @Test
    public void testShopGradeResolvedNull() throws Exception {
        testShopGradeResolved(2, "true", null);
    }

    @Test
    public void testShopGradeNotResolved() throws Exception {
        testShopGradeResolved(5, "false", Boolean.TRUE);
    }

    private void testShopGradeResolved(Integer avgGrade, String problem, Boolean resolved) throws Exception {
        AbstractGrade grade = createTestShopGrade1(FAKE_SHOP);
        grade.setGr0(avgGrade - 3);
        long gradeId = gradeCreationHelper.createApprovedGrade(grade);
        if (resolved != null) {
            dbGradeService.resolveGrade(gradeId, FAKE_USER, resolved);
        }
        final Integer resolveValue = Optional.ofNullable(resolved).map(it -> it ? 1 : 0).orElse(0);
        String expected = String.format(file("/data/shop_grades_6.txt"),
            gradeId, avgGrade, problem, resolveValue);

        prepareMatView();
        String response = invokeAndRetrieveResponse(
            get("/api/grade/shop/" + FAKE_SHOP),
            status().is2xxSuccessful()
        );
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testShopGradeVotes() throws Exception {
        AbstractGrade grade = createTestShopGrade1(FAKE_SHOP);
        grade.getGradeFactorValuesFull().clear(); // without factors
        long gradeId = gradeCreationHelper.createApprovedGrade(grade);
        pgJdbcTemplate.update("insert INTO KARMA_GRADE_VOTE(GRADE_ID, AGREE, REJECT, LAST_UPDATE_TABLE) " +
                "VALUES (?, ?, ?, ?)",
                gradeId, 23, 17, "1555444803744");

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP),
                status().is2xxSuccessful()
        );
        pgJdbcTemplate.update("DELETE from KARMA_GRADE_VOTE where GRADE_ID = ?", gradeId);
        String expected = String.format(file("/data/shop_grades_9.txt"), gradeId);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testShopGradeVotesWithFactors() throws Exception {
        AbstractGrade grade = createTestShopGrade1(FAKE_SHOP);
        long gradeId = gradeCreationHelper.createApprovedGrade(grade);
        pgJdbcTemplate.update("insert INTO KARMA_GRADE_VOTE(GRADE_ID, AGREE, REJECT, LAST_UPDATE_TABLE) " +
                "VALUES (?, ?, ?, ?)",
                gradeId, 23, 17, "1555444803744");

        prepareMatView();
        String response = invokeAndRetrieveResponse(
                get("/api/grade/shop/" + FAKE_SHOP),
                status().is2xxSuccessful()
        );
        pgJdbcTemplate.update("DELETE from KARMA_GRADE_VOTE where GRADE_ID = ?", gradeId);
        String expected = String.format(file("/data/shop_grades_10.txt"), gradeId);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    public static ShopGrade createTestShopGrade1(long shopId) {
        return GradeCreationHelper.constructShopGrade(shopId, FAKE_USER);
    }

    private ShopGrade createTestShopGrade2(long shopId) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, FAKE_USER_2)
            .fillShopGradeCreationFields("2726572", null);
        grade.setText("Комментарий для теста");
        grade.setPro(null);
        grade.setContra(null);
        grade.setCreated(new Date(1517776536000L));
        grade.setRecommend(false);
        grade.setGradeFactorValues(List.of(new GradeFactorValue(3, "Соответствие товара описанию", "", 4)));
        grade.setGr0(-2);
        return grade;
    }


    private String file(String file) throws IOException {
        return IOUtils.readInputStream(
                getClass().getResourceAsStream(file));
    }

}

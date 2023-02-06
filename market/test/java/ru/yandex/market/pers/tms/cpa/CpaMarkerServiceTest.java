package ru.yandex.market.pers.tms.cpa;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.author.AuthorService;
import ru.yandex.market.pers.grade.core.model.core.GradeValue;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.core.db.DbGradeAdminService.FAKE_MODERATOR;
import static ru.yandex.market.pers.tms.cpa.CpaMarkerService.BLUE_SHOP_ID;

/**
 * @author vvolokh
 * 31.07.2018
 */
public class CpaMarkerServiceTest extends MockedPersTmsTest {

    private static final long TEST_RESOURCE_ID = 1L;
    private static final long TEST_AUTHOR_UID = 1L;

    @Autowired
    private CpaMarkerService cpaMarkerService;
    @Autowired
    private AuthorService authorService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    @Qualifier("ytJdbcTemplate")
    JdbcTemplate hahnJdbcTemplate;
    @Autowired
    YtClient ytClient;

    @Test
    public void testShopGrades() {
        cleanupData(30, 1);
        long testShopGradeId = gradeCreator.createShopGrade(TEST_AUTHOR_UID, TEST_RESOURCE_ID, GradeValue.EXCELLENT.toAvgGrade());
        long blueShopGradeId = gradeCreator.createShopGrade(TEST_AUTHOR_UID, BLUE_SHOP_ID, GradeValue.EXCELLENT.toAvgGrade());
        mockYtToReturnMarkNeededGrade(List.of(new OrderGradeInfo(null, blueShopGradeId), new OrderGradeInfo(null, testShopGradeId)));

        cpaMarkerService.mark();

        assertTrue(isVerified(blueShopGradeId));
        assertTrue(isBlue(blueShopGradeId));
        assertTrue(isVerified(testShopGradeId));
        assertTrue(isBlue(testShopGradeId));

        ArgumentCaptor<List<PurchaseCheckGradeInfo>> listCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytClient, Mockito.times(2)).append(any(), any(), listCaptor.capture());
        List<PurchaseCheckGradeInfo> grades = listCaptor
            .getAllValues()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        assertEquals(2, grades.size());
        assertEquals(testShopGradeId, grades.get(0).getId());
        assertEquals(TEST_RESOURCE_ID, grades.get(0).getResourceId().longValue());
        assertEquals(blueShopGradeId, grades.get(1).getId());
        assertEquals(BLUE_SHOP_ID, grades.get(1).getResourceId().longValue());
    }

    @Test
    public void testShopGradesBySpammers() {
        cleanupData(30, 1);
        addSpammers(List.of(TEST_AUTHOR_UID, TEST_AUTHOR_UID + 1000, TEST_AUTHOR_UID + 2000));
        long testFirstVerifiedShopGradeId =
            gradeCreator.createShopGrade(TEST_AUTHOR_UID, TEST_RESOURCE_ID, GradeValue.EXCELLENT.toAvgGrade(), ModState.SPAMMER);
        long testSecondVerifiedShopGradeId =
            gradeCreator.createShopGrade(TEST_AUTHOR_UID + 1000, TEST_RESOURCE_ID, GradeValue.GOOD.toAvgGrade(), ModState.SPAMMER);
        long testNotVerifiedShopGradeId =
            gradeCreator.createShopGrade(TEST_AUTHOR_UID + 2000, TEST_RESOURCE_ID, GradeValue.NORMAL.toAvgGrade(), ModState.SPAMMER);
        mockYtToReturnMarkNeededGrade(List.of(
            new OrderGradeInfo(null,testFirstVerifiedShopGradeId),
            new OrderGradeInfo(null, testSecondVerifiedShopGradeId))
        );

        cpaMarkerService.mark();

        validateModState(testFirstVerifiedShopGradeId, ModState.UNMODERATED);
        validateModState(testSecondVerifiedShopGradeId, ModState.UNMODERATED);
        validateModState(testNotVerifiedShopGradeId, ModState.SPAMMER);
        assertFalse(authorInSpamList(TEST_AUTHOR_UID, TEST_AUTHOR_UID + 1000));
        assertTrue(authorInSpamList(TEST_AUTHOR_UID + 2000));
    }

    @Test
    public void testBlueModelGrade() {
        cleanupData(1, 30);
        Long testModelGradeId = gradeCreator.createModelGrade(TEST_RESOURCE_ID, TEST_AUTHOR_UID);
        mockYtToReturnMarkNeededGrade(Collections.singletonList(new OrderGradeInfo(123L, testModelGradeId)));

        cpaMarkerService.mark();

        assertTrue(isVerified(testModelGradeId));
        assertTrue(isBlue(testModelGradeId));
        assertTrue(validateOrderId(testModelGradeId, 123));

    }

    @Test
    public void testBlueShopGradeWithoutOrder() {
        cleanupData(30, 1);
        long blueShopGrade = gradeCreator.createShopGrade(TEST_AUTHOR_UID, BLUE_SHOP_ID, GradeValue.EXCELLENT.toAvgGrade());
        mockYtToReturnMarkNeededGrade(Collections.emptyList());

        cpaMarkerService.mark();

        assertFalse(isBlue(blueShopGrade));
    }

    @Test
    public void testNotBlueModelGrade() {
        cleanupData(1, 30);
        Long testModelGradeId = gradeCreator.createModelGrade(TEST_RESOURCE_ID, TEST_AUTHOR_UID);
        mockYtToReturnMarkNeededGrade(Collections.emptyList());

        cpaMarkerService.mark();

        assertFalse(isBlue(testModelGradeId));

    }

    @Test
    public void testDumpGradesToYt() {
        cleanupData(30, 1);
        Long firstGradeId = gradeCreator.createShopGrade(TEST_AUTHOR_UID, BLUE_SHOP_ID, GradeValue.EXCELLENT.toAvgGrade());
        Long secondGradeId = gradeCreator.createShopGrade(TEST_AUTHOR_UID + 1, BLUE_SHOP_ID, GradeValue.TERRIBLE.toAvgGrade());
        Long thirdGradeId = gradeCreator.createShopGrade(TEST_AUTHOR_UID + 1, TEST_RESOURCE_ID, GradeValue.TERRIBLE.toAvgGrade());
        mockYtToReturnMarkNeededGrade(List.of(new OrderGradeInfo(null, firstGradeId), new OrderGradeInfo(null, thirdGradeId)));

        cpaMarkerService.mark();

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytClient, Mockito.times(2)).append(any(), any(), listCaptor.capture());
        Assert.assertEquals(2, listCaptor.getValue().size());

        assertTrue(isVerified(firstGradeId));
        assertTrue(isBlue(firstGradeId));
        assertFalse(isBlue(secondGradeId));
        assertTrue(isBlue(thirdGradeId));
        assertEquals((Integer) 1, pgJdbcTemplate.queryForObject("select cpa from grade where id=" + thirdGradeId,
            Integer.class));
    }

    @Test
    public void testCpaFixModelGrade() {
        cleanupData(1, 30);
        Long testModelGradeId = gradeCreator.createModelGrade(TEST_RESOURCE_ID, TEST_AUTHOR_UID);
        mockYtToReturnMarkNeededGrade(List.of(new OrderGradeInfo(123L, testModelGradeId)));

        cpaMarkerService.fixCpaMark();

        assertTrue(isBlue(testModelGradeId));
        assertTrue(validateOrderId(testModelGradeId, 123));
    }

    private boolean validateOrderId(Long gradeId, Integer expectedOrderId) {
        Integer orderId = pgJdbcTemplate.queryForObject("select model_order_id " +
            "from grade_model where grade_id = " + String.valueOf(gradeId), Integer.class);
        return Objects.equals(orderId, expectedOrderId);
    }

    private boolean isVerified(Long gradeId) {
        Integer verified =
            pgJdbcTemplate.queryForObject("select verified from grade where id=" + String.valueOf(gradeId),
                Integer.class);
        if (verified == null) {
            throw new NullPointerException("Query for verified column of grade with id=" + gradeId + " returned null");
        }
        return verified.equals(1);
    }

    private boolean isBlue(Long gradeId) {
        Integer blue =
            pgJdbcTemplate.queryForObject("select cpa from grade where id=" + String.valueOf(gradeId),
                Integer.class);
        if (blue == null) {
            throw new NullPointerException("Query for blue column of grade with id=" + gradeId + " returned null");
        }
        return blue.equals(1);
    }

    private void cleanupData(Integer shopGradePeriod, Integer modelGradePeriod) {
        configurationService.mergeValue(CpaMarkerService.SHOP_GRADES_PERIOD_KEY, String.valueOf(shopGradePeriod));
        configurationService.mergeValue(CpaMarkerService.MODEL_GRADES_PERIOD_KEY, String.valueOf(modelGradePeriod));
        pgJdbcTemplate.update(
            "update grade set cpa = 0 " +
                "where cr_time > now() - make_interval(days :=  ?) " +
                "  and cpa is null",
            Math.max(shopGradePeriod, modelGradePeriod));
    }

    private void mockYtToReturnMarkNeededGrade(List<OrderGradeInfo> answer) {
        when(hahnJdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(answer);
    }

    private void addSpammers(List<Long> spammers) {
        authorService.markAsSpammmers(
            spammers.stream().map(String::valueOf).collect(Collectors.toList()),
            FAKE_MODERATOR,
            "test"
        );
    }

    private void validateModState(long gradeId, ModState modState) {
        assertTrue(pgJdbcTemplate.query(
            "select 1 from grade.grade g where id = ? and mod_state = ?",
            ResultSet::next,
            gradeId,
            modState.value()));
    }

    private Boolean authorInSpamList(long... ids) {
        return pgJdbcTemplate.query(
            "select 1 from grade.spammer s where user_id = any(?) and del_action_data_id is null",
            ResultSet::next,
            (Object) ids
        );
    }
}

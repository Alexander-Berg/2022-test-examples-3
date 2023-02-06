package ru.yandex.market.pers.grade.core.db;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.filter.QueryFilter;
import ru.yandex.common.framework.filter.SimpleQueryConditionFilter;
import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.filter.TimeFilter;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.model.AgitationEntity;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.FactorCreator;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.TestUtil;
import ru.yandex.market.pers.grade.core.model.AuthorIdAndYandexUid;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class DbGradeServiceTest extends DbGradeServiceTestBase {
    private static final Logger log = Logger.getLogger(DbGradeServiceTest.class);

    @Autowired
    private DbGradeVoteService gradeVoteService;
    @Autowired
    private PersAuthorClient authorClient;
    @Autowired
    private FactorCreator factorCreator;

    @Test
    public void testCreateShopGrade() throws Exception {
        createShopLoad();
    }

    @Test
    public void testCreateWithoutPassportLoadLongHeaders() throws Exception {
        AbstractGrade testGrade = createShopGradeWithouthLoadLongHeaders();
        List<AbstractGrade> grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(1, grades.size());
        int size = gradeService.countGrades(createSessionUidFilter());
        assertEquals(1, size);
        assertTrue(same(testGrade, grades.get(0)));
    }

    /**
     * Создаем первый НЕ анонимный отзыв на магазин
     * Создаем второй анонимный отзыв на тот же магазин
     * Логинимся в первую сессию
     * Проверяем статусы отзывов: первый - прошедший, второй - последний
     * @throws Exception
     */
    @Test
    public void testBindGradesUserFirst() throws Exception {
        AbstractGrade userGrade = createShopLoad();
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());

        AbstractGrade anonymousGrade = createShopGradeWithouthLoadLongHeaders();
        grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());

        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        gradeService.bindGrades2User(UID, TEST_SESSIONUID);
        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        for (AbstractGrade grade : grades) {
            TestUtil.printXmlConvertable(grade);
        }
        assertEquals(1, grades.size());

        grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(0, grades.size());
        int size = gradeService.countGrades(createSessionUidFilter());
        assertEquals(0, size);

        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        assertEquals(1, grades.size());
        final AbstractGrade grade = grades.get(0);
        assertTrue(same(grade, anonymousGrade));
    }

    /**
     * Создаем первый анонимный отзыв на магазин
     * Создаем второй НЕ анонимный отзыв на тот же магазин
     * Логинимся в первую сессию
     * Проверяем статусы отзывов: первый - прошедший, второй - последний
     * @throws Exception
     */
    @Test
    public void testBindGradesAnonymousFirst() throws Exception {
        AbstractGrade anonymousGrade = createShopGradeWithouthLoadLongHeaders();
        List<AbstractGrade> grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        AbstractGrade userGrade = createShopLoad();
        grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());

        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        gradeService.bindGrades2User(UID, TEST_SESSIONUID);
        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        for (AbstractGrade grade : grades) {
            TestUtil.printXmlConvertable(grade);
        }
        assertEquals(1, grades.size());

        grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(0, grades.size());
        int size = gradeService.countGrades(createSessionUidFilter());
        assertEquals(0, size);

        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        assertEquals(1, grades.size());
        final AbstractGrade grade = grades.get(0);
        assertTrue(same(grade, userGrade));
    }

    /**
     * Создаем первый НЕ анонимный отзыв на магазин, а так же два отзыва на модельки
     * Создаем второй анонимный отзыв на тот же магазин
     * Логинимся в первую сессию
     * Проверяем статусы отзывов: первый - прошедший, второй и модельные - последние с одинаковым автором
     * @throws Exception
     */
    @Test
    public void testBindGradesAnonymousModesGrades() throws Exception {
        AbstractGrade userModelGrade1 = createModelLoad(MODEL_ID);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilter(UID), null);
        assertEquals(1, grades.size());

        AbstractGrade userModelGrade2 = createModelLoad(MODEL_ID_1);
        grades = gradeService.findGrades(createAuthorFilter(), null);
        assertEquals(2, grades.size());

        AbstractGrade anonymousShopGrade = createShopGradeWithouthLoadLongHeaders();
        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        AbstractGrade userGradeShop = createShopLoad();
        grades = gradeService.findGrades(createAuthorFilter(), null);
        assertEquals(3, grades.size());

        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        gradeService.bindGrades2User(UID, TEST_SESSIONUID);
        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        for (AbstractGrade grade : grades) {
            TestUtil.printXmlConvertable(grade);
        }
        assertEquals(3, grades.size());

        grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(0, grades.size());
        int size = gradeService.countGrades(createSessionUidFilter());
        assertEquals(0, size);

        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        assertEquals(3, grades.size());
        assertTrue(oneOf(grades.get(0), userModelGrade1, userModelGrade2, userGradeShop));
        assertTrue(oneOf(grades.get(1), userModelGrade1, userModelGrade2, userGradeShop));
        assertTrue(oneOf(grades.get(2), userModelGrade1, userModelGrade2, userGradeShop));
    }

    /**
     * Создаем анонимные отзывы на модельки и первый анонимный отзыв на магазин
     * Создаем второй НЕ анонимный отзыв на тот же магазин
     * Логинимся в первую сессию
     * Проверяем, что автор проставился анонимным отзывам
     * Проверяем статусы отзывов: первый анонимный на магазин стал прошедшим, второй и модельные - последние с одинаковым автором
     * @throws Exception
     */
    @Test
    public void testBindGradesUserShopGradeAfterModesGrades() throws Exception {
        AbstractGrade anonymousModelGrade1 = createModelLoadWithoutPassport(MODEL_ID);
        List<AbstractGrade> grades = gradeService.findGrades(createModelAndNoAuthorFilter(MODEL_ID), null);
        assertEquals(1, grades.size());

        AbstractGrade anonymousModelGrade2 = createModelLoadWithoutPassport(MODEL_ID_1);
        grades = gradeService.findGrades(createModelAndNoAuthorFilter(MODEL_ID_1), null);
        assertEquals(1, grades.size());

        AbstractGrade anonymousShopGrade = createShopGradeWithouthLoadLongHeaders();
        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        AbstractGrade userGradeShop = createShopLoad();
        grades = gradeService.findGrades(createAuthorFilter(), null);
        assertEquals(1, grades.size());

        grades = gradeService.findGrades(createNoAuthorAndShopFilter(SHOP_ID), null);
        assertEquals(1, grades.size());

        gradeService.bindGrades2User(UID, TEST_SESSIONUID);
        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        for (AbstractGrade grade : grades) {
            TestUtil.printXmlConvertable(grade);
        }
        assertEquals(3, grades.size());

        grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(0, grades.size());
        int size = gradeService.countGrades(createSessionUidFilter());
        assertEquals(0, size);

        grades = gradeService.findGrades(createAuthorNewFilter(), null);
        assertEquals(3, grades.size());
        assertTrue(oneOf(grades.get(0), anonymousModelGrade1, anonymousModelGrade2, userGradeShop));
        assertTrue(oneOf(grades.get(1), anonymousModelGrade1, anonymousModelGrade2, userGradeShop));
        assertTrue(oneOf(grades.get(2), anonymousModelGrade1, anonymousModelGrade2, userGradeShop));
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        createShopGradeWithouthLoadLongHeaders();
        createShopLoad(SHOP_ID);

        assertEquals(1, gradeService.findGrades(createSessionUidFilter(), null).size());
        gradeService.killGrades(new SimpleQueryFilter(), new AuthorIdAndYandexUid(null, TEST_SESSIONUID));
        assertEquals(0, gradeService.findGrades(createSessionUidFilter(), null).size());

        List<AbstractGrade> authoredGrades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, authoredGrades.size());
        assertEquals(authoredGrades.get(0).getState(), GradeState.LAST);
    }

    @Test
    public void testDeleteAllUnauthorizedWithNull() throws Exception {
        createShopGradeWithouthLoadLongHeaders();
        gradeService.killGrades(new SimpleQueryFilter(), new AuthorIdAndYandexUid(null, TEST_SESSIONUID));
        List<AbstractGrade> grades = gradeService.findGrades(createSessionUidFilter(), null);
        assertEquals(0, grades.size());
    }

    @Test
    public void testDeleteAll() throws Exception {
        createShopLoad(SHOP_ID);
        createShopLoad(SHOP_ID_1);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(2, grades.size());

        SimpleQueryFilter filter = new SimpleQueryFilter();
        gradeService.killGrades(filter, new AuthorIdAndYandexUid(UID, null));
        grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(0, grades.size());
    }

    @Test
    public void testDeleteGivenShopGrades() throws Exception {
        createShopLoad(SHOP_ID);
        createShopLoad(SHOP_ID_1);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(2, grades.size());

        SimpleQueryFilter filter = new SimpleQueryFilter();
        filter.addSelector("resource_id", SHOP_ID);
        filter.addSelector("type", 0);
        gradeService.killGrades(filter, new AuthorIdAndYandexUid(UID, null));
        grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());
    }

    @Test
    public void testDeleteHardGrades() {
        createShopLoad(SHOP_ID);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilter(), null);
        assertEquals(1, grades.size());

        gradeService.killAllUserGrades(UID);
        Long gradeId = grades.get(0).getId();
        AbstractGrade grade = gradeService.getGrade(gradeId);
        assertEquals(GradeState.DELETED, grade.getState());

        Instant now = Instant.now();
        createShopLoad(SHOP_ID_1);
        List<AbstractGrade> newGrades = gradeService.findGrades(createAuthorFilter(), null);
        Long newGradeId = newGrades.get(0).getId();

        gradeService.deleteHardGrades(UID, now);
        AbstractGrade deletedGrade = gradeService.getGrade(gradeId);
        assertNull(deletedGrade);

        AbstractGrade newGrade = gradeService.getGrade(newGradeId);
        assertNotNull(newGrade);
        assertEquals(GradeState.LAST, newGrade.getState());

    }

    @Test
    public void testUniqueSessionId() throws Exception {
        System.out.println(getUniqueSessionId());
    }

    @Test
    public void testSetOldState() throws Exception {
        final ShopGrade firstTestGrade = createTestShopGrade();
        final ShopGrade secondTestGrade = createTestShopGrade();
        gradeCreator.createGrade(firstTestGrade);
        gradeCreator.createGrade(secondTestGrade);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(2, grades.size());
        assertEquals(GradeState.LAST, grades.get(0).getState());
        assertEquals(GradeState.PREVIOUS, grades.get(1).getState());
    }

    @Test
    public void testNullComment() throws Exception {
        final ShopGrade testGrade = createTestShopGrade(null);
        gradeCreator.createGrade(testGrade);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());
        assertNull(grades.get(0).getText());
    }

    @Test
    public void testAccurateDate() throws Exception {
        final ShopGrade testGrade = createTestShopGrade(null);
        final Date created = DateUtil.DEFAULT_FULL_FORMAT.parse("2006-09-15 10:10:10");
        testGrade.setCreated(created);
        gradeCreator.createGrade(testGrade);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilterForShopGrades(), null);
        assertEquals(1, grades.size());
        assertEquals(created, grades.get(0).getCreated());
    }

    @Test
    public void testIncorrectSymbolsInDb() {
        int days = 60;
        log.info("Start find incorrect symbols");
        final List<Long> wrongGradeIds = new ArrayList<>();
        final Calendar fromDate = Calendar.getInstance();
        fromDate.add(Calendar.DAY_OF_MONTH, -days);

        final Calendar toDate = Calendar.getInstance();
        toDate.add(Calendar.DAY_OF_MONTH, -30);
        QueryFilter filter =
                new SimpleQueryConditionFilter(new TimeFilter("time", "cr_time", fromDate.getTime(), toDate.getTime()));

        // or check grade_id
        // Object[] values = Arrays.asList().toArray();
        // QueryFilter filter = new SimpleQueryConditionFilter(new InCondition("id", "id", values));

        final List<AbstractGrade> grades = gradeService.findGrades(filter, null);
        final List<AbstractGrade> modelGrades = new ArrayList<>();
        final List<AbstractGrade> shopGrades = new ArrayList<>();
        log.debug("Found " + grades.size() + " grades");

        for (ListIterator<AbstractGrade> it = grades.listIterator(); it.hasNext();) {
            AbstractGrade g = it.next();
            it.remove();
        }
        // exportGrades(grades, "grades.csv");
        // cropGradesShortText(grades);
        // cropModelGradesText(modelGrades);
        // cropShopGradesText(shopGrades);
        log.debug("Found " + wrongGradeIds.size() + " grades: models " + modelGrades.size() + " shops "
                + shopGrades.size());
        log.debug("ids = " + wrongGradeIds);
        log.info("Finish find incorrect symbols");
    }

    @Test
    public void testGradeCreationFailWithInvalidFactorValue() {
        Long factorId = null;
        try {
            factorId = factorCreator.addFactorAndReturnId("Имя факторa", 1, 10);
            ModelGrade grade = createTestModelGradePassport(1L);
            grade.setGradeFactorValues(List.of(new GradeFactorValue(factorId, 8)));
            createGrade(grade);

            Assert.fail();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported value: 8 for factor with id: " + factorId, ex.getMessage());
        }
    }

    @Test
    public void testOldSourceIsNull() {
        ModelGrade grade1 = createTestModelGradePassport(1L);
        createGrade(grade1);
        List<AbstractGrade> userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertNull(userGrades.get(0).getSource());

        ModelGrade grade2 = createTestModelGradePassport(1L);
        grade2.setSource("new test source");
        createGrade(grade2);
        userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("new test source", userGrades.get(0).getSource());

        ModelGrade grade3 = createTestModelGradePassport(1L);
        grade3.setSource("should not be saved in source");
        createGrade(grade3);
        userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("new test source", userGrades.get(0).getSource());
    }

    @Test
    public void testSourceChanges() {
        ModelGrade grade1 = createTestModelGradePassport(1L);
        grade1.setSource("market-poll");
        grade1.setText(null);
        grade1.setPro(null);
        grade1.setContra(null);
        createGrade(grade1);
        List<AbstractGrade> userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("market-poll", userGrades.get(0).getSource());
        Assert.assertEquals("market-poll", userGrades.get(0).getRealSource());

        // ban grade
        pgJdbcTemplate.update("update grade set mod_state = ? where id = ?",
            ModState.AUTOMATICALLY_REJECTED.value(),
            userGrades.get(0).getId());

        ModelGrade grade2 = createTestModelGradePassport(1L);
        grade2.setSource("market-email");
        createGrade(grade2);
        userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("market-poll", userGrades.get(0).getSource());
        Assert.assertEquals("market-email", userGrades.get(0).getRealSource());
    }

    @Test
    public void testSourceChangesDeleted() {
        ModelGrade grade1 = createTestModelGradePassport(1L);
        grade1.setSource("market-poll");
        grade1.setText(null);
        grade1.setPro(null);
        grade1.setContra(null);
        createGrade(grade1);
        List<AbstractGrade> userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("market-poll", userGrades.get(0).getSource());
        Assert.assertEquals("market-poll", userGrades.get(0).getRealSource());

        // delete grade
        pgJdbcTemplate.update("update grade set state = ? where id = ?",
            GradeState.DELETED.value(),
            userGrades.get(0).getId());

        ModelGrade grade2 = createTestModelGradePassport(1L);
        grade2.setSource("market-email");
        createGrade(grade2);
        userGrades = gradeService.findUserGrades(UID, null);
        Assert.assertEquals("market-poll", userGrades.get(0).getSource());
        Assert.assertEquals("market-email", userGrades.get(0).getRealSource());
    }

    @Test
    public void testModelGradeAgitationBase() {
        ModelGrade grade = createTestModelGradePassport(1L);
        grade.setPhotos(new ArrayList<>());
        createGrade(grade);
        verify(authorClient, times(1)).markContentUpdated(
            eq(UID),
            eq(AgitationEntity.MODEL),
            eq("1"),
            eq(1L),
            argThat(matchAgitation(List.of(AgitationType.MODEL_GRADE, AgitationType.MODEL_GRADE_TEXT))),
            argThat(matchAgitation(Collections.emptyList()))
        );

        verify(authorClient, times(0)).completeAgitation(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    public void testModelGradeAgitationModelYandexUid() {
        ModelGrade grade = createTestModelGradeWithoutPassport(1);
        createGrade(grade);
        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );

        verify(authorClient, times(1)).completeAgitation(
            AgitationUserType.YANDEXUID,
            TEST_YANDEXUID,
            AgitationType.MODEL_GRADE,
            "1"
        );
    }

    @Test
    public void testModelGradeAgitation() {
        // create model grade without text
        ModelGrade grade = createTestModelGradePassportNoText(1L);
        grade.setReportModel(GradeCreator.mockReportModel(1L, 444L, null, null));
        createGrade(grade);

        verify(authorClient, times(1)).markContentUpdated(
            eq(UID),
            eq(AgitationEntity.MODEL),
            eq("1"),
            eq(444L),
            argThat(matchAgitation(List.of(AgitationType.MODEL_GRADE))),
            argThat(matchAgitation(Collections.emptyList()))
        );

        // create text grade
        initMocks();
        grade = createTestModelGradePassportNoText(1L);
        grade.setText("text");
        grade.setReportModel(GradeCreator.mockReportModel(1L, 444L, null, null));
        createGrade(grade);

        verify(authorClient, times(1)).markContentUpdated(
            eq(UID),
            eq(AgitationEntity.MODEL),
            eq("1"),
            eq(444L),
            argThat(matchAgitation(List.of(AgitationType.MODEL_GRADE_TEXT))),
            argThat(matchAgitation(Collections.emptyList()))
        );

        // create grade with same text
        initMocks();
        grade = createTestModelGradePassportNoText(1L);
        grade.setText("text");
        grade.setReportModel(GradeCreator.mockReportModel(1L, 444L, null, null));
        createGrade(grade);

        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );

        // remove text from grade, add photo
        initMocks();
        grade = createTestModelGradePassportNoText(1L);
        grade.setText(null);
        grade.setReportModel(GradeCreator.mockReportModel(1L, 444L, null, null));
        grade.setPhotos(List.of(Photo.buildForTest("group", "name", null)));
        createGrade(grade);

        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );

        // add text with same photo, add photo
        initMocks();
        grade = createTestModelGradePassportNoText(1L);
        grade.setText("text");
        grade.setReportModel(GradeCreator.mockReportModel(1L, 444L, null, null));
        grade.setPhotos(List.of(Photo.buildForTest("group", "name", null)));
        createGrade(grade);

        verify(authorClient, times(0)).markContentUpdated(
            eq(UID),
            eq(AgitationEntity.MODEL),
            eq("1"),
            eq(444L),
            argThat(matchAgitation(List.of(AgitationType.MODEL_GRADE_PHOTO))),
            argThat(matchAgitation(List.of(AgitationType.MODEL_GRADE_TEXT)))
        );
    }

    @Test
    public void testShopGradeAgitation() {
        ShopGrade grade = createTestShopGrade(123L, "text");
        createGrade(grade);
        verify(authorClient, times(1)).markContentUpdated(
            eq(UID),
            eq(AgitationEntity.SHOP),
            eq("123"),
            isNull(),
            argThat(matchAgitation(List.of(AgitationType.SHOP_GRADE, AgitationType.SHOP_GRADE_TEXT))),
            argThat(matchAgitation(Collections.emptyList()))
        );

        verify(authorClient, times(0)).completeAgitation(
            any(),
            any(),
            any(),
            any()
        );
    }


    @Test
    public void testModelGradeAgitationShopYandexUid() {
        ShopGrade grade = createTestShopGradeWithoutPassport();
        createGrade(grade);
        verify(authorClient, times(0)).markContentUpdated(
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        );

        verify(authorClient, times(1)).completeAgitation(
            AgitationUserType.YANDEXUID,
            TEST_YANDEXUID,
            AgitationType.SHOP_GRADE,
            String.valueOf(SHOP_ID)
        );
    }

    @Test
    public void testGradeEmptyText() throws Exception {
        ModelGrade grade = GradeCreator.constructModelGrade(MODEL_ID, UID);
        grade.setText(null);
        grade.setPro(" ");
        grade.setContra("");

        gradeCreator.createGrade(grade);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilter(UID), null);
        assertEquals(1, grades.size());

        assertNull(grades.get(0).getText());
        assertNull(grades.get(0).getPro());
        assertNull(grades.get(0).getContra());
    }

    @Test
    public void testGradeWithText() throws Exception {
        ModelGrade grade = GradeCreator.constructModelGrade(MODEL_ID, UID);
        grade.setText(" Some text   ");
        grade.setPro("Non-user line break\n");
        grade.setContra("");

        gradeCreator.createGrade(grade);
        List<AbstractGrade> grades = gradeService.findGrades(createAuthorFilter(UID), null);
        assertEquals(1, grades.size());

        assertEquals("Some text", grades.get(0).getText());
        assertEquals("Non-user line break",grades.get(0).getPro());
        assertNull(grades.get(0).getContra());
    }

    private <T> ArgumentMatcher<List<T>> matchAgitation(List<T> target){
        return source -> {
            if(source == null) {
                return false;
            }
            return new HashSet<>(source).equals(new HashSet<>(target));
        };
    }

}

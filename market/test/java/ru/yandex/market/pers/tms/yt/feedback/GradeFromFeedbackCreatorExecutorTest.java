package ru.yandex.market.pers.tms.yt.feedback;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.grade.client.model.DeliveredBy;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.MarketBusinessScheme;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.model.GradeFilter;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.GradeValue;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.service.common.util.PersUtils;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;
import ru.yandex.market.util.ListUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.yt.feedback.GradeFromFeedbackCreatorExecutor.DEF_LAST_EXECUTED;
import static ru.yandex.market.pers.tms.yt.feedback.GradeFromFeedbackCreatorExecutor.LAST_EXECUTED_KEY;

public class GradeFromFeedbackCreatorExecutorTest extends MockedPersTmsTest {
    private static final long FEEDBACK_ID = 1L;
    private static final long USER_ID = 123L;
    private static final long SUPPLIER_ID = 4321L;
    private static final long ORDER_ID = 11111L;
    private static final String SOME_TEXT = "some text";

    private static final long ANY_GUILTY_QUESTION = 100;
    private static final long SHOP_GUILTY_QUESTION = 101;
    private static final long LATE_GUILTY_QUESTION = FeedbackContext.LATE_ORDER_QUESTION_ID;
    private static final long OTHER_QUESTION = 102;
    private static final long GOOD_QUESTION = 103;
    private static final long GOOD_QUESTION_VERY = 1;

    private static final Long UPDATED_AT = GradeFromFeedbackCreatorExecutor.DEF_LAST_EXECUTED + 10101L;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private GradeFromFeedbackCreatorExecutor executor;
    @Autowired
    private DbGradeService gradeService;
    @Autowired
    private YtExportHelper ytExportHelper;
    @Autowired
    private GradeModeratorModificationProxy moderationProxy;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    ComplexMonitoring complexMonitoring;


    @Before
    public void initYtMocks() {
        mockGuiltQuery();
        mockQuestionsQuery();
        mockConfigurationQuery();
    }

    @Test
    public void testLastExecutionTimeUpdates() throws Exception {
        configurationService.mergeValue(LAST_EXECUTED_KEY, DEF_LAST_EXECUTED);
        mockMainQuery(buildBasicFeedback());

        executor.runTmsJob();

        assertEquals(UPDATED_AT, configurationService.getValueAsLong(LAST_EXECUTED_KEY));
    }

    @Test
    public void testFeedbackBasicBad() throws Exception {
        // simply bad grade for dsbs
        FeedbackData feedback = buildBasicFeedback();
        feedback.questions.add(ANY_GUILTY_QUESTION);
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, SOME_TEXT, null, "Торт был со стеклом");
        assertEquals(SUPPLIER_ID, grades.get(0).getResourceId().longValue());
        assertEquals(USER_ID, grades.get(0).getAuthorUid().longValue());
        assertEquals(DeliveredBy.SHOP, grades.get(0).getDeliveredBy());
        assertTrue(grades.get(0).getCpa());
        assertTrue(grades.get(0).getVerified());

        // check no security data passed (default ip and port)
        Map<String, Object> secData = readSecData(grades.get(0).getId());
        assertNotNull(secData);
        assertEquals("127.0.0.1", secData.get("ip"));
        assertNull(secData.get("port"));
        assertTrue(((String)secData.get("headers")).contains("feedback: true"));
    }

    @Test
    public void testFeedbackBasicBadWithIp() throws Exception {
        // simply bad grade for dsbs
        FeedbackData feedback = buildBasicFeedback();
        feedback.ip = "some_ip";
        feedback.port = 12342;
        feedback.headersMap = PersUtils.toMap("myheader", "somevalue");
        feedback.questions.add(SHOP_GUILTY_QUESTION);
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());

        // check security data passed (inherited ip and port)
        Map<String, Object> secData = readSecData(grades.get(0).getId());
        assertNotNull(secData);
        assertEquals("some_ip", secData.get("ip"));
        assertEquals(12342, secData.get("port"));
        assertFalse(((String)secData.get("headers")).contains("feedback: true"));
        assertTrue(((String)secData.get("headers")).contains("myheader: somevalue"));
    }

    @Test
    public void testFeedbackBasicNothingToBlame() throws Exception {
        // bad grade, no links to the shop -> do not create grade
        mockMainQuery(buildBasicFeedback());

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(0, grades.size());
    }

    @Test
    public void testFeedbackBasicMarket() throws Exception {
        // always blame order with market supplier, even when links are not added
        FeedbackData feedback = buildBasicFeedback();
        feedback.supplierId = FeedbackData.MARKET_SUPPLIER_ID;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, SOME_TEXT, null, null);
        assertEquals(FeedbackData.MARKET_SUPPLIER_ID, grades.get(0).getResourceId().longValue());
        assertEquals(USER_ID, grades.get(0).getAuthorUid().longValue());
    }


    @Test
    public void testFeedbackBasicMarketMultiSupplier() throws Exception {
        // always blame order with market supplier, even withing multi-supplier order
        FeedbackData feedback = buildBasicFeedback();
        feedback.supplierId = FeedbackData.MARKET_SUPPLIER_ID;
        feedback.singleSupplier = false;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, SOME_TEXT, null, null);
        assertEquals(FeedbackData.MARKET_SUPPLIER_ID, grades.get(0).getResourceId().longValue());
        assertEquals(USER_ID, grades.get(0).getAuthorUid().longValue());
    }

    @Test
    public void testFeedbackBasicNotBest() throws Exception {
        // goot, but not excellent grade - save as passed
        FeedbackData feedback = buildBasicFeedback();
        feedback.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.GOOD, SOME_TEXT, null, null);
        assertEquals(SUPPLIER_ID, grades.get(0).getResourceId().longValue());
        assertEquals(USER_ID, grades.get(0).getAuthorUid().longValue());
    }


    @Test
    public void testFeedbackOkText() throws Exception {
        // 5star feedback -> create grade with text
        FeedbackData feedback = buildBasicFeedback();
        feedback.grade = GradeValue.EXCELLENT.toAvgGrade();
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.EXCELLENT, SOME_TEXT, null, null);
    }

    @Test
    public void testFeedbackWithPro() throws Exception {
        // 5star feedback -> create grade with text
        FeedbackData dbsFeedback = buildBasicFeedback();
        dbsFeedback.grade = GradeValue.EXCELLENT.toAvgGrade();
        dbsFeedback.setQuestions(PersUtils.toMap(GOOD_QUESTION, null));

        FeedbackData fbsFeedback = buildBasicFeedback();
        fbsFeedback.grade = GradeValue.EXCELLENT.toAvgGrade();
        fbsFeedback.scheme = MarketBusinessScheme.FBS;
        fbsFeedback.setQuestions(PersUtils.toMap(GOOD_QUESTION, null));

        mockMainQuery(List.of(dbsFeedback, fbsFeedback));

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(2, grades.size());
        assertGrade(grades.get(0), GradeValue.EXCELLENT, SOME_TEXT, "Курьер пользовался средствами защиты", null);
        assertGrade(grades.get(1), GradeValue.EXCELLENT, SOME_TEXT, null, null);
    }

    @Test
    public void testFeedbackBadTextSingleSup() throws Exception {
        // один саплаер - значит если сработает хоть одна проверка на негатив,
        // то вопросы должны скинуться в текст
        // причём добавиться должны все вопросы в порядке id
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            ANY_GUILTY_QUESTION, null,
            SHOP_GUILTY_QUESTION, 123,
            OTHER_QUESTION, null
        ));
        // должен добавить доп. вопрос
        feedback.wasLate = true;
        mockMainQuery(feedback);

        executor.runTmsJob();

        String expectedContra = "• Привезли завтра, а я не дома\n" +
            "• Торт был со стеклом\n" +
            "• Вместо носков привезли вискарь\n" +
            "• Плохая доставка";

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, SOME_TEXT, null, expectedContra);

    }

    @Test
    public void testFeedbackBadTextClickAndCollect() throws Exception {
        // то же, что и предыдущий тест, но это click&collect
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            ANY_GUILTY_QUESTION, null,
            SHOP_GUILTY_QUESTION, 123,
            OTHER_QUESTION, null
        ));
        // должен добавить доп. вопрос
        feedback.wasLate = true;
        feedback.scheme = MarketBusinessScheme.CLICK_AND_COLLECT;
        mockMainQuery(feedback);

        executor.runTmsJob();

        String expectedContra = "• Привезли завтра, а я не дома\n" +
            "• Торт был со стеклом\n" +
            "• Вместо носков привезли вискарь\n" +
            "• Плохая доставка";

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, SOME_TEXT, null, expectedContra);

    }

    @Test
    public void testFeedbackBadTextSingleSupNoText() throws Exception {
        // то же, но пользователь не оставил текста - только кликал по чекбоксам.
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            SHOP_GUILTY_QUESTION, null
        ));
        feedback.text = null;
        feedback.scheme = MarketBusinessScheme.FBS;
        mockMainQuery(feedback);

        executor.runTmsJob();

        String expectedContra = "Вместо носков привезли вискарь";

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, null, null, expectedContra);
        assertEquals(DeliveredBy.MARKET, grades.get(0).getDeliveredBy());
    }

    @Test
    public void testFeedbackBadTextSingleSupEmptyText() throws Exception {
        // то же, но пользователь не оставил текста - только кликал по чекбоксам.
        // и так как это dsbs - поставим указанную оценку в итоге
        // Поле текста пустое - не должны сгенерить буллеты
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            SHOP_GUILTY_QUESTION, null
        ));
        feedback.text = "  ";
        mockMainQuery(feedback);

        executor.runTmsJob();

        String expectedContra = "Вместо носков привезли вискарь";

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, null, null, expectedContra);
        assertEquals(DeliveredBy.SHOP, grades.get(0).getDeliveredBy());
    }

    @Test
    public void testFeedbackBadWithEmptyEmptyContra() throws Exception {
        // то же, но пользователь не оставил текста - только кликал по чекбоксам.
        // это FBS, чекбокс учитываем при формировании оценки,
        // тк вина продавца, что он поздно привез товар в сортировочный центр
        // но в отзыве этот чекбокс не отображаем
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            LATE_GUILTY_QUESTION, null
        ));
        feedback.scheme = MarketBusinessScheme.FBS;
        feedback.text = null;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGrade(grades.get(0), GradeValue.TERRIBLE, null, null, null);
        assertEquals(DeliveredBy.MARKET, grades.get(0).getDeliveredBy());
    }

    @Test
    public void testFeedbackBadTextSingleSupNoTextOnlyCommon() throws Exception {
        // то же, но пользователь не оставил текста - только кликал по чекбоксам.
        // отметил нейтрально-плохой вопрос и ничего больше. Нет смысла превращать в отзыв
        // не создадим никакого отзыва
        FeedbackData feedback = buildBasicFeedback();
        feedback.setQuestions(PersUtils.toMap(
            OTHER_QUESTION, null
        ));
        feedback.text = null;
        feedback.scheme = MarketBusinessScheme.FBS;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(0, grades.size());
    }

    @Test
    public void testFeedbackWithBadFeedback() throws Exception {
        int cases = 9;

        // Сразу много кейсов на плохой фидбек.
        // Один магазин, разные заказы.
        // Каждый заказ рассматривается отдельно по набору данных в нём, поэтому проверки независимы.
        List<FeedbackData> feedbacks = IntStream.range(0, cases)
            .mapToObj(x -> {
                FeedbackData result = buildBasicFeedback();
                result.orderId = ORDER_ID + x;
                result.text = SOME_TEXT + (ORDER_ID + x);
                return result;
            })
            .collect(Collectors.toList());

        feedbacks.get(0).wasLate = true;

        // кейсы на одного саплаера
        feedbacks.get(2).setQuestions(PersUtils.toMap(ANY_GUILTY_QUESTION, null));
        feedbacks.get(2).scheme = MarketBusinessScheme.FBY;

        feedbacks.get(3).setQuestions(PersUtils.toMap(SHOP_GUILTY_QUESTION, null));
        feedbacks.get(3).scheme = MarketBusinessScheme.FBY;

        feedbacks.get(4).setQuestions(PersUtils.toMap(LATE_GUILTY_QUESTION, null));
        feedbacks.get(4).scheme = MarketBusinessScheme.FBY;

        // кейсы на несколько саплаеров (притворимся, что пришли такие данные)
        feedbacks.get(5).singleSupplier = false;
        feedbacks.get(5).scheme = MarketBusinessScheme.FBY;
        feedbacks.get(5).setQuestions(PersUtils.toMap(ANY_GUILTY_QUESTION, 111));

        feedbacks.get(6).singleSupplier = false;
        feedbacks.get(6).scheme = MarketBusinessScheme.FBY;
        feedbacks.get(6).setQuestions(PersUtils.toMap(SHOP_GUILTY_QUESTION, 111));

        feedbacks.get(7).singleSupplier = false;
        feedbacks.get(7).scheme = MarketBusinessScheme.FBY;
        feedbacks.get(7).setQuestions(PersUtils.toMap(LATE_GUILTY_QUESTION, 111));

        //вопрос не входит в список тех, которые учитываем при выставлении негативной оценки
        feedbacks.get(8).singleSupplier = false;
        feedbacks.get(8).scheme = MarketBusinessScheme.FBY;
        feedbacks.get(8).setQuestions(PersUtils.toMap(OTHER_QUESTION, 111));

        // Не забудь изменить переменную "cases", если добавил больше кейсов

        mockMainQuery(feedbacks);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(3, grades.size());

        // map order -> grade
        Map<Long, ShopGrade> gradesMap = ListUtils.toMap(grades, x -> Long.parseLong(x.getOrderId()), x -> x);
        assertEquals(3, gradesMap.size());

        gradesMap.forEach((key, grade) -> {
            assertGradeVal(grade, GradeValue.TERRIBLE);
            assertEquals(SOME_TEXT + grade.getOrderId(), grade.getText());
            assertEquals(GradeState.LAST, grade.getState());
            // проверки на генерацию pro/contra в отдельном тесте, чтобы не усложнять этот
        });
    }


    @Test
    public void testFeedbackWithSkipBadFeedback() throws Exception {
        int cases = 5;

        // Сразу много кейсов на скип фидбека.
        // Исходный фидбек всегда негативный, но он не относится к указанному магазину, и потому игнорится
        // Один магазин, разные заказы.
        // Каждый заказ рассматривается отдельно по набору данных в нём, поэтому проверки независимы.
        List<FeedbackData> feedbacks = IntStream.range(0, cases)
            .mapToObj(x -> {
                FeedbackData result = buildBasicFeedback();
                result.orderId = ORDER_ID + x;
                return result;
            })
            .collect(Collectors.toList());

        // кейсы на одного саплаера: указан негативный ответ, но магазин не того типа, чтобы сделать плохую оценку
        feedbacks.get(0).setQuestions(PersUtils.toMap(SHOP_GUILTY_QUESTION, null));
        feedbacks.get(0).scheme = MarketBusinessScheme.FBY;

        feedbacks.get(1).setQuestions(PersUtils.toMap(LATE_GUILTY_QUESTION, null));
        feedbacks.get(1).scheme = MarketBusinessScheme.FBY;

        // кейсы на несколько саплаеров (притворимся, что пришли такие данные)
        // указан плохой ответ, и все флаги есть, но его нельзя ассоциировать с этим магазином
        feedbacks.get(2).singleSupplier = false;
        feedbacks.get(2).setQuestions(PersUtils.toMap(SHOP_GUILTY_QUESTION, null));
        feedbacks.get(2).scheme = MarketBusinessScheme.FBY;

        feedbacks.get(3).singleSupplier = false;
        feedbacks.get(3).setQuestions(PersUtils.toMap(LATE_GUILTY_QUESTION, null));
        feedbacks.get(3).scheme = MarketBusinessScheme.FBY;

        // товар привязан к какому-то вопросу, который не критичен для принятия решения
        feedbacks.get(4).singleSupplier = false;
        feedbacks.get(4).setQuestions(PersUtils.toMap(OTHER_QUESTION, 111));
        feedbacks.get(4).scheme = MarketBusinessScheme.FBY;

        // Не забудь изменить переменную "cases", если добавил больше кейсов

        mockMainQuery(feedbacks);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(0, grades.size());
    }

    @Test
    public void testFeedbackAfterMod() throws Exception {
        // создаём обычный негативный фидбек
        FeedbackData feedback = buildBasicFeedback();
        feedback.wasLate = true;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGradeVal(grades.get(0), GradeValue.TERRIBLE);
        assertEquals(ModState.UNMODERATED, grades.get(0).getModState());

        // апрув исходного отзыва
        moderationProxy.moderateGradeReplies(List.of(grades.get(0).getId()),
            List.of(), DbGradeAdminService.FAKE_MODERATOR, ModState.APPROVED);
        grades = getAllGrades();
        assertEquals(1, grades.size());
        assertEquals(ModState.APPROVED, grades.get(0).getModState());

        // попытка оставить тот же фидбек - ничего не происходит (т.к. текст тот же)
        executor.runTmsJob();

        grades = getAllGrades();
        assertEquals(1, grades.size());
        assertEquals(ModState.APPROVED, grades.get(0).getModState());
        assertEquals(SOME_TEXT, grades.get(0).getText());

        // меняем текст - сбрасывается
        FeedbackData otherFeedback = buildBasicFeedback();
        otherFeedback.wasLate = true;
        otherFeedback.text = SOME_TEXT + " changed";
        mockMainQuery(otherFeedback);

        executor.runTmsJob();

        grades = getAllGrades();
        assertEquals(2, grades.size());
        assertEquals(GradeState.PREVIOUS, grades.get(0).getState());
        assertEquals(ModState.APPROVED, grades.get(0).getModState());
        assertEquals(GradeState.LAST, grades.get(1).getState());
        assertEquals(ModState.UNMODERATED, grades.get(1).getModState());
        assertEquals(SOME_TEXT + " changed", grades.get(1).getText());
    }

    @Test
    public void testFeedbackAfterBadMod() throws Exception {
        // создаём обычный негативный фидбек
        FeedbackData feedback = buildBasicFeedback();
        feedback.wasLate = true;
        mockMainQuery(feedback);

        executor.runTmsJob();

        List<ShopGrade> grades = getAllGrades();
        assertEquals(1, grades.size());
        assertGradeVal(grades.get(0), GradeValue.TERRIBLE);
        assertEquals(ModState.UNMODERATED, grades.get(0).getModState());

        // баним
        moderationProxy.moderateGradeReplies(List.of(grades.get(0).getId()),
            List.of(), DbGradeAdminService.FAKE_MODERATOR, ModState.REJECTED);
        grades = getAllGrades();
        assertEquals(1, grades.size());
        assertEquals(ModState.REJECTED, grades.get(0).getModState());

        // пробуем создать с изменением текста - результат также будет забанен
        FeedbackData otherFeedback = buildBasicFeedback();
        otherFeedback.wasLate = true;
        otherFeedback.text = SOME_TEXT + " changed";
        mockMainQuery(otherFeedback);

        executor.runTmsJob();

        grades = getAllGrades();
        assertEquals(2, grades.size());
        assertEquals(GradeState.PREVIOUS, grades.get(0).getState());
        assertEquals(ModState.REJECTED, grades.get(0).getModState());
        assertEquals(GradeState.LAST, grades.get(1).getState());
        assertEquals(ModState.REJECTED, grades.get(1).getModState());
        assertEquals(SOME_TEXT + " changed", grades.get(1).getText());
    }

    @Test
    public void testDeletedGradeIsNotCreated() throws Exception {
        FeedbackData data = buildBasicFeedback();
        data.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;

        ShopGrade grade = GradeCreator.constructShopGrade(SUPPLIER_ID, USER_ID);
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource(GradeSource.FEEDBACK.value());
        long id = gradeCreator.createGrade(grade);
        gradeService.killGrades(List.of(id));

        mockMainQuery(data);
        executor.runTmsJob();

        List<AbstractGrade> grades = gradeService.findGrades(new SimpleQueryFilter(), null);
        assertEquals(1, grades.size());
        assertTrue(grades.stream().noneMatch(g -> GradeState.LAST == g.getState()));
    }

    @Test
    public void testResolvedGradeIsNotUpdated() throws Exception {
        FeedbackData data = buildBasicFeedback();
        data.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;

        ShopGrade grade = GradeCreator.constructShopGrade(SUPPLIER_ID, USER_ID);
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource(GradeSource.FEEDBACK.value());
        long id = gradeCreator.createGrade(grade);

        gradeService.resolveGrade(id, USER_ID, true);

        mockMainQuery(data);
        executor.runTmsJob();

        List<AbstractGrade> grades = gradeService.findGrades(new SimpleQueryFilter(), null);
        assertEquals(1, grades.size());
    }

    @Test
    public void testManuallyChangedGradeIsNotUpdated() throws Exception {
        FeedbackData data = buildBasicFeedback();
        data.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;

        // create feedback grade
        ShopGrade grade = GradeCreator.constructShopGrade(SUPPLIER_ID, USER_ID);
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource(GradeSource.FEEDBACK.value());
        long id = gradeCreator.createGrade(grade);

        // update by user
        grade.setSource("other source");
        grade.setText("changed text");
        long idChanged = gradeCreator.createGrade(grade);

        assertNotEquals(id, idChanged);

        mockMainQuery(data);
        executor.runTmsJob();

        List<AbstractGrade> grades = gradeService.findGrades(new SimpleQueryFilter(), null);
        assertEquals(2, grades.size());
    }

    @Test
    public void testCreateCloneWhenUncertain() throws Exception {
        // пользователь оставил отзыв на магазин сам (неважно на какой заказ). Фидбек создаст ещё один отзыв.
        FeedbackData data = buildBasicFeedback();
        data.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;

        // create feedback grade
        ShopGrade grade = GradeCreator.constructShopGrade(SUPPLIER_ID, USER_ID);
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource("something");
        long id = gradeCreator.createGrade(grade);

        mockMainQuery(data);
        executor.runTmsJob();

        List<AbstractGrade> grades = gradeService.findGrades(new SimpleQueryFilter(), null);
        assertEquals(2, grades.size());
        assertTrue(grades.stream().allMatch(g -> GradeState.LAST == g.getState()));
    }

    @Test
    public void testUpdateAfterGeneratedFeedback() throws Exception {
        FeedbackData data = buildBasicFeedback();
        data.grade = FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE;

        // create feedback grade
        ShopGrade grade = GradeCreator.constructShopGrade(SUPPLIER_ID, USER_ID);
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource(GradeSource.FEEDBACK.value());
        long id = gradeCreator.createGrade(grade);

        mockMainQuery(data);
        executor.runTmsJob();

        List<AbstractGrade> grades = gradeService.findGrades(new SimpleQueryFilter(), null);
        assertEquals(2, grades.size());
        assertEquals(1, grades.stream().filter(g -> GradeState.LAST == g.getState()).count());
    }

    @Test
    public void testNegativeSupplierIdFeedback() throws Exception {
        FeedbackData feedback = buildBasicFeedback();
        feedback.supplierId = -1;
        mockMainQuery(feedback);

        executor.runTmsJob();

        //there are no grades, cause negative supplierId
        List<ShopGrade> grades = getAllGrades();
        assertEquals(0, grades.size());
        assertEquals(MonitoringStatus.WARNING, complexMonitoring.getResult().getStatus());
    }

    private FeedbackData buildBasicFeedback() {
        FeedbackData result = new FeedbackData();
        result.id = FEEDBACK_ID;
        result.grade = GradeValue.TERRIBLE.toAvgGrade();
        result.text = SOME_TEXT;
        result.orderId = ORDER_ID;
        result.crTime = Timestamp.from(Instant.ofEpochMilli(UPDATED_AT));
        result.userId = USER_ID;
        result.supplierId = SUPPLIER_ID;
        result.scheme = MarketBusinessScheme.DBS;
        result.singleSupplier = true;
        result.wasLate = false;
        return result;

    }

    private void mockMainQuery(FeedbackData feedback) {
        mockMainQuery(List.of(feedback));
    }

    private void mockMainQuery(List<FeedbackData> feedbacks) {
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == null) {
                return null;
            }
            Function<JsonNode, FeedbackData> parser = invocation.getArgument(2);
            Consumer<List<FeedbackData>> consumer = invocation.getArgument(3);

            consumer.accept(ListUtils.toList(prepareFeedbackNode(feedbacks), parser));
            return null;
        }).when(ytExportHelper.getHahnYtClient()).consumeTableBatched(
            ArgumentMatchers.argThat(argument -> argument.toString().contains("feedback_to_grade")),
            anyInt(),
            any(Function.class),
            any(Consumer.class)
        );
    }

    private void mockConfigurationQuery() {
        when(ytExportHelper.getHahnYtClient().readNodes(
            ArgumentMatchers.argThat(argument -> argument != null && argument.toString().endsWith(
                "/configuration"))
        )).thenReturn(toNodes(List.of(
            Map.of("id", FeedbackConfiguration.MIN_POSITIVE_GRADE_KEY,
                "value", FeedbackConfiguration.MIN_POSITIVE_GRADE_DEFAULT_VALUE)
        )));
    }

    private void mockGuiltQuery() {
        when(ytExportHelper.getHahnYtClient().readNodes(
            ArgumentMatchers.argThat(argument -> argument != null && argument.toString().endsWith(
                "/order_question_guilt"))
        )).thenReturn(toNodes(List.of(
            Map.of("guilt_type", FeedbackContext.ANY_GUILTY_QUESTIONS,
                "order_question_id", ANY_GUILTY_QUESTION),
            Map.of("guilt_type", FeedbackContext.SHOP_GUILTY_QUESTIONS,
                "order_question_id", SHOP_GUILTY_QUESTION),
            Map.of("guilt_type", FeedbackContext.LATE_GUILTY_QUESTIONS,
                "order_question_id", LATE_GUILTY_QUESTION)
        )));
    }

    private void mockQuestionsQuery() {
        when(ytExportHelper.getHahnYtClient().readNodes(
            ArgumentMatchers.argThat(argument -> argument != null && argument.toString().endsWith("/order_question"))
        )).thenReturn(toNodes(List.of(
            Map.of("id", ANY_GUILTY_QUESTION,
                "title", "Торт был со стеклом",
                "pos_fl", false,
                "neg_fl", true,
                "shop_fl", true,
                "cons_neg", arrayAsString(),
                "need_pub", arrayAsString()),
            Map.of("id", SHOP_GUILTY_QUESTION,
                "title", "Вместо носков привезли вискарь",
                "pos_fl", false,
                "neg_fl", true,
                "shop_fl", true,
                "cons_neg", arrayAsString(MarketBusinessScheme.FBS, MarketBusinessScheme.DBS, MarketBusinessScheme.CLICK_AND_COLLECT),
                "need_pub", arrayAsString(MarketBusinessScheme.FBS, MarketBusinessScheme.DBS, MarketBusinessScheme.CLICK_AND_COLLECT)),
            Map.of("id", LATE_GUILTY_QUESTION,
                "title", "Привезли завтра, а я не дома",
                "pos_fl", false,
                "neg_fl", true,
                "shop_fl", true,
                "cons_neg", arrayAsString(MarketBusinessScheme.FBS, MarketBusinessScheme.DBS, MarketBusinessScheme.FBY_PLUS, MarketBusinessScheme.CLICK_AND_COLLECT),
                "need_pub", arrayAsString(MarketBusinessScheme.DBS, MarketBusinessScheme.CLICK_AND_COLLECT)),
            Map.of("id", OTHER_QUESTION,
                "title", "Плохая доставка",
                "pos_fl", false,
                "neg_fl", true,
                "shop_fl", false,
                "need_pub", arrayAsString(MarketBusinessScheme.DBS, MarketBusinessScheme.CLICK_AND_COLLECT),
                "cons_neg", arrayAsString(MarketBusinessScheme.DBS, MarketBusinessScheme.CLICK_AND_COLLECT)),
            Map.of("id", GOOD_QUESTION,
                "title", "Курьер пользовался средствами защиты",
                "pos_fl", true,
                "neg_fl", false,
                "shop_fl", false,
                "need_pub", arrayAsString(MarketBusinessScheme.DBS),
                "cons_neg", ""),
            Map.of("id", GOOD_QUESTION_VERY,
                "title", "Курьер сделал мне хорошо",
                "pos_fl", true,
                "neg_fl", false,
                "shop_fl", false,
                "need_pub", arrayAsString(MarketBusinessScheme.DBS),
                "cons_neg","")
        )));
    }

    private <T> List<JsonNode> toNodes(List<T> items) {
        try {
            String json = mapper.writeValueAsString(items);
            JsonNode node = mapper.readTree(json);
            List<JsonNode> result = new ArrayList<>();
            node.iterator().forEachRemaining(result::add);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<JsonNode> prepareFeedbackNode(List<FeedbackData> feedbacks) {
        return toNodes(ListUtils.toList(feedbacks, FeedbackData::toJsonMap));
    }

    private List<ShopGrade> getAllGrades() {
        return gradeService.loadGradesPg(new GradeFilter()).stream()
            .map(x -> (ShopGrade) x)
            .sorted(Comparator.comparing(AbstractGrade::getId))
            .collect(Collectors.toList());
    }


    private void assertGradeVal(ShopGrade grade, GradeValue value) {
        assertEquals(value.toGr0(), grade.getGradeValue().intValue());
    }

    private void assertGrade(ShopGrade grade, GradeValue value, String text, String pro, String contra) {
        assertGradeVal(grade, value);
        assertEquals(text, grade.getText());
        assertEquals(pro, grade.getPro());
        assertEquals(contra, grade.getContra());
    }

    @NotNull
    private Map<String, Object> readSecData(long gradeId) {
        List<Map<String, Object>> result = pgJdbcTemplate.query(
            "select ip, port, http_headers\n" +
                "from security_data\n" +
                "where grade_id = ?",
            (rs, rowNum) -> PersUtils.buildMap(
                "ip", rs.getString("ip"),
                "port", DbUtil.getInteger(rs, "port"),
                "headers", rs.getString("http_headers")
            ),
            gradeId
        );
        return result.isEmpty() ? null : result.get(0);
    }

    private static String arrayAsString() {
        return arrayAsString(MarketBusinessScheme.values());
    }

    private static String arrayAsString(MarketBusinessScheme... schemes) {
        return Stream.of(schemes).map(MarketBusinessScheme::name).collect(Collectors.joining(","));
    }

}

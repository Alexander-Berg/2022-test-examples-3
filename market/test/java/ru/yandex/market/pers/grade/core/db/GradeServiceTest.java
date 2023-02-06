package ru.yandex.market.pers.grade.core.db;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author kukabara
 */
public class GradeServiceTest extends MockedTest {
    @Autowired
    private DbGradeService gradeService;

    @Autowired
    private GradeCreator gradeCreator;


    @Test
    public void testCreateAndResolve() {
        AbstractGrade grade = GradeCreator.constructShopGradeRnd();
        long gradeId = gradeCreator.createGrade(grade);

        assertFalse("Not authorized resolving",
            gradeService.resolveGrade(gradeId, grade.getAuthorUid() + 1, true)
        );
        assertTrue(gradeService.resolveGrade(gradeId, grade.getAuthorUid(), true));

        grade.setText("other text");
        long resolvedGradeId = gradeCreator.createGrade(grade);
        assertNotEquals(resolvedGradeId, gradeId);
        AbstractGrade aGrade = gradeService.getGrade(resolvedGradeId);
        assertTrue(aGrade instanceof ShopGrade);
        ShopGrade sGrade = (ShopGrade) aGrade;
        assertEquals("other text", sGrade.getText());
    }

    @Test
    public void testAnonymity() {
        AbstractGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setAnonymous(Anonymity.NONE);
        grade.setModState(getModStateByAnonimity(grade.getAnonymous()));
        long gradeId = gradeCreator.createGrade(grade);

        AbstractGrade aGrade = gradeService.getGrade(gradeId);
        assertEquals(ModState.UNMODERATED, aGrade.getModState());


        // edit only grade anonimity
        grade.setAnonymous(Anonymity.HIDE_ALL);
        grade.setModState(getModStateByAnonimity(grade.getAnonymous()));
        long anonymGradeId = gradeCreator.createGrade(grade);
        assertEquals("Return the same gradeId for almoust the same grades", gradeId, anonymGradeId);
        aGrade = gradeService.getGrade(anonymGradeId);
        assertEquals(ModState.AUTOMATICALLY_REJECTED, aGrade.getModState());

        // edit anonimity and text
        grade.setText("new text");
        long anonymGradeId2 = gradeCreator.createGrade(grade);
        assertEquals("Return the same gradeId for almoust the same grades", gradeId, anonymGradeId);
        aGrade = gradeService.getGrade(anonymGradeId2);
        assertEquals(ModState.AUTOMATICALLY_REJECTED, aGrade.getModState());
        assertNotEquals("Should return different ids for different texts", gradeId, anonymGradeId2);
    }

    @Test
    public void testCreateAndSmallEditShopGrade() {
        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setModState(ModState.APPROVED);
        long gradeId = gradeCreator.createGrade(grade);

        // edit only grade anonimity, order-id, delivery type
        grade.setAnonymous(Anonymity.HIDE_ALL);
        grade.setModState(getModStateByAnonimity(Anonymity.HIDE_ALL));

        Integer newRegionId = null;
        grade.setRegionId(newRegionId);
        grade.setDelivery(Delivery.INSTORE);

        String newOrderId = "12345";
        grade.setOrderId(newOrderId);

        long newGrade = gradeCreator.createGrade(grade);

        AbstractGrade gr = gradeService.getGrade(newGrade);
        assertEquals(Anonymity.HIDE_ALL, gr.getAnonymous());
        assertEquals(ModState.AUTOMATICALLY_REJECTED, gr.getModState());
        assertEquals(newRegionId, gr.getRegionId());
        assertEquals(newOrderId, ((ShopGrade) gr).getOrderId());
        assertEquals(Delivery.INSTORE, ((ShopGrade) gr).getDelivery());
        assertEquals("Return the same gradeId for almost the same grades", gradeId, newGrade);
    }

    @Test
    public void testCreateAndSmallEditModelGrade()  {
        Long baseOrderId = 1234L;

        ModelGrade grade =  GradeCreator.constructModelGradeRnd();
        grade.setModState(ModState.APPROVED);
        long gradeId = gradeCreator.createGrade(grade);
        setModelOrderId(baseOrderId, gradeId);

        // edit only grade anonimity, order-id, delivery type
        grade.setAnonymous(Anonymity.HIDE_ALL);
        grade.setModState(getModStateByAnonimity(Anonymity.HIDE_ALL));
        grade.setUsageTime(UsageTime.SEVERAL_MONTHS);
        long newGrade = gradeCreator.createGrade(grade);
        assertEquals(baseOrderId, getModelOrderId(newGrade));

        AbstractGrade gr = gradeService.getGrade(newGrade);
        assertEquals("Return the same gradeId for almost the same grades", gradeId, newGrade);
        assertEquals(Anonymity.HIDE_ALL, gr.getAnonymous());
        assertEquals(ModState.AUTOMATICALLY_REJECTED, gr.getModState());
        assertEquals(UsageTime.SEVERAL_MONTHS, ((ModelGrade) gr).getUsageTime());
    }

    @Test
    public void testIgnoreSerpGradeIfMarketGradePresents() {
        final String oldText = "some old text";
        final String oldPro = "old pro";
        final String oldContra = "old contra";
        final String oldSource = null;

        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setModState(ModState.APPROVED);
        mockShopGradeSourceAndContents(grade, oldText, oldPro, oldContra, oldSource);
        long gradeId = gradeCreator.createGrade(grade);

        mockShopGradeSourceAndContents(grade, "new text", "new pro", "new contra", GradeSource.SERP.value());
        long newGradeId = gradeCreator.createGrade(grade);

        AbstractGrade gr = gradeService.getGrade(newGradeId);
        assertEquals(gradeId, gr.getId().longValue());
        assertEquals(oldSource, gr.getSource());
        assertEquals(oldText, gr.getText());
        assertEquals(oldPro, gr.getPro());
        assertEquals(oldContra, gr.getContra());
    }

    @Test
    public void testIgnoreCabinetGradeIfMarketGradePresents() {
        final String oldText = "some old text";
        final String oldPro = "old pro";
        final String oldContra = "old contra";
        final String oldSource = null;

        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        ModState modState = ModState.APPROVED;
        grade.setModState(modState);
        mockShopGradeSourceAndContents(grade, oldText, oldPro, oldContra, oldSource);
        long gradeId = gradeCreator.createGrade(grade);

        mockShopGradeSourceAndContents(grade, "new text", "new pro", "new contra", GradeSource.CABINET.value());
        long cabinetGradeId = gradeCreator.createGrade(grade);

        AbstractGrade cabinetGradeAttempt = gradeService.getGrade(cabinetGradeId);
        assertEquals(gradeId, cabinetGradeAttempt.getId().longValue());
        assertEquals(oldSource, cabinetGradeAttempt.getSource());
        assertEquals(oldText, cabinetGradeAttempt.getText());
        assertEquals(oldPro, cabinetGradeAttempt.getPro());
        assertEquals(oldContra, cabinetGradeAttempt.getContra());
    }

    /**
     * Создаем анонимный отзыв на магазин
     * Создаем не анонимный отзыв на тот же магазин
     * Логинимся в сессию из первого отзыва автором из второго отзыва
     * <p>
     * Проверяем, что
     * - анонимый отзыв теперь не анонимный
     * - единственный последний отзыв у автора тот, который последний по дате
     */
    @Test
    public void testBindGrades() {
        String yandexUidToBind = "1111111111111111142";

        //создаем анонимный
        AbstractGrade grade1 = GradeCreator.constructShopGradeRnd();
        grade1.setAuthorUid(null);
        grade1.setModState(ModState.APPROVED);
        long gradeId1 = gradeCreator.createGradeUnlogin(grade1, yandexUidToBind);

        AbstractGrade aGrade1 = gradeService.getGrade(gradeId1);
        assertEquals(ModState.APPROVED, aGrade1.getModState()); // принятый модератором
        assertEquals(GradeState.LAST, aGrade1.getState());   //последний
        assertNull(aGrade1.getAuthorUid()); // анонимный


        // создаем не анонимный
        AbstractGrade grade2 = GradeCreator.constructShopGradeRnd();
        grade2.setResourceId(grade1.getResourceId());
        grade2.setModState(ModState.APPROVED);
        long gradeId2 = gradeCreator.createGrade(grade2);
        Long authorId = grade2.getAuthorUid();

        AbstractGrade aGrade2 = gradeService.getGrade(gradeId2);
        assertEquals(ModState.APPROVED, aGrade2.getModState()); // принятый модератором
        assertEquals(GradeState.LAST, aGrade2.getState()); // последний
        assertEquals(authorId, aGrade2.getAuthorUid()); // не анонимный

        // логинимся в первую сессию
        gradeService.bindGrades2User(grade2.getAuthorUid(), yandexUidToBind);

        //анонимный
        aGrade1 = gradeService.getGrade(gradeId1);
        assertEquals(GradeState.PREVIOUS, aGrade1.getState()); // уже не последний
        assertEquals(authorId, aGrade1.getAuthorUid()); // уже не анонимный

        //не анонимный
        aGrade2 = gradeService.getGrade(gradeId2);
        assertEquals(GradeState.LAST, aGrade2.getState()); // все еще последний
        assertEquals(authorId, aGrade2.getAuthorUid()); // не анонимный
    }


    //----------------------------------
    // Helpers
    //----------------------------------

    private void setModelOrderId(long orderId, long gradeId) {
        pgJdbcTemplate.update("update grade.grade_model set model_order_id = ? where grade_id = ?", orderId, gradeId);
    }

    private static ModState getModStateByAnonimity(Anonymity anonymous) {
        return anonymous == Anonymity.HIDE_ALL ? ModState.AUTOMATICALLY_REJECTED : ModState.UNMODERATED;
    }

    private Long getModelOrderId(long gradeId) {
        return pgJdbcTemplate.queryForObject("select model_order_id from grade_model where grade_id = ?", Long.class, gradeId);
    }

    private void mockShopGradeSourceAndContents(ShopGrade grade,
                                                String text,
                                                String pro,
                                                String contra,
                                                String source) {
        grade.setSource(source);
        grade.setText(text);
        grade.setPro(pro);
        grade.setContra(contra);
    }
}

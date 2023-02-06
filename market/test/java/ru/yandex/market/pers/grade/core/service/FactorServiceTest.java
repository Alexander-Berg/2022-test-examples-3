package ru.yandex.market.pers.grade.core.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.FactorCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.ugc.FactorService;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.grade.core.ugc.model.RadioFactorValue;

/**
 * @author grigor-vlad
 * 17.11.2021
 */
public class FactorServiceTest extends MockedTest {

    private static final long FASHION_CATEGORY_ID = 1L;

    @Autowired
    private FactorService factorService;

    @Autowired
    private FactorCreator factorCreator;

    @Before
    public void clearNonShopFactors() {
        pgJdbcTemplate.update("delete from grade_factor where category_id is not null");
    }

    @Test
    public void testGradeFactorValueInputValidation() {
        Long radioFactorId1 = factorCreator.addRadioFactorAndReturnId("Вещь соответствует заявленному размеру?",
            FASHION_CATEGORY_ID,
            10,
            true,
            List.of(
                new RadioFactorValue(0, 0, "Да"),
                new RadioFactorValue(1, 20, "Нет, большемерит"),
                new RadioFactorValue(2, 20, "Нет, большемерит")
            ));
        Long radioFactorId2 = factorCreator.addRadioFactorAndReturnId("Будете рекомендовать?",
            FASHION_CATEGORY_ID,
            10,
            true,
            List.of(
                new RadioFactorValue(0, 0, "Да"),
                new RadioFactorValue(1, 10, "Нет")
            ));
        Long starFactorId = factorCreator.addFactorAndReturnId("Качество", FASHION_CATEGORY_ID, 20);

        factorService.validateFactorValues(List.of(
            new GradeFactorValue(radioFactorId1, 1),
            new GradeFactorValue(radioFactorId2, 0),
            new GradeFactorValue(starFactorId, 4)
        ));
    }

    @Test
    public void testGradeFactorValueInputValidationForRadioType() {
        Long radioFactorId = null;
        try {
            radioFactorId = factorCreator.addRadioFactorAndReturnId( "Будете рекомендовать?",
                FASHION_CATEGORY_ID,
                10,
                true,
                List.of(
                    new RadioFactorValue(0, 0, "Да"),
                    new RadioFactorValue(6, 20, "Нет")
                ));
            factorService.validateFactorValues(List.of(new GradeFactorValue(radioFactorId, 2)));
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported value: 2 for factor with id: " + radioFactorId, ex.getMessage());
        }
    }

    @Test
    public void testGradeFactorValueInputValidationForStarsType() {
        Long factorId = null;
        try {
            factorId = factorCreator.addFactorAndReturnId("Качество", FASHION_CATEGORY_ID, 0);
            factorService.validateFactorValues(List.of(new GradeFactorValue(factorId, 6)));
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported value: 6 for factor with id: " + factorId, ex.getMessage());
        }
    }

    @Test
    public void testGradeFactorForShop() {
        try {
            //factorId = 1 is shop factor
            factorService.validateFactorValues(List.of(new GradeFactorValue(1, 6)));
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Unsupported value: 6 for factor with id: 1", ex.getMessage());
        }
    }

    @Test
    public void testNotExistedFactorId() {
        try {
            factorService.validateFactorValues(List.of(new GradeFactorValue(1000, 2),
                new GradeFactorValue(2000, 0)));
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().startsWith("Unknown factor ids"));
        }

    }
}

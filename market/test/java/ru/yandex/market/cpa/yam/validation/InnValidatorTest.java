package ru.yandex.market.cpa.yam.validation;

import org.junit.Test;

import ru.yandex.market.api.cpa.yam.validation.InnValidator;

import static org.junit.Assert.assertTrue;

public class InnValidatorTest {

    @Test
    public void checkInnPositive() throws Exception {
        //Физлица + ИП - 12 цифр
        assertTrue(InnValidator.checkInn("437730231589"));
        assertTrue(InnValidator.checkInn("834476450203"));
        //Юрлица - 10 цифр
        assertTrue(InnValidator.checkInn("2877008453"));
        assertTrue(InnValidator.checkInn("1858806078"));
    }
}

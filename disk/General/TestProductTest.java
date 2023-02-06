package ru.yandex.chemodan.eventlog.events;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class TestProductTest {
    @Test
    public void testYandexEge() {
        Assert.equals("product_id=yandex_ege" +
                "\tproduct_period=months: 12" +
                "\tproduct_is_free=True" +
                "\tproduct_name_ru=Подготовка к ЕГЭ" +
                "\tproduct_name_ua=Підготовка до ЕГЭ" +
                "\tproduct_name_en=Preparation for university entrance exams" +
                "\tproduct_name_tr=Üniversite sınavına hazırlık",
                TestProduct.FREE_YANDEX_EGE.toString()
        );
    }
}

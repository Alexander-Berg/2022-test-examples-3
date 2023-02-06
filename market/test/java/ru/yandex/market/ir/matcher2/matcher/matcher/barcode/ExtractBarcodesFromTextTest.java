package ru.yandex.market.ir.matcher2.matcher.matcher.barcode;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.barcode.ExtractBarcodesFromText;

public class ExtractBarcodesFromTextTest {

    @Test
    public void test() {
        //12 символов
        Collection<String> barcodes = ExtractBarcodesFromText.extract("Телефон 740617302516");
        Assert.assertEquals(barcodes.size(), 1);
        //11 символов
        barcodes = ExtractBarcodesFromText.extract("Телефон 74061730251");
        Assert.assertEquals(barcodes.size(), 0);
        //24 символа
        barcodes = ExtractBarcodesFromText.extract("Телефон 740617302516740617302516");
        Assert.assertEquals(barcodes.size(), 0);
        //12 символов с буквой в конце
        barcodes = ExtractBarcodesFromText.extract("Телефон 740617302516f");
        Assert.assertEquals(barcodes.size(), 1);
        //два одинаковых 12 символьных баркода
        barcodes = ExtractBarcodesFromText.extract("Телефон 740617302516 740617302516");
        Assert.assertEquals(barcodes.size(), 1);
        //ISBN 12 символов + баркод 12 символов, одинаковые
        barcodes = ExtractBarcodesFromText.extract("Телефон 7-4-06-17302516 740617302516");
        Assert.assertEquals(barcodes.size(), 1);
        //Два разных ISBN
        barcodes = ExtractBarcodesFromText.extract("Телефон 7-4-06-17302516 740-617-302517");
        Assert.assertEquals(barcodes.size(), 2);
        //Два разных баркода
        barcodes = ExtractBarcodesFromText.extract("Телефон 740617302516 740617302517");
        Assert.assertEquals(barcodes.size(), 2);
    }
}

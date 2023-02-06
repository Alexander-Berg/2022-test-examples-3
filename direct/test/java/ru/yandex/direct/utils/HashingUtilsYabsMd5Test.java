package ru.yandex.direct.utils;

import java.math.BigInteger;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class HashingUtilsYabsMd5Test {

    @Parameterized.Parameter
    public String dataMd5;

    @Parameterized.Parameter(value = 1)
    public String data;

    @Parameterized.Parameters(name = "Проверка значения хеша для фразы: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                // empty
                {"7203772011789518145", ""},
                // min
                {"3276236132", "Отделка +в 2 +к 109 метра -кв -квартиры -новостройки"},
                {"4698888750", "где купить масло кедровый эффект ~0"},
                {"37694718977", "аккумулятор D7000 -nikon -никон"},
                // max
                {"18446744062736283516", "постельный комплект ликвидация смешарики"},
                {"18446744045454743896", "продажа Мониторы Жк E2250swa"},
                {"18446744043024212601", "построй свою историю lego"},
                // rand
                {"2662553531132949541", "!сертификат !соответствия !на !смесь !напольную !вебер !ветонит ~0"},
                {"1862605101838126775", "Чарлтон Кольцо"},
                {"17199837872897126050", "ПИЛКИ ШКОЛА МАНИКЮРА -отзывы -спб"},
                {"11374162228141751238", "ремонт таунхауса 950 м2"},
                {"15113752260563908347", "лицензия +на обращение +с отходами в свердловская область"},
                {"14717115094686060904", "Svf1521 F 1rb Black"},
                {"790628461008127939", "регулятор давления топлива atego"},
                {"16300543168669915921", "купить больших магазины спб"},
                {"10794994837375423242",
                        "Устранение поломок кофемашин Saeco -philips -голицыно -зеленограде -москве -нахабино"},
                {"12664695874568397714", "[!novum !lilyum !hotel] ~0"},
                {"4755904239303458758", "магазин онлайн дешевых часов"},
                {"12336293621284161543", "шифер плоский +в минске"},
                {"10218726801850510963", "[20BD011A0AYYAND0]"},
                {"1021590384744327002", "как закрыть ип нотариус -без -заверять"},
                {"13262534870203763986", "Ремонт тнвд дизельных двигателей !GL Москва -!mercedes"},
                {"1484406171336722867", "продажа ковриков в багажник Кия"},
                {"6994941666870338769", "заказать раков +на дом +в москве -вареных -живых"},
                {"14143363170709617317", "TOYOTA 4243126160"},
                {"8043861203209855347", "объемные поделки для дачи"},
                // near max value long - below
                {"9223372035849224535", "афиша махачкала на неделю -кинотеатр"},
                {"9223372034208109569", "+Кухни +на +заказ +Бордо +Производитель"},
                {"9223372028433960109", "Intel E3-1220V5 Skylake"},
                // near max value long - above
                {"9223372039544831299", "console server менеджмент"},
                {"9223372049298202476", "электрический чайник scarlett sc ek18p26 купить"},
                {"9223372049635447894", "Ноутбук Satellite P845 чистка вентилятора"},
        });
    }

    @Test
    public void test() {
        BigInteger expectedHash = new BigInteger(dataMd5);
        BigInteger actual = HashingUtils.getYabsMd5HalfHashUtf8(data);
        assertThat(actual, comparesEqualTo(expectedHash));
    }
}

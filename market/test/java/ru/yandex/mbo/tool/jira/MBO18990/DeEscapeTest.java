package ru.yandex.mbo.tool.jira.MBO18990;

import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.mbo.tool.jira.MBO18990.DeEscape.deescapeString;

@SuppressWarnings("LineLength")
public class DeEscapeTest {

    @Test
    public void deescapeStringTest01() {
        Assert.assertEquals("Ololo", deescapeString("Ololo"));
    }

    @Test
    public void deescapeStringTest02() {
        Assert.assertEquals("Ol&#32lo", deescapeString("Ol&#32lo"));
    }

    @Test
    public void deescapeStringTest03() {
        Assert.assertEquals("Ololo", deescapeString("Ol&#111;lo"));
    }

    @Test
    public void deescapeStringTest04() {
        Assert.assertEquals("Ololo", deescapeString("Ol&amp;#111;lo"));
    }

    @Test
    public void deescapeStringTest05() {
        Assert.assertEquals("Ol&amp#111;lo", deescapeString("Ol&amp#111;lo"));
    }

    @Test
    public void deescapeStringTest06() {
        Assert.assertEquals("Capital o with umlaut is &Ouml", deescapeString("Capital o with umlaut is &Ouml"));
    }

    @Test
    public void deescapeStringTest07() {
        Assert.assertEquals("Capital o with umlaut is Ö", deescapeString("Capital o with umlaut is &Ouml;"));
    }

    @Test
    public void deescapeStringTest08() {
        Assert.assertEquals("Capital o with umlaut is &ampOuml;", deescapeString("Capital o with umlaut is &ampOuml;"));
    }

    @Test
    public void deescapeStringTest09() {
        Assert.assertEquals("Capital o with umlaut is Ö", deescapeString("Capital o with umlaut is &amp;Ouml;"));
    }

    @Test
    public void deescapeStringTest10() {
        Assert.assertEquals("Capital o with umlaut is &Ouml", deescapeString("Capital o with umlaut is &amp;Ouml"));
    }

    @Test
    public void deescapeStringTest11() {
        Assert.assertEquals("&Ouml is Capital o with umlaut", deescapeString("&Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest12() {
        Assert.assertEquals("Ö is Capital o with umlaut", deescapeString("&Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest13() {
        Assert.assertEquals("&ampOuml; is Capital o with umlaut", deescapeString("&ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest14() {
        Assert.assertEquals("Ö is Capital o with umlaut", deescapeString("&amp;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest15() {
        Assert.assertEquals("&Ouml is Capital o with umlaut", deescapeString("&amp;Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest16() {
        Assert.assertEquals("&Ouml is Capital o with umlaut", deescapeString("&amp;amp;amp;Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest17() {
        Assert.assertEquals("&ampOuml is Capital o with umlaut", deescapeString("&amp;amp;ampOuml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest18() {
        Assert.assertEquals("&ampamp;Ouml is Capital o with umlaut", deescapeString("&amp;ampamp;Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest19() {
        Assert.assertEquals("&ampampOuml is Capital o with umlaut", deescapeString("&amp;ampampOuml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest20() {
        Assert.assertEquals("&ampamp;amp;Ouml is Capital o with umlaut", deescapeString("&ampamp;amp;Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest21() {
        Assert.assertEquals("&ampamp;ampOuml is Capital o with umlaut", deescapeString("&ampamp;ampOuml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest22() {
        Assert.assertEquals("&ampampamp;Ouml is Capital o with umlaut", deescapeString("&ampampamp;Ouml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest23() {
        Assert.assertEquals("&ampampampOuml is Capital o with umlaut", deescapeString("&ampampampOuml is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest24() {
        Assert.assertEquals("Ö is Capital o with umlaut", deescapeString("&amp;amp;amp;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest25() {
        Assert.assertEquals("&ampOuml; is Capital o with umlaut", deescapeString("&amp;amp;ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest26() {
        Assert.assertEquals("&ampamp;Ouml; is Capital o with umlaut", deescapeString("&amp;ampamp;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest27() {
        Assert.assertEquals("&ampampOuml; is Capital o with umlaut", deescapeString("&amp;ampampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest28() {
        Assert.assertEquals("&ampamp;amp;Ouml; is Capital o with umlaut", deescapeString("&ampamp;amp;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest29() {
        Assert.assertEquals("&ampamp;ampOuml; is Capital o with umlaut", deescapeString("&ampamp;ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest30() {
        Assert.assertEquals("&ampampamp;Ouml; is Capital o with umlaut", deescapeString("&ampampamp;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest31() {
        Assert.assertEquals("&ampampampOuml; is Capital o with umlaut", deescapeString("&ampampampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest32() {
        Assert.assertEquals("&ampamp#38;ampOuml; is Capital o with umlaut", deescapeString("&ampamp#38;ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest33() {
        Assert.assertEquals("&ampamp#38ampOuml; is Capital o with umlaut", deescapeString("&ampamp#38ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest34() {
        Assert.assertEquals("&amp#38#38;Ouml; is Capital o with umlaut", deescapeString("&amp#38#38;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest35() {
        Assert.assertEquals("&amp#38#38Ouml; is Capital o with umlaut", deescapeString("&amp#38#38Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest36() {
        Assert.assertEquals("&amp#38;#38;Ouml; is Capital o with umlaut", deescapeString("&amp#38;#38;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest37() {
        Assert.assertEquals("&amp#38;#38Ouml; is Capital o with umlaut", deescapeString("&amp#38;#38Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest38() {
        Assert.assertEquals("&amp#38;&amp#35;38Ouml; is Capital o with umlaut", deescapeString("&amp#38;&amp#35;38Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest39() {
        Assert.assertEquals("https://www.example.com/foo/?bar=baz&param=42&quux", deescapeString("https://www.example.com/foo/?bar=baz&param=42&quux"));
    }

    @Test
    public void deescapeStringTest40() {
        Assert.assertEquals("http://url.test.com/service?param1=o&amp;param2=ololo", deescapeString("http://url.test.com/service?param1=o&amp;param2=ololo"));
    }

    @Test
    public void deescapeStringTest41() {
        Assert.assertEquals("Ololohttp://url.test.com/service?param1=o&param2=ololo", deescapeString("Ololohttp://url.test.com/service?param1=o&param2=ololo"));
    }

    @Test
    public void deescapeStringTest42() {
        Assert.assertEquals("Ololo&http://url.test.com/service?param1=o&param2=ololo", deescapeString("Ololo&amp;http://url.test.com/service?param1=o&param2=ololo"));
    }

    @Test
    public void deescapeStringTest43() {
        Assert.assertEquals("Ololo&http://url.test.com/service?param1=o&param2=ololo &olo", deescapeString("Ololo&amp;http://url.test.com/service?param1=o&param2=ololo &amp;olo"));
    }

    @Test
    public void deescapeStringTest44() {
        Assert.assertEquals("&ampamp#x26;ampOuml; is Capital o with umlaut", deescapeString("&ampamp#x26;ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest45() {
        Assert.assertEquals("&ampamp;#x26;ampOuml; is Capital o with umlaut", deescapeString("&ampamp;#x26;ampOuml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest46() {
        Assert.assertEquals("&amp#x26#38;Ouml; is Capital o with umlaut", deescapeString("&amp#x26#38;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest47() {
        Assert.assertEquals("&amp#38#x26Ouml; is Capital o with umlaut", deescapeString("&amp#38#x26Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest48() {
        Assert.assertEquals("&amp#x26;#x26;Ouml; is Capital o with umlaut", deescapeString("&amp#x26;#x26;Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest49() {
        Assert.assertEquals("&amp#x26;#x26Ouml; is Capital o with umlaut", deescapeString("&amp#x26;#x26Ouml; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest50() {
        Assert.assertEquals("Ö is Capital o with umlaut", deescapeString("&#x00D6; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest51() {
        Assert.assertEquals("&amp#38;&amp#x23;38&#x23x00D6 is Capital o with umlaut", deescapeString("&amp#38;&amp#x23;38&#x23x00D6 is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest52() {
        Assert.assertEquals("&amp#38;&amp#x23;38#x00D6 is Capital o with umlaut", deescapeString("&amp#38;&amp#x23;38#x00D6 is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest53() {
        Assert.assertEquals("&Ö is Capital o with umlaut", deescapeString("&amp;&#x00D6; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest54() {
        Assert.assertEquals("&Ö is Capital o with umlaut", deescapeString("&amp;&#x00d6; is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest55() {
        Assert.assertEquals("&&#x00D6 is Capital o with umlaut", deescapeString("&amp;&#x00D6 is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest56() {
        Assert.assertEquals("&&#x00d6 is Capital o with umlaut", deescapeString("&amp;&#x00d6 is Capital o with umlaut"));
    }

    @Test
    public void deescapeStringTest57() {
        Assert.assertEquals(
            "Этот курс для тех, кто: · хочет успешно сдать ЕГЭ по английскому языку; " +
                "· хочет видеть прогресс в процессе подготовки к ЕГЭ по английскому языку; " +
                "· хочет обрести уверенность в своих силах и знаниях. Пособие содержит: " +
                "· 15 тематических тестов в формате ЕГЭ 2016 с подробными. " +
                "· списками слов по тематике каждого теста. " +
                "· Инструкции по выполнению заданий. " +
                "· Официальные критерии оценивания письменных и устных заданий 2016. " +
                "· Подробный алгоритм выполнения Заданий 39 и 40 с комментариями. " +
                "· Подробный алгоритм выполнения Заданий 3 и 4 устной части с комментариями. " +
                "Формулировка заданий точно соответствует КИМ ЕГЭ 2016. Пособие можно использовать как для " +
                "аудиторной, так и для самостоятельной работы. Ответы ко всем тестам, аудиозаписи и тексты к ним, " +
                "рекомендации для выполнения письменных и устных заданий к каждому тесту вы можете скачать " +
                "бесплатно на сайте производителя.",
            deescapeString("Этот курс для тех, кто: &#183; хочет успешно сдать ЕГЭ по английскому языку; " +
                "&#183; хочет видеть прогресс в процессе подготовки к ЕГЭ по английскому языку; " +
                "&#183; хочет обрести уверенность в своих силах и знаниях. Пособие содержит: " +
                "&#183; 15 тематических тестов в формате ЕГЭ 2016 с подробными. " +
                "&#183; списками слов по тематике каждого теста. " +
                "&#183; Инструкции по выполнению заданий. " +
                "&#183; Официальные критерии оценивания письменных и устных заданий 2016. " +
                "&#183; Подробный алгоритм выполнения Заданий 39 и 40 с комментариями. " +
                "&#183; Подробный алгоритм выполнения Заданий 3 и 4 устной части с комментариями. " +
                "Формулировка заданий точно соответствует КИМ ЕГЭ 2016. Пособие можно использовать как для " +
                "аудиторной, так и для самостоятельной работы. Ответы ко всем тестам, аудиозаписи и тексты к ним, " +
                "рекомендации для выполнения письменных и устных заданий к каждому тесту вы можете скачать " +
                "бесплатно на сайте производителя."));
    }

    @Test
    public void deescapeStringTest58() {
        Assert.assertEquals(
            "Этот курс для тех, кто: · хочет успешно сдать ЕГЭ по английскому языку; " +
                "· хочет видеть прогресс в процессе подготовки к ЕГЭ по английскому языку; " +
                "· хочет обрести уверенность в своих силах и знаниях. Пособие содержит: " +
                "· 15 тематических тестов в формате ЕГЭ 2016 с подробными. " +
                "· списками слов по тематике каждого теста. " +
                "· Инструкции по выполнению заданий. " +
                "· Официальные критерии оценивания письменных и устных заданий 2016. " +
                "· Подробный алгоритм выполнения Заданий 39 и 40 с комментариями. " +
                "· Подробный алгоритм выполнения Заданий 3 и 4 устной части с комментариями. " +
                "Формулировка заданий точно соответствует КИМ ЕГЭ 2016. Пособие можно использовать как для " +
                "аудиторной, так и для самостоятельной работы. Ответы ко всем тестам, аудиозаписи и тексты к ним, " +
                "рекомендации для выполнения письменных и устных заданий к каждому тесту вы можете скачать " +
                "бесплатно на сайте производителя.",
            deescapeString("Этот курс для тех, кто: &#183; хочет успешно сдать ЕГЭ по английскому языку; " +
                "&#38;#183; хочет видеть прогресс в процессе подготовки к ЕГЭ по английскому языку; " +
                "&#38;#183; хочет обрести уверенность в своих силах и знаниях. Пособие содержит: " +
                "&#38;#183; 15 тематических тестов в формате ЕГЭ 2016 с подробными. " +
                "&#38;#183; списками слов по тематике каждого теста. " +
                "&#38;#183; Инструкции по выполнению заданий. " +
                "&#38;#183; Официальные критерии оценивания письменных и устных заданий 2016. " +
                "&#38;#183; Подробный алгоритм выполнения Заданий 39 и 40 с комментариями. " +
                "&#183; Подробный алгоритм выполнения Заданий 3 и 4 устной части с комментариями. " +
                "Формулировка заданий точно соответствует КИМ ЕГЭ 2016. Пособие можно использовать как для " +
                "аудиторной, так и для самостоятельной работы. Ответы ко всем тестам, аудиозаписи и тексты к ним, " +
                "рекомендации для выполнения письменных и устных заданий к каждому тесту вы можете скачать " +
                "бесплатно на сайте производителя."));
    }

    @Test
    public void deescapeStringTest59() {
        Assert.assertNull(deescapeString(null));
    }

    @Test
    public void deescapeStringTest60() {
        Assert.assertEquals("", deescapeString(""));
    }

}

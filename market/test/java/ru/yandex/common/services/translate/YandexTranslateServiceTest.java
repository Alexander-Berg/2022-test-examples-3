package ru.yandex.common.services.translate;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 22/10/15
 */
public class YandexTranslateServiceTest {

    private final YandexTranslateService translateService = new YandexTranslateService("market");

    @Ignore
    @Test
    public void testTranslate() {
        testRuEn("Яндекс", "Yandex");
        testRuEn("лес", "forest");
        testRuEn("дом", "house");
        testRuEn("афвыафыафыаф", "avyavaharyam");
    }

    private void testRuEn(String string, String expectedTranslation) {
        Assert.assertEquals(expectedTranslation, translateService.translate(string, Language.RUS, Language.ENG));
    }

}
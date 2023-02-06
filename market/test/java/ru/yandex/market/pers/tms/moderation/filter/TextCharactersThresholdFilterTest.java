package ru.yandex.market.pers.tms.moderation.filter;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author imelnikov
 */
public class TextCharactersThresholdFilterTest {
    @Test
    public void testCaps() {
        TextCharactersThresholdFilter f = TextCharactersThresholdFilter.caps();

        assertTrue(f.match(getTestObject("text")).getMatched().isEmpty());
        assertFalse(f.match(getTestObject("МНЕ ДОСТАВИЛИ ЗАКАЗ ДОМОЙ АБСОЛЮТНО БЕСПЛАТНО")).getMatched().isEmpty());
        assertTrue(f.match(getTestObject("ААА ААА ААА ааа ааа ааа ааа ааа ааа ааа а")).getMatched().isEmpty());
        assertFalse(f.match(getTestObject("ААА ААА ААА ааа ааа ааа ааа ааа ааа аа")).getMatched().isEmpty());
    }

    @Test
    public void testLatin() {
        TextCharactersThresholdFilter f = TextCharactersThresholdFilter.latin();

        assertTrue(f.match(getTestObject("Вполне нормальный текст")).getMatched().isEmpty());
        assertTrue(f.match(getTestObject("Текст, где есть nemnogo латиницы")).getMatched().isEmpty());
        assertTrue("=50% latin", f.match(getTestObject("А теперь na grani")).getMatched().isEmpty());
        assertFalse("=50% latin + one special", f.match(getTestObject("А теперь - na grani")).getMatched().isEmpty());
        assertTrue(">51% cyrillic", f.match(getTestObject("А теперь ы na grani")).getMatched().isEmpty());
        assertFalse(">51% latin", f.match(getTestObject("А теперь za vdv dvd")).getMatched().isEmpty());
        assertFalse(f.match(getTestObject("А теперь vypiem za luybovvv")).getMatched().isEmpty());
        assertFalse(f.match(getTestObject("Что за ??? 최고의 검토에서 행복한 사용자")).getMatched().isEmpty());
    }

    @NotNull
    private List<Object4Moderation> getTestObject(String s) {
        return singletonList(Object4Moderation.forModeration(2, ModState.READY, s));
    }
}

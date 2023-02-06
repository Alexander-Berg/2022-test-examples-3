package ru.yandex.market.crm.campaign.services.sending;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zloddey
 */
class PreviewValuesTest {
    private final PreviewValues previewValues = new PreviewValues();

    /**
     * Имена всех переменных должны быть в нижнем регистре, чтобы они корректно находились при подстановке
     */
    @Test
    public void allNamesMustBeInLowerCase() {
        Set<String> names = previewValues.createPreviewValues().keySet();
        Set<String> lowerCaseNames = names.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        assertEquals(lowerCaseNames, names);
    }
}

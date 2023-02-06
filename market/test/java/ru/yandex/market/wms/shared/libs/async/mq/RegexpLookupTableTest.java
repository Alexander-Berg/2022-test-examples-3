package ru.yandex.market.wms.shared.libs.async.mq;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegexpLookupTableTest {
    /**
     * Проверяем, что для каждого ключа возвращается соответствующее ему значение
     */
    @Test
    public void ordered() {
        RegexpLookupTable<String> table = new RegexpLookupTable<>();
        table.addEntry("a", "1");
        table.addEntry("b", "2");
        table.addEntry("c", "3");
        Assertions.assertEquals(Optional.of("1"), table.find("a"));
        Assertions.assertEquals(Optional.of("2"), table.find("b"));
        Assertions.assertEquals(Optional.of("3"), table.find("c"));
    }

    /**
     * Проверяем, что возвращается первое совпавшее значение, даже если подходят несколько.
     */
    @Test
    public void returnsValuesAccordingToPriority() {
        RegexpLookupTable<String> table = new RegexpLookupTable<>();
        table.addEntry("a", "1");
        table.addEntry("a", "2");
        Assertions.assertEquals(Optional.of("1"), table.find("a"));
    }

    /**
     * Проверяем, что для несовпадающего значения возвращается пустой Optional.
     */
    @Test
    public void emptyForNonMatching() {
        RegexpLookupTable<String> table = new RegexpLookupTable<>();
        table.addEntry("a", "1");
        table.addEntry("b", "2");
        Assertions.assertEquals(Optional.empty(), table.find("c"));
    }

    /**
     * Проверяем, что пустая таблица выполняет поиск без исключений.
     */
    @Test
    public void emptyTable() {
        RegexpLookupTable<String> table = new RegexpLookupTable<>();
        Assertions.assertEquals(Optional.empty(), table.find("a"));
    }
}

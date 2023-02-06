package ru.yandex.market.core.feed.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit-тесты для {@link FeedFileType}.
 *
 * @author Vladislav Bauer
 */
class FeedFileTypeTest {

    /**
     * Проверить поиск типа файла фида по имени.
     * Тип файла действительно можно найти.
     */
    @Test
    void testFindByNamePositive() {
        assertAll(
                () -> assertThat(FeedFileType.values(), arrayWithSize(3)),
                () -> assertEquals(FeedFileType.YML, FeedFileType.findByName("yml")),
                () -> assertEquals(FeedFileType.XLS, FeedFileType.findByName("XLS")),
                () -> assertEquals(FeedFileType.CSV, FeedFileType.findByName("CsV"))
        );
    }

    /**
     * Проверить что по пустому имени нельзя найти тип файла фида.
     */
    @Test
    void testFindByNameNegative() {
        assertAll(
                () -> assertNull(FeedFileType.findByName(null)),
                () -> assertNull(FeedFileType.findByName("")),
                () -> assertNull(FeedFileType.findByName(" "))
        );
    }

    /**
     * Проверить уникальность кодов в типах файла.
     */
    @Test
    void testCodeUniqueness() {
        final FeedFileType[] types = FeedFileType.values();
        final Set<Integer> codes = Arrays.stream(types)
                .map(FeedFileType::getCode)
                .collect(Collectors.toSet());

        assertEquals(types.length, codes.size());
    }

}

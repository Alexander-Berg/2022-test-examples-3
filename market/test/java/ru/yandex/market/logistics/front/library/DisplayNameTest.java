package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.DisplayName;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

class DisplayNameTest {

    @Test
    void testDisplayName() {

        Set<String> expectedTitles = new HashSet<>(Arrays.asList("имя", "active"));

        DetailData detailView = ViewUtils.getDetail(new MockDto(), Mode.VIEW, false);

        Set<String> actualFieldTitles = detailView.getMeta()
            .getFields()
            .stream()
            .map(DetailField::getTitle)
            .collect(Collectors.toSet());

        Assertions.assertEquals(expectedTitles, actualFieldTitles, "field order must be equal");
    }

    private static class MockDto {

        @DisplayName("айди")
        private Long id = 1L;

        @DisplayName("имя")
        private String name = "name";

        private boolean active = true;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }
    }
}

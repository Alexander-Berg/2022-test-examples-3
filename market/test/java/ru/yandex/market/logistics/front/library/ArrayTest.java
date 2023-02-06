package ru.yandex.market.logistics.front.library;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.Array;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.Type;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayTest {

    @Test
    void arrayColumn() {
        GridData gridData = ViewUtils.getGridView(Collections.singletonList(new TestDtoWithArrayColumn()), Mode.VIEW);
        Optional<Type> actualColumnArrayType = gridData.getMeta().getColumns().stream()
            .filter(column -> "partners".equals(column.getName()))
            .findFirst().map(GridColumn::getArrayType);
        assertThat(actualColumnArrayType)
            .hasValueSatisfying(value -> assertThat(value)
                .isEqualTo(Type.NUMBER)
            );
    }

    @Test
    void withoutArrayColumn() {
        GridData gridData =
            ViewUtils.getGridView(Collections.singletonList(new TestDtoWithoutArrayColumn()), Mode.VIEW);
        Optional<Type> actualColumnArrayType = gridData.getMeta().getColumns().stream()
            .filter(column -> "partners".equals(column.getName()))
            .findFirst().map(GridColumn::getArrayType);
        assertThat(actualColumnArrayType).isEmpty();
    }

    private static class TestDto {
        private final long id = 1;

        public long getId() {
            return id;
        }
    }

    private static class TestDtoWithArrayColumn extends TestDto {
        @Array(type = Integer.class)
        private Set<Integer> partners;

        public Set<Integer> getPartners() {
            return partners;
        }
    }

    private static class TestDtoWithoutArrayColumn extends TestDto {
        private Set<Integer> partners;

        public Set<Integer> getPartners() {
            return partners;
        }
    }
}

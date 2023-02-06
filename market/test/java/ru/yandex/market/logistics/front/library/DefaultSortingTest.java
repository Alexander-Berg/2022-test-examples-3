package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.DefaultSorting;
import ru.yandex.market.logistics.front.library.dto.DefaultSort;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.SortDirection;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

class DefaultSortingTest {

    @Test
    void sortTest() {
        List<MockDto> mockDtos = Arrays.asList(new MockDto(1L, "name1"), new MockDto(2L, "name2"));
        GridData gridView = ViewUtils.getGridView(mockDtos, (long) mockDtos.size(), Mode.VIEW);
        SoftAssertions.assertSoftly(softAssertions -> {
                softAssertions.assertThat(gridView.getMeta()
                    .getDefaultSort())
                    .extracting(DefaultSort::getFieldName)
                    .isEqualTo("name");

                softAssertions.assertThat(gridView.getMeta()
                    .getDefaultSort())
                    .extracting(DefaultSort::getType)
                    .isEqualTo(SortDirection.ASC);
            }
        );
    }

    @DefaultSorting(fieldName = "name")
    private static class MockDto {
        private Long id;

        private String name;

        MockDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

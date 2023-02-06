package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewTest {
    private static final Long TOTAL_COUNT = 1L;
    private static final MockDto FIRST = new MockDto(
        1L,
        "first",
        true,
        123,
        Arrays.asList(1L, 2L, 3L)
    );
    private static final MockDto SECOND = new MockDto(
        2L,
        "second",
        false,
        222,
        Arrays.asList(1L, 2L)
    );
    private static final List<MockDto> MOCK_DTO_LIST = Arrays.asList(FIRST, SECOND);

    @Test
    void gridTest() {
        List<Object> list = Arrays.asList(FIRST, SECOND);
        assertThat(MockDto.getGridData(MOCK_DTO_LIST, TOTAL_COUNT))
            .isEqualToComparingFieldByFieldRecursively(ViewUtils.getGridView(list, TOTAL_COUNT, Mode.VIEW));
    }

    @Test
    void gridIdError() {
        //передаем список объектов у который нет id (Integer), ожидаем исключение
        List<Object> list = Arrays.asList(123, 345);
        assertThatThrownBy(() -> ViewUtils.getGridView(list, 2L, Mode.VIEW))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void emptyGridView() {
        assertThat(ViewUtils.getGridView(Collections.emptyList(), 0L, Mode.VIEW))
            .isEqualToComparingFieldByFieldRecursively(new GridData(0L, Mode.VIEW));
    }

    @Test
    void modeNotProvided() {
        assertThatThrownBy(() -> ViewUtils.gridDataBuilder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("mode");
    }

    @Test
    void gridWithFilledMetaButEmptyItems() {
        assertThat(ViewUtils.gridDataBuilder().withMode(Mode.VIEW).withDtoClass(MockDto.class).build())
            .isEqualToComparingFieldByFieldRecursively(MockDto.getGridData(Collections.emptyList(), 0L));
    }

    @Test
    void gridWithFilledMetaAndItems() {
        List<MockDto> dtoList = Collections.singletonList(new MockDto(1L, "hello", true, 10, Collections.emptyList()));
        assertThat(ViewUtils.gridDataBuilder().withMode(Mode.VIEW).withDtoList(dtoList).build())
            .isEqualToComparingFieldByFieldRecursively(MockDto.getGridData(dtoList, 1L));
    }
}

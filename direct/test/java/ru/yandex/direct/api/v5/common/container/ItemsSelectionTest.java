package ru.yandex.direct.api.v5.common.container;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_OFFSET;

@ParametersAreNonnullByDefault
public class ItemsSelectionTest {

    private static final List<Long> IDS = Arrays.asList(1L, 2L, 3L, 4L, 5L);
    private static final Long LIMIT = 1L;
    private static final Long OFFSET = 1L;

    @Test
    public void defaultIdsTest() {
        ItemsSelection selection = new ItemsSelection();
        assertThat(selection.getIds()).isNull();
    }

    @Test
    public void defaultLimitTest() {
        ItemsSelection selection = new ItemsSelection();
        assertThat(selection.getLimit()).isEqualTo(DEFAULT_LIMIT);
    }

    @Test
    public void defaultOffsetTest() {
        ItemsSelection selection = new ItemsSelection();
        assertThat(selection.getOffset()).isEqualTo(DEFAULT_OFFSET);
    }

    @Test
    public void customIdsTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.setIds(IDS);
        assertThat(selection.getIds()).isEqualTo(IDS);
    }

    @Test
    public void customLimitTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.setLimit(LIMIT);
        assertThat(selection.getLimit()).isEqualTo(LIMIT);
    }

    @Test
    public void customOffsetTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.setOffset(OFFSET);
        assertThat(selection.getOffset()).isEqualTo(OFFSET);
    }

    @Test
    public void withCustomIdsTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.withIds(IDS);
        assertThat(selection.getIds()).isEqualTo(IDS);
    }

    @Test
    public void withCustomLimitTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.withLimit(LIMIT);
        assertThat(selection.getLimit()).isEqualTo(LIMIT);
    }

    @Test
    public void withCustomOffsetTest() {
        ItemsSelection selection = new ItemsSelection();
        selection.withOffset(OFFSET);
        assertThat(selection.getOffset()).isEqualTo(OFFSET);
    }
}

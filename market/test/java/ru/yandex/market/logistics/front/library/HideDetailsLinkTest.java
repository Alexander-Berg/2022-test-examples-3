package ru.yandex.market.logistics.front.library;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.HideDetailsLink;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class HideDetailsLinkTest {

    @Test
    void hideDetailsLinks() {
        GridData gridData = ViewUtils.getGridView(Collections.singletonList(new WithAnnotation()), Mode.VIEW);
        assertThat(gridData.getMeta().isHideDetailsLink())
            .isEqualTo(true);
    }

    @Test
    void showDetailsLinks() {
        GridData gridData = ViewUtils.getGridView(Collections.singletonList(new WithoutAnnotation()), Mode.VIEW);
        assertThat(gridData.getMeta().isHideDetailsLink())
            .isEqualTo(null);
    }

    private static class TestDto {
        private final long id = 1;

        public long getId() {
            return id;
        }
    }

    @HideDetailsLink
    private static class WithAnnotation extends TestDto {
    }

    private static class WithoutAnnotation extends TestDto {
    }
}

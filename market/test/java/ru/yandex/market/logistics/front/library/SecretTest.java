package ru.yandex.market.logistics.front.library;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.Secret;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.SecretObject;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SecretTest {
    @Test
    void secretDetailColumn() {
        DetailData detailData = ViewUtils.getDetail(new TestDto(), Mode.VIEW, false);
        Optional<SecretObject> actualSecret = detailData.getMeta().getFields().stream()
            .filter(column -> "secret".equals(column.getName()))
            .findFirst().map(DetailField::getSecret);
        assertThat(actualSecret)
            .hasValueSatisfying(value -> assertThat(value)
                .isEqualToComparingFieldByFieldRecursively(new SecretObject("test"))
            );
    }

    @Test
    void secretDetailColumnWithNoSecretColumn() {
        DetailData detailData = ViewUtils.getDetail(new TestDto(), Mode.VIEW, false);
        assertThat(detailData.getMeta().getFields().stream()
            .filter(column -> "noSecret".equals(column.getName()))
            .findFirst().map(DetailField::getSecret)).isEmpty();
    }

    @Test
    void secretGridColumn() {
        GridData gridData = ViewUtils.getGridView(Collections.singletonList(new TestDto()), Mode.VIEW);
        Optional<SecretObject> actualSecret = gridData.getMeta().getColumns().stream()
            .filter(column -> "secret".equals(column.getName()))
            .findFirst().map(GridColumn::getSecret);
        assertThat(actualSecret)
            .hasValueSatisfying(value -> assertThat(value)
                .isEqualToComparingFieldByFieldRecursively(new SecretObject("test"))
            );
    }

    @Test
    void secretGridColumnWithNoSecretColumn() {
        GridData gridData = ViewUtils.getGridView(Collections.singletonList(new TestDto()), Mode.VIEW);
        assertThat(gridData.getMeta().getColumns().stream()
            .filter(column -> "noSecret".equals(column.getName()))
            .findFirst().map(GridColumn::getSecret)).isEmpty();
    }

    static class TestDto {
        private final long id = 1;

        @Secret(slug = "test")
        private String secret;
        private String noSecret;

        public long getId() {
            return id;
        }

        public String getSecret() {
            return secret;
        }

        public String getNoSecret() {
            return noSecret;
        }
    }
}

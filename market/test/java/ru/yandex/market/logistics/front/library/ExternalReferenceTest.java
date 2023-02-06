package ru.yandex.market.logistics.front.library;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.Type;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalReferenceTest {

    @Test
    void test() {
        TestDtoDefaultValues dto = new TestDtoDefaultValues();

        GridData gridView = ViewUtils.getGridView(Collections.singletonList(dto), Mode.VIEW);

        assertThat(gridView.getMeta().getColumns()).hasSize(1);
        GridColumn column = gridView.getMeta().getColumns().get(0);

        assertThat(column.getType()).isEqualTo(Type.EXTERNAL_REFERENCE);

        assertThat(gridView.getItems()).hasSize(1);
        GridItem item = gridView.getItems().get(0);

        assertThat(item.getValues().get("externalReference")).isNotNull().isEqualTo(dto.getExternalReference());
    }

    private static class TestDtoDefaultValues {
        private final long id = 1;
        private final ExternalReferenceObject externalReference = new ExternalReferenceObject()
            .setDisplayName("some name")
            .setOpenNewTab(true)
            .setUrl("some.Url");

        public long getId() {
            return id;
        }

        public ExternalReferenceObject getExternalReference() {
            return externalReference;
        }
    }
}

package ru.yandex.market.ff.util.excel.export;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.mockito.Mockito.when;

public abstract class AbstractFileExportTest {

    protected final ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);
    protected GridReader gridReader;

    protected SoftAssertions assertions;

    @BeforeEach
    public void init() {
        gridReader = new XlsxGridReader(paramService);
        when(paramService.getMaxFileSizeForXlsxGridReader()).thenReturn(500_000);
    }

    @BeforeEach
    public void initAssertions() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    protected void assertColumnHasValue(@Nonnull GridRow row, int columnIndex, @Nonnull String value) {
        GridCell cell = row.getCell(columnIndex);
        Optional<String> maybeValue = cell.getRawValue();
        assertions.assertThat(maybeValue).isPresent();
        assertions.assertThat(maybeValue.get()).isEqualTo(value);
    }

    protected void assertColumnIsEmpty(@Nonnull GridRow row, int columnIndex) {
        GridCell cell = row.getCell(columnIndex);
        Optional<String> maybeValue = cell.getRawValue();
        assertions.assertThat(maybeValue).isPresent();
        assertions.assertThat(maybeValue.get()).isEqualTo("");
    }
}

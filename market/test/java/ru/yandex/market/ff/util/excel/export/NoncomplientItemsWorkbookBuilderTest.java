package ru.yandex.market.ff.util.excel.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.NonconformityType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.service.util.excel.export.NoncomplientItemsWorkbookBuilder;

public class NoncomplientItemsWorkbookBuilderTest extends AbstractFileExportTest {

    @Test
    public void exportToExcelDocIsCorrectForTwoSameItems() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        RegistryUnit item1 = RegistryUnit.itemBuilder()
                .unitInfo(UnitInfo.builder()
                        .unitCountsInfo(UnitCountsInfo.builder()
                                .unitCount(UnitCount.builder()
                                        .count(2)
                                        .type(UnitCountType.NON_COMPLIENT)
                                        .nonconformityAttributes(Collections.singleton(
                                                NonconformityType.UNKNOWN_SKU
                                        ))
                                        .build())
                                .build())
                        .parentUnitId(RegistryUnitId.builder()
                                .part(UnitPartialId.builder()
                                        .type(RegistryUnitIdType.BOX_ID)
                                        .value("BOX1")
                                        .build())
                                .build())
                        .unitId(RegistryUnitId.builder()
                                .part(UnitPartialId.builder()
                                        .type(RegistryUnitIdType.SHOP_SKU)
                                        .value("1234")
                                        .build())
                                .part(UnitPartialId.builder()
                                        .type(RegistryUnitIdType.CONSIGNMENT_ID)
                                        .value("Привет")
                                        .build())
                                .build())
                        .build())
                .unitMeta(UnitMeta.builder()
                        .name("Товар 1")
                        .barcode("1234")
                        .url("http://yandex.ru")
                        .url("http://ya.ru")
                        .description("На самом видном месте")
                        .build())
                .build();
        RegistryUnit item2 = RegistryUnit.itemBuilder()
                .unitInfo(UnitInfo.builder()
                        .unitCountsInfo(UnitCountsInfo.builder()
                                .unitCount(UnitCount.builder()
                                        .count(1)
                                        .type(UnitCountType.NON_COMPLIENT)
                                        .nonconformityAttributes(List.of(
                                                NonconformityType.NO_LIFE_TIME,
                                                NonconformityType.NO_RUSSIAN_INFO
                                        ))
                                        .build())
                                .build())
                        .parentUnitId(RegistryUnitId.builder()
                                .part(UnitPartialId.builder()
                                        .type(RegistryUnitIdType.BOX_ID)
                                        .value("BOX1")
                                        .build())
                                .build())
                        .unitId(RegistryUnitId.builder()
                                .part(UnitPartialId.builder()
                                        .type(RegistryUnitIdType.CONSIGNMENT_ID)
                                        .value("Медвед")
                                        .build())
                                .build())
                        .build())
                .unitMeta(UnitMeta.builder()
                        .barcode("1232")
                        .manufacturedDate(LocalDate.of(2021, 9, 1)
                                .atStartOfDay()
                                .atOffset(ZoneOffset.UTC))
                        .expirationDate(OffsetDateTime.parse("2026-10-30T11:00:00+03:00"))
                        .build())
                .build();
        NoncomplientItemsWorkbookBuilder builder =
                new NoncomplientItemsWorkbookBuilder(
                        List.of(
                                item1,
                                item2
                        ));

        item2.getUnitMeta().setUrls(null);
        item2.getUnitMeta().setName(null);

        builder.build(outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        Grid grid = gridReader.read(inputStream);
        Grid expected = gridReader.read(
                this.getClass().getResourceAsStream("/util/excel/export/non-complient-example.xlsx"));

        assertRowsIsCorrect(grid, expected);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertRowsIsCorrect(@Nonnull Grid actual, Grid expected) {
        int actualNumberOfRows = actual.getNumberOfRows();
        int expectedNumberOfRows = expected.getNumberOfRows();
        if (expectedNumberOfRows != actualNumberOfRows) {
            throw new AssertionFailedError("Количество строк не совпадает");
        }
        for (int i = 0; i < expectedNumberOfRows; i++) {
            final GridRow actualRow = actual.getRow(i);
            final GridRow expectedRow = expected.getRow(i);
            int actualNumberOfCells = actualRow.size();
            int expectedNumberOfCells = expectedRow.size();
            if (actualNumberOfCells != expectedNumberOfCells) {
                throw new AssertionFailedError("Количество столбцов не совпадает");
            }
            for (int j = 0; j < expectedNumberOfCells; j++) {
                String actualValue = actualRow.getCell(j).getRawValue().orElse(null);
                String expectedValue = expectedRow.getCell(j).getRawValue().orElse(null);
                if (!Objects.equals(actualValue, expectedValue) && !equalNumbers(actualValue, expectedValue)) {
                    throw new AssertionFailedError(
                            String.format("Значения не совпадают %s != %s", actualValue, expectedValue));

                }
            }
        }
    }

    private boolean equalNumbers(String actualValue, String expectedValue) {
        try {
            return Double.valueOf(actualValue).equals(Double.valueOf(expectedValue));
        } catch (Exception ignored) {

        }
        return false;
    }
}

package ru.yandex.market.common.excel.out.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.excel.ColumnSpec;
import ru.yandex.market.common.excel.InternalColumnName;
import ru.yandex.market.common.excel.out.ExcelWriterFactory;
import ru.yandex.market.common.excel.wrapper.PoiCell;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ExcelWriterTest {

    @Test
    void writeFullXls() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        writeFull(false, buf -> {
            try {
                ExcelWriterFactory.withSettingsFull(
                        is,
                        columnSpecs(),
                        data(),
                        false
                ).write(buf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void writeFullXlsx() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        writeFull(true, buf -> {
            try {
                ExcelWriterFactory.withSettingsFull(
                        is,
                        columnSpecs(),
                        data(),
                        true
                ).write(buf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void writeFullXlsxStreamData() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        writeFull(true, buf -> {
            try {
                ExcelWriterFactory.withSettingsFullDataStreaming(
                        is,
                        columnSpecs(),
                        dataStream(),
                        true
                ).write(buf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void writeFull(boolean useStreaming, Consumer<OutputStream> writer) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer.accept(buf);
        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(buf.toByteArray()));
        if (useStreaming) {
            ExcelAssert.assertCells(wb, ImmutableMap.<String, String>builder()
                    .put("Инструкция!B5", "1").put("Инструкция!D5", "NEW D5").put("Инструкция!E5", "NEW E5")
                    .put("Инструкция!F5", "11").put("Инструкция!G5", "01.05.2018")
                    .put("Инструкция!B6", "2").put("Инструкция!D6", "NEW D6").put("Инструкция!E6", "NEW E6")
                    .put("Инструкция!F6", "12").put("Инструкция!G6", "02.05.2018")
                    .put("Инструкция!B7", "3").put("Инструкция!C7", "").put("Инструкция!D7", "NEW D7")
                    .put("Инструкция!E7", "NEW E7").put("Инструкция!F7", "13").put("Инструкция!G7", "03.05.2018")
                    .build()
            );
        } else {
            ExcelAssert.assertCells(wb, ImmutableMap.<String, String>builder()
                    .put("Инструкция!B5", "1").put("Инструкция!C5", "OLD C5").put("Инструкция!D5", "NEW D5")
                    .put("Инструкция!E5", "NEW E5").put("Инструкция!F5", "11").put("Инструкция!G5", "01.05.2018")
                    .put("Инструкция!B6", "2").put("Инструкция!C6", "OLD C6").put("Инструкция!D6", "NEW D6")
                    .put("Инструкция!E6", "NEW E6").put("Инструкция!F6", "12").put("Инструкция!G6", "02.05.2018")
                    .put("Инструкция!B7", "3").put("Инструкция!C7", "").put("Инструкция!D7", "NEW D7")
                    .put("Инструкция!E7", "NEW E7").put("Инструкция!F7", "13").put("Инструкция!G7", "03.05.2018")
                    .build()
            );
        }
    }

    @Test
    void writePartialXls() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ExcelWriterFactory.withSettingsUpdate(
                is,
                columnSpecs(),
                new InternalColumnName("h1"),
                TestDataObject::getF1,
                data()
        ).write(buf);

        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(buf.toByteArray()));
        ExcelAssert.assertCells(wb, ImmutableMap.<String, String>builder()
                .put("Инструкция!B5", "1").put("Инструкция!C5", "OLD C5").put("Инструкция!D5", "OLD D5")
                .put("Инструкция!E5", "NEW E5").put("Инструкция!F5", "11").put("Инструкция!G5", "01.05.2018")
                .put("Инструкция!B6", "  2  ").put("Инструкция!C6", "OLD C6").put("Инструкция!D6", "OLD D6")
                .put("Инструкция!E6", "NEW E6").put("Инструкция!F6", "12").put("Инструкция!G6", "02.05.2018")
                .put("Инструкция!B7", "").put("Инструкция!C7", "").put("Инструкция!D7", "")
                .put("Инструкция!E7", "").put("Инструкция!F7", "").put("Инструкция!G7", "")
                .build()
        );
    }

    @Test
    void writePartialByRowNumXls() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ExcelWriterFactory.writeByRownum(
                is,
                columnSpecs(),
                dataAsMap()
        ).write(buf);

        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(buf.toByteArray()));
        ExcelAssert.assertCells(wb, ImmutableMap.<String, String>builder()
                .put("Инструкция!B5", "1").put("Инструкция!C5", "OLD C5")
                .put("Инструкция!D5", "OLD D5").put("Инструкция!E5", "NEW E5")
                .put("Инструкция!B6", "  2  ").put("Инструкция!C6", "OLD C6")
                .put("Инструкция!D6", "OLD D6").put("Инструкция!E6", "NEW E6")
                .put("Инструкция!B7", "").put("Инструкция!C7", "")
                .put("Инструкция!D7", "").put("Инструкция!E7", "NEW E7")
                .build()
        );
    }

    @Test
    void appendByKeyXls() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("xls/write-test.xls");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ExcelWriterFactory.writeByRownum(
                is,
                Collections.singletonList(ColumnSpec.of(
                        new InternalColumnName("h4"),
                        TestDataObject::getF4,
                        PoiCell::setStringAppending)),
                dataAsMap()
        ).write(buf);


        Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(buf.toByteArray()));
        ExcelAssert.assertCells(wb, ImmutableMap.<String, String>builder()
                .put("Инструкция!B5", "1").put("Инструкция!C5", "OLD C5")
                .put("Инструкция!D5", "OLD D5").put("Инструкция!E5", "OLD E5\nNEW E5")
                .put("Инструкция!B6", "  2  ").put("Инструкция!C6", "OLD C6")
                .put("Инструкция!D6", "OLD D6").put("Инструкция!E6", "OLD E6\nNEW E6")
                .put("Инструкция!B7", "").put("Инструкция!C7", "")
                .put("Инструкция!D7", "").put("Инструкция!E7", "NEW E7")
                .build()
        );
    }

    Collection<ColumnSpec<TestDataObject>> columnSpecs() {
        return Arrays.asList(
                ColumnSpec.forString(new InternalColumnName("h1"), TestDataObject::getF1),
                ColumnSpec.forString(new InternalColumnName("h2"), TestDataObject::getF2),
                ColumnSpec.forString(new InternalColumnName("h3"), TestDataObject::getF3),
                ColumnSpec.forString(new InternalColumnName("h4"), TestDataObject::getF4),
                ColumnSpec.forNumber(new InternalColumnName("h5"), TestDataObject::getF5),
                ColumnSpec.forDate(new InternalColumnName("h6"), TestDataObject::getF6)
        );
    }

    Collection<TestDataObject> data() {
        return Arrays.asList(
                new TestDataObject("1", "NEW E5", "NEW D5", "NEW E5", 11, dateAsInstant(2018, Month.MAY, 1)),
                new TestDataObject("2", "NEW E6", "NEW D6", "NEW E6", 12, dateAsInstant(2018, Month.MAY, 2)),
                new TestDataObject("3", "NEW E7", "NEW D7", "NEW E7", 13, dateAsInstant(2018, Month.MAY, 3))
        );
    }

    Stream<TestDataObject> dataStream() {
        return Stream.of(
                new TestDataObject("1", "NEW E5", "NEW D5", "NEW E5", 11, dateAsInstant(2018, Month.MAY, 1)),
                new TestDataObject("2", "NEW E6", "NEW D6", "NEW E6", 12, dateAsInstant(2018, Month.MAY, 2)),
                new TestDataObject("3", "NEW E7", "NEW D7", "NEW E7", 13, dateAsInstant(2018, Month.MAY, 3))
        );
    }

    Map<Integer, TestDataObject> dataAsMap() {
        Map<Integer, TestDataObject> map = new LinkedHashMap<>();
        int i = 0;
        for (TestDataObject testDataObject : data()) {
            map.put(i++, testDataObject);
        }
        return map;
    }

    private static Instant dateAsInstant(int year, Month month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}

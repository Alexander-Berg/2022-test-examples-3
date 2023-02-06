package ru.yandex.market.pricelabs.exports;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.exports.TestFormats.Item;
import ru.yandex.market.pricelabs.exports.params.ColumnList;
import ru.yandex.market.pricelabs.exports.params.CsvParameters;
import ru.yandex.market.pricelabs.exports.params.CsvParameters.CsvParametersBuilder;
import ru.yandex.market.pricelabs.exports.params.ExcelParameters.ExcelParametersBuilder;

public class ExporterTest {

    private static final List<KeyValueSample> SAMPLES = List.of(
            new KeyValueSample(1, "тест 1"),
            new KeyValueSample(2, "тест 2"),
            new KeyValueSample(3, "тест 3"),
            new KeyValueSample(4, "тест 4"));

    @ParameterizedTest(name = "[{index}] {0} {2}")
    @MethodSource("exports")
    void testExport(ExporterTestContext<KeyValueSample> context, Object params, String resource) {
        var output = new ByteArrayOutputStream();
        var exporter = context.exporter();
        var export = exporter.export(params, null, KeyValueSample.class, output);

        export.add(SAMPLES.get(0));
        export.addAll(SAMPLES.subList(1, 3));
        export.add(SAMPLES.get(3));

        export.close();

        context.verify(resource, output.toByteArray());
    }

    @ParameterizedTest(name = "[{index}] {0} {2}")
    @MethodSource("exports")
    void testExportAndClose(ExporterTestContext<KeyValueSample> context, Object params, String resource) {
        var output = new ByteArrayOutputStream();
        var exporter = context.exporter();
        var export = exporter.export(params, null, KeyValueSample.class, output);

        export.exportAndClose(SAMPLES);

        context.verify(resource, output.toByteArray());
    }

    @ParameterizedTest(name = "[{index}] {0} {3}")
    @MethodSource("customFormats")
    void testFormats(ExporterTestContext<TestFormats> context, @Nullable Object params,
                     @Nullable ExportMapperCustomizer<TestFormats> customizer, String resource) {

        TestFormats sample = new TestFormats();
        sample.setA("AAAA");
        sample.setB("BBBB");
        sample.setCc("2012-12");
        sample.setD(1576682786000L);
        sample.setX(List.of(new Item("a", 1), new Item("b", 2), new Item("c", 3)));

        var output = new ByteArrayOutputStream();
        context.exporter().export(params, customizer, TestFormats.class, output).exportAndClose(List.of(sample));

        context.verify(resource, output.toByteArray());
    }

    private static Item get(TestFormats testFormats, int pos) {
        @Nullable var list = testFormats.getX();
        if (list == null || list.size() <= pos) {
            return new Item();
        } else {
            var ret = list.get(pos);
            return ret != null ? ret : new Item();
        }
    }

    static Object[][] exports() {
        final Class<KeyValueSample> clazz = KeyValueSample.class;
        var csv = new ExporterTestContextCsv<>();
        var csvUtf = new ExporterTestContextCsv<>(StandardCharsets.UTF_8);
        var excel = new ExporterTestContextExcel<>();
        var json = new ExporterTestContextJson<>(clazz);

        return new Object[][]{
                {csv, null, "pricelabs/exports/expect-simple.csv"},
                {csvUtf, new CsvParametersBuilder().encoding(StandardCharsets.UTF_8).build(),
                        "pricelabs/exports/expect-simple.csv"},
                {csv, new CsvParametersBuilder().encoding(CsvParameters.WIN1251).build(),
                        "pricelabs/exports/expect-simple.csv"},
                {csv, CsvParameters.DEFAULT, "pricelabs/exports/expect-simple.csv"},
                {csv, new CsvParametersBuilder().quote('\'').delimiter(',').build(),
                        "pricelabs/exports/expect-simple-separated.csv"},
                {csv, new CsvParametersBuilder().columnList(new ColumnList("key")).build(),
                        "pricelabs/exports/expect-simple-key.csv"},
                {csv, new CsvParametersBuilder().columnList(new ColumnList("value")).build(),
                        "pricelabs/exports/expect-simple-value.csv"},
                {excel, null, "pricelabs/exports/expect-simple.xlsx"},
                {excel, new ExcelParametersBuilder().sheetName("sheet").build(),
                        "pricelabs/exports/expect-simple-sheet.xlsx"},
                {excel, new ExcelParametersBuilder().columnList(new ColumnList("key")).build(),
                        "pricelabs/exports/expect-simple-key.xlsx"},
                {excel, new ExcelParametersBuilder().columnList(new ColumnList("value")).build(),
                        "pricelabs/exports/expect-simple-value.xlsx"},
                {json, null, "pricelabs/exports/expect-simple.json"}
        };
    }

    static Object[][] customFormats() {
        final Class<TestFormats> clazz = TestFormats.class;
        var csv = new ExporterTestContextCsv<>();
        var excel = new ExporterTestContextExcel<>();
        var json = new ExporterTestContextJson<>(clazz);


        var customizer = new ExportMapperCustomizer<TestFormats>();
        customizer.register("x", "excela1", "a1", String.class, value -> get(value, 0).getA());
        customizer.register("x", "excelb1", "b1", Integer.class, value -> get(value, 0).getB());

        customizer.register("x", "excela2", "a2", String.class, value -> get(value, 1).getA());
        customizer.register("x", "excelb2", "b2", Integer.class, value -> get(value, 1).getB());

        return new Object[][]{
                {csv, null, null, "pricelabs/exports/export-csv.csv"},
                {csv, null, customizer, "pricelabs/exports/export-csv-custom.csv"},
                {json, null, null, "pricelabs/exports/export-json.json"},
                {excel, null, null, "pricelabs/exports/export-excel.xlsx"},
                {excel, null, customizer, "pricelabs/exports/export-excel-custom.xlsx"}
        };
    }

}

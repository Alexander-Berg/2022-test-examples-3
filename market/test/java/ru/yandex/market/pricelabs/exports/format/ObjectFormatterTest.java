package ru.yandex.market.pricelabs.exports.format;

import java.util.LinkedHashMap;

import lombok.Data;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.exports.TestFormats;
import ru.yandex.market.pricelabs.generated.server.pub.model.AnalyticsPerFeedResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.ExportFileParams.FileTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.exports.format.ValueFormat.CUSTOM;
import static ru.yandex.market.pricelabs.exports.format.ValueFormat.LONG_TO_TIMESTAMP;
import static ru.yandex.market.pricelabs.exports.format.ValueFormat.YYYYMM_TO_MONTH;
import static ru.yandex.market.pricelabs.exports.format.ValueInclude.ALWAYS;
import static ru.yandex.market.pricelabs.exports.format.ValueStyle.CENTER;
import static ru.yandex.market.pricelabs.exports.format.ValueStyle.TITLE;

class ObjectFormatterTest {

    @Test
    void testSameInstance() {
        assertSame(ObjectFormatter.getInstance(), ObjectFormatter.getInstance());
        assertSame(ObjectFormatter.getDefaultFormat(), ObjectFormatter.getDefaultFormat());

        ObjectFormatter instance = ObjectFormatter.getInstance();
        var csv = instance.getFormat(TestFormats.class, FileTypeEnum.csv);
        assertSame(csv, instance.getFormat(TestFormats.class, FileTypeEnum.csv));

        var xlsx = instance.getFormat(TestFormats.class, FileTypeEnum.xlsx);
        assertSame(xlsx, instance.getFormat(TestFormats.class, FileTypeEnum.xlsx));

        var json = instance.getFormat(TestFormats.class, FileTypeEnum.json);
        assertSame(json, instance.getFormat(TestFormats.class, FileTypeEnum.json));

        assertNotSame(csv, xlsx);
        assertNotSame(csv, json);
        assertNotSame(xlsx, json);
    }

    @Test
    void testDefaultFormat() {

        assertEquals(column().build(), ObjectFormatter.getDefaultFormat());
    }

    @Test
    void testParseNoFormat() {
        ObjectFormatter instance = ObjectFormatter.getInstance();

        var csv = instance.getFormat(SampleClass.class, FileTypeEnum.csv);
        var xlsx = instance.getFormat(SampleClass.class, FileTypeEnum.xlsx);
        var json = instance.getFormat(SampleClass.class, FileTypeEnum.json);
        assertTrue(csv.isEmpty());
        assertTrue(xlsx.isEmpty());
        assertTrue(json.isEmpty());

        assertSame(csv, xlsx);
        assertSame(csv, json);
        assertSame(xlsx, json);
    }

    @Test
    void testParse() {
        ObjectFormatter instance = ObjectFormatter.getInstance();

        var csv = instance.getFormat(TestFormats.class, FileTypeEnum.csv);
        var xlsx = instance.getFormat(TestFormats.class, FileTypeEnum.xlsx);
        var json = instance.getFormat(TestFormats.class, FileTypeEnum.json);

        assertEquals(new LinkedHashMap<>() {{
            put("a", column().title("Поле 1").build());
            put("cc", column().title("C").size(1).include(ALWAYS).format(YYYYMM_TO_MONTH).build());
            put("b", column().build());
            put("d", column().title("D").format(LONG_TO_TIMESTAMP).build());
            put("x", column().include(ALWAYS).format(CUSTOM).build());
        }}, csv);

        assertEquals(new LinkedHashMap<>() {{
            put("d", column().title("Поле B").style(CENTER).format(LONG_TO_TIMESTAMP).build());
            put("cc", column().title("Поле A").style(TITLE).format(YYYYMM_TO_MONTH).build());
            put("x", column().include(ALWAYS).format(CUSTOM).build());
        }}, xlsx);

        assertTrue(json.isEmpty());
    }

    @Test
    void testParseSame() {
        ObjectFormatter instance = ObjectFormatter.getInstance();

        var csv = instance.getFormat(AnalyticsPerFeedResponse.class, FileTypeEnum.csv);
        var xlsx = instance.getFormat(AnalyticsPerFeedResponse.class, FileTypeEnum.xlsx);
        var json = instance.getFormat(AnalyticsPerFeedResponse.class, FileTypeEnum.json);

        assertFalse(csv.isEmpty());
        assertEquals(csv, xlsx);
        assertEquals(csv, json);
    }

    static ColumnFormat.ColumnFormatBuilder column() {
        return new ColumnFormat.ColumnFormatBuilder()
                .format(ValueFormat.DEFAULT)
                .include(ValueInclude.DEFAULT)
                .style(ValueStyle.NONE);
    }

    @Data
    static class SampleClass {
        String a;
    }
}

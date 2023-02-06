package ru.yandex.market.request.trace;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class TskvRecordBuilderTest {

    @Test
    public void shouldBuildSimpleTskv() {
        TskvRecordBuilder builder = new TskvRecordBuilder();

        builder.add("a bc", "d\t\0\n\r\\\"f");
        builder.add("gh", "fds");

        assertEquals("tskv\ta bc=d\\t\\0\\n\\r\\\\\\\"f\tgh=fds", builder.build());
    }

    @Test
    public void shouldBuildTskv() {
        TskvRecordBuilder builder = new TskvRecordBuilder();

        builder.add("a bc", "d\t\0f");
        builder.add("gh", "fds");
        builder.add("c d", 1);
        builder.add("x y", Instant.ofEpochMilli(1577389176123L));
        builder.add("z a", Arrays.asList(1L, 2L));
        builder.add("x y", Instant.ofEpochMilli(1577389176123L).atZone(ZoneId.of("UTC")),
                DateTimeFormatter.ofPattern("\rd\tM\nuuuu"));
        builder.add("abc", Arrays.asList(8,"text"));

        assertEquals("tskv\ta bc=d\\t\\0f\tgh=fds\tc d=1\tx y=2019-12-26T19:39:36.123Z\tz a=1,2\t"+
                "x y=\\r26\\t12\\n2019\tabc=8,text", builder.build());
    }

    @Test
    public void testGenericMethods() {
        TskvRecordBuilder builder = new TskvRecordBuilder();

        builder.add("a", (Object) "a");
        builder.add("b", (Object) 1);
        builder.add("c", (Object) Instant.ofEpochSecond(1234567890));
        builder.add("d", (Object) Arrays.asList("a", 1, Instant.ofEpochSecond(1234567890)));
        builder.add("e", (Object) null);
        builder.add("f", (TemporalAccessor) null);
        builder.add("g", (Collection<?>) null);

        HashMap<String, Object> map = new HashMap<>();
        map.put("h", "h");
        map.put("i", 1);
        map.put("j", Instant.ofEpochSecond(1234567890));
        map.put("k", Arrays.asList("a", 1));
        builder.add(map);

        HashMap<String, String> stringMap = new HashMap<>();
        stringMap.put("l", "l");
        stringMap.put("m", "m");
        builder.add(stringMap);

        assertEquals("tskv\ta=a\tb=1\tc=2009-02-13T23:31:30.000Z\td=a,1," +
                "2009-02-13T23:31:30Z\th=h\ti=1\tj=2009-02-13T23:31:30.000Z\tk=a,1\tl=l\tm=m", builder.build());
    }
}

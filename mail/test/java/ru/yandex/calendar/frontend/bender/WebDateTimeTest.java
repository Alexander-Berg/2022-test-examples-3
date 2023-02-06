package ru.yandex.calendar.frontend.bender;

import java.io.ByteArrayOutputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function2;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.bender.BenderParserSerializer;
import ru.yandex.misc.bender.MembersToBind;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.config.BenderConfiguration;
import ru.yandex.misc.bender.config.CustomMarshallerUnmarshallerFactoryBuilder;
import ru.yandex.misc.bender.serialize.MarshallerContext;
import ru.yandex.misc.io.OutputStreamOutputStreamSource;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class WebDateTimeTest extends CalendarTestBase {

    @BenderBindAllFields
    public static class Holder {
        public final WebDateTime dateTime;

        public Holder(WebDateTime dateTime) {
            this.dateTime = dateTime;
        }
    }

    private static final BenderParserSerializer<Holder> parserSerializer = BenderParserSerializer.cons(
            ClassX.wrap(Holder.class),
            BenderConfiguration.cons(
                    MembersToBind.ALL_FIELDS, false,
                    CustomMarshallerUnmarshallerFactoryBuilder.cons()
                            .add(WebDateTime.class,
                                    new WebDateTimeMarshallerUnmarshaller(),
                                    new WebDateTimeMarshallerUnmarshaller())
                            .build()));

    @Test
    public void serialize() {
        Function<Holder, String> serializeF = (h) -> new String(parserSerializer.getSerializer().serializeJson(h));

        Function2<Holder, MarshallerContext, String> serializeCtxF = (h, ctx) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            parserSerializer.getSerializer().serializeJson(h, new OutputStreamOutputStreamSource(baos), ctx);

            return new String(baos.toByteArray());
        };
        Holder zonedHolder = new Holder(WebDateTime.dateTime(MoscowTime.dateTime(2015, 7, 27, 20, 0)));
        Holder localHolder = new Holder(WebDateTime.localDateTime(new LocalDateTime(2015, 7, 27, 20, 0)));

        Assert.equals("{\"dateTime\":\"2015-07-27T20:00:00\"}", serializeF.apply(zonedHolder));
        Assert.equals("{\"dateTime\":\"2015-07-27T20:00:00\"}", serializeF.apply(localHolder));

        MarshallerContext localCtx = new WebDateFormatMarshallerContext(false);
        Assert.equals(serializeF.apply(zonedHolder), serializeCtxF.apply(zonedHolder, localCtx));
        Assert.equals(serializeF.apply(localHolder), serializeCtxF.apply(localHolder, localCtx));

        MarshallerContext zonedCtx = new WebDateFormatMarshallerContext(true);
        Assert.equals("{\"dateTime\":\"2015-07-27T20:00:00+03:00\"}", serializeCtxF.apply(zonedHolder, zonedCtx));
        Assert.equals("{\"dateTime\":\"2015-07-27T20:00:00\"}", serializeCtxF.apply(localHolder, zonedCtx));
    }

    @Test
    public void parse() {
        Assert.equals(new DateTime(2015, 7, 27, 20, 0, DateTimeZone.forOffsetHours(3)), parserSerializer.getParser()
                .parseJson("{\"dateTime\":\"2015-07-27T20:00:00+03:00\"}").dateTime.getDateTime());

        Assert.equals(new LocalDateTime(2015, 7, 27, 20, 0), parserSerializer.getParser()
                .parseJson("{\"dateTime\":\"2015-07-27T20:00:00\"}").dateTime.getLocalDateTime());
    }
}

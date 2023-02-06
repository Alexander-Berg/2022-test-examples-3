package ru.yandex.market.mbo.flume.interceptor;

import java.util.HashMap;

import org.apache.flume.Context;
import org.apache.flume.event.SimpleEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.flume.channel.SimpleEventBuilder;

/**
 * @author moskovkin@yandex-team.ru
 * @since 17.08.17
 */
public class TimestampHeaderInterceptorTest {
    public static final String HEADER = "timestamp";

    private static final String EXISTING_VALUE = "1000";

    private SimpleEvent headerExistEvent;
    private SimpleEvent noHeaderEvent;

    @Before
    public void prepareTestData() {
        headerExistEvent = SimpleEventBuilder.aSimpleEvent()
                .addHeader(HEADER, EXISTING_VALUE)
                .build();
        noHeaderEvent = SimpleEventBuilder.aSimpleEvent()
                .build();
    }

    public TimestampHeaderInterceptor createInterceptor(boolean preserve) throws Exception {
        TimestampHeaderInterceptor.Builder builder = new TimestampHeaderInterceptor.Builder();

        HashMap<String, String> config = new HashMap<>();
        config.put(TimestampHeaderInterceptor.Builder.HEADER_PARAM, HEADER);
        config.put(TimestampHeaderInterceptor.Builder.PRESERVE_PARAM, String.valueOf(preserve));

        Context context = new Context(config);
        builder.configure(context);

        return (TimestampHeaderInterceptor) builder.build();
    }

    @Test
    public void preserveExistingTest() throws Exception {
        TimestampHeaderInterceptor interceptor = createInterceptor(true);
        interceptor.intercept(headerExistEvent);

        String headerValue = headerExistEvent.getHeaders().get(HEADER);
        Assert.assertEquals(EXISTING_VALUE, headerValue);
    }

    @Test
    public void dropExistingTest() throws Exception {
        TimestampHeaderInterceptor interceptor = createInterceptor(false);

        long timestamp = System.currentTimeMillis();
        interceptor.intercept(headerExistEvent);

        String headerValue = headerExistEvent.getHeaders().get(HEADER);
        Assert.assertNotEquals(EXISTING_VALUE, headerValue);

        long eventTimestamp = Long.parseLong(headerValue);
        Assert.assertTrue(eventTimestamp >= timestamp);
    }

    @Test
    public void addNewTest() throws Exception {
        TimestampHeaderInterceptor interceptor = createInterceptor(true);

        long timestamp = System.currentTimeMillis();
        interceptor.intercept(noHeaderEvent);

        String headerValue = noHeaderEvent.getHeaders().get(HEADER);
        Assert.assertNotNull(headerValue);

        long eventTimestamp = Long.parseLong(headerValue);
        Assert.assertTrue(eventTimestamp >= timestamp);
    }
}

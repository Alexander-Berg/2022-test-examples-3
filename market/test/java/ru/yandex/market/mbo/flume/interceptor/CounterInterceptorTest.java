package ru.yandex.market.mbo.flume.interceptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.flume.channel.SimpleEventBuilder;

/**
 * @author moskovkin@yandex-team.ru
 * @since 17.08.17
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CounterInterceptorTest {
    public static final String HEADER = "counter";

    public static final String WRONG_HEADER_VALUE = "Be Be Be";
    public static final String CORRECT_HEADER_VALUE = "1000";
    public static final String NEXT_CORRECT_HEADER_VALUE = "1001";
    public static final String INITIAL_HEADER = "1";
    public static final String DROP_HEADER_VALUE = "1999";
    public static final String LEAVE_HEADER_VALUE = "1998";

    public static final String DROP_COUNT = "2000";


    private SimpleEvent wrongCountEvent;
    private SimpleEvent correctHeaderEvent;
    private SimpleEvent noCountEvent;
    private SimpleEvent dropEvent;
    private SimpleEvent leaveEvent;

    private List<Event> testEvents;

    private CounterInterceptor interceptor;

    @Before
    public void prepareTestData() {
        wrongCountEvent = SimpleEventBuilder.aSimpleEvent()
                .addHeader(HEADER, WRONG_HEADER_VALUE)
                .build();
        correctHeaderEvent = SimpleEventBuilder.aSimpleEvent()
                .addHeader(HEADER, CORRECT_HEADER_VALUE)
                .build();
        noCountEvent = SimpleEventBuilder.aSimpleEvent()
                .build();
        dropEvent = SimpleEventBuilder.aSimpleEvent()
                .addHeader(HEADER, DROP_HEADER_VALUE)
                .build();
        leaveEvent = SimpleEventBuilder.aSimpleEvent()
                .addHeader(HEADER, LEAVE_HEADER_VALUE)
                .build();

        testEvents = Arrays.asList(
                noCountEvent,
                correctHeaderEvent,
                wrongCountEvent,
                dropEvent,
                leaveEvent
        );
    }

    @Before
    public void setUp() throws Exception {
        CounterInterceptor.Builder builder = new CounterInterceptor.Builder();
        HashMap<String, String> config = new HashMap<>();
        config.put(CounterInterceptor.Builder.HEADER_PARAM, HEADER);
        config.put(CounterInterceptor.Builder.DROP_COUNT_PARAM, DROP_COUNT);
        Context context = new Context(config);
        builder.configure(context);

        interceptor = (CounterInterceptor) builder.build();
    }

    @Test
    public void interceptTest() throws Exception {
        interceptor.intercept(testEvents);

        Assert.assertEquals(INITIAL_HEADER, noCountEvent.getHeaders().get(HEADER));
        Assert.assertEquals(NEXT_CORRECT_HEADER_VALUE, correctHeaderEvent.getHeaders().get(HEADER));
        Assert.assertEquals(WRONG_HEADER_VALUE, wrongCountEvent.getHeaders().get(HEADER));

    }

    @Test
    public void dropTest() throws Exception {
        List<Event> intercepted = interceptor.intercept(testEvents);

        Assert.assertArrayEquals(new Event[]
            {
                noCountEvent,
                correctHeaderEvent,
                wrongCountEvent,
                //dropEvent should be filtered out
                leaveEvent
            },
            intercepted.toArray()
        );
    }
}

package ru.yandex.market.http.test;

/**
 * @author dimkarp93
 */
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.http.MultipleRequestHeadersProcessor;
import ru.yandex.market.http.RequestHeadersProcessor;

public class RequestHeadersProcessorTest {
    @Test
    public void unionNullFirst() {
        RequestHeadersProcessor processor = RequestHeadersProcessor.union(null, RequestHeadersProcessor.STUB);
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple = (MultipleRequestHeadersProcessor) processor;
        Assert.assertEquals(1, multiple.getProcessors().size());
        Assert.assertNotNull(multiple.getProcessors().iterator().next());
    }

    @Test
    public void unionNullSecond() {
        RequestHeadersProcessor processor = RequestHeadersProcessor.union(RequestHeadersProcessor.STUB,  null);
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple = (MultipleRequestHeadersProcessor) processor;
        Assert.assertEquals(1, multiple.getProcessors().size());
        Assert.assertNotNull(multiple.getProcessors().iterator().next());
    }

    @Test
    public void unionNotNullBoth() {
        RequestHeadersProcessor processor = RequestHeadersProcessor.union(RequestHeadersProcessor.STUB,  RequestHeadersProcessor.STUB);
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple = (MultipleRequestHeadersProcessor) processor;
        Assert.assertEquals(2, multiple.getProcessors().size());
        Iterator<RequestHeadersProcessor> it = multiple.getProcessors().iterator();
        Assert.assertNotNull(it.next());
        Assert.assertNotNull(it.next());
    }

    @Test
    public void unionNotNullMultipleFirst() {
        RequestHeadersProcessor processor = RequestHeadersProcessor.union(
                new MultipleRequestHeadersProcessor(
                        Arrays.asList(RequestHeadersProcessor.STUB,  RequestHeadersProcessor.STUB)
                ),
                RequestHeadersProcessor.STUB);
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple = (MultipleRequestHeadersProcessor) processor;
        Assert.assertEquals(3, multiple.getProcessors().size());
        Iterator<RequestHeadersProcessor> it = multiple.getProcessors().iterator();
        Assert.assertNotNull(it.next());
        Assert.assertNotNull(it.next());
        Assert.assertNotNull(it.next());
    }

    @Test
    public void unionNotNullMultipleSecond() {
        RequestHeadersProcessor processor = RequestHeadersProcessor.union(
                RequestHeadersProcessor.STUB,
                new MultipleRequestHeadersProcessor(
                        Arrays.asList(RequestHeadersProcessor.STUB,  RequestHeadersProcessor.STUB)
                ));
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple = (MultipleRequestHeadersProcessor) processor;
        Assert.assertEquals(2, multiple.getProcessors().size());
        Iterator<RequestHeadersProcessor> it = multiple.getProcessors().iterator();
        Assert.assertNotNull(it.next());

        RequestHeadersProcessor processor2 = it.next();
        Assert.assertTrue(processor instanceof MultipleRequestHeadersProcessor);
        MultipleRequestHeadersProcessor multiple2 = (MultipleRequestHeadersProcessor) processor2;
        Assert.assertEquals(2, multiple2.getProcessors().size());
        Iterator<RequestHeadersProcessor> it2 = multiple2.getProcessors().iterator();
        Assert.assertNotNull(it2.next());
        Assert.assertNotNull(it2.next());
    }
}

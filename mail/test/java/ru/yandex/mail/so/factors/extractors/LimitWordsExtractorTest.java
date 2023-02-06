package ru.yandex.mail.so.factors.extractors;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.BasicFuture;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.mail.so.factors.SoFactor;
import ru.yandex.mail.so.factors.types.LongSoFactorType;
import ru.yandex.mail.so.factors.types.StringSoFactorType;
import ru.yandex.test.util.TestBase;

public class LimitWordsExtractorTest extends TestBase {
    public LimitWordsExtractorTest() {
        super(false, 0L);
    }

    private void test(
        final int maxWords,
        final String input,
        final String output,
        final long outputWordCount)
        throws Exception
    {
        BasicFuture<List<SoFactor<?>>> future =
            new BasicFuture<>(EmptyFutureCallback.INSTANCE);
        LimitWordsExtractor.INSTANCE.extract(input, maxWords, future);
        Assert.assertTrue(future.isDone());
        List<SoFactor<?>> results = future.get();
        Assert.assertEquals(2, results.size());
        SoFactor<?> result = results.get(0);
        Assert.assertEquals(
            StringSoFactorType.STRING,
            result.type());
        Assert.assertEquals(
            output,
            StringSoFactorType.STRING.cast(result.value()));
        result = results.get(1);
        Assert.assertEquals(LongSoFactorType.LONG, result.type());
        Assert.assertEquals(
            outputWordCount,
            LongSoFactorType.LONG.cast(result.value()).longValue());
    }

    @Test
    public void test() throws Exception {
        test(4, "Привет, дивный мир!", "Привет, дивный мир!", 3);
        test(3, "Привет, дивный мир!", "Привет, дивный мир!", 3);
        test(2, "Привет, дивный мир!", "Привет, дивный ", 2);
        test(1, "Привет, дивный мир!", "Привет,", 1);
        test(1, "  Привет, дивный мир!", "  Привет,", 1);

        test(4, "Привет, дивный мир", "Привет, дивный мир", 3);
    }
}


package ru.yandex.mail.so.factors.dsl;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.ConstFunction;
import ru.yandex.function.NullConsumer;
import ru.yandex.http.util.BasicFuture;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.mail.so.factors.BasicSoFunctionInputs;
import ru.yandex.mail.so.factors.SoFactor;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorFactoryContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so.factors.types.SoFactorTypesRegistry;
import ru.yandex.mail.so.factors.types.StringSoFactorType;
import ru.yandex.stater.NullStatersRegistrar;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SoDslTest extends TestBase {
    public SoDslTest() {
        super(false, 0L);
    }

    private SoFactorsExtractorsRegistry load(final String name)
        throws Exception
    {
        SoFactorsExtractorsRegistry registry =
            new SoFactorsExtractorsRegistry(
                NullStatersRegistrar.INSTANCE,
                new SoFactorTypesRegistry());
        SoFactorsExtractorFactoryContext context =
            new SoFactorsExtractorFactoryContext(
                null,
                registry,
                new ConstFunction<>(NullConsumer.instance()),
                new LongAdder(),
                new LongAdder(),
                Thread.currentThread().getThreadGroup(),
                // We don't expect any http clients in this test
                null,
                // As well as external data
                null,
                new HashMap<>(),
                new HashMap<>(),
                logger,
                null,
                0L);
        new DslParser(context).parse(
            new BufferedReader(new StringReader(loadResourceAsString(name))));
        return registry;
    }

    private void testStringFunction(
        final SoFactorsExtractorsRegistry registry,
        final String name,
        final List<String> inputs,
        final List<String> expectedOutputs)
        throws Exception
    {
        SoFactorsExtractorContext context =
            new DummySoFactorsExtractorContext(logger);
        int size = inputs.size();
        BasicSoFunctionInputs functionInputs =
            new BasicSoFunctionInputs(context.accessViolationHandler(), size);
        for (int i = 0; i < size; ++i) {
            String input = inputs.get(i);
            if (input != null) {
                functionInputs.set(
                    i,
                    StringSoFactorType.STRING.createFactor(input));
            }
        }
        BasicFuture<List<SoFactor<?>>> future =
            new BasicFuture<>(EmptyFutureCallback.INSTANCE);
        logger.info("Applying extractor <" + name + '>');
        registry.getExtractor(name).extract(context, functionInputs, future);
        List<SoFactor<?>> factors = future.get();
        size = factors.size();
        List<String> actualOutputs = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            SoFactor<?> factor = factors.get(i);
            String actual;
            if (factor == null) {
                actual = null;
            } else if (factor.type() == StringSoFactorType.STRING) {
                actual = StringSoFactorType.STRING.cast(factor.value());
            } else {
                throw new AssertionError(
                    "At position " + i
                    + " factor expected to be string value, but was: "
                    + factor);
            }
            actualOutputs.add(actual);
        }
        YandexAssert.check(
            YandexAssert.checkersFor(expectedOutputs),
            actualOutputs);
    }

    @Test
    public void test() throws Exception {
        try (SoFactorsExtractorsRegistry registry = load("dsl-test1.dsl")) {
            testStringFunction(
                registry,
                "my_chain",
                Arrays.asList("[", "]", "hello", "world"),
                Arrays.asList("[hello, world]", "[world, hello]", "hello"));

            logger.info("Let's test null input");
            testStringFunction(
                registry,
                "my_chain",
                Arrays.asList("<", ">", null, "word"),
                Arrays.asList("<word>", null, "word"));

            logger.info("Let's multiply string");
            testStringFunction(
                registry,
                "string_x9",
                Collections.singletonList("ab"),
                Collections.singletonList("ababababababababab"));
            testStringFunction(
                registry,
                "string_x27",
                Collections.singletonList("a"),
                Collections.singletonList("aaaaaaaaaaaaaaaaaaaaaaaaaaa"));

            logger.info("Let's widen borders");
            testStringFunction(
                registry,
                "borders_x3",
                Arrays.asList("a", "b", "c"),
                Arrays.asList("aaabccc", "aaa, ccc"));

            logger.info("Let's test sequential concat and local function");
            testStringFunction(
                registry,
                "sequential_concat",
                Arrays.asList("A", "B", "C"),
                Collections.singletonList("A:B:C"));
            logger.info("Let's test null extractor");
            testStringFunction(
                registry,
                "bare_concat_2",
                Arrays.asList("A", "B"),
                Collections.singletonList("AB"));
        }
    }

    @Test
    public void testMultiReturns() throws Exception {
        try (SoFactorsExtractorsRegistry registry = load("dsl-test1.dsl")) {
            testStringFunction(
                registry,
                "multi_returns",
                Arrays.asList("1", "2", "3"),
                Arrays.asList(null, "1", "3", "1", null, "2", "3", "1", null, "3"));
            testStringFunction(
                registry,
                "multi_returns",
                Arrays.asList("1", "2", null),
                Arrays.asList(null, "1", null, null, null, null, null, "1", "2", null));
        }
    }

    @Test
    public void testConsts() throws Exception {
        try (SoFactorsExtractorsRegistry registry = load("dsl-test1.dsl")) {
            testStringFunction(
                registry,
                "const_concat_1",
                Arrays.asList("my string"),
                Arrays.asList("\"Preamble\":\nHello, world -> my string", "suffix", "puffix"));
            testStringFunction(
                registry,
                "const_concat_2",
                Arrays.asList("my string"),
                Arrays.asList("nothing", "suffix", "puffix"));
        }
    }

    @Test
    public void testCritical() throws Exception {
        try (SoFactorsExtractorsRegistry registry = load("dsl-test1.dsl")) {
            testStringFunction(
                registry,
                "critical_concat",
                Arrays.asList("hello", "world"),
                Arrays.asList("hello, hello", "hello, world", "suffix"));
            try {
                testStringFunction(
                    registry,
                    "critical_concat",
                    Arrays.asList("hello", null),
                    Arrays.asList("hello, hello", "hello, world", "suffix"));
                Assert.fail();
            } catch (Exception e) {
            }
            testStringFunction(
                registry,
                "wrap_critical_concat",
                Arrays.asList("hello", "world"),
                Arrays.asList("before", "hello, hello", "hello, world", "suffix", "after"));
            testStringFunction(
                registry,
                "wrap_critical_concat",
                Arrays.asList("hello", null),
                Arrays.asList("before", null, null, null, "after"));
            try {
                testStringFunction(
                    registry,
                    "bypass_critical_concat",
                    Arrays.asList("hello", null),
                    Arrays.asList("before", "hello, hello", "hello, world", "suffix", "after"));
                Assert.fail();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testPragmaCritical() throws Exception {
        try (SoFactorsExtractorsRegistry registry = load("dsl-test2.dsl")) {
            testStringFunction(
                registry,
                "critical_concat",
                Arrays.asList("hello", "world"),
                Arrays.asList("hello, world"));
            try {
                testStringFunction(
                    registry,
                    "critical_concat",
                    Arrays.asList("hello", null),
                    Arrays.asList("hello"));
                Assert.fail();
            } catch (Exception e) {
            }
            testStringFunction(
                registry,
                "noncritical_concat",
                Arrays.asList("hello", "world"),
                Arrays.asList("hello, world"));
            testStringFunction(
                registry,
                "noncritical_concat",
                Arrays.asList("hello", null),
                Arrays.asList(new String[]{null}));
        }
    }
}


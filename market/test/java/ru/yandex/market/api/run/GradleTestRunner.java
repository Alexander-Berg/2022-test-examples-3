package ru.yandex.market.api.run;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import ru.yandex.market.api.run.runner.RandomRunners;

import java.lang.reflect.Method;
import java.util.*;

import static ru.yandex.market.api.run.ApiRandomTestRunListener.API_RANDOM_TEST_RUN_LISTENER;
import static ru.yandex.market.api.run.StreamHelper.directAllOutputToNull;
import static ru.yandex.market.api.run.StreamHelper.restoreAllOutput;

/**
 * @author dimkarp93
 */
public class GradleTestRunner {

    private static final long DEFAULT_SEED = System.currentTimeMillis();

    private static final String SEPARATE_LINE = StringUtils.repeat('=', 80);

    private static Random random;

    private static int totalTestCount = 0;
    private static int successedTestCount = 0;
    private static int ignoreTestCount = 0;
    private static int failureTestCount = 0;

    private static long seed;

    private static boolean isDefaultSeed = true;

    private static boolean failed = false;

    private static TestStatusFormatter testStatusFormatter = new TestStatusFormatter();

    public static void main(String[] args) {
        try {
            mainForWrap(args);
        } catch (Throwable t) {
            failed = true;
        } finally {
            restoreAllOutput();
            System.out.print(API_RANDOM_TEST_RUN_LISTENER.getFormatter().getOutput());
            printTestTotalInfo(seed, isDefaultSeed, failed);
            System.exit(failed ? 1 : 0);
        }
    }

    private static void mainForWrap(String[] args) throws Throwable {
        try {
            if (args.length > 0) {
                seed = Long.parseLong(args[0]);
                isDefaultSeed = false;
            } else {
                seed = DEFAULT_SEED;
            }
        } catch (Exception e) {
            seed = DEFAULT_SEED;
        }

        Reflections reflections = new Reflections(ConfigurationBuilder.build()
                .addScanners(new MethodAnnotationsScanner())
        );
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Test.class);

        Multimap<Class<?>, Method> testMethodsMultimaps = ArrayListMultimap.create();

        for (Method method: methods) {
            testMethodsMultimaps.put(method.getDeclaringClass(), method);
        }

        List<Class<?>> testClasses = Lists.newArrayList(testMethodsMultimaps.keySet());


        random = new Random(seed);

        Collections.shuffle(testClasses, random);

        directAllOutputToNull();

        API_RANDOM_TEST_RUN_LISTENER.setFormatter(testStatusFormatter);


        runTests(testClasses, testMethodsMultimaps);
    }

    private static void runTests(List<Class<?>> testClasses, Multimap<Class<?>, Method> testMethodsMultimaps) throws Exception {
        JUnitCore jUnitCore = new JUnitCore();
        for (Class<?> testClass: testClasses) {

            RunWith runnerWith = testClass.getAnnotation(RunWith.class);
            Ignore ignore = testClass.getAnnotation(Ignore.class);

            if (null != ignore) {
                Collection<Method> ignoreMethods = testMethodsMultimaps.get(testClass);
                for (Method method: ignoreMethods) {
                    testStatusFormatter.formatTestIgnored(Description.createTestDescription(testClass, method.getName()));
                }
                ignoreTestCount += ignoreMethods.size();
                continue;
            }


            try {
                Class<? extends Runner> runnerClass;
                if (null != runnerWith) {
                    runnerClass = RandomRunners.valueOf(runnerWith.value()).getWrapperRunner();
                } else {
                    runnerClass = RandomRunners.DEFAULT.getWrapperRunner();
                }
                Runner runner = runnerClass.getDeclaredConstructor(Random.class, Class.class)
                        .newInstance(random, testClass);
                Result result = jUnitCore.run(runner);

                successedTestCount += result.getRunCount();
                failureTestCount += result.getFailureCount();
                ignoreTestCount += result.getIgnoreCount();

                if (!result.wasSuccessful()) {
                    throw new RuntimeException("");
                }
            } catch (IllegalArgumentException e) {
                failed = true;
            }

        }
    }

    private static void printTestTotalInfo(long seed, boolean isDefaultSeed, boolean failed) {
        System.out.println();
        System.out.println(SEPARATE_LINE);

        successedTestCount -= failureTestCount;
        totalTestCount = successedTestCount + failureTestCount + ignoreTestCount;

        System.out.printf("Results: %s (%d tests, %d successes, %d failures, %d skipped)%n",
                failed ? "FAILURE" : "SUCCESS", totalTestCount, successedTestCount,
                failureTestCount, ignoreTestCount);

        if (isDefaultSeed) {
            System.out.println("Default seed");
        } else {
            System.out.println("Seed is set by user");
        }

        System.out.printf("Test started with seed: %s%n", seed);
        System.out.println("For use test with writing above seed, set -Pseed=<seed> argument");
        System.out.println(SEPARATE_LINE);
        System.out.println();

    }
}

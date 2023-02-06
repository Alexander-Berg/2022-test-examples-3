package ru.yandex.direct.scheduler.hourglass.implementations;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import ru.yandex.direct.scheduler.Hourglass;
import ru.yandex.direct.scheduler.HourglassDaemon;
import ru.yandex.direct.scheduler.hourglass.ParamDescription;
import ru.yandex.direct.scheduler.support.DirectJob;
import ru.yandex.direct.scheduler.support.DirectParameterizedJob;
import ru.yandex.direct.scheduler.support.DirectShardedJob;
import ru.yandex.direct.scheduler.support.ParameterizedBy;
import ru.yandex.direct.scheduler.support.ParametersSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TaskParameterizerTest {
    private static ApplicationContext ctx;
    private TaskParameterizer taskParameterizer;
    private final List<Integer> shards = List.of(1, 2, 3, 4);

    @BeforeClass
    public static void createContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ParamSrc.class);
        applicationContext.refresh();
        ctx = applicationContext;
    }

    @Before
    public void init() {
        taskParameterizer = new TaskParameterizer(ctx, shards);
    }

    @Test
    public void testSimpleScheduleJob() {
        List<ParamDescription> descriptions = taskParameterizer.getAllParameters(SimpleScheduleJob.class);

        assertEquals(1, descriptions.size());
        assertNotNull(descriptions.get(0));
        assertTrue(descriptions.get(0).paramMap().containsKey("is_daemon"));

        assertFalse(Boolean.parseBoolean(descriptions.get(0).paramMap().get("is_daemon")));
    }

    @Test
    public void testSimpleCronScheduleJob() {
        List<ParamDescription> descriptions = taskParameterizer.getAllParameters(CronScheduleJob.class);

        assertEquals(1, descriptions.size());
        assertNotNull(descriptions.get(0));
        assertTrue(descriptions.get(0).paramMap().containsKey("is_daemon"));
        assertFalse(Boolean.parseBoolean(descriptions.get(0).paramMap().get("is_daemon")));
    }

    @Test
    public void testShardedJob() {
        List<ParamDescription> descriptions = taskParameterizer.getAllParameters(ShardedJob.class);
        assertEquals(shards.size(), descriptions.size());

        for (int i = 0; i < shards.size(); i++) {
            assertNotNull(descriptions.get(i));
            assertTrue(descriptions.get(i).paramMap().containsKey("is_daemon"));
            assertFalse(Boolean.parseBoolean(descriptions.get(i).paramMap().get("is_daemon")));

            assertTrue(descriptions.get(i).paramMap().containsKey("shard"));
            assertEquals((i + 1) + "", descriptions.get(i).paramMap().get("shard"));
        }
    }

    @Test
    public void testParameterizedJob() {
        List<ParamDescription> descriptions = taskParameterizer.getAllParameters(ParameterizedJob.class);

        List<String> params = List.of("1", "3", "42");

        assertEquals(params.size(), descriptions.size());

        for (int i = 0; i < params.size(); i++) {
            assertNotNull(descriptions.get(i));
            assertTrue(descriptions.get(i).paramMap().containsKey("is_daemon"));
            assertFalse(Boolean.parseBoolean(descriptions.get(i).paramMap().get("is_daemon")));

            assertTrue(descriptions.get(i).paramMap().containsKey("param"));
            assertEquals(params.get(i), descriptions.get(i).paramMap().get("param"));
        }
    }

    @Test
    public void testParameterizedDaemon() {
        List<ParamDescription> descriptions = taskParameterizer.getAllParameters(ParameterizedDaemon.class);

        List<String> params = List.of("1", "3", "42");

        assertEquals(params.size(), descriptions.size());

        for (int i = 0; i < params.size(); i++) {
            assertNotNull(descriptions.get(i));
            assertTrue(descriptions.get(i).paramMap().containsKey("is_daemon"));
            assertTrue(Boolean.parseBoolean(descriptions.get(i).paramMap().get("is_daemon")));

            assertTrue(descriptions.get(i).paramMap().containsKey("param"));
            assertEquals(params.get(i), descriptions.get(i).paramMap().get("param"));
        }
    }

    @Test
    public void testDefaultConditionDaemonJob() {
        List<ParamDescription> descriptions =
                taskParameterizer.getAllParameters(DefaultConditionDaemonJob.class);

        assertEquals(1, descriptions.size());
        assertNotNull(descriptions.get(0));
        assertTrue(descriptions.get(0).paramMap().containsKey("is_daemon"));
        assertTrue(Boolean.parseBoolean(descriptions.get(0).paramMap().get("is_daemon")));
    }

    @Component
    public static class ParamSrc implements ParametersSource<Long> {
        @Override
        public List<Long> getAllParamValues() {
            return Arrays.asList(1L, 3L, 42L);
        }

        @Override
        public String convertParamToString(Long paramValue) {
            return paramValue.toString();
        }

        @Override
        public Long convertStringToParam(String string) {
            return Long.valueOf(string);
        }
    }

    @Hourglass(periodInSeconds = 10)
    public class SimpleScheduleJob extends DirectJob {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }

    @Hourglass(cronExpression = "0 0 8 * * ?")
    public class CronScheduleJob extends DirectJob {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }

    @Hourglass(periodInSeconds = 10)
    public class ShardedJob extends DirectShardedJob {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }


    @Hourglass(periodInSeconds = 60)
    @ParameterizedBy(parametersSource = ParamSrc.class)
    public class ParameterizedJob extends DirectParameterizedJob<String> {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }

    @HourglassDaemon(runPeriod = 0)
    @Hourglass(periodInSeconds = 0)
    @ParameterizedBy(parametersSource = ParamSrc.class)
    public class ParameterizedDaemon extends DirectParameterizedJob<String> {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }

    @HourglassDaemon(runPeriod = 0)
    @Hourglass(periodInSeconds = 0)
    public class DefaultConditionDaemonJob extends DirectJob {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }
}

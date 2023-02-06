package ru.yandex.market.pricelabs.tms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeDeepCopier;
import ru.yandex.market.pricelabs.AbstractSpringConfiguration;
import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.bindings.csv.CSVMapper;
import ru.yandex.market.pricelabs.juggler.CoreJugglerApi;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.timing.TimingConfig;
import ru.yandex.market.pricelabs.tms.api.InternalApi;
import ru.yandex.market.pricelabs.tms.api.SharedApi;
import ru.yandex.market.pricelabs.tms.jobs.QuartzJobs;
import ru.yandex.market.pricelabs.tms.juggler.JugglerConfig;
import ru.yandex.market.pricelabs.tms.juggler.TmsJugglerApi;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.ProcessingConfig;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.programs.AdvProgramTmsConfig;
import ru.yandex.market.pricelabs.tms.services.database.TableServiceImpl;
import ru.yandex.market.pricelabs.tms.yql.YqlConfig;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.migrations.TableVersionService;

@ContextHierarchy(
        @ContextConfiguration(name = "tms", classes = {
                AbstractTmsSpringConfiguration.TmsConfiguration.class
        })
)
@TestExecutionListeners(TmsResetListener.class)
public abstract class AbstractTmsSpringConfiguration extends AbstractSpringConfiguration {

    private static final Map<Object, Map<Object, List<?>>> CACHE = new ConcurrentHashMap<>();

    @Autowired
    protected TestControls testControls;

    @Autowired
    protected ExecutorSources executors;

    public static <Any> List<Any> readCsv(Class<Any> clazz, String resource) {
        return readCsv(CSVMapper.mapper(clazz), resource);
    }

    public static <Any> List<Any> readCsv(YTBinder<Any> ytBinder, String resource) {
        return readCsv(CSVMapper.mapper(ytBinder), resource);
    }

    public static <Any> List<Any> readCsv(CSVMapper<Any> csvMapper, String resource) {
        return getImpl(csvMapper, resource, () -> CoreTestUtils.readCsv(csvMapper, resource));
    }

    public static <Any> List<Any> readYson(YTBinder<Any> binder, String resource) {
        return getImpl(binder, resource, () -> Utils.fromYsonResource(resource, binder));
    }

    @SuppressWarnings("unchecked")
    private static <Any> List<Any> getImpl(Object mapper, Object key, Supplier<List<Any>> mappingFunction) {
        var map = CACHE.computeIfAbsent(mapper, m -> new ConcurrentHashMap<>());
        List<Any> list = (List<Any>) map.computeIfAbsent(key, r -> mappingFunction.get());
        return TmsTestUtils.map(list, YTreeDeepCopier::deepCopyOf);
    }

    @Configuration
    @PropertySource({
            "classpath:tms.properties",
            "classpath:unittest/tms.properties"
    })
    @Import({ConfigurationForTests.Services.class,
            ConfigurationForTests.UtilityMethods.class,
            ConfigurationForTests.Mds.class,
            ConfigurationForTests.Juggler.class,
            ConfigurationForTests.Exports.class,
            PostgresConfigurationForTests.class,
            ProcessingConfig.class,
            ConfigurationForTests.Quartz.class,
            ConfigurationForTests.MyDatabaseSchedulerFactoryConfig.class,
            QuartzJobs.class,
            ConfigurationForTests.YtConfiguration.class,
            SharedApi.class,
            ExecutorSources.class,
            InternalApi.class,
            TimingConfig.class,
            CoreJugglerApi.class,
            TmsJugglerApi.class,
            JugglerConfig.class,
            YqlConfig.class,
            AdvProgramTmsConfig.class
    })
    public static class TmsConfiguration {

        @Bean
        public YtCleanupContextListener ytCleanupTestListener(@Autowired ConfigurableApplicationContext context) {
            return new YtCleanupContextListener(context);
        }

        @Bean
        @DependsOn({"postgresLiquibase"})
        public Object bootstrap(@Qualifier("internalTableVersionService") TableVersionService tableVersionService) {
            ((TableServiceImpl) tableVersionService).clearTables();
            return "Test Bootstrap is complete";
        }
    }

}

package ru.yandex.direct.jobs.fatconfiguration;

import com.yandex.ydb.table.TableClient;
import org.jooq.impl.TableImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.jobs.configuration.JobsConfiguration;
import ru.yandex.direct.ytcomponents.config.DirectDynTablesMapping;
import ru.yandex.direct.ytcomponents.config.OverridableTableMappings;
import ru.yandex.direct.ytwrapper.client.TestYtClusterConfigProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.dynamic.TableMappings;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicConfig;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicTypesafeConfig;
import ru.yandex.direct.ytwrapper.dynamic.YtQueryComposer;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.direct.ytwrapper.utils.TableAvailabilityChecker;

import static ru.yandex.direct.configuration.YdbConfiguration.HOURGLASS_YDB_TABLE_CLIENT_BEAN;

@Configuration
@Import({
        JobsConfiguration.class,
        CoreTestingConfiguration.class
})
public class JobsFatTestingConfiguration {

    @Bean
    public YtDynamicConfig ytDynamicConfig(DirectConfig directConfig) {
        DirectConfig ytConfig = directConfig.getBranch("yt");
        return new YtDynamicTypesafeConfig(ytConfig.getConfig());
    }

    @Bean
    public OverridableTableMappings overridableTableMappings(DirectDynTablesMapping directDynTablesMapping) {
        return new OverridableTableMappings(directDynTablesMapping);
    }

    @Bean
    public TableMappings tableMappings(OverridableTableMappings overridableTableMappings) {
        return overridableTableMappings;
    }

    @Bean
    @Primary
    public TableAvailabilityChecker noopAvailabilityChecker(TableMappings tableMappings) {
        return new TableAvailabilityChecker(tableMappings) {
            @Override
            public boolean isTableAvailable(YtDynamicOperator ytDynamicOperator, TableImpl table) {
                // чтобы не сыпать логами про то, что нет таблички
                return true;
            }
        };
    }

    @Bean
    @Lazy
    public YtProvider ytProvider(TableMappings tableMappings, YtDynamicConfig dynConfig) {
        return new YtProvider(new TestYtClusterConfigProvider(), dynConfig, new YtQueryComposer(tableMappings, true));
    }

    @MockBean(name = HOURGLASS_YDB_TABLE_CLIENT_BEAN)
    public TableClient tableClient;
}

package ru.yandex.market.wms.radiator.test;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.wms.radiator.core.config.DataSourcesConfiguration;
import ru.yandex.market.wms.radiator.core.config.DateTimeConfiguration;
import ru.yandex.market.wms.radiator.core.config.SolomonClientConfiguration;
import ru.yandex.market.wms.radiator.core.config.TokenContextHolderConfiguration;
import ru.yandex.market.wms.radiator.core.config.WmsDataSourceTypeContextHolderConfiguration;
import ru.yandex.market.wms.radiator.core.config.properties.MutableWarehousesProperties;
import ru.yandex.market.wms.radiator.core.config.properties.WarehousePropertiesConfiguration;
import ru.yandex.market.wms.radiator.service.stocks.now.NowTimeService;

import static ru.yandex.market.request.trace.Module.DB_WMS_ROV;
import static ru.yandex.market.request.trace.Module.DB_WMS_SOF;

@Import({
        IntegrationTestDbConfiguration.class,
        MutableWarehousesProperties.class,
        WarehousePropertiesConfiguration.class,
        DataSourcesConfiguration.class,
//        XmlMappingConfiguration.class,
//        RequestProcessingConfiguration.class,
        TokenContextHolderConfiguration.class,
        WmsDataSourceTypeContextHolderConfiguration.class,
        SolomonClientConfiguration.class,
        DateTimeConfiguration.class
})
@Configuration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
})
public class IntegrationTestBackendConfiguration {

    @Bean
    @Primary
    public MutableWarehousesProperties testMutableWarehousesProperties() {
        return new MutableWarehousesProperties() {{
            warehouse.put(
                    IntegrationTestConstants.WH_1_KEY,
                    warehouse(
                            IntegrationTestConstants.WH_1_ID,
                            IntegrationTestConstants.WH_1_TOKEN,
                            dataSource(IntegrationTestConstants.WH_1_KEY, DB_WMS_ROV), 1
                    )
            );
            warehouse.put(
                    IntegrationTestConstants.WH_2_KEY,
                    warehouse(
                            IntegrationTestConstants.WH_2_ID,
                            IntegrationTestConstants.WH_2_TOKEN,
                            dataSource(IntegrationTestConstants.WH_2_KEY, DB_WMS_SOF), 2
                    )
            );
        }};
    }

    private MutableWarehousesProperties.Warehouse warehouse(
            String id, String token, MutableWarehousesProperties.DataSource datasource, int redisDb)
    {
        MutableWarehousesProperties.Warehouse warehouse = new MutableWarehousesProperties.Warehouse();
        warehouse.setId(id);
        warehouse.setToken(token);
        warehouse.setDatasource(datasource);
        warehouse.setRedis(redis(redisDb));
        return warehouse;
    }

    private static MutableWarehousesProperties.DataSource dataSource(String key, Module module) {
        MutableWarehousesProperties.DataSource dataSource = new MutableWarehousesProperties.DataSource();
        dataSource.setDriverClassName(java.util.Optional.of("org.h2.Driver"));
        dataSource.setUrl("jdbc:h2:mem:" + key + ";MODE=MSSQLServer");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setModuleName(module.name());
        return dataSource;
    }

    private static MutableWarehousesProperties.Redis redis(int db) {
        MutableWarehousesProperties.Redis redis = new MutableWarehousesProperties.Redis();
        redis.setDb(db);
        redis.setHost("localhost");
        redis.setSentinels(Set.of());
        return redis;
    }

    @Bean
    @Primary
    public NowTimeService testNowTimeService() {
        return () -> IntegrationTestConstants.DATE_TIME;
    }


    @Bean
    public Executor executor() {
        return Runnable::run;
    }

    @MockBean
    public Yt yt;
}

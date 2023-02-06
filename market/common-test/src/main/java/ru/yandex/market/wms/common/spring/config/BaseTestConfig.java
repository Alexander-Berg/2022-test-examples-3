package ru.yandex.market.wms.common.spring.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.service.DimensionsService;
import ru.yandex.market.wms.common.service.ShelfLifeService;
import ru.yandex.market.wms.common.service.validation.rule.shelflife.dates.CreationDateTimeIsLessThanExpirationDateTimeValidationRule;
import ru.yandex.market.wms.common.service.validation.rule.shelflife.dates.CreationDateTimeIsNotInFutureValidationRule;
import ru.yandex.market.wms.common.service.validation.rule.shelflife.dates.CreationDateTimeIsNotTooOldValidationRule;
import ru.yandex.market.wms.common.service.validation.rule.shelflife.dates.ShelfLifeIsNotTooBigValidationRule;
import ru.yandex.market.wms.common.spring.dao.implementation.PackDaoImpl;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;
import ru.yandex.market.wms.trace.Module;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@Configuration
@Profile({Profiles.TEST})
public class BaseTestConfig {

    public DatabaseDataSourceConnection dbUnitDatabaseConnection(String schemaName, DataSource dataSource) {
        DatabaseConfigBean dbConfig = new DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new H2DataTypeFactory());
        dbConfig.setAllowEmptyFields(true);

        DatabaseDataSourceConnectionFactoryBean dbConnectionFactory =
                new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnectionFactory.setSchema(schemaName);
        dbConnectionFactory.setDatabaseConfig(dbConfig);

        try {
            return dbConnectionFactory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
    }

    @Bean
    DbConfigService dbConfigService(NSqlConfigDao nSqlConfigDao) {
        DbConfigService spy = Mockito.spy(new DbConfigService(nSqlConfigDao));
        Mockito.doReturn("99").when(spy).getConfig(eq("WAREHOUSE_PREFIX"), anyString());
        return spy;
    }

    @Bean
    public UuidGenerator uuidGenerator() {
        return new FixedUuidGenerator("6d809e60-d707-11ea-9550-a9553a7b0571");
    }

    @Bean
    public ShelfLifeService shelfLifeService(DbConfigService dbConfigService,
                                             Clock clock) {
        return new ShelfLifeService(dbConfigService, clock, Arrays.asList(
                new CreationDateTimeIsLessThanExpirationDateTimeValidationRule(),
                new CreationDateTimeIsNotInFutureValidationRule(clock),
                new CreationDateTimeIsNotTooOldValidationRule(clock),
                new ShelfLifeIsNotTooBigValidationRule()
        ));
    }

    @Bean
    public DimensionsService dimensionsService(@Qualifier("packDao") PackDaoImpl packDao,
                                               @Qualifier("enterprisePackDao") PackDaoImpl enterprisePackDao,
                                               @Qualifier("skuDao") SkuDaoImpl skuDao,
                                               @Qualifier("enterpriseSkuDao") SkuDaoImpl enterpriseSkuDao) {
        return new DimensionsService(packDao, enterprisePackDao, skuDao, enterpriseSkuDao);
    }

    @ConditionalOnMissingBean(Module.class)
    @Bean(name = "applicationModuleName")
    public Module applicationModuleName() {
        return Module.DATA_CREATOR;
    }
}

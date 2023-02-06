package ru.yandex.market.billing.tasks.shopdata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.core.environment.DBEnvironmentService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.lock.DbmsLockService;
import ru.yandex.market.mbi.lock.LockService;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ShopDataManualTest.Config.class)
class ShopDataManualTest {

    @Autowired
    @Qualifier("shopDataExecutor")
    ShopDataExecutor shopDataExecutor;

    @Test
    void test() {
        shopDataExecutor.doJob(null);
    }

    @Configuration
    @Import(ShopDataJobConfig.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new SingleConnectionDataSource(
                    "jdbc:oracle:thin:@dev.billing.yandex.ru",
                    "sysdev", "sysdev", true
            );
        }

        @Bean({"readOnlyJdbcTemplate", "jdbcTemplate"})
        JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(dataSource());
        }

        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        TransactionTemplate transactionTemplate() {
            return new TransactionTemplate(platformTransactionManager());
        }

        @Bean
        NamedHistoryMdsS3Client namedHistoryMdsS3Client() {
            return new LocalFsS3Client(System.getProperty("java.io.tmpdir"));
        }

        @Bean
        LockService lockService() {
            return new DbmsLockService(jdbcTemplate());
        }

        @Bean
        EnvironmentService environmentService() {
            return new DBEnvironmentService(jdbcTemplate(), transactionTemplate(), lockService());
        }
    }

    static class LocalFsS3Client implements NamedHistoryMdsS3Client {

        private final String path;

        LocalFsS3Client(String path) {
            this.path = path;
        }

        @Nonnull
        @Override
        public ResourceLocation upload(@Nonnull String configurationName, @Nonnull ContentProvider source) {
            Path location = Paths.get(path);
            location = location.resolve(configurationName);
            try {
                Files.copy(source.getInputStream(), location);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            System.out.println(location);
            return ResourceLocation.create(path, configurationName);
        }

        @Nonnull
        @Override
        public <D> D downloadLast(@Nonnull String configurationName, @Nonnull ContentConsumer<D> destination) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public ResourceListing deleteOld(@Nonnull String configurationName) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public URL getUrl(@Nonnull ResourceLocation location) {
            throw new UnsupportedOperationException();
        }

    }
}

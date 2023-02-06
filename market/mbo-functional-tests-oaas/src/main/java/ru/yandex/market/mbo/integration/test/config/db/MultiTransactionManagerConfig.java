package ru.yandex.market.mbo.integration.test.config.db;

import java.util.List;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Transaction manager which concatenates multiple transaction managers in one.
 * Useful in tests to rollback changes in several DataBases simultaneously via
 * {@link org.springframework.transaction.annotation.Transactional}.
 *
 * @author s-ermakov
 */
@Configuration
public class MultiTransactionManagerConfig {

    @Primary
    @Bean(name = "multiTransactionManager")
    public ChainedTransactionManager multiTransactionManager(
        List<? extends PlatformTransactionManager> allTransactionManagers
    ) {
        // Should filter not configured transaction managers.
        List<? extends PlatformTransactionManager> filteredTransactionManagers = allTransactionManagers.stream()
            .map(tm -> (DataSourceTransactionManager) tm)
            .filter(tm -> tm.getDataSource() != null)
            .filter(tm -> tm.getDataSource() instanceof HikariDataSource
                && !StringUtils.isEmpty(((HikariDataSource) tm.getDataSource()).getJdbcUrl()))
            .collect(Collectors.toList());
        PlatformTransactionManager[] array = filteredTransactionManagers.toArray(new PlatformTransactionManager[0]);
        return new ChainedTransactionManager(array);
    }
}

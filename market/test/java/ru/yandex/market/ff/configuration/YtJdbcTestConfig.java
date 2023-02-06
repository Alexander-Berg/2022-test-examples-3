package ru.yandex.market.ff.configuration;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.ff.util.YqlOverPgUtils;

@Configuration
public class YtJdbcTestConfig {

    @Bean
    @Qualifier("yqlDataSource")
    public DataSource yqlDataSource(@Qualifier("dataSource") DataSource dataSource) {
        return ProxyDataSourceBuilder
                .create(dataSource)
                .queryTransformer(transformInfo -> YqlOverPgUtils.convertYqlToPgSql(transformInfo.getQuery()))
                .build();
    }

    @Bean
    @Qualifier("yqlAutoClusterNamedJdbcTemplate")
    public NamedParameterJdbcTemplate yqlAutoClusterNamedJdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        var namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(yqlDataSource(dataSource));
        return namedParameterJdbcTemplate;
    }
}

package ru.yandex.market.config.yt;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

@Configuration
public class DatasourceConfig {

    private static final int YQL_SYNTAX_VERSION = 1;
    private static final int CONNECTION_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(20);

    @Value("${mbi_billing.hive.username}")
    private String yqlUser;

    @Value("${mbi.robot.yql.token}")
    private String yqlToken;

    @Value("${yql.jdbc.url}")
    private String yqlHost;

    @Value("${mbi.yt.host}")
    private String ytCluster;


    @Bean
    public Yt yt() {
        YtConfiguration configuration = YtConfiguration.builder()
                .withUser(yqlUser)
                .withApiHost(ytCluster)
                .withToken(yqlToken)
                .build();

        return YtUtils.http(configuration);
    }

    @Bean
    public DataSource yqlDataSource() {
        final YqlProperties properties = new YqlProperties();

        properties.setUser(yqlUser);
        properties.setPassword(yqlToken);
        properties.setSyntaxVersion(YQL_SYNTAX_VERSION);
        properties.setConnectionTimeout(CONNECTION_TIMEOUT);

        return new YqlDataSource(yqlHost, properties);
    }

    @Bean
    public NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(yqlDataSource());
    }
}

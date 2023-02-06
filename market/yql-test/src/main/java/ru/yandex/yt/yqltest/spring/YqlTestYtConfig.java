package ru.yandex.yt.yqltest.spring;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;
import ru.yandex.yt.yqltest.YqlTestRunner;
import ru.yandex.yt.yqltest.YtClient;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
@Configuration
public class YqlTestYtConfig {

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_READ_TIMEOUT_MS = 60000;


    @Value("${USE_YT_RECIPE:false}")
    private boolean useRecipe;

    @Bean
    @Qualifier("ytDataSource")
    public DataSource yqlDatasource(@Value("${yqltest.jdbc.url}") String url,
                                    @Value("${yqltest.recipe.jdbc.url}") String urlRecipe,
                                    @Value("${yqltest.jdbc.username}") String user,
                                    @Value("${yqltest.yt.token}") String password) {
        String targetUrl = useRecipe ? urlRecipe : url;

        YqlProperties yqlProperties = new YqlProperties();
        yqlProperties.setUser(user);
        yqlProperties.setPassword(password);

        return new YqlDataSource(targetUrl, yqlProperties);
    }

    @Bean
    @Qualifier("yqlJdbcTemplate")
    public JdbcTemplate yqlJdbcTemplate(@Qualifier("ytDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public YtClient ytClient(@Value("${yqltest.yt.token}") String oAuthToken,
                             @Value("${yqltest.yt.host}") String ytHost,
                             @Value("${yqltest.recipe.yt.host}") String ytHostRecipe) {
        String targetHost = useRecipe ? ytHostRecipe : ytHost;

        YtConfiguration.Builder configuration = YtConfiguration.builder()
            .withApiHost(targetHost)
            .withToken(oAuthToken)
            .withSimpleCommandsRetries(DEFAULT_RETRY_COUNT)
            .withHeavyCommandsRetries(DEFAULT_RETRY_COUNT)
            .withHeavyCommandsTimeout(Duration.of(DEFAULT_READ_TIMEOUT_MS, ChronoUnit.MILLIS));

        return new YtClient(Yt.builder(configuration.build())
            .http()
            .build());
    }

    @Bean
    public YqlTestRunner yqlTestRunner(YtClient ytClient,
                                       @Qualifier("yqlJdbcTemplate") JdbcTemplate yqlJdbcTemplate,
                                       @Value("${yqltest.base.path}") String basePath,
                                       @Value("${yqltest.cleanAfterRun:true}") boolean cleanAfterRun) {
        YqlTestRunner result = new YqlTestRunner(ytClient, yqlJdbcTemplate, basePath);
        result.setCleanAfterRun(cleanAfterRun);
        return result;
    }

}

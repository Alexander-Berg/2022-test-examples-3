package ru.yandex.market.core.config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.core.fulfillment.report.OrderReportYtDao;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

@Configuration
@Profile("functionalTest")
public class StatisticsReportConfigTest {
    private final String mbiYtToken = "unused";
    private final String statisticsReportClasterUrl = "hahn.yt.yandex.net";
    private final String yqlJdbcUrl = "jdbc:yql://yql.yandex.net:443/hahn";
    private final String statisticsReportFolder = "//home/market/development/mbi/billing/statistics-report";

    @Bean
    @Qualifier("statisticsReportYt")
    public Yt statisticsReportYt() {
        return YtUtils.http(Objects.requireNonNull(statisticsReportClasterUrl), mbiYtToken);
    }

    @Bean
    public NamedParameterJdbcTemplate statisticsReportYqlNamedParameterJdbcTemplate() {
        final YqlProperties properties = new YqlProperties();
        properties.setPassword(mbiYtToken);
        properties.setSyntaxVersion(1);
        final YqlDataSource dataSource = new YqlDataSource(yqlJdbcUrl, properties);
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @Qualifier("orderReportYtDao")
    public OrderReportYtDao orderReportYtDao() {
        return new OrderReportYtDao(statisticsReportYqlNamedParameterJdbcTemplate(), statisticsReportYt(), statisticsReportFolder);
    }
}

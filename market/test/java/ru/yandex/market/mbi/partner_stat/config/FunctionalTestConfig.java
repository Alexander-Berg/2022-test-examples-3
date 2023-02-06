package ru.yandex.market.mbi.partner_stat.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.mbi.partner_stat.repository.expenses.ExpenseStatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatDictionaryClickHouseDao;
import ru.yandex.market.mbi.partner_stat.service.stat.StatService;
import ru.yandex.market.mbi.partner_stat.yt.TestYQLReader;
import ru.yandex.market.mbi.partner_stat.yt.TestYQLReaderDataSupplier;

/**
 * Конфигурация функциональных тестов.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@Configuration
@ParametersAreNonnullByDefault
public class FunctionalTestConfig {
    public static final Instant TEST_CLOCK_TIME =
            LocalDateTime.of(2021, 2, 3, 12, 0, 0)
                    .atZone(ZoneId.systemDefault()).toInstant();

    @Value("${mbi.partner_stat.yql.token:}")
    private String yqlToken;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        final var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public StatClickHouseDao statClickHouseDao() {
        return Mockito.mock(StatClickHouseDao.class);
    }

    @Bean
    public ExpenseStatClickHouseDao expenseStatClickHouseDao() {
        return Mockito.mock(ExpenseStatClickHouseDao.class);
    }

    @Bean
    public StatDictionaryClickHouseDao statDictionaryClickHouseDao() {
        return Mockito.mock(StatDictionaryClickHouseDao.class);
    }

    @Bean
    public StatService statService() {
        return new StatService(statClickHouseDao(), expenseStatClickHouseDao(), statDictionaryClickHouseDao(), clock());
    }

    @Bean
    public NamedParameterJdbcTemplate yqlJdbcTemplate() {
        return Mockito.mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public Clock clock() {
        return Clock.fixed(TEST_CLOCK_TIME, ZoneId.systemDefault());
    }

    @Bean
    public TestYQLReaderDataSupplier testYQLReaderDataSupplier() {
        return Mockito.spy(new TestYQLReaderDataSupplier());
    }

    @Bean
    public TestYQLReader yqlReader() {
        return new TestYQLReader(testYQLReaderDataSupplier());
    }

    @Bean
    public Yt yt() {
        String ytProxy = System.getenv("YT_PROXY");
        return YtUtils.http(ytProxy, yqlToken);
    }
}

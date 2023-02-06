package ru.yandex.market.mbo.utils;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author s-ermakov
 */
public class PostgresSQLBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, String beanName) throws BeansException {
        if (bean instanceof JdbcTemplate && Objects.equals(beanName, "postgresJdbcTemplate")) {
            JdbcTemplate jdbcTemplate = (JdbcTemplate) bean;
            // MBO-17115
            // Создаем C.UTF-8 правило сортировки, так как в embedded Postgres его нет
            // Следующие важные вещи:
            // - скорее всего это правило сортировки будет отличаться от того, что на продакшене,
            // поэтому полагаться на его корректность не стоит
            // - правило должно быть создано в той же схеме, что и выполняются запросы, иначе его система "не видит"
            // - если какой-то тест снова начал падать, то можно попробовать использовать
            // другие правила сортировки в качестве базовых
            if (SystemUtils.IS_OS_MAC_OSX) {
                jdbcTemplate.execute("CREATE COLLATION IF NOT EXISTS \"C.UTF-8\" FROM \"ru_RU.UTF-8\";");
            } else if (SystemUtils.IS_OS_WINDOWS) {
                jdbcTemplate.execute("CREATE COLLATION IF NOT EXISTS \"C.UTF-8\" FROM \"ucs_basic\";");
            }
        }
        return bean;
    }
}

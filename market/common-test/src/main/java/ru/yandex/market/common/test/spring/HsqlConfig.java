package ru.yandex.market.common.test.spring;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.market.common.test.jdbc.HsqlSqlTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Базовый конфиг для тестов, использующих базу HSQL.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public abstract class HsqlConfig extends DbUnitTestConfig {

    @Override
    @Nonnull
    protected EmbeddedDatabaseType databaseType() {
        return EmbeddedDatabaseType.HSQL;
    }

    @Override
    @Nonnull
    protected StringTransformer createSqlTransformer() {
        return new HsqlSqlTransformer();
    }
}

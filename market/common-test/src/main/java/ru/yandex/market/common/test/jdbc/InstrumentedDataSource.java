package ru.yandex.market.common.test.jdbc;


import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ru.yandex.market.common.test.transformer.CompositeStringTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Проксирующий датасурс, оборачивающий подключения к базе данных в {@link InstrumentedConnection}.
 * Используется для создания датасурсов для юнит-тестов
 *
 * @author zoom
 */
public class InstrumentedDataSource extends SimpleDriverDataSource {

    private StringTransformer stringTransformer;

    public InstrumentedDataSource(StringTransformer stringTransformer) {
        this.stringTransformer = stringTransformer;
    }

    public void withExtraTransformer(StringTransformer extraTransformer, Runnable callback) {
        final StringTransformer oldValue = this.stringTransformer;
        try {
            this.stringTransformer = new CompositeStringTransformer() {
                @Override
                protected void customizeTransformers(final List<StringTransformer> transformers) {
                    transformers.add(extraTransformer);
                    transformers.add(oldValue);
                }
            };
            callback.run();
        } finally {
            this.stringTransformer = oldValue;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new InstrumentedConnection(super.getConnection(), stringTransformer);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new InstrumentedConnection(super.getConnection(username, password), stringTransformer);
    }
}

package ru.yandex.market.sqb.test.db.misc;

import javax.annotation.Nonnull;

import org.dbunit.DefaultOperationListener;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;

/**
 * DBUnit {@link DefaultOperationListener}, который позволяет использовать множественные схемы.
 *
 * @author Vladislav Bauer
 */
public class CustomOperationLister extends DefaultOperationListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionRetrieved(@Nonnull final IDatabaseConnection connection) {
        super.connectionRetrieved(connection);

        try {
            final DatabaseConfig config = connection.getConfig();
            config.setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, Boolean.TRUE);
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, Boolean.TRUE);
            config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, Boolean.FALSE);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}

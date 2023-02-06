package ru.yandex.market.checkout.checkouter.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
public class MigrateMultiOrderIdCommandTest extends AbstractServicesTestBase {

    private static final String INSERT_ORDER_PROPERTY_SQL =
            "INSERT INTO ORDER_PROPERTY (ORDER_ID, NAME, TEXT_VALUE) VALUES (?,?,?)";

    private static final String FIND_ALL_DIFFERENT_MULTI_ORDER_IDS_WITH_MRID = "SELECT " +
            "       array_agg(a1.order_id ORDER BY a1.order_id) AS order_ids," +
            "       array_agg(a1.TEXT_VALUE ORDER BY a1.order_id) AS multi_order_ids, " +
            "       a2.TEXT_VALUE            AS mrid " +
            "FROM (SELECT ORDER_ID, TEXT_VALUE FROM ORDER_PROPERTY WHERE NAME = 'multiOrderId') a1" +
            "         JOIN" +
            "         (SELECT ORDER_ID, TEXT_VALUE FROM ORDER_PROPERTY WHERE NAME = 'mrid') a2" +
            "         ON a1.ORDER_ID = a2.ORDER_ID " +
            "GROUP BY mrid " +
            "HAVING count(DISTINCT a1.TEXT_VALUE) > 1";
    @Autowired
    private MigrateMultiOrderIdCommand migrateMultiOrderIdCommand;
    @Autowired
    private OrderInsertHelper orderInsertHelper;

    @Test
    public void testEmptyBase() {
        TestTerminal testTerminal = new TestTerminal(new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream());
        migrateMultiOrderIdCommand.executeCommand(null, testTerminal);

        List<Map<String, Object>> maps = masterJdbcTemplate.
                queryForList(FIND_ALL_DIFFERENT_MULTI_ORDER_IDS_WITH_MRID);
        assertTrue(maps.isEmpty());
    }

    @Test
    public void testFillBase() throws IllegalAccessException {
        transactionTemplate.execute(status -> {
            // Null case
            orderInsertHelper.insertOrder(0L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    0L, "mrid", "1111");
            orderInsertHelper.insertOrder(1L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    1L, "mrid", "2222");

            // Correct 1 record
            orderInsertHelper.insertOrder(2L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    2L, "mrid", "3333");
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    2L, "multiOrderId", "aaaa");

            // Incorrect 3 records
            orderInsertHelper.insertOrder(3L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    3L, "mrid", "4444");
            orderInsertHelper.insertOrder(4L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    4L, "mrid", "4444");
            orderInsertHelper.insertOrder(5L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    5L, "mrid", "4444");

            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    3L, "multiOrderId", "bbbb");
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    4L, "multiOrderId", "cccc");
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    5L, "multiOrderId", "dddd");

            // Correct 2 records
            orderInsertHelper.insertOrder(6L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    6L, "mrid", "5555");
            orderInsertHelper.insertOrder(7L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    7L, "mrid", "5555");

            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    6L, "multiOrderId", "eeee");

            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    7L, "multiOrderId", "eeee");

            // Incorrect 2 records
            orderInsertHelper.insertOrder(8L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    8L, "mrid", "6666");
            orderInsertHelper.insertOrder(9L, OrderProvider.getBlueOrder());
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    9L, "mrid", "6666");

            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    8L, "multiOrderId", "ffff");
            masterJdbcTemplate.update(INSERT_ORDER_PROPERTY_SQL,
                    9L, "multiOrderId", "gggg");

            return null;
        });

        TestTerminal testTerminal = new TestTerminal(new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream());
        List<Map<String, Object>> badOrdersBeforeMigration = masterJdbcTemplate.
                queryForList(FIND_ALL_DIFFERENT_MULTI_ORDER_IDS_WITH_MRID);
        if (badOrdersBeforeMigration.size() != 2) {
            throw new IllegalAccessException("Bad Input, expect 2 records");
        }

        migrateMultiOrderIdCommand.executeCommand(null, testTerminal);

        List<Map<String, Object>> badOrdersAfterMigration = masterJdbcTemplate.
                queryForList(FIND_ALL_DIFFERENT_MULTI_ORDER_IDS_WITH_MRID);
        assertTrue(badOrdersAfterMigration.isEmpty());
    }
}

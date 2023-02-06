package ru.yandex.market.abo.core.shopdata.clch;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.core.shopdata.clch.ShopDataCheckerLoader.HANDLER_BEAN_NAME;
import static ru.yandex.market.abo.core.shopdata.clch.ShopDataTypeDbLoader.SHOP_COLUMN;
import static ru.yandex.market.abo.core.shopdata.clch.ShopDataTypeDbLoader.VALUE_COLUMN;

/**
 * @author artemmz
 * @date 08.11.17.
 */
class ShopDataCheckerLoaderDbTest extends EmptyTest {
    Set<String> NEEDED_COLUMNS = Set.of(SHOP_COLUMN, VALUE_COLUMN);

    @Autowired
    private ShopDataCheckerLoader shopDataCheckerLoader;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testQueries() {
        List<ShopDataTypeChecker> checkers = shopDataCheckerLoader.loadCheckers();
        assertFalse(checkers.isEmpty());
        checkers.forEach(ch -> ch.findAlikeShops(774L));
    }

    @Test
    void testQueriesColumnNames() {
        List<String> clchQueries = jdbcTemplate.queryForList("SELECT QUERY FROM SD_CHECKER_QUERY WHERE ACTIVE " +
                "and query not like '" + HANDLER_BEAN_NAME + "%'", String.class);

        StreamEx.of(clchQueries)
                .mapToEntry(Function.identity(), String::toLowerCase)
                .mapValues(query -> StringUtils.substringBetween(query, "select", "from"))
                .forKeyValue((query, selectPart) -> assertTrue(NEEDED_COLUMNS.stream().allMatch(selectPart::contains),
                        query + " does not contain " + NEEDED_COLUMNS));
    }
}
package ru.yandex.market.billing.fulfillment.stocks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.StockDao;
import ru.yandex.market.core.fulfillment.model.ShopSkuInfo;
import ru.yandex.market.terminal.TestTerminal;

import static ru.yandex.market.billing.fulfillment.stocks.ImportAllShopSkuCommand.OPTION_CHUNK;
import static ru.yandex.market.billing.fulfillment.stocks.ImportAllShopSkuCommand.OPTION_CLUSTER;
import static ru.yandex.market.billing.fulfillment.stocks.ImportAllShopSkuCommand.OPTION_TABLE;
import static ru.yandex.market.billing.fulfillment.stocks.ImportAllShopSkuCommand.OPTION_THREADS;
import static ru.yandex.market.core.fulfillment.StockDao.shopSkuInfoRowMapper;

@Disabled("только для локального запуска")
public class ImportAllShopSkuCommandTest extends FunctionalTest {

    private static final String OPTION_VALUE_CLUSTER = "hahn";
    private static final String OPTION_VALUE_TABLE = "//home/market/users/mexicano/sku_info_2022_04_25_limited_300000";
    private static final String OPTION_VALUE_THREADS = "10";
    private static final String OPTION_VALUE_CHUNK = "10000";

    private static final String GET_SHOP_SKU_INFOS = /*language=sql*/ "" +
            "select " +
            "   supplier_id, " +
            "   shop_sku, " +
            "   length, " +
            "   width, " +
            "   height, " +
            "   weight " +
            "from shops_web.shop_sku_info";

    @Autowired
    ImportAllShopSkuCommand command;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    StockDao stockDao;

    @Test
    @DbUnitDataSet(before = "ImportAllShopSkuCommandTest.simple.before.csv")
    public void simple() {
        CommandInvocation commandInvocation = new CommandInvocation("", new String[0],
                Map.of(OPTION_CLUSTER, OPTION_VALUE_CLUSTER,
                        OPTION_TABLE, OPTION_VALUE_TABLE,
                        OPTION_THREADS, OPTION_VALUE_THREADS,
                        OPTION_CHUNK, OPTION_VALUE_CHUNK));
        command.executeCommand(commandInvocation, new TestTerminal());

        List<ShopSkuInfo> allShopSkus = getShopSkuInfos();
        Optional<ShopSkuInfo> modified = stockDao.getShopSkuInfo(465232L, "1076314");
        Optional<ShopSkuInfo> unmodified = stockDao.getShopSkuInfo(1L, "1");
        System.out.println();
    }

    public List<ShopSkuInfo> getShopSkuInfos() {
        return namedParameterJdbcTemplate.query(
                GET_SHOP_SKU_INFOS,
                new MapSqlParameterSource(),
                (rs, rowNum) -> shopSkuInfoRowMapper(rs)
        );
    }
}

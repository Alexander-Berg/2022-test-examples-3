package ru.yandex.market.billing.imports.shopsku;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.TestTerminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_CHUNK;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_CLUSTER;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_TABLE;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_THREADS;
import static ru.yandex.market.billing.imports.shopsku.ShopSkuInfoDao.shopSkuInfoRowMapper;

@Disabled("только для локального запуска")
public class ImportAllShopSkuCommandDebugTest extends FunctionalTest {

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
    CommandExecutor commandExecutor;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    ShopSkuInfoDao shopSkuInfoDao;

    @Autowired
    String ytToken;

    ImportAllShopSkuCommand command;

    @BeforeEach
    public void beforeEach() {
        Yt hahnYt = YtUtils.http("hahn.yt.yandex.net", ytToken);
        command = new ImportAllShopSkuCommand(hahnYt, null, shopSkuInfoDao, commandExecutor);
    }

    @Test
    @DbUnitDataSet(before = "ImportAllShopSkuCommandDebugTest.debug.before.csv")
    public void debug() {
        command.executeCommand(createCommandInvocation(), new TestTerminal());

        List<ShopSkuInfo> allShopSkus = getShopSkuInfos();
        Map<SupplierShopSkuKey<SupplierShopSkuKey>, ShopSkuInfo> allShopSkusMap = StreamEx.of(allShopSkus)
                .mapToEntry(i -> new SupplierShopSkuKey<>(i.getSupplierId(), i.getShopSku()), Function.identity())
                .toMap();
        ShopSkuInfo modified = allShopSkusMap.get(new SupplierShopSkuKey<>(465232, "1076314"));
        ShopSkuInfo unmodified = allShopSkusMap.get(new SupplierShopSkuKey<>(1, "1"));
        System.out.println();
    }

    private CommandInvocation createCommandInvocation() {
        return new CommandInvocation("", new String[0],
                Map.of(OPTION_CLUSTER, OPTION_VALUE_CLUSTER,
                        OPTION_TABLE, OPTION_VALUE_TABLE,
                        OPTION_THREADS, OPTION_VALUE_THREADS,
                        OPTION_CHUNK, OPTION_VALUE_CHUNK));
    }

    private List<ShopSkuInfo> getShopSkuInfos() {
        return namedParameterJdbcTemplate.query(
                GET_SHOP_SKU_INFOS,
                new MapSqlParameterSource(),
                (rs, rowNum) -> shopSkuInfoRowMapper(rs)
        );
    }
}

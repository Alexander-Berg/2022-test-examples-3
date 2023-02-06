package ru.yandex.market.fintech.fintechutils.service.yt;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.helpers.yt.IteratorProvidedConsumerTables;
import ru.yandex.market.fintech.fintechutils.helpers.yt.YTreeMapNodeBuilder;
import ru.yandex.market.fintech.fintechutils.service.yt.util.YtConverters;


class YtImportServiceTest extends AbstractFunctionalTest {

    private static final List<YTreeMapNode> data = List.of(
            new YTreeMapNodeBuilder()
                    .addLong("order_delivery_region_id", 3)
                    .addString("order_delivery_region_province_name", "Замоскворечье")
                    .addString("order_delivery_region_federal_district_name", "Москва")
                    .build(),
            new YTreeMapNodeBuilder()
                    .addLong("order_delivery_region_id", 4)
                    .addString("order_delivery_region_province_name", "Обухово")
                    .addString("order_delivery_region_federal_district_name", "Санкт-Петербург")
                    .build(),
            new YTreeMapNodeBuilder()
                    .addLong("order_delivery_region_id", 5)
                    .addString("order_delivery_region_province_name", "Екатеринбург")
                    .addString("order_delivery_region_federal_district_name", "Уральский федеральный округ")
                    .build(),
            new YTreeMapNodeBuilder()
                    .addLong("order_delivery_region_id", 6)
                    .addString("order_delivery_region_province_name", "Плаза")
                    .addString("order_delivery_region_federal_district_name", "Лотте")
                    .build()
    );

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private YtImportService importService;

    @BeforeEach
    void setUp() {
        Yt yt = Mockito.mock(Yt.class);
        YtTables iteratorTables = new IteratorProvidedConsumerTables(data);
        Mockito.doReturn(iteratorTables).when(yt).tables();
        importService = new YtImportServiceImpl(yt, jdbcTemplate, transactionTemplate, 3);
    }

    @Test
    @DbUnitDataSet(
            after = "YtLowConsistencyCacheTest.after.csv"
    )
    void testImportWithTruncate() {
        importService.truncateAndRefillTableData(
                YPath.cypressRoot(),
                "fintech_utils.region_id_district_province",
                YtConverters.REGION_ID_DISTRICT_PROVINCE_CONVERTER
        );
    }

    @Test
    @DbUnitDataSet(
            after = "YtLowConsistencyCacheTest.after.csv"
    )
    void testImportWithUpsert() {
        importService.upsertTableData(
                YPath.cypressRoot(),
                "fintech_utils.region_id_district_province",
                "region_id",
                YtConverters.REGION_ID_DISTRICT_PROVINCE_CONVERTER
        );
    }

}

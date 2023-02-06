package ru.yandex.market.mboc.common.erp;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.erp.model.ErpLogisticsMasterData;
import ru.yandex.market.mboc.common.erp.model.ImportStatus;

/**
 * @author amaslak
 * @noinspection Duplicates, ConstantConditions
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpLogisticsMasterDataExporterDaoTest {

    private JdbcTemplate jdbcTemplate;

    private ErpLogisticsMasterDataExporterDao erpExporter;

    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() {
        String dbName = getClass().getSimpleName() + UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:erp/erp.sql'" +
                ";MODE=MSSQLServer"
        );

        jdbcTemplate = new JdbcTemplate(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        erpExporter = new ErpLogisticsMasterDataExporterDaoImpl(jdbcTemplate, transactionTemplate::execute);

        enhancedRandom = new EnhancedRandomBuilder()
            .seed(293)
            .stringLengthRange(0, 20)
            .build();
    }

    @Test
    public void whenExportingEmptyMasterDataCollectionShouldNotFail() {
        //noinspection CodeBlock2Expr
        Assertions.assertThatCode(() -> {
            erpExporter.insertSupply(new ArrayList<>());
        }).doesNotThrowAnyException();

        int count = jdbcTemplate.queryForObject("select count(*) from mbossku_supply", Integer.class);
        Assertions.assertThat(count).isEqualTo(0);
    }

    @Test
    public void whenExportingMasterDataShouldInsertEachFieldCorrectly() {
        List<ErpLogisticsMasterData> dataset = enhancedRandom.objects(ErpLogisticsMasterData.class, 1000)
            .collect(Collectors.toList());

        erpExporter.insertSupply(dataset);

        int count = jdbcTemplate.queryForObject("select count(*) from mbossku_supply", Integer.class);

        Assertions.assertThat(count).isEqualTo(dataset.size());

        jdbcTemplate.query("select * from mbossku_supply order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingle(dataset.get(rowNum), rs);
            return null;
        });
    }

    private void checkSingle(final ErpLogisticsMasterData c, final ResultSet rs) {
        SoftAssertions.assertSoftly(s -> {

            var assertThat = s.assertThat(Collections.singletonList(rs));

            assertThat.extracting(r -> {
                r.getInt("id");
                return r.wasNull();
            }).containsOnly(Boolean.FALSE);

            assertThat.extracting(r -> {
                r.getInt("import_ts");
                return r.wasNull();
            }).containsOnly(Boolean.TRUE);

            assertThat.extracting(r -> r.getString("RS_ID")).containsOnly(c.getRealSupplierId());
            assertThat.extracting(r -> r.getString("SSKU")).containsOnly(c.getSskuId());
            assertThat.extracting(r -> r.getInt("SHIPMENT_QUANTUM")).containsOnly(c.getShipmentQuantum());
            assertThat.extracting(r -> r.getInt("MIN_SHIPMENT")).containsOnly(c.getMinShipment());
            assertThat.extracting(r -> r.getInt("DELIVERY_TIME")).containsOnly(c.getDeliveryTime());
            assertThat.extracting(r -> r.getInt("WAREHOUSE_ID")).containsOnly(c.getWarehouseId());
            assertThat.extracting(r -> r.getString("CALENDAR_ID")).containsOnly(c.getCalendarId());
            assertThat.extracting(r -> r.getInt("QTY_IN_PACK")).containsOnly(c.getQuantityInPack());

            assertThat.extracting(r -> (Date) r.getTimestamp("mod_ts"))
                .usingElementComparator(Comparator.comparing(Date::getTime))
                .containsOnly(c.getModificationTs());
            assertThat.extracting(r -> (Date) r.getTimestamp("export_ts"))
                .usingElementComparator(Comparator.comparing(Date::getTime))
                .containsOnly(c.getExportTs());
            assertThat.extracting(r -> ImportStatus.forId(r.getInt("import_status"))).containsOnly(c.getStatus());

        });

    }

}

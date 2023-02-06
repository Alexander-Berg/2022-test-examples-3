package ru.yandex.market.mbo.erp.dao;

import com.google.common.base.Strings;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.conf.databases.ErpDbConfig;
import ru.yandex.market.mbo.erp.model.ErpCategory;
import ru.yandex.market.mbo.erp.model.ErpSku;
import ru.yandex.market.mbo.erp.model.ImportStatus;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpExporterSqlTest {

    private JdbcTemplate jdbcTemplate;
    private ErpExporter erpExporter;

    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() {
        ErpDbConfig config = new ErpDbConfig();

        String dbName = getClass().getSimpleName() + UUID.randomUUID().toString();
        BasicDataSource dataSource = config.pbdDataSource(
            "org.h2.Driver",
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:ru/yandex/market/mbo/erp/dao/erp.sql'" +
                ";MODE=MSSQLServer",
            "",
            ""
        );
        jdbcTemplate = config.pbdJdbcTemplate(dataSource);
        TransactionTemplate transactionTemplate = config.pbdTransactionTemplate(dataSource);

        erpExporter = new ErpExporter(jdbcTemplate, transactionTemplate, null);
        enhancedRandom = new EnhancedRandomBuilder()
            .seed(29883)
            .stringLengthRange(0, 100)
            .randomize(BigDecimal.class, (Supplier<BigDecimal>) () ->
                BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble())
                    .setScale(16, BigDecimal.ROUND_HALF_UP) // 16-digit precision set on sql table
            )
            .build();
    }

    @Test
    public void testTablesExist() {
        Assert.assertNotNull(jdbcTemplate);
        jdbcTemplate.query("select 1", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from MBOImpCategory", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from MBOImpSKU", rs -> { /*do nothing */ });
    }

    @Test
    public void testEmptyCategoriesBatch() {
        erpExporter.writeCategoriesBatch(new ArrayList<>());

        int count = jdbcTemplate.queryForObject("select count(*) from MBOImpCategory", Integer.class);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testRandomCategoriesBatch() {
        List<ErpCategory> dataset = enhancedRandom.objects(ErpCategory.class, 1000).collect(Collectors.toList());

        erpExporter.writeCategoriesBatch(dataset);

        int count = jdbcTemplate.queryForObject("select count(*) from MBOImpCategory", Integer.class);
        Assert.assertEquals(dataset.size(), count);

        jdbcTemplate.query("select * from MBOImpCategory order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleCategory(dataset.get(rowNum), rs);
            return null;
        });
    }

    private void checkSingleCategory(ErpCategory c, ResultSet rs) throws SQLException {
        rs.getInt("id");
        Assert.assertFalse(rs.wasNull());

        rs.getTimestamp("import_ts");
        Assert.assertTrue(rs.wasNull());

        Assert.assertEquals(c.getParentId(), rs.getInt("parent_id"));
        Assert.assertEquals(c.isDeleted(), rs.getBoolean("deleted"));
        Assert.assertEquals(c.isPublished(), rs.getBoolean("published"));
        Assert.assertEquals(c.getSessionId(), rs.getInt("session_id"));
        Assert.assertEquals(c.getName(), rs.getString("name"));
        Assert.assertEquals(c.getUniqueName(), rs.getString("unique_name"));
        Assert.assertEquals(c.getNotes(), rs.getString("notes"));
        Assert.assertEquals(c.getModificationTs(), rs.getTimestamp("mod_ts"));
        Assert.assertEquals(c.getExportTs(), rs.getTimestamp("export_ts"));
        Assert.assertEquals(c.getStatus(), ImportStatus.forId(rs.getInt("import_status")));
        Assert.assertEquals(c.isNeedCertificate(), rs.getBoolean("need_certificate"));
    }

    @Test
    public void testEmptySku() {
        erpExporter.writeSkuBatch(new ArrayList<>());

        int count = jdbcTemplate.queryForObject("select count(*) from MBOImpSKU", Integer.class);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testSku() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 1000).collect(Collectors.toList());

        erpExporter.writeSkuBatch(dataset);

        int count = jdbcTemplate.queryForObject("select count(*) from MBOImpSKU", Integer.class);
        Assert.assertEquals(dataset.size(), count);

        jdbcTemplate.query("select * from MBOImpSKU order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleSku(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testLargeVendorCodeSku() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 2, "vendorCode").collect(Collectors.toList());
        dataset.get(0).setVendorCode(Strings.repeat("x", ErpExporter.VENDOR_CODE_LIMIT));
        dataset.get(1).setVendorCode(Strings.repeat("x", ErpExporter.VENDOR_CODE_LIMIT + 1));

        erpExporter.writeSkuBatch(dataset);

        // resulted vendor code should be limited to VENDOR_CODE_LIMIT
        dataset.get(1).setVendorCode(Strings.repeat("x", ErpExporter.VENDOR_CODE_LIMIT));

        jdbcTemplate.query("select * from MBOImpSKU order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleSku(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testLargeBarCodesSku() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 2, "barCodes").collect(Collectors.toList());
        dataset.get(0).setBarCodes(Strings.repeat("x", ErpExporter.BAR_CODES_LIMIT));
        dataset.get(1).setBarCodes(Strings.repeat("x", ErpExporter.BAR_CODES_LIMIT + 1));

        erpExporter.writeSkuBatch(dataset);

        // resulted bar code should be limited to BAR_CODES_LIMIT
        dataset.get(1).setBarCodes(Strings.repeat("x", ErpExporter.BAR_CODES_LIMIT));

        jdbcTemplate.query("select * from MBOImpSKU order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleSku(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testLargeNameSku() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 2, "name").collect(Collectors.toList());
        dataset.get(0).setName(Strings.repeat("x", ErpExporter.NAME_BYTES_LIMIT));
        dataset.get(1).setName(Strings.repeat("x", ErpExporter.NAME_BYTES_LIMIT + 1));

        erpExporter.writeSkuBatch(dataset);

        // resulted name should be limited to NAME_BYTES_LIMIT
        dataset.get(1).setName(Strings.repeat("x", ErpExporter.NAME_BYTES_LIMIT));

        jdbcTemplate.query("select * from MBOImpSKU order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleSku(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testWriteDifferentTypes() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 3).collect(Collectors.toList());
        dataset.get(0).setErpSkuType(ErpSku.ErpSkuType.GURU_WITH_IS_SKU);
        dataset.get(1).setErpSkuType(ErpSku.ErpSkuType.SKU);
        dataset.get(2).setErpSkuType(ErpSku.ErpSkuType.PARTNER_SKU);

        erpExporter.writeSkuBatch(dataset);

        jdbcTemplate.query("select * from MBOImpSKU", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingleSku(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testExportNotDefinedVendor() {
        List<ErpSku> dataset = enhancedRandom.objects(ErpSku.class, 2).collect(Collectors.toList());
        ErpSku first = dataset.get(0);
        ErpSku second = dataset.get(1);

        second.setVendorId((int) KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        second.setVendorName("Не определен");
        second.setRawVendorName("Эппле");

        erpExporter.writeSkuBatch(dataset);

        jdbcTemplate.query("select * from MBOImpSKU", (RowMapper<Void>) (rs, rowNum) -> {
            if (rowNum == 0) {
                Assert.assertEquals(first.getVendorId(), rs.getInt("vendor_id"));
                Assert.assertEquals(first.getVendorName(), rs.getString("vendor_name"));
            }
            if (rowNum == 1) {
                Assert.assertNull(rs.getObject("vendor_id"));
                Assert.assertEquals("Эппле", rs.getString("vendor_name"));
            }
            return null;
        });
    }

    private void checkSingleSku(ErpSku c, ResultSet rs) throws SQLException {
        rs.getInt("id");
        Assert.assertFalse(rs.wasNull());

        rs.getTimestamp("import_ts");
        Assert.assertTrue(rs.wasNull());

        Assert.assertEquals(c.getMskuId(), rs.getLong("MSKU"));
        Assert.assertEquals(c.getName(), rs.getString("name"));
        Assert.assertEquals(c.getErpSkuType() == ErpSku.ErpSkuType.PARTNER_SKU, rs.getBoolean("is_psku"));
        Assert.assertEquals(c.isDeleted(), rs.getBoolean("deleted"));
        Assert.assertEquals(c.isPublished(), rs.getBoolean("published"));
        Assert.assertEquals(c.getSessionId(), rs.getInt("session_id"));

        Assert.assertEquals(c.getCategoryId(), rs.getInt("category_id"));
        Assert.assertEquals(c.getVendorId(), rs.getInt("vendor_id"));
        Assert.assertEquals(c.getVendorName(), rs.getString("vendor_name"));
        Assert.assertEquals(c.getVendorCode(), rs.getString("vendor_code"));
        Assert.assertEquals(c.getBarCodes(), rs.getString("barcodes"));
        Assert.assertEquals(c.getPictureUrl(), rs.getString("picture_url"));
        Assert.assertEquals(c.getPackageDepth(), rs.getBigDecimal("gross_depth"));
        Assert.assertEquals(c.getPackageHeight(), rs.getBigDecimal("gross_length"));
        Assert.assertEquals(c.getPackageWeight(), rs.getBigDecimal("gross_weight"));
        Assert.assertEquals(c.getPackageWidth(), rs.getBigDecimal("gross_width"));

        Assert.assertEquals(c.getModificationTs(), rs.getTimestamp("mod_ts"));
        Assert.assertEquals(c.getExportTs(), rs.getTimestamp("export_ts"));
        Assert.assertEquals(c.getStatus(), ImportStatus.forId(rs.getInt("import_status")));
    }
}

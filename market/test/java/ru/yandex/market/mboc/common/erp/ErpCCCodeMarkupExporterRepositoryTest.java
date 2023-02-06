package ru.yandex.market.mboc.common.erp;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;

import static ru.yandex.market.ir.http.MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED;
import static ru.yandex.market.ir.http.MdmIrisPayload.CisHandleMode.NOT_DEFINED;
import static ru.yandex.market.ir.http.MdmIrisPayload.CisHandleMode.NO_RESTRICTION;

public class ErpCCCodeMarkupExporterRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private ErpCCCodeMarkupExporterRepository erpExporter;
    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() {
        String dbName = getClass().getSimpleName() + UUID.randomUUID();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:erp/erp.sql'" +
                ";MODE=MSSQLServer"
        );

        jdbcTemplate = new JdbcTemplate(dataSource);
        erpExporter = new ErpCCCodeMarkupExporterRepositoryImpl(jdbcTemplate);

        enhancedRandom = new EnhancedRandomBuilder()
            .seed(293)
            .stringLengthRange(0, 20)
            .build();
    }

    @Test
    public void whenExportingEmptyChangesCollectionShouldNotFail() {
        Assertions.assertThatCode(() -> {
            erpExporter.insertCCCodeMarkupChanges(new ArrayList<>());
        }).doesNotThrowAnyException();

        int count = jdbcTemplate.queryForObject("select count(*) from MDMSSKUMasterDataIN", Integer.class);
        Assertions.assertThat(count).isEqualTo(0);
    }

    @Test
    public void whenExportingMasterDataShouldInsertEachFieldCorrectly() {
        List<ErpCCCodeMarkupChange> dataset = enhancedRandom.objects(ErpCCCodeMarkupChange.class, 1000)
            .collect(Collectors.toList());

        erpExporter.insertCCCodeMarkupChanges(dataset);

        int count = jdbcTemplate.queryForObject("select count(*) from MDMSSKUMasterDataIN", Integer.class);

        Assertions.assertThat(count).isEqualTo(dataset.size());

        jdbcTemplate.query("select * from MDMSSKUMasterDataIN order by id", (RowMapper<Void>) (rs, rowNum) -> {
            checkSingle(dataset.get(rowNum), rs);
            return null;
        });
    }

    @Test
    public void testFindLatestChanges() {
        //given
        String shopSku = "213.321";
        ErpCCCodeMarkupChange change1 = new ErpCCCodeMarkupChange(
          shopSku, "12.12", Cis.REQUIRED, "", true, List.of("RU"), NOT_DEFINED
        );
        ErpCCCodeMarkupChange change2 = new ErpCCCodeMarkupChange(
            shopSku, "12.12", Cis.OPTIONAL, "", false, List.of("RU"), NO_RESTRICTION
        );

        //when
        erpExporter.insertCCCodeMarkupChanges(List.of(change1));
        ErpCCCodeMarkupChange after1 = erpExporter.findLatestChanges(List.of(shopSku)).get(shopSku);
        erpExporter.insertCCCodeMarkupChanges(List.of(change2));
        ErpCCCodeMarkupChange after2 = erpExporter.findLatestChanges(List.of(shopSku)).get(shopSku);

        //then
        Assertions.assertThat(after1).isEqualTo(change1);
        Assertions.assertThat(after2).isEqualTo(change2);
        Assertions.assertThat(erpExporter.findAll()).containsExactlyInAnyOrder(change1, change2);
    }

    @Test
    public void testInsertAndFind() {
        //given
        List<ErpCCCodeMarkupChange> changes = List.of(
            new ErpCCCodeMarkupChange(
                "001.001", "1.23.43.54", Cis.REQUIRED, "1.23.43.54.1", true, List.of("RU"), NO_RESTRICTION
            ),
            new ErpCCCodeMarkupChange(
                "002.002", "1.23.43.54", Cis.NONE, "1.23.43.54.1", false, List.of(), NOT_DEFINED
            ),
            new ErpCCCodeMarkupChange(
                "003.003", null, Cis.NONE, null, null, null, ACCEPT_ONLY_DECLARED
            ),
            new ErpCCCodeMarkupChange(
                "004.004", "", Cis.OPTIONAL, "", false, List.of("RU", "US"), ACCEPT_ONLY_DECLARED
            ),
            new ErpCCCodeMarkupChange(
                "005.005", "", Cis.DISTINCT, "12.12", false, List.of(), NO_RESTRICTION
            )
        );
        List<String> shopSkus = changes.stream()
            .map(ErpCCCodeMarkupChange::getShopSku)
            .collect(Collectors.toList());

        //when
        erpExporter.insertCCCodeMarkupChanges(changes);

        //then
        Assertions.assertThat(erpExporter.findAll()).containsExactlyInAnyOrderElementsOf(changes);
        Assertions.assertThat(erpExporter.findLatestChanges(shopSkus).values())
            .containsExactlyInAnyOrderElementsOf(changes);
    }

    private void checkSingle(final ErpCCCodeMarkupChange c, final ResultSet rs) {
        SoftAssertions.assertSoftly(s -> {
            var assertThat = s.assertThat(Collections.singletonList(rs));

            assertThat.extracting(r -> {
                r.getInt("id");
                return r.wasNull();
            }).containsOnly(Boolean.FALSE);

            assertThat.extracting(r -> {
                r.getTimestamp("export_ts");
                return r.wasNull();
            }).containsOnly(Boolean.FALSE);

            assertThat.extracting(r -> {
                r.getString("SSKU_DATA");
                return r.wasNull();
            }).containsOnly(Boolean.FALSE);

            assertThat.extracting(r -> {
                r.getTimestamp("import_ts");
                return r.wasNull();
            }).containsOnly(Boolean.TRUE);

            assertThat.extracting(r -> {
                r.getString("import_status");
                return r.wasNull();
            }).containsOnly(Boolean.FALSE);

            assertThat.extracting(r -> r.getString("SSKU")).containsOnly(c.getShopSku());
            assertThat.extracting(r -> new JSONObject(r.getString("SSKU_DATA")).get("TNVED"))
                .containsOnly(c.getPrefixHSCode());

            switch (c.getHonestSignStatus()) {
                case NONE:
                    assertThat.extracting(r -> new JSONObject(r.getString("SSKU_DATA")).get("cargotype"))
                        .containsOnly("cis-none");
                    break;
                case DISTINCT:
                    assertThat.extracting(r -> new JSONObject(r.getString("SSKU_DATA")).get("cargotype"))
                        .containsOnly("cis-distinct");
                    break;
                case OPTIONAL:
                    assertThat.extracting(r -> new JSONObject(r.getString("SSKU_DATA")).get("cargotype"))
                        .containsOnly("cis-optional");
                    break;
                case REQUIRED:
                    assertThat.extracting(r -> new JSONObject(r.getString("SSKU_DATA")).get("cargotype"))
                        .containsOnly("cis-required");
                    break;
                default:
                    break;
            }
        });
    }
}

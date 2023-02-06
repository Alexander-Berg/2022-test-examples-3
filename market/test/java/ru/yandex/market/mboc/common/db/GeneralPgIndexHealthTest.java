package ru.yandex.market.mboc.common.db;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.github.mfvanek.pg.model.index.DuplicatedIndexes;
import io.github.mfvanek.pg.model.index.ForeignKey;
import io.github.mfvanek.pg.model.index.IndexWithNulls;
import io.github.mfvanek.pg.model.index.IndexWithSize;
import io.github.mfvanek.pg.model.table.Table;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests of health of postgres db. Don't add new exceptions without exceptional reason.
 * <p>
 * See https://ivvakhrushev.at.yandex-team.ru/1
 * Or https://st.yandex-team.ru/MBO-24340
 */
public class GeneralPgIndexHealthTest extends BasePgIndexHealthTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    protected List<String> getSchemasToAnalyze() {
        var schemas = jdbcTemplate.queryForList("select schema_name from information_schema.schemata;", String.class);
        return schemas.stream()
            .filter(schema -> !schema.contains("mdm") // *mdm* schemas analyzed in MdmPgIndexHealthTest
                && !schema.equals("public") // ignore public schema
                && !schema.equals("information_schema") // ignore system schema
                && !schema.equals("pg_catalog") // ignore system schema
                && !schema.equals("default")) // ignore system schema
            .collect(Collectors.toList());
    }

    @Test
    public void getDuplicatedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> duplicatedIndexes = getDuplicatedIndexes();

        Assertions.assertThat(duplicatedIndexes).isEmpty();
    }

    @Test
    public void getIntersectedIndexesShouldReturnNothing() {
        List<DuplicatedIndexes> intersectedIndexes = getIntersectedIndexes();

        Assertions.assertThat(intersectedIndexes).containsExactlyInAnyOrder(
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_g", 0),
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_n_g_state", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_nft_st", 0),
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_state", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_n_state", 0),
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.qrtz_triggers_pkey", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_nft_misfire", 0),
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_nft_st_misfire_grp", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_nft_misfire", 0),
                IndexWithSize.of("mbo_category.qrtz_triggers", "mbo_category.idx_qrtz_t_nft_st_misfire", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mbo_category.qrtz_fired_triggers", "mbo_category.idx_qrtz_ft_inst_job_req_rcvry", 0),
                IndexWithSize.of("mbo_category.qrtz_fired_triggers", "mbo_category.idx_qrtz_ft_trig_inst_name", 0)
            )
        );
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        List<ForeignKey> foreignKeys = getForeignKeysNotCoveredWithIndex();

        Assertions.assertThat(foreignKeys).containsExactlyInAnyOrder(
            ForeignKey.of("mbo_category.category_info", "category_info_content_manager_uid_fkey",
                List.of("content_manager_uid")),
            ForeignKey.of("mbo_category.category_info", "category_info_input_manager_uid_fkey",
                List.of("input_manager_uid")),
            ForeignKey.of("mbo_category.category_supplier_vendor", "category_supplier_vendor_category_id_fkey",
                List.of("category_id")),
            ForeignKey.of("mbo_category.category_supplier_vendor", "category_supplier_vendor_supplier_id_fkey",
                List.of("supplier_id")),
            ForeignKey.of("mbo_category.notifications_log", "notifications_log_supplier_id_fkey",
                List.of("supplier_id")),
            ForeignKey.of("mbo_category.processing_ticket_info", "processing_ticket_supplier_id_fk",
                List.of("supplier_id")),
            ForeignKey.of("mbo_category.mapping_file_row", "mapping_file_row_file_id_fkey",
                List.of("file_id")),
            ForeignKey.of("mbo_category.offer_master_data", "offer_master_data_to_supplier_fk",
                List.of("supplier_id"))
            );
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnNothing() {
        List<Table> tables = getTablesWithoutPrimaryKey();

        Assertions.assertThat(tables).containsExactlyInAnyOrder(
            Table.of("mbo_category.offer_deleted_backup", 0)
        );
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        List<IndexWithNulls> indexesWithNulls = getIndexesWithNullValues();

        Assertions.assertThat(indexesWithNulls).containsExactlyInAnyOrder(
            IndexWithNulls.of("mbo_category.offer", "mbo_category.offer_category_id_idx", 0, "category_id"),
            IndexWithNulls.of("mbo_category.offer", "mbo_category.ix_offer_upload_yt_ts", 0, "upload_to_yt_stamp"),
            IndexWithNulls.of("mbo_category.offer", "mbo_category.model_id_idx", 0, "model_id"),
            IndexWithNulls.of("mbo_category.mbo_user", "mbo_category.mbo_user_staff_login_idx", 0, "staff_login"),
            IndexWithNulls.of("mbo_category.category", "mbo_category.ix_category_name", 0, "name"),
            IndexWithNulls.of("mbo_category.datacamp_offers_content_processing",
                "mbo_category.idx_datacamp_offers_content_processing_group", 0, "group_id"),
            IndexWithNulls.of("mbo_category.anti_mapping",
                "mbo_category.anti_mapping_upload_stamp_idx", 0, "upload_stamp"),
            IndexWithNulls.of("mbo_category.mapping_file", "mbo_category.mbo_category_mapping_file_completed", 0,
                "completed"),
            IndexWithNulls.of("mbo_category.mapping_file_row", "mbo_category.mbo_category_mapping_file_row_processed",
                0, "processed"),
            IndexWithNulls.of("mbo_category.mapping_file_row", "mbo_category.mbo_category_mapping_file_row_skipped",
                0, "skipped"),
            IndexWithNulls.of("mbo_category.offer_processing_assignment", "mbo_category.offer_processing_assignment_category_index", 0, "category_id"),
            IndexWithNulls.of("mbo_category.offer_processing_assignment", "mbo_category.offer_processing_assignment_ticket_deadline_index", 0, "ticket_deadline"),
            IndexWithNulls.of("mbo_category.offer_processing_assignment", "mbo_category.offer_processing_assignment_processing_ticket_id_index", 0, "processing_ticket_id"),
            IndexWithNulls.of("mbo_category.offer_queue_processing", "mbo_category.offer_queue_processing_completed_idx", 0, "processed_ts"),
            IndexWithNulls.of("mbo_category.offer", "mbo_category.ix_offer_upload_yt_ts_not_dc", 0, "upload_to_yt_stamp")
        );
    }
}

package ru.yandex.market.mbo.mdm.common.db;

import java.util.List;

import io.github.mfvanek.pg.model.index.DuplicatedIndexes;
import io.github.mfvanek.pg.model.index.ForeignKey;
import io.github.mfvanek.pg.model.index.IndexWithNulls;
import io.github.mfvanek.pg.model.index.IndexWithSize;
import io.github.mfvanek.pg.model.table.Table;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Tests of health of postgres db. Don't add new exceptions without exceptional reason.
 * <p>
 * See https://ivvakhrushev.at.yandex-team.ru/1
 * Or https://st.yandex-team.ru/MBO-24340
 */
public class MdmPgIndexHealthTest extends BasePgIndexHealthTest {

    @Override
    protected List<String> getSchemasToAnalyze() {
        return List.of("mdm_tms", "mdm", "mdm_audit");
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
                IndexWithSize.of("mdm.category_rsl", "mdm.category_rsl_category_id_idx", 0),
                IndexWithSize.of("mdm.category_rsl", "mdm.category_rsl_pk", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm.item_metric", "mdm.item_metric_pkey", 0),
                IndexWithSize.of("mdm.item_metric", "mdm.item_metric_supplier_id_shop_sku_idx", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm.master_data", "mdm.master_data_supplier_id_idx", 0),
                IndexWithSize.of("mdm.master_data", "mdm.unique_offer_master_data", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm.msku_rsl", "mdm.msku_rsl_msku_id_idx", 0),
                IndexWithSize.of("mdm.msku_rsl", "mdm.msku_rsl_pk", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm.ssku_rsl", "mdm.ssku_rsl_pk", 0),
                IndexWithSize.of("mdm.ssku_rsl", "mdm.ssku_rsl_supplier_id_shop_sku_idx", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_g", 0),
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_n_g_state", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_nft_st", 0),
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_state", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_n_state", 0),
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.qrtz_triggers_pkey", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_nft_misfire", 0),
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_nft_st_misfire_grp", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_nft_misfire", 0),
                IndexWithSize.of("mdm_tms.qrtz_triggers", "mdm_tms.idx_qrtz_t_nft_st_misfire", 0)
            ),
            DuplicatedIndexes.of(
                IndexWithSize.of("mdm_tms.qrtz_fired_triggers", "mdm_tms.idx_qrtz_ft_inst_job_req_rcvry", 0),
                IndexWithSize.of("mdm_tms.qrtz_fired_triggers", "mdm_tms.idx_qrtz_ft_trig_inst_name", 0)
            )
        );
    }

    @Test
    public void getForeignKeysNotCoveredWithIndexShouldReturnNothing() {
        List<ForeignKey> foreignKeys = getForeignKeysNotCoveredWithIndex();

        Assertions.assertThat(foreignKeys).containsExactlyInAnyOrder(
            ForeignKey.of("mdm.offer_document", "offer_document_document_id_fkey",
                List.of("document_id")),
            ForeignKey.of("mdm.customs_comm_code", "customs_comm_code_good_group_id_fkey",
                List.of("good_group_id")),
            ForeignKey.of("mdm.auto_markup_history", "auto_markup_history_query_id_fkey",
                List.of("query_id"))
        );
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        List<IndexWithNulls> indexesWithNulls = getIndexesWithNullValues();

        Assertions.assertThat(indexesWithNulls).containsExactlyInAnyOrder(
            IndexWithNulls.of("mdm.master_data", "mdm.master_data_msku_import_status_idx", 0, "msku_import_status"),
            IndexWithNulls.of("mdm.master_data", "mdm.master_data_modified_timestamp_idx", 0, "modified_timestamp"),
            IndexWithNulls.of("mdm.offer_document", "mdm.offer_document_modified_timestamp_idx", 0,
                "modified_timestamp"),
            IndexWithNulls.of("mdm.category_rsl", "mdm.category_rsl_modified_at_idx", 0, "modified_at"),
            IndexWithNulls.of("mdm.msku_rsl", "mdm.msku_rsl_modified_at_idx", 0, "modified_at"),
            IndexWithNulls.of("mdm.ssku_rsl", "mdm.ssku_rsl_modified_at_idx", 0, "modified_at"),
            IndexWithNulls.of("mdm.service_offer_migration", "mdm.service_offer_migration_processed_ts_idx", 0,
                "processed_timestamp"),
            IndexWithNulls.of("mdm.reference_item", "mdm.idx_reference_item_internal_modified_ts", 0,
                "internal_modified_ts"),
            IndexWithNulls.of("mdm.auto_markup_history", "mdm.auto_markup_history_started_at_idx", 0, "started_at"),
            IndexWithNulls.of("mdm.msku_and_ssku_queue", "mdm.msku_and_ssku_queue_key_idx", 0, "supplier_id"),
            IndexWithNulls.of("mdm.msku_to_mbo_queue", "mdm.msku_to_mbo_queue_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.msku_to_refresh", "mdm.msku_to_refresh_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.send_reference_item_queue", "mdm.send_reference_item_queue_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.send_to_datacamp_queue", "mdm.send_to_datacamp_queue_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.send_to_erp_queue", "mdm.send_to_erp_queue_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.ssku_to_refresh", "mdm.ssku_to_refresh_processed_timestamp_idx", 0, "processed_timestamp"),
            IndexWithNulls.of("mdm.golden_ssku_entity", "mdm.idx_golden_ssku_entity_update_ts", 0, "update_ts")
        );
    }

    @Test
    public void getTablesWithoutPrimaryKeyShouldReturnNothing() {
        List<Table> tablesWithoutPrimaryKey = getTablesWithoutPrimaryKey();

        Assertions.assertThat(tablesWithoutPrimaryKey).containsExactlyInAnyOrder(
            Table.of("mdm.mdm_external_reference_projection", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_1h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_2h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_3h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_4h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_5h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_6h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_7h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_8h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_9h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_10h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_11h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_12h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_13h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_14h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_15h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_16h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_17h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_18h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_19h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_20h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_21h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_22h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_23h", 0),
            Table.of("mdm.send_to_datacamp_queue_processed_24h", 0)
        );
    }
}

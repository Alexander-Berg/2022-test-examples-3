package ru.yandex.market.robot.db.liquibase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.robot.shared.raw_model.Picture;
import ru.yandex.market.robot.shared.raw_model.Status;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author jkt on 01.12.17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LiquibaseTestConfig.class)
public class MarketRawModelLiquibaseChangeSetsTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void shouldContainModelStatusType() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::model_status))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            Arrays.stream(Status.values()).map(Enum::name).toArray(String[]::new)
        );
    }

    @Test
    public void shouldContainPictureDownloadStatusType() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::model_picture_download_status))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            Arrays.stream(Picture.DownloadStatus.values()).map(Enum::name).toArray(String[]::new)
        );
    }

    @Test
    public void shouldContainTicketPipelineType() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::ticket_pipeline_type))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            Arrays.stream(AutoGenerationApi.TicketPipelineType.values()).map(Enum::name).toArray(String[]::new)
        );
    }

    @Test
    public void shouldContainRobotTmsModelTaskType() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::rtms_model_task_type))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            "CREATE_GURU_FOR_ALL_AUTO_GENERATED_MODEL",
            "CREATE_GURU_FOR_SELECTED_AUTO_GENERATED_MODEL",
            "UPDATE_GURU_FOR_ALL_AUTO_GENERATED_MODEL",
            "PUBLISH_GURU_FOR_ALL_AUTO_GENERATED_MODEL",
            "PUBLISH_GURU_FOR_SELECTED_AUTO_GENERATED_MODEL"
        );
    }

    @Test
    public void shouldContainRobotTmsModelTaskState() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::rtms_model_task_state))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            "NEW", "PROCESSED", "CANCELED", "FINISHED", "FAILED"
        );
    }

    @Test
    public void shouldContainRobotTmsModelProcessStatus() {
        String modelStatusValuesSql = "SELECT unnest(enum_range(NULL::rtms_model_process_status))";

        List<String> modelStatusValues = jdbcTemplate.queryForList(modelStatusValuesSql, String.class);

        assertThat(modelStatusValues).containsExactlyInAnyOrder(
            "NOT_STARTED", "WRONG_STATE", "FINISHED", "FAILED", "LOST"
        );
    }

    @Test
    public void shouldContainCreatedSequences() {
        String sql = "SELECT sequence_name FROM information_schema.sequences";

        List<String> sequences = jdbcTemplate.queryForList(sql, String.class);

        assertThat(sequences).containsExactlyInAnyOrder(
            "s_raw_model_id", "s_token_alias", "ticket_id_seq", "rtms_models_task_id_seq"
        );
    }

    @Test
    public void shouldContainCreatedIndexes() {
        String sql = "SELECT indexname from pg_indexes where schemaname = 'public'";

        List<String> sequences = jdbcTemplate.queryForList(sql, String.class);

        assertThat(sequences).containsExactlyInAnyOrder(
            "categories_pkey",
            "market_relation_idx",
            "market_relation_model_id_idx",
            "market_relation_model_index",
            "market_relation_mrk_id_idx",
            "market_relation_param_history_idx",
            "market_relation_param_history_ind",
            "market_relation_param_idx",
            "market_relation_param_model_id_index_idx",
            "model_param_index_idx",
            "model_param_internal_idx",
            "model_param_mid",
            "model_param_name_index",
            "model_picture_history_pkey",
            "model_picture_pkey",
            "model_pkey",
            "model_rsv",
            "model_sku_relation_index",
            "model_source_id_idx",
            "models_check_images_task_result_pkey",
            "models_check_models_pkey",
            "models_check_sample_pkey",
            "models_check_sku_pkey",
            "models_check_sku_rlv_image_result_pkey",
            "models_check_sku_rlv_param_result_pkey",
            "models_check_sku_rlv_task_result_pkey",
            "models_check_sku_sample_pkey",
            "models_check_task_ind",
            "models_check_task_pkey",
            "pk_databasechangeloglock",
            "pk_models_check_logs_task_result",
            "pk_sources",
            "pk_task_status",
            "pk_ticket",
            "pk_ticket_type",
            "rtms_model_process_pkey",
            "rtms_models_task_pkey",
            "task_status_name_key",
            "ticket_request_id_key",
            "title_processor_pkey",
            "tokens_aliases_name_idx",
            "version_number_index",
            "version_pkey",
            "pk_ticket_sku_link",
            "pk_autogeneration_success_date",
            "market_relation_model_market_model_id_idx",
            "pk_models_check_not_ok_step",
            "market_relation_history_category_vendor_idx",
            "ticket_updated_ts_idx",
            "market_relation_history_created_date_idx",
            "market_relation_model_history_created_date_idx",
            "market_relation_param_history_created_date_idx",
            "model_history_created_date_idx",
            "model_param_history_created_date_idx",
            "model_picture_history_download_date_idx",
            "model_recommendation_history_created_date_idx"
        );
    }

    @Test
    public void shouldContainTables() {
        String sql = "SELECT table_name from information_schema.tables where table_schema ='public'";

        List<String> tables = jdbcTemplate.queryForList(sql, String.class);

        assertThat(tables).containsExactlyInAnyOrder(
            "databasechangeloglock",
            "databasechangelog",

            "categories",
            "category_params",
            "action_log",
            "matching_log",
            "session_log",
            "market_relation",
            "market_relation_history",
            "market_relation_model",
            "market_relation_model_history",
            "market_relation_param",
            "market_relation_param_history",
            "model",
            "model_history",
            "model_param",
            "model_param_history",
            "model_picture",
            "model_picture_history",
            "model_recommendation",
            "model_recommendation_history",
            "models_check_models",
            "models_check_sample",
            "models_check_images_task_result",
            "models_check_task",
            "models_check_params_task_result",
            "sources",
            "version",
            "title_processor",
            "ticket_type",
            "task_status",
            "ticket",
            "tokens_aliases",
            "tokens_join",
            "model_sku_relation",
            "models_check_logs_task_result",
            "models_check_sku_sample",
            "models_check_sku",
            "models_check_sku_rlv_task_result",
            "models_check_sku_rlv_image_result",
            "models_check_sku_rlv_param_result",
            "rtms_models_task",
            "rtms_model_process",
            "ticket_sku_link",
            "autogeneration_success_date",
            "models_check_not_ok_step");
    }

    @Test
    public void shouldContainColumnsInModelPictureTable() {
        String sql =
            "SELECT column_name\n" +
                "FROM information_schema.columns\n" +
                "WHERE table_schema ='public' AND table_name = 'model_picture'";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class);
        assertThat(columns).containsExactlyInAnyOrder(
            "url",
            "type",
            "status",
            "source_url",
            "picture_hash",
            "model_id",
            "last_version_number",
            "index",
            "first_version_number",
            "deleted",
            "download_timestamp",
            "download_status",
            "download_error"
        );
    }

    @Test
    public void shouldContainColumnsInTicketSkuLinkTable() {
        String sql =
            "SELECT column_name\n" +
                "FROM information_schema.columns\n" +
                "WHERE table_schema ='public' AND table_name = 'ticket_sku_link'";
        List<String> columns = jdbcTemplate.queryForList(sql, String.class);
        assertThat(columns).containsExactlyInAnyOrder(
            "ticket_id",
            "vendor_sku_id",
            "generated_sku_id"
        );
    }

    @Test
    public void shouldContainCreatedRowFieldInHistoryTables() {
        String baseSql = "SELECT column_name\n" +
            "FROM information_schema.columns\n" +
            "WHERE table_schema ='public' AND table_name = ";
        String[] tableNames = {"\'market_relation_history\'", "\'market_relation_model_history\'",
            "\'market_relation_param_history\'", "\'model_history\'", "\'model_param_history\'",
            "\'model_picture_history\'", "\'model_recommendation_history\'"};
        for (String tableName : tableNames) {
            List<String> columns = jdbcTemplate.queryForList(baseSql + tableName, String.class);
            assertThat(columns).contains("created_row_date");
        }
    }
}

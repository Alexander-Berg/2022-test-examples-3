package ru.yandex.market.mbo.db.recipe;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.recipes.RecipeServiceDao;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeGoodState;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeSqlFilter;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RecipeServiceDaoTest extends BaseIntegrationTest {

    private static final String[] IGNORED_FIELDS = {"nid", "modificationDate", "reportInspectionDate"};

    @Autowired
    private AuditService auditService;

    @Autowired
    @Qualifier("contentPgNamedJdbcTemplate")
    private NamedParameterJdbcTemplate postgresNamedJdbcTemplate;

    @Autowired
    @Qualifier("contentPgTransactionTemplate")
    private TransactionTemplate postgresTxTemplate;

    private RecipeServiceDao recipeServiceDao;

    @Before
    public void setUp() throws Exception {
        recipeServiceDao = new RecipeServiceDao(postgresNamedJdbcTemplate, postgresTxTemplate, auditService);
        initPostgres();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void initPostgres() {
        postgresNamedJdbcTemplate.update("CREATE SCHEMA IF NOT EXISTS market_content",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE SEQUENCE IF NOT EXISTS market_content.recipe_id_seq " +
            "START WITH 319860461867 " +
            "INCREMENT BY 68614",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.recipe (" +
                "id bigint PRIMARY KEY," +
                "hid bigint," +
                "name text," +
                "header text," +
                "popularity bigint," +
                "published boolean," +
                "user_id bigint," +
                "modification_date timestamp with time zone," +
                "correct boolean," +
                "is_navigation boolean," +
                "sponsored boolean," +
                "err_msg bytea," +
                "city_with_empty_report bigint," +
                "report_inspection_date timestamp with time zone," +
                "button_index bigint," +
                "is_seo boolean," +
                "is_button boolean," +
                "button_name text," +
                "contains_reviews boolean," +
                "discount boolean," +
                "discount_and_promo boolean," +
                "aprice boolean," +
                "total_offers_msk bigint," +
                "total_models_msk bigint," +
                "without_filters boolean," +
                "search_query text," +
                "auto_generated boolean," +
                "cut_price boolean," +
                "good_state boolean" +
                ")",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.recipe_err (" +
                "recipe_id bigint," +
                "cause text," +
                "message text" +
                ")",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.recipe_filter (" +
                "recipe_id bigint," +
                "param_id bigint," +
                "param_type varchar(50)," +
                "min_value numeric," +
                "max_value numeric," +
                "bool_value boolean," +
                "position bigint," +
                "PRIMARY KEY (recipe_id, param_id)" +
                ")",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.recipe_filter_value (" +
                "recipe_id bigint," +
                "param_id bigint," +
                "value_id bigint," +
                "PRIMARY KEY (recipe_id, param_id, value_id)" +
                ")",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.parameter (" +
                "id bigint primary key ," +
                "type varchar(36)," +
                "measure_id bigint," +
                "unit_id bigint," +
                "is_size_measure char," +
                "min_value numeric," +
                "max_value numeric," +
                "xsl_name varchar(750)," +
                "precision bigint," +
                "use_for_gurulight char," +
                "use_for_guru char," +
                "category_hid bigint not null," +
                "manual_inheritance bigint," +
                "hyper_id bigint not null," +
                "ui_type varchar(4000)," +
                "other_allowed bigint," +
                "exportable bigint," +
                "is_service bigint," +
                "read_only bigint," +
                "modified bigint," +
                "multifield bigint," +
                "necessary bigint," +
                "omit_on_copy bigint," +
                "ordered bigint," +
                "published bigint," +
                "publish_level bigint," +
                "unit varchar(4000)," +
                "output_index bigint," +
                "template_index bigint," +
                "view_type_creation bigint," +
                "view_type_form bigint," +
                "view_type_table bigint," +
                "mapping varchar(765)," +
                "comment_for_operator text," +
                "binding_param bigint," +
                "hidden bigint," +
                "creation_param bigint," +
                "use_in_filters bigint," +
                "no_advfilters bigint," +
                "has_bool_no bigint," +
                "default_value varchar(4000)," +
                "description text," +
                "importance numeric," +
                "adv_filter_index bigint," +
                "common_filter_index bigint," +
                "incut_filter_index bigint," +
                "super_filter_index bigint," +
                "sort_method varchar(255)," +
                "mbo_dump_id bigint," +
                "override_param bigint," +
                "tag_unused varchar(1500)," +
                "who_access varchar(3000)," +
                "comments text," +
                "multivalue bigint," +
                "do_not_formalize_patterns bigint," +
                "subtype varchar(300)," +
                "cluster_breaker bigint," +
                "only_for_cluster_card bigint," +
                "main_size_measure bigint," +
                "auto_tasks_count bigint," +
                "auto_tasks_offers_count bigint," +
                "auto_task_formalized varchar(300)," +
                "through bigint," +
                "join_tag_id bigint," +
                "formalizer_tag_id bigint," +
                "cluster_filter bigint," +
                "weight bigint," +
                "super_parameter char," +
                "type_id bigint," +
                "mandatory char," +
                "position bigint," +
                "allow_in_titlemaker char," +
                "global_override bigint default 0 not null," +
                "dont_use_as_alias varchar(1)," +
                "auto_tasks_period bigint," +
                "fill_difficulty bigint," +
                "param_level bigint," +
                "fill_from_offers bigint default 0," +
                "is_managed_externally bigint default 0," +
                "highlight_original_value bigint default 0," +
                "modify_ts timestamp(6) default CURRENT_TIMESTAMP," +
                "short_enum_count bigint," +
                "short_enum_sort_type varchar(48) default NULL," +
                "matching_type varchar(60)," +
                "use_for_images bigint default 0," +
                "model_filter_index bigint," +
                "required_for_index bigint default 0 not null," +
                "important bigint default 0," +
                "use_in_sku bigint default 0," +
                "formalization_scope varchar(90)," +
                "sku_parameter_mode varchar(60)," +
                "extract_in_skubd bigint," +
                "skutching_type varchar(90)," +
                "clean_if_skutching_failed bigint," +
                "notify_stores bigint default 0," +
                "show_on_sku_tab bigint default 0," +
                "quoted_in_title bigint default 0," +
                "clear_if_absent_in_sku bigint default 0," +
                "copy_first_sku_pic_to_picker bigint default 0," +
                "blue_grouping bigint," +
                "comment_for_partner text," +
                "formalization_scope_red varchar(90) default 'ALL'," +
                "dont_formalize_ptrns_red bigint," +
                "dont_use_as_alias_red varchar(1)," +
                "mdm_parameter bigint," +
                "measure_id_red bigint," +
                "unit_id_red bigint," +
                "local_value_inh_strategy varchar(90) default 'INHERIT' not null," +
                "mandatory_for_partner bigint," +
                "name_for_partner varchar(750)" +
                ")",
            Collections.emptyMap());

        postgresNamedJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.enum_option (" +
                "id bigint primary key ," +
                "name varchar(1000)," +
                "priority bigint," +
                "tag varchar(100)," +
                "color_code varchar(100)," +
                "parent_vendor_id bigint," +
                "numeric_value numeric," +
                "do_not_use_as_alias char default 0," +
                "active char default 1," +
                "published char default 0," +
                "param_id bigint not null," +
                "operator_comments varchar(4000)," +
                "mbo_dump_id bigint," +
                "modified_ts timestamp(6) with time zone default CURRENT_TIMESTAMP," +
                "default_for_matcher bigint default 0," +
                "is_top_value bigint default 0," +
                "is_filter_value bigint default 1," +
                "is_default_value bigint default 0," +
                "picker_image_url varchar(4000)," +
                "picker_image_name varchar(500)," +
                "inheritance_strategy varchar(90) default 'INHERIT' not null," +
                "is_filter_value_red bigint default 1," +
                "is_default_value_red bigint default 0," +
                "display_name varchar(1000)," +
                "parent_option_id bigint," +
                "category_id bigint" +
                ")",
            Collections.emptyMap());
    }

    @Test
    public void testCreateUpdateDelete() {
        Recipe recipe = createRecipe();

        boolean created = recipeServiceDao.saveRecipe(recipe);
        List<Recipe> createdRecipes = recipeServiceDao.getNavigationRecipesById(
            Collections.singletonList(recipe.getId()),
            RecipeSqlFilter.Field.GOOD_STATE,
            true
        );

        assertThat(created).isTrue();
        assertThat(createdRecipes)
            .usingElementComparatorIgnoringFields(IGNORED_FIELDS)
            .containsExactly(recipe);

        Recipe updatedRecipe = createdRecipes.get(0);
        updatedRecipe.setGoodState(RecipeGoodState.ANY);

        created = recipeServiceDao.saveRecipe(updatedRecipe);
        List<Recipe> savedRecipes = recipeServiceDao.getNavigationRecipesById(
            Collections.singletonList(updatedRecipe.getId()),
            RecipeSqlFilter.Field.GOOD_STATE,
            true
        );

        assertThat(created).isFalse();
        assertThat(savedRecipes)
            .usingElementComparatorIgnoringFields(IGNORED_FIELDS)
            .containsExactly(updatedRecipe);

        recipeServiceDao.deleteRecipes(0L, Collections.singletonList(updatedRecipe.getId()));

        List<Recipe> recipesAfterDelete = recipeServiceDao.getNavigationRecipesById(
            Collections.singletonList(updatedRecipe.getId()),
            RecipeSqlFilter.Field.GOOD_STATE,
            true
        );

        assertThat(recipesAfterDelete).isEmpty();
    }

    public Recipe createRecipe() {
        return RandomTestUtils.randomObject(Recipe.class, "id");
    }
}

package ru.yandex.market.mbo.integration.test.params;

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.market.mbo.common.processing.ConcurrentUpdateException;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.db.MeasureUnitCache;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterDAO;
import ru.yandex.market.mbo.db.params.row.mappers.ParameterRowMapper;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ParameterDAOTest extends BaseIntegrationTest {

    private static final RowMapper<Tuple2<Option, Long>> OPTION_ROW_MAPPER =
        (rs, rowNum) -> {
            OptionImpl option = new OptionImpl();
            option.setId(rs.getLong("id"));
            option.setParamId(rs.getLong("param_id"));
            option.setPublished(rs.getBoolean("published"));
            option.setActive(rs.getBoolean("active"));
            option.setDontUseAsAlias(rs.getBoolean("do_not_use_as_alias"));
            option.setTag(rs.getString("tag"));
            option.setCode(rs.getString("color_code"));
            option.setNumericValue(rs.getBigDecimal("numeric_value"));
            option.setPriority(rs.getInt("priority"));
            option.setDefaultForMatcher(rs.getBoolean("default_for_matcher"));
            option.setTopValue(rs.getBoolean("is_top_value"));
            option.setDefaultValue(rs.getBoolean("is_default_value"));
            option.setFilterValue(rs.getBoolean("is_filter_value"));
            option.setInheritanceStrategy(Option.InheritanceStrategy.valueOf(rs.getString("inheritance_strategy")));
            option.setDisplayName(rs.getString("display_name"));
            option.setComment(rs.getString("comments"));
            long categoryId = rs.getLong("category_id");
            if (!rs.wasNull()) {
                option.setCategoryId(categoryId);
            }
            Long parentOptionId = rs.getLong("parent_option_id");
            return Tuple2.tuple(option, parentOptionId);
        };

    @Inject
    JdbcTemplate contentJdbcTemplate;

    @Inject
    MboDbSelector funcTestsDbSelector;

    @Inject
    private ParameterDAO parameterDAO;

    @Resource(name = "parameterLoaderService")
    private IParameterLoaderService parameterLoader;

    private ParameterRowMapper parameterRowMapper;


    @Before
    public void setUp() {
        parameterRowMapper = new ParameterRowMapper(Mockito.mock(MeasureUnitCache.class));
    }

    @Test
    @Transactional("contentTransactionManager")
    public void insertParameter() {
        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        Parameter parameter = p.build();

        parameterDAO.insertParameter(parameter);

        CategoryParam resultParam = contentJdbcTemplate.queryForObject(
            "select p.*, '' join_tag, 0 join_lang_id, '' formalizer_tag from parameter p where p.id = ?",
            parameterRowMapper,
            parameter.getId()
        );

        assertThat(resultParam).isEqualToComparingOnlyGivenFields(parameter,
            "id",
            "xslName",
            "mdmParameter"
        );
    }

    @Test
    @Transactional("contentTransactionManager")
    public void insertEnumOption() {
        Option option = insertOption();
        checkOptionEnumEquals(option);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void updateEnumOption() {
        Option option = insertOption();
        Option updatedOption = updateOption(option);
        checkOptionEnumEquals(updatedOption);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void inheritEnumOption() {
        Option option = insertOption();
        Option inherited = new OptionImpl(option);
        inherited.setPublished(true);
        insertOption(inherited);
        inherited.setPublished(false);
        Option updatedOption = updateOptionInherited(inherited);
        checkOptionEnumEquals(updatedOption);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void insertBoolOptionNoPKError() {
        insertOptionBool();
        insertOptionBool();
        insertOptionBool();
    }

    @Test
    @Transactional("contentTransactionManager")
    public void getXslNameDuplication() {
        TovarCategory category = RandomTestUtils.randomObject(TovarCategory.class);
        insertCategoryWithName(category);

        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        p.setCategoryHid(category.getHid());
        Parameter parameter = p.build();
        parameterDAO.insertParameter(parameter);

        List<ParameterInfo> duplication = parameterDAO
            .getXslNameDuplication(category.getHid(), parameter.getXslName());

        ParameterInfo expected = new ParameterInfo(parameter.getId(), parameter.getXslName(),
            category.getHid(), category.getName());
        assertThat(duplication).usingFieldByFieldElementComparator().containsExactly(expected);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void getXslNameDuplicationWithoutCategoryName() {
        TovarCategory category = RandomTestUtils.randomObject(TovarCategory.class);
        insertCategoryWithName(category);

        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        p.setCategoryHid(category.getHid());
        Parameter parameter = p.build();
        parameterDAO.insertParameter(parameter);

        List<ParameterInfo> duplication = parameterDAO
            .getXslNameDuplication(category.getHid(), parameter.getXslName());

        ParameterInfo expected = new ParameterInfo(parameter.getId(), parameter.getXslName(),
            category.getHid(), category.getName());
        assertThat(duplication).usingFieldByFieldElementComparator().containsExactly(expected);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void getXslNameDuplicationWithInherited() {
        TovarCategory parentCategory = RandomTestUtils.randomObject(TovarCategory.class);
        insertCategoryWithName(parentCategory);

        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        p.setCategoryHid(parentCategory.getHid());
        Parameter parameter = p.build();
        parameterDAO.insertParameter(parameter);

        TovarCategory childCategory = RandomTestUtils.randomObject(TovarCategory.class);
        childCategory.setParentHid(parentCategory.getHid());
        insertCategory(childCategory);

        List<ParameterInfo> duplication = parameterDAO
            .getXslNameDuplication(childCategory.getHid(), parameter.getXslName());

        ParameterInfo expected = new ParameterInfo(parameter.getId(), parameter.getXslName(),
            parentCategory.getHid(), parentCategory.getName());
        assertThat(duplication).usingFieldByFieldElementComparator().containsExactly(expected);
    }

    @Test
    @Transactional("contentTransactionManager")
    public void getXslNameDuplicationWithBrokenParamInheritanceFindsNothing() {
        TovarCategory parentCategory = RandomTestUtils.randomObject(TovarCategory.class);
        insertCategory(parentCategory);

        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        p.setCategoryHid(parentCategory.getHid());
        Parameter parameter = p.build();
        parameterDAO.insertParameter(parameter);

        insertParamBreakInheritance(parameter);

        TovarCategory childCategory = RandomTestUtils.randomObject(TovarCategory.class);
        childCategory.setParentHid(parentCategory.getHid());
        insertCategory(childCategory);

        List<ParameterInfo> duplication = parameterDAO
            .getXslNameDuplication(childCategory.getHid(), parameter.getXslName());

        assertThat(duplication).isEmpty();
    }

    @Test
    public void testParameterUpdateIsConsistent() {
        TovarCategory category = RandomTestUtils.randomObject(TovarCategory.class);
        category.setHid(IParameterLoaderService.GLOBAL_ENTITIES_HID);
        insertCategory(category);

        CategoryParamBuilder p = RandomTestUtils.randomObject(CategoryParamBuilder.class);
        p.setCategoryHid(category.getHid());

        Parameter parameter = p.build();
        parameterDAO.insertParameter(parameter);

        CategoryParam param = parameterLoader.loadParameter(parameter.getId());
        parameterDAO.updateParameter(param, false);
        assertThatThrownBy(() -> parameterDAO.updateParameter(param, false))
            .isInstanceOf(ConcurrentUpdateException.class);
    }

    private void checkOptionEnumEquals(Option option) {
        Option resultOption = null;
        Option currentOption = null;
        boolean set = false;
        Tuple2<Option, Long> tuple;
        Long id = option.getId();
        do {
            tuple = contentJdbcTemplate.queryForObject(
                "select * from enum_option where id = ?", OPTION_ROW_MAPPER, id
            );
            Option got = tuple._1;
            id = tuple._2;
            if (!set) {
                resultOption = got;
                currentOption = resultOption;
                set = true;
            } else {
                currentOption.setParent(got);
                currentOption = got;
            }
        } while (id != null && id != 0);
        assertThat(resultOption).isEqualToComparingFieldByField(option);
    }

    private Option insertOption() {
        OptionBuilder optionBuilder = RandomTestUtils.randomObject(OptionBuilder.class);
        Option option = optionBuilder.build();
        // init with default boolean values
        option.setDefaultForMatcher(false);
        option.setActive(false);
        option.setComment("some_comment");
        Parameter parameter = CategoryParamBuilder.newBuilder().setId(option.getParamId()).build();
        parameterDAO.insertEnumOption(parameter, (OptionImpl) option);
        return option;
    }

    private Option insertOption(Option option) {
        Parameter parameter = CategoryParamBuilder.newBuilder().setId(option.getParamId()).build();
        parameterDAO.insertEnumOption(parameter, (OptionImpl) option);
        return option;
    }

    private Option insertOptionBool() {
        OptionBuilder optionBuilder = RandomTestUtils.randomObject(OptionBuilder.class);
        optionBuilder.setId(0);
        Option option = optionBuilder.build();
        Parameter parameter = CategoryParamBuilder.newBuilder().setId(option.getParamId()).build();
        parameterDAO.insertBoolOption(parameter, option);
        return option;
    }

    private Option updateOption(Option optionToUpdate) {
        OptionBuilder optionBuilder = RandomTestUtils.randomObject(OptionBuilder.class);
        Option option = optionBuilder.build();
        option.setId(optionToUpdate.getId());
        option.setParamId(optionToUpdate.getParamId());
        // init with default boolean values
        option.setDefaultForMatcher(false);
        option.setActive(false);
        parameterDAO.updateEnumOption(option);
        return option;
    }

    private Option updateOptionInherited(Option inheritedOptionToUpdate) {
        parameterDAO.updateEnumOption(inheritedOptionToUpdate);
        return inheritedOptionToUpdate;
    }

    private void insertParamBreakInheritance(Parameter parameter) {
        contentJdbcTemplate.update(
            "insert into gl_param_break_inheritance (CATEGORY_ID, PARAM_ID) values (?, ?)",
            parameter.getCategoryHid(),
            parameter.getId()
        );
    }

    private void insertCategoryWithName(TovarCategory category) {
        insertCategory(category);
        insertCategoryName(category);
    }

    private void insertCategory(TovarCategory category) {
        funcTestsDbSelector.getJdbcTemplate().update(
            "insert into mc_category (hyper_id, tovar_id, tovar_entity_id, parent_id) values (?, ?, ?, ?)",
            category.getHid(),
            category.getTovarId(),
            category.getId(),
            category.getParentHid()
        );
    }

    private void insertCategoryName(TovarCategory category) {
        funcTestsDbSelector.getJdbcTemplate().update(
            "insert into mc_category_name (ID, CATEGORY_ID, NAME, LANG_ID) values (?, ?, ?, 225)",
            category.getHid(),
            category.getHid(),
            category.getName()
        );
    }
}

package ru.yandex.market.sqb.service.config.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.model.common.NameValueModel;
import ru.yandex.market.sqb.model.conf.AliasModel;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.model.conf.TemplateModel;
import ru.yandex.market.sqb.model.vo.AliasVO;
import ru.yandex.market.sqb.model.vo.IncludeVO;
import ru.yandex.market.sqb.model.vo.ParameterVO;
import ru.yandex.market.sqb.model.vo.QueryVO;
import ru.yandex.market.sqb.model.vo.TemplateVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractModelConverterTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link QueryModelConverter}.
 *
 * @author Vladislav Bauer
 */
public class QueryModelConverterTest extends AbstractModelConverterTest<QueryVO, QueryModel> {

    private static final String TEST_BASE = "test query base";
    private static final String TEST_DESCRIPTION = "test query description";


    @Test
    void testSinglePositive() {
        final QueryModelConverter converter = createConverter();
        final QueryVO queryVO = createObject();
        final QueryModel queryModel = converter.convert(queryVO);

        assertThat(queryModel.getBase(), equalTo(queryVO.getBase()));
    }

    @Test
    void testAliasesWhenOkCompositeConversion() {
        QueryModel queryModel = convertSimpleCompositionModel();
        List<AliasModel> aliases = queryModel.getAliases();
        List<String> aliasNames = aliases.stream().map(AliasModel::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(
                aliasNames,
                Matchers.containsInAnyOrder("QUOTE_MARK", "TAB", "SPACE")
        );
    }

    @Test
    void testParametersWhenOkCompositeConversion() {
        QueryModel queryModel = convertSimpleCompositionModel();
        List<ParameterModel> parameters = queryModel.getParameters();
        List<String> parameterNames = parameters.stream()
                .map(NameValueModel<String>::getName)
                .collect(Collectors.toList());

        MatcherAssert.assertThat(
                parameterNames,
                Matchers.containsInAnyOrder("RED_MARKET", "DELIVERY_COST", "ENABLED")
        );
    }

    @Test
    void testTemplatesWhenOkCompositeConversion() {
        QueryModel queryModel = convertSimpleCompositionModel();
        List<TemplateModel> templates = queryModel.getTemplates();
        List<String> templateNames = templates.stream().map(TemplateModel::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(
                templateNames,
                containsInAnyOrder("PARAM_VALUE", "FEATURE_VALUE", "CONST")
        );
    }

    @Test
    void testDuplicateAliases() {
        QueryModelConverter converter = createConverter();
        QueryVO simpleObject = createSimpleObject();
        QueryModel includeModel = new QueryModel.Builder()
                .setAliases(ImmutableList.of(
                        new AliasModel("TAB", "Tab")
                ))
                .setOrders(Collections.emptyList())
                .setParameters(Collections.emptyList())
                .setTemplates(Collections.emptyList())
                .setMeta(StringUtils.EMPTY)
                .setBase(StringUtils.EMPTY)
                .setDescription(StringUtils.EMPTY)
                .setInclude(null)
                .build();
        Assertions.assertThrows(SqbValidationException.class, () -> converter.convert(simpleObject, includeModel));
    }

    @Test
    void testDuplicateParams() {
        QueryModelConverter converter = createConverter();
        QueryVO simpleObject = createSimpleObject();
        QueryModel includeModel = new QueryModel.Builder()
                .setAliases(Collections.emptyList())
                .setOrders(Collections.emptyList())
                .setParameters(
                        ImmutableList.of(
                                new ParameterModel(
                                        "RED_MARKET",
                                        "1",
                                        "Description",
                                        "boolean",
                                        "",
                                        "",
                                        null,
                                        Collections.emptyList()
                                )
                        ))
                .setTemplates(Collections.emptyList())
                .setMeta(StringUtils.EMPTY)
                .setBase(StringUtils.EMPTY)
                .setDescription(StringUtils.EMPTY)
                .setInclude(null)
                .build();
        Assertions.assertThrows(SqbValidationException.class, () -> converter.convert(simpleObject, includeModel));
    }

    @Test
    void testDuplicateTemplates() {
        QueryModelConverter converter = createConverter();
        QueryVO simpleObject = createSimpleObject();
        QueryModel includeModel = new QueryModel.Builder()
                .setAliases(Collections.emptyList())
                .setOrders(Collections.emptyList())
                .setParameters(Collections.emptyList())
                .setTemplates(ImmutableList.of(
                        new TemplateModel(
                                "FEATURE_VALUE",
                                "1",
                                "",
                                "NUMERIC"
                        )
                ))
                .setMeta(StringUtils.EMPTY)
                .setBase(StringUtils.EMPTY)
                .setDescription(StringUtils.EMPTY)
                .setInclude(null)
                .build();
        Assertions.assertThrows(SqbValidationException.class, () -> converter.convert(simpleObject, includeModel));
    }


    @Nonnull
    @Override
    protected QueryVO createObject() {
        final QueryVO queryVO = new QueryVO();
        queryVO.setBase(TEST_BASE);
        queryVO.setDescription(TEST_DESCRIPTION);
        queryVO.setAliases(Collections.emptyList());
        queryVO.setTemplates(Collections.emptyList());
        queryVO.setParameters(Collections.emptyList());
        return queryVO;
    }

    private QueryModel convertSimpleCompositionModel() {
        QueryModelConverter converter = createConverter();
        QueryVO simpleObject = createSimpleObject();
        QueryModel queryModel = createSimpleQuery();
        return converter.convert(simpleObject, queryModel);
    }

    private QueryVO createSimpleObject() {
        QueryVO queryVO = new QueryVO();
        queryVO.setBase(QueryVO.TEMPLATE);
        queryVO.setDescription(TEST_DESCRIPTION);
        queryVO.setAliases(
                ImmutableList.of(
                        createAlias("TAB", "\t"),
                        createAlias("SPACE", " ")
                )
        );
        queryVO.setTemplates(
                ImmutableList.of(
                        createTemplate("PARAM_VALUE", "10"),
                        createTemplate("FEATURE_VALUE", "SUCCESS")
                )
        );
        queryVO.setParameters(
                ImmutableList.of(
                        createParameter("RED_MARKET", "SUCCESS"),
                        createParameter("DELIVERY_COST", "10")
                )
        );
        IncludeVO includeVO = new IncludeVO();
        includeVO.setName("common-data.xml");
        includeVO.setOverride(false);
        queryVO.setInclude(includeVO);
        return queryVO;
    }

    private AliasVO createAlias(String name, String value) {
        AliasVO aliasVO = new AliasVO();
        aliasVO.setName(name);
        aliasVO.setValue(value);
        return aliasVO;
    }

    private TemplateVO createTemplate(String name, String sql) {
        TemplateVO templateVO = new TemplateVO();
        templateVO.setName(name);
        templateVO.setSql(sql);
        templateVO.setCondition("join");
        return templateVO;
    }

    private ParameterVO createParameter(String name, String value) {
        ParameterVO parameterVO = new ParameterVO();
        parameterVO.setName(name);
        parameterVO.setValueAttr(value);
        parameterVO.setDescriptionAttr("description");
        return parameterVO;
    }

    private QueryModel createSimpleQuery() {
        return new QueryModel.Builder()
                .setAliases(
                        Collections.singletonList(
                                new AliasModel("QUOTE_MARK", "chr(148")
                        )
                ).setOrders(Collections.emptyList())
                .setMeta("meta")
                .setBase("base")
                .setDescription("description")
                .setInclude(null)
                .setTemplates(
                        ImmutableList.of(
                                new TemplateModel(
                                        "CONST",
                                        "1",
                                        "",
                                        "NUMERIC"
                                )
                        )
                ).setParameters(
                        ImmutableList.of(
                                new ParameterModel(
                                        "ENABLED",
                                        "1",
                                        "Description",
                                        "boolean",
                                        "",
                                        "",
                                        null,
                                        Collections.emptyList()
                                )
                        )
                ).build();
    }

    @Nonnull
    @Override
    protected QueryModelConverter createConverter() {
        return new QueryModelConverter();
    }

}

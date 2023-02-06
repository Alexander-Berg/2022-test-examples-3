package ru.yandex.market.sqb.service.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.conf.ArgumentModel;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;
import ru.yandex.market.sqb.util.SqbRenderingUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.CREATOR_ALIAS;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.CREATOR_ARGUMENT;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.GENERATED_COUNT;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.PREFIX_ALIAS;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.PREFIX_ARGUMENT;
import static ru.yandex.market.sqb.test.TestUtils.checkConstructor;

/**
 * Unit-тесты для {@link QueryBuilderParamFactory}.
 *
 * @author Vladislav Bauer
 */
class QueryBuilderParamFactoryTest {

    @Test
    void testConstructorContract() {
        checkConstructor(QueryBuilderParamFactory.class);
    }

    @Test
    void testCreateQueryParams() {
        final String namesBlock = ObjectGenerationUtils.createName();
        final String queriesBlock = ObjectGenerationUtils.createName();
        final QueryModel query = createQuery(GENERATED_COUNT);

        final Map<String, String> params =
                QueryBuilderParamFactory.createQueryParams(query, namesBlock, queriesBlock);

        assertThat(params.size(), equalTo(GENERATED_COUNT + 2));
        assertThat(params.get(SqbRenderingUtils.SYS_PARAM_NAMES_BLOCK), equalTo(namesBlock));
        assertThat(params.get(SqbRenderingUtils.SYS_PARAM_QUERIES_BLOCK), equalTo(queriesBlock));
        checkGeneratedParams(params, PREFIX_ALIAS);
    }

    @Test
    void testCreateParameterParams() {
        final QueryModel query = createQuery(GENERATED_COUNT);
        final ParameterModel parameter = createParameter(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        final String queryName = ObjectGenerationUtils.createName();

        final Map<String, String> params =
                QueryBuilderParamFactory.createParameterParams(query, parameter, queryName);

        assertThat(params.get(SqbRenderingUtils.SYS_QUERY_NAME), equalTo(queryName));
        assertThat(params.get(SqbRenderingUtils.PARAM_NAME), equalTo(parameter.getName()));
        assertThat(params.get(SqbRenderingUtils.PARAM_VALUE), equalTo(parameter.getValue()));
        checkGeneratedParams(params, PREFIX_ALIAS);
        checkGeneratedParams(params, SqbRenderingUtils.PREFIX_PARAM + PREFIX_ARGUMENT);
    }

    @Test
    void testCreateParameterValueParams() {
        final QueryModel query = createQuery(GENERATED_COUNT);
        final ParameterModel parameter = createParameter(null, null, null);
        final String queryName = ObjectGenerationUtils.createName();

        final Map<String, String> params =
                QueryBuilderParamFactory.createParameterValueParams(query, parameter, queryName);

        assertThat(params.get(SqbRenderingUtils.SYS_QUERY_NAME), nullValue());
        assertThat(params.get(SqbRenderingUtils.PARAM_NAME), nullValue());
        assertThat(params.get(SqbRenderingUtils.PARAM_VALUE), nullValue());
        checkGeneratedParams(params, PREFIX_ALIAS);
        checkGeneratedParams(params, SqbRenderingUtils.PREFIX_PARAM + PREFIX_ARGUMENT);
    }

    @Test
    void testCreateAliasParams() {
        final QueryModel query = createQuery(GENERATED_COUNT);
        final Map<String, String> params = QueryBuilderParamFactory.createAliasParams(query);

        assertThat(params.size(), equalTo(GENERATED_COUNT));
        checkGeneratedParams(params, PREFIX_ALIAS);
    }

    @Test
    void testCreateArgumentParams() {
        final ParameterModel parameter = createParameter(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        final Map<String, String> params = QueryBuilderParamFactory.createArgumentParams(parameter);

        assertThat(params.size(), equalTo(GENERATED_COUNT));
        checkGeneratedParams(params, SqbRenderingUtils.PREFIX_PARAM + PREFIX_ARGUMENT);
    }


    private QueryModel createQuery(final int aliasCount) {
        return new QueryModel.Builder()
                .setAliases(ObjectGenerationUtils.createObjects(aliasCount, CREATOR_ALIAS))
                .setOrders(Collections.emptyList())
                .setParameters(Collections.emptyList())
                .setTemplates(Collections.emptyList())
                .setMeta(StringUtils.EMPTY)
                .setBase(StringUtils.EMPTY)
                .setDescription(StringUtils.EMPTY)
                .setInclude(null)
                .build();
    }

    private ParameterModel createParameter(final String sql, final String condition, final String template) {
        final List<ArgumentModel> arguments = ObjectGenerationUtils.createObjects(GENERATED_COUNT, CREATOR_ARGUMENT);
        return new ParameterModel(
                StringUtils.EMPTY,
                StringUtils.EMPTY,
                StringUtils.EMPTY,
                StringUtils.EMPTY,
                sql,
                condition,
                template,
                arguments
        );
    }

    private void checkGeneratedParams(final Map<String, String> params, final String prefix) {
        IntStream.range(0, GENERATED_COUNT).forEach(
                index -> assertThat(
                        params.get(prefix + index),
                        equalTo(String.valueOf(index))
                )
        );
    }

}

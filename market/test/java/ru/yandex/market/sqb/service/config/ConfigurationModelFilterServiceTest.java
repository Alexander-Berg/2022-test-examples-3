package ru.yandex.market.sqb.service.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.common.HasName;
import ru.yandex.market.sqb.model.conf.AliasModel;
import ru.yandex.market.sqb.model.conf.OrderModel;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.model.conf.TemplateModel;
import ru.yandex.market.sqb.model.filter.QueryModelFilter;
import ru.yandex.market.sqb.model.filter.QueryModelFilterBuilder;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Unit-тесты для {@link ConfigurationModelFilterService}.
 *
 * @author Vladislav Bauer
 */
class ConfigurationModelFilterServiceTest {

    private static final List<AliasModel> ALIASES = ObjectGenerationUtils.createObjects(
            ObjectGenerationUtils.GENERATED_COUNT,
            ObjectGenerationUtils.CREATOR_ALIAS
    );

    private static final List<TemplateModel> TEMPLATES = ObjectGenerationUtils.createObjects(
            ObjectGenerationUtils.GENERATED_COUNT,
            ObjectGenerationUtils.CREATOR_TEMPLATE
    );

    private static final List<ParameterModel> PARAMETERS = ObjectGenerationUtils.createObjects(
            ObjectGenerationUtils.GENERATED_COUNT,
            ObjectGenerationUtils.CREATOR_PARAMETER
    );

    private static final QueryModelFilter FILTER_NONE = createFilterNone();
    private static final QueryModelFilter FILTER_ALL = createFilterAll();


    @Test
    void testFilterAll() {
        checkFilter(ALIASES, TEMPLATES, PARAMETERS, FILTER_ALL, not(empty()));
    }

    @Test
    void testFilterAliases() {
        checkFilter(ALIASES, null, null, FILTER_NONE, empty());
    }

    @Test
    void testFilterTemplates() {
        checkFilter(null, TEMPLATES, null, FILTER_NONE, empty());
    }

    @Test
    void testFilterParameters() {
        checkFilter(null, null, PARAMETERS, FILTER_NONE, empty());
    }


    private void checkFilter(
            final List<AliasModel> aliases, final List<TemplateModel> templates, final List<ParameterModel> parameters,
            final QueryModelFilter filter, final Matcher<Collection<?>> matcher
    ) {
        final List<OrderModel> orders = Collections.emptyList();
        final QueryModel query = new QueryModel.Builder()
                .setAliases(aliases)
                .setOrders(orders)
                .setTemplates(templates)
                .setParameters(parameters)
                .setDescription(EMPTY)
                .setBase(EMPTY)
                .setMeta(EMPTY)
                .setInclude(null)
                .build();
        final ConfigurationModelFilterService service = new ConfigurationModelFilterService();

        final QueryModel newQuery = service.filter(query, filter);
        assertThat(newQuery.getAliases(), matcher);
        assertThat(newQuery.getTemplates(), matcher);
        assertThat(newQuery.getParameters(), matcher);
    }

    private static QueryModelFilter createFilterNone() {
        return new QueryModelFilterBuilder()
                .setParameterFilters(createPredicatesNone())
                .setAliasFilters(createPredicatesNone())
                .setTemplateFilters(createPredicatesNone())
                .build();
    }

    private static QueryModelFilter createFilterAll() {
        return new QueryModelFilterBuilder()
                .setParameterFilters(createPredicatesAll())
                .setAliasFilters(createPredicatesAll())
                .setTemplateFilters(createPredicatesAll())
                .build();
    }

    private static <T extends HasName> List<Predicate<T>> createPredicatesAll() {
        return Collections.singletonList((value) -> true);
    }

    private static <T extends HasName> List<Predicate<T>> createPredicatesNone() {
        return Arrays.asList(
                HasName.byNames(ObjectGenerationUtils.createName()),
                Predicate.isEqual(null)
        );
    }

}

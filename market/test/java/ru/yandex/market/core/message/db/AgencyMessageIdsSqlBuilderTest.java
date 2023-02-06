package ru.yandex.market.core.message.db;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.core.message.model.HeaderFilter;
import ru.yandex.market.mbi.util.db.jdbc.TNumberTbl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit тесты для {@link AgencyMessageIdsSqlBuilder}.
 *
 * @author avetokhin 16/12/16.
 */
public class AgencyMessageIdsSqlBuilderTest extends UserMessageIdsSqlBuilderTest {

    private static final long AGENCY_ID = 1;

    @Test
    public void buildQueryParamsTestNoFilterMultipleShops() {
        final Set<Long> availableShops = Stream.of(1L, 2L).collect(Collectors.toSet());

        final UserMessageIdsSqlBuilder builder = new AgencyMessageIdsSqlBuilder(AGENCY_ID, availableShops);
        checkDefault(builder, paramMap -> {
            assertThat(paramMap.get(AgencyMessageIdsSqlBuilder.PARAM_SHOP_IDS), instanceOf(TNumberTbl.class));
            checkCommonParams(paramMap, null, null, null);
        });

    }

    @Test
    public void buildQueryParamsTestNoFilterSingleShop() {
        final Long shopId = 2L;
        final Set<Long> availableShops = Collections.singleton(shopId);

        final UserMessageIdsSqlBuilder builder = new AgencyMessageIdsSqlBuilder(AGENCY_ID, availableShops);
        checkDefault(builder, paramMap -> {
            assertThat(paramMap.get(AgencyMessageIdsSqlBuilder.PARAM_SHOP_ID), equalTo(shopId));
            checkCommonParams(paramMap, null, null, null);
        });
    }


    @Test
    public void buildQueryParamsTestWithEmptyFilterSingleShop() {
        final Long shopId = 2L;
        final Set<Long> availableShops = Collections.singleton(shopId);

        final HeaderFilter filter = HeaderFilter.newBuilder().build();

        final UserMessageIdsSqlBuilder builder = new AgencyMessageIdsSqlBuilder(AGENCY_ID, availableShops)
                .withHeaderFilter(filter);
        checkDefault(builder, paramMap -> {
            assertThat(paramMap.get(AgencyMessageIdsSqlBuilder.PARAM_SHOP_ID), equalTo(shopId));
            checkCommonParams(paramMap, null, null, null);
        });
    }

    @Test
    public void buildQueryParamsTestWithFilterSingleShop() {
        final Long shopId = 2L;
        final Set<Long> availableShops = Collections.singleton(shopId);

        final Date fromDate = new Date();
        final Date toDate = new Date();
        final Long themeId = 5L;

        final HeaderFilter filter = HeaderFilter.newBuilder()
                .withDateTo(toDate).withDateFrom(fromDate).withThemeId(themeId).build();

        final UserMessageIdsSqlBuilder builder = new AgencyMessageIdsSqlBuilder(AGENCY_ID, availableShops)
                .withHeaderFilter(filter);
        checkDefault(builder, paramMap -> {
            assertThat(paramMap.get(AgencyMessageIdsSqlBuilder.PARAM_SHOP_ID), equalTo(shopId));
            checkCommonParams(paramMap, themeId, Timestamp.from(fromDate.toInstant()), Timestamp.from(toDate.toInstant()));
        });
    }

    private void checkDefault(final UserMessageIdsSqlBuilder builder, final TestCase test) {
        final MapSqlParameterSource params = builder.buildQueryParams();

        assertThat(params, notNullValue());
        final Map<String, Object> paramMap = params.getValues();

        final Object agencyId = paramMap.get(AgencyMessageIdsSqlBuilder.PARAM_AGENCY_ID);
        assertThat(agencyId, equalTo(AGENCY_ID));

        test.test(paramMap);
    }
}

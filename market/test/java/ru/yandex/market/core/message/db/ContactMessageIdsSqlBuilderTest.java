package ru.yandex.market.core.message.db;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.core.message.model.HeaderFilter;
import ru.yandex.market.core.message.model.UserMessageAccess;
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
public class ContactMessageIdsSqlBuilderTest extends UserMessageIdsSqlBuilderTest {

    private static final long USER_ID = 1;
    private static final Set<UserMessageAccess> ACCESS_SET =
            Stream.of(new UserMessageAccess(1L, null), new UserMessageAccess(2L, 3L)).collect(Collectors.toSet());

    @Test
    public void buildQueryParamsTestNoFilter() {
        final UserMessageIdsSqlBuilder builder = new ContactMessageIdsSqlBuilder(USER_ID, ACCESS_SET);
        checkDefault(builder, paramMap -> checkCommonParams(paramMap, null, null, null));
    }

    @Test
    public void buildQueryParamsTestWithEmptyFilter() {
        final HeaderFilter filter = HeaderFilter.newBuilder().build();
        final UserMessageIdsSqlBuilder builder = new ContactMessageIdsSqlBuilder(USER_ID, ACCESS_SET)
                .withHeaderFilter(filter);

        checkDefault(builder, paramMap -> checkCommonParams(paramMap, null, null, null));
    }

    @Test
    public void buildQueryParamsTestWithFilterSingleShop() {
        final Date fromDate = new Date();
        final Date toDate = new Date();
        final Long themeId = 5L;

        final HeaderFilter filter = HeaderFilter.newBuilder()
                .withDateTo(toDate).withDateFrom(fromDate).withThemeId(themeId).build();

        final UserMessageIdsSqlBuilder builder = new ContactMessageIdsSqlBuilder(USER_ID, ACCESS_SET)
                .withHeaderFilter(filter);
        checkDefault(builder, paramMap -> {
            checkCommonParams(paramMap, themeId, Timestamp.from(fromDate.toInstant()), Timestamp.from(toDate.toInstant()));
        });
    }

    private void checkDefault(final UserMessageIdsSqlBuilder builder, final TestCase test) {
        final MapSqlParameterSource params = builder.buildQueryParams();

        assertThat(params, notNullValue());
        final Map<String, Object> paramMap = params.getValues();

        assertThat(paramMap.get(UserMessageIdsSqlBuilder.PARAM_CONTACT_ACCESS_SET), instanceOf(TNumberTbl.class));

        final Object userId = paramMap.get(UserMessageIdsSqlBuilder.PARAM_USER_ID);
        assertThat(userId, equalTo(USER_ID));

        test.test(paramMap);
    }
}

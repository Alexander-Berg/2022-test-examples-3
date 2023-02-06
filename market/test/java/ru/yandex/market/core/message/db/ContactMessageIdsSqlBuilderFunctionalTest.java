package ru.yandex.market.core.message.db;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.message.model.UserMessageAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Unit тесты для {@link AgencyMessageIdsSqlBuilder}.
 *
 * @author avetokhin 16/12/16.
 */
public class ContactMessageIdsSqlBuilderFunctionalTest extends FunctionalTest {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;


    private static final long USER_ID = 1;
    private static final Set<UserMessageAccess> ACCESS_SET =
            Stream.of(new UserMessageAccess(1L, 4L), new UserMessageAccess(2L, 3L)).collect(Collectors.toSet());

    @Test
    @DbUnitDataSet(before = "ContactMessageIdsSqlBuilderFunctionalTest.before.csv")
    public void buildQueryParamsTestNoFilterFunctional() {

        final UserMessageIdsSqlBuilder builder = new ContactMessageIdsSqlBuilder(USER_ID, ACCESS_SET);

        assertEquals(101, getQuery(builder));
    }

    private Integer getQuery(UserMessageIdsSqlBuilder builder) {
        return jdbcTemplate.queryForObject(builder.buildSql(), builder.buildQueryParams(), Integer.class);
    }

}

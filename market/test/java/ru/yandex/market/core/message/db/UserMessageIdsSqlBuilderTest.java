package ru.yandex.market.core.message.db;

import java.sql.Timestamp;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Ignore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link UserMessageIdsSqlBuilder}.
 *
 * @author avetokhin 16/12/16.
 */
@Ignore
public abstract class UserMessageIdsSqlBuilderTest {

    /**
     * Проверить общие параметры.
     */
    protected void checkCommonParams(@Nonnull final Map<String, Object> paramMap,
                                     @Nullable final Long themeId,
                                     @Nullable final Timestamp fromDate,
                                     @Nullable final Timestamp toDate) {
        assertThat(themeId, equalTo(paramMap.get(UserMessageIdsSqlBuilder.PARAM_THEME_ID)));
        assertThat(fromDate, equalTo(paramMap.get(UserMessageIdsSqlBuilder.PARAM_DATE_FROM)));
        assertThat(toDate, equalTo(paramMap.get(UserMessageIdsSqlBuilder.PARAM_DATE_TO)));
    }

    protected interface TestCase {
        void test(final Map<String, Object> params);
    }

}

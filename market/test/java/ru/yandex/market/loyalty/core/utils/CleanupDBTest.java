package ru.yandex.market.loyalty.core.utils;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CoreCollectionUtils.minus;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CleanupDBTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Pattern truncateEntryPattern = Pattern.compile("(?<tablename>[A-Za-z0-9_]+)[,]?");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void shouldAllTablesTruncated() throws IOException {
        Set<String> allTables = allTables();
        Set<String> truncatedTables = getTruncatedTables();

        assertThat(minus(allTables, truncatedTables), containsInAnyOrder("THROTTLING_CONTROL"));
        assertThat(minus(truncatedTables, allTables), is(empty()));
    }

    @NotNull
    private Set<String> getTruncatedTables() throws IOException {
        Stream.Builder<String> truncatedTables = Stream.builder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                resourceLoader.getResource("classpath:/sql/truncate_tables.sql").getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("cascade") && !line.contains("truncate")) {
                    Matcher m = truncateEntryPattern.matcher(line);
                    if (m.matches()) {
                        truncatedTables.accept(m.group("tablename"));
                    }
                }
            }
        }

        return toUpperCase(truncatedTables.build());
    }

    @NotNull
    private Set<String> allTables() {
        List<String> tables = jdbcTemplate.queryForList("" +
                        "SELECT tablename" +
                        "  FROM pg_tables" +
                        " WHERE schemaname = 'public'" +
                        "   AND tablename NOT IN (" +
                        "    'databasechangeloglock', " +
                        "    'databasechangelog', " +
                        "    'shedlock'," +
                        "    'last_checkouter_event_id'," +
                        "    'simple_transactions'," +
                        "    'uids_of_old_orders_in_application'," +
                        "    'exclusions_config'" +
                        ") ",
                String.class);

        return toUpperCase(tables.stream());
    }

    @NotNull
    private static Set<String> toUpperCase(Stream<String> stream) {
        return stream.map(String::toUpperCase).collect(Collectors.toSet());
    }
}

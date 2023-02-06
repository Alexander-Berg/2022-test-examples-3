package ru.yandex.market.notification.notifications;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyNotificationDataExtractorTest extends FunctionalTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void extractEmptyData() {
        var emptyResult = jdbcTemplate.query(
                "select * from shops_web.partner",
                new LegacyNotificationDataExtractor()
        );
        assertThat(emptyResult).isNull();
    }

    @Test
    void extractData() {
        jdbcTemplate.batchUpdate(
                "insert into shops_web.partner (id, type, manager_id) values (?, ?, ?)",
                List.of(
                        new Object[]{100501L, "whatever1", 0L},
                        new Object[]{100502L, "whatever2", 0L}
                )
        );

        var result = jdbcTemplate.query(
                "select * from shops_web.partner",
                new LegacyNotificationDataExtractor()
        );
        assertThat(result).isNotNull();
        assertThat(result.getHeaders().stream().map(String::toUpperCase))
                .containsExactlyInAnyOrder("ID", "TYPE", "MANAGER_ID");
        assertThat(result.getRows().stream()
                .map(r -> r.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().toUpperCase(),
                                Map.Entry::getValue
                        ))
                )
        ).containsExactlyInAnyOrder(
                Map.of("ID", "100501", "TYPE", "whatever1", "MANAGER_ID", "0"),
                Map.of("ID", "100502", "TYPE", "whatever2", "MANAGER_ID", "0")
        );
    }
}

package ru.yandex.market.tpl.tms.service.test_partition;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestTransferTableToPartitionService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String sqlParamCreatedAt = "created";
    private final String sqlCreateRecord = "insert into test_table_for_partition(external_id, created_at, info)" +
            " values (1, :" + sqlParamCreatedAt + ", 'TEST');";
    private final Clock clock;

    public void createNewRecord() {
        LocalDateTime time = LocalDateTime.now(clock);
        jdbcTemplate.update(sqlCreateRecord, new MapSqlParameterSource().addValue(sqlParamCreatedAt,
                time));
    }
}

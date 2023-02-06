package ru.yandex.market.tpl.tms.executor.system;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.core.domain.idempotence.CommandIdempotencyKey;
import ru.yandex.market.tpl.core.domain.idempotence.CommandIdempotencyKeyId;
import ru.yandex.market.tpl.core.domain.idempotence.TplCommandIdempotencyKeyRepository;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.hamcrest.Matchers.is;

@RequiredArgsConstructor
public class CleanIdempotencyKeyExecutorTest extends TplTmsAbstractTest {
    private final CleanIdempotencyKeyExecutor executor;
    private final TplCommandIdempotencyKeyRepository tplCommandIdempotencyKeyRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    @SneakyThrows
    @Test
    void shouldDeleteOldIdempotenceKeys() {
        CommandIdempotencyKey key = new CommandIdempotencyKey(new CommandIdempotencyKeyId(
                "command", new UUID(0x12345678, 0x90ABCDEF)
        ));

        tplCommandIdempotencyKeyRepository.save(key);

        ClockUtil.initFixed(clock, LocalDate.now().plusDays(5L).atTime(11, 49));

        executor.doRealJob(null);

        MatcherAssert.assertThat(countAllRecords(), is(0L));
    }

    @SneakyThrows
    @Test
    void shouldNotDeleteNewIdempotenceKeys() {
        CommandIdempotencyKey key = new CommandIdempotencyKey(new CommandIdempotencyKeyId(
                "command", new UUID(0x12345678, 0x90ABCDEF)
        ));

        ClockUtil.initFixed(clock, LocalDateTime.now());

        tplCommandIdempotencyKeyRepository.save(key);

        MatcherAssert.assertThat(countAllRecords(), is(1L));

        executor.doRealJob(null);

        MatcherAssert.assertThat(countAllRecords(), is(1L));
    }

    private Long countAllRecords() {
        return jdbcTemplate.queryForObject(
                "select count(*) from command_idempotency_key",
                Map.of(),
                Long.class
        );
    }
}

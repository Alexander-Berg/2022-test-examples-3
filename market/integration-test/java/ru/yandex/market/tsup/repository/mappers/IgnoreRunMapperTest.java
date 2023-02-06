package ru.yandex.market.tsup.repository.mappers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.user_log.IgnoreRun;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class IgnoreRunMapperTest extends AbstractContextualTest {

    private final IgnoreRun ignore1 = new IgnoreRun(
            1L,
            ZonedDateTime.of(2022, 1, 1, 12, 10, 0, 0, ZoneId.systemDefault())
    );

    private final IgnoreRun ignore1Plus1Hour = new IgnoreRun(
            1L,
            ZonedDateTime.of(2022, 1, 1, 13, 10, 0, 0, ZoneId.systemDefault())
    );

    private final IgnoreRun ignore2 = new IgnoreRun(
            2L,
            ZonedDateTime.of(2022, 1, 1, 14, 10, 0, 0, ZoneId.systemDefault())
    );

    @Autowired
    private IgnoreRunMapper ignoreRunMapper;

    @Test
    @ExpectedDatabase(
            value = "/repository/ignore_run/after_insert.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        ignoreRunMapper.insert(ignore1);
    }

    @Test
    @ExpectedDatabase(
            value = "/repository/ignore_run/after_insert_on_conflict.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertOnSameRunChangesItsIgnoreTo() {
        ignoreRunMapper.insert(ignore1);
        ignoreRunMapper.insert(ignore1Plus1Hour);
    }

    @Test
    void findByIds() {
        ignoreRunMapper.insert(ignore1);
        ignoreRunMapper.insert(ignore2);
        Long nonExistentId = 3L;
        var actual = ignoreRunMapper.findByIds(List.of(ignore1.getRunId(), ignore2.getRunId(), nonExistentId));
        assertThat(actual)
                .containsExactlyInAnyOrder(ignore1, ignore2);
    }

    @Test
    void findByIdsEmptyList() {
        assertThat(ignoreRunMapper.findByIds(Collections.emptyList())).isEmpty();
    }
}

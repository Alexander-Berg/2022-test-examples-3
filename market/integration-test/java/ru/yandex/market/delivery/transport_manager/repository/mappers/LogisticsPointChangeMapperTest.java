package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogisticPointChange;

public class LogisticsPointChangeMapperTest extends AbstractContextualTest {
    @Autowired
    private LogisticsPointChangeMapper mapper;

    private static final LogisticPointChange FIRST = new LogisticPointChange()
        .setId(1L)
        .setOldPointId(1L)
        .setNewPointId(2L)
        .setChangedAt(LocalDateTime.parse("2021-12-12T04:39:25"));

    private static final LogisticPointChange SECOND = new LogisticPointChange()
        .setId(2L)
        .setOldPointId(3L)
        .setNewPointId(4L)
        .setChangedAt(LocalDateTime.parse("2021-12-13T04:39:25"));

    @Test
    @ExpectedDatabase(
        value = "/repository/logistic_point/change/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        mapper.insert(List.of(FIRST, SECOND));
    }

    @Test
    @DatabaseSetup("/repository/logistic_point/change/changes.xml")
    @ExpectedDatabase(
        value = "/repository/logistic_point/change/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void delete() {
        mapper.delete(Set.of(3L, 4L));
    }

    @Test
    @DatabaseSetup("/repository/logistic_point/change/after/after_insert.xml")
    void get() {
        Set<LogisticPointChange> changes = mapper.get();
        softly.assertThat(changes).containsExactlyInAnyOrder(FIRST, SECOND);
    }


}

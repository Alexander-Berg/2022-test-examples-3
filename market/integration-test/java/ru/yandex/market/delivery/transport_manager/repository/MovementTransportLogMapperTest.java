package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementTransportLog;
import ru.yandex.market.delivery.transport_manager.domain.filter.RejectedTransportFilter;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementTransportLogMapper;

public class MovementTransportLogMapperTest extends AbstractContextualTest {

    @Autowired
    private MovementTransportLogMapper logMapper;

    @Autowired
    private MovementMapper movementMapper;

    private static final RejectedTransportFilter FILTER =
        new RejectedTransportFilter()
            .setTransport(2L);

    @Test
    @DatabaseSetup(
        value = {
            "/repository/transportation_task/transport_metadata.xml",
            "/repository/transportation_task/single_movement.xml"
        }
    )
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_log_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertTest() {
        Movement movement = movementMapper.getById(1);
        logMapper.insertNotRejected(1L, movement);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/after/after_log_insertion.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_log_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertConflictTest() {
        Movement movement = movementMapper.getById(1);
        logMapper.insertNotRejected(1L, movement);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/before_log_rejection.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_log_rejection.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void rejectTest() {
        Movement movement = movementMapper.getById(1);
        logMapper.reject(movement);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/before_log_deletion.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_log_deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNotRejected() {
        Movement movement = movementMapper.getById(1);
        logMapper.deleteNotRejected(movement);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    void searchRejectedOrderTest() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "day");
        List<Long> logs = logMapper.searchRejected(FILTER, pageRequest).stream()
            .map(MovementTransportLog::getId)
            .collect(Collectors.toList());
        softly.assertThat(logs).containsExactly(4L, 2L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    void searchRejectedTransportFilterTest() {
        RejectedTransportFilter filter = new RejectedTransportFilter()
            .setPartner(1L);
        List<Long> logs = logMapper.searchRejected(filter, Pageable.unpaged()).stream()
            .map(MovementTransportLog::getId)
            .collect(Collectors.toList());
        softly.assertThat(logs).containsExactly(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    void countRejectedTest() {
        softly.assertThat(logMapper.countRejected(FILTER)).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_logs_deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void delete() {
        logMapper.delete(Set.of(1L, 4L));
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/before/admin_logs_search.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_rejected_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addRejected() {
        logMapper.addRejected(1L, LocalDate.of(2021, 3, 18));
    }
}

package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.TransportationTaskRegisterIdDto;
import ru.yandex.market.delivery.transport_manager.domain.enums.ClientName;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterAxaptaRequestStatus;
import ru.yandex.market.delivery.transport_manager.domain.filter.TransportationTaskFilter;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationTaskMapper;

class TransportationTaskMapperTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskMapper transportationTaskMapper;

    private static final TransportationTask FULL_TRANSPORTATION_TASK =
        new TransportationTask()
            .setId(2L)
            .setStatus(TransportationTaskStatus.ENRICHING)
            .setRegisterId(1L)
            .setTransportationIds(Set.of(1L, 2L, 3L))
            .setLogisticPointFromId(3L)
            .setLogisticPointToId(4L)
            .setExternalId(2L)
            .setCreated(LocalDateTime.ofInstant(Instant.parse("2021-01-06T16:00:00.00Z"), ZoneOffset.UTC))
            .setClientName(ClientName.MBOC);

    private static final TransportationTask TRANSPORTATION_TASK =
        new TransportationTask()
            .setId(1L)
            .setStatus(TransportationTaskStatus.NEW)
            .setRegisterId(1L)
            .setLogisticPointFromId(1L)
            .setLogisticPointToId(2L)
            .setExternalId(1L)
            .setCreated(LocalDateTime.ofInstant(Instant.parse("2021-01-28T18:18:00.00Z"), ZoneOffset.UTC))
            .setUpdated(LocalDateTime.ofInstant(Instant.parse("2021-01-28T18:18:00.00Z"), ZoneOffset.UTC))
            .setClientName(ClientName.MBOC);

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-01-28T18:18:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/transportation/all_kinds_of_transportation.xml",
            "/repository/transportation_task/transportation_tasks.xml",
            "/repository/transportation_task/transportation_task_transportations.xml",
        }
    )
    void getById() {
        TransportationTask fullTask = transportationTaskMapper.getById(2L);
        TransportationTask task = transportationTaskMapper.getById(1L);

        assertThatModelEquals(FULL_TRANSPORTATION_TASK, fullTask);
        assertThatModelEquals(TRANSPORTATION_TASK, task);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void find() {
        TransportationTaskFilter filter = new TransportationTaskFilter().setStatus(AdminTransportationTaskStatus.NEW);
        List<TransportationTask> transportationTasks = transportationTaskMapper.find(filter, Pageable.unpaged());
        softly.assertThat(transportationTasks).containsExactlyInAnyOrder(
            transportationTaskMapper.getById(1L),
            transportationTaskMapper.getById(5L)
        );
    }

    @Test
    @DatabaseSetup(value = {
            "/repository/transportation/all_kinds_of_transportation.xml",
            "/repository/transportation_task/transportation_tasks.xml",
            "/repository/transportation_task/transportation_task_transportations.xml"
    })
    void findFull() {
        TransportationTaskFilter filter = new TransportationTaskFilter().setStatus(AdminTransportationTaskStatus.NEW);
        List<TransportationTask> transportationTasks = transportationTaskMapper.findFull(filter, Pageable.unpaged());
        softly.assertThat(transportationTasks).containsExactlyInAnyOrder(
                transportationTaskMapper.getById(1L),
                transportationTaskMapper.getById(5L)
        );
        softly.assertThat(transportationTasks.stream()
                        .filter(t -> t.getId() == 5)
                        .findFirst()
                        .get()
                        .getTransportationIds())
                .isNotEmpty();
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/validation_errors.xml"
    })
    void getValidationErrors() {
        List<String> errors = transportationTaskMapper.getValidationErrors(4L);
        softly.assertThat(errors).containsExactlyInAnyOrder("Error1", "Error2", "Error3");
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void setStatusInternal() {
        transportationTaskMapper.setStatus(List.of(1L, 2L), TransportationTaskStatus.INVALID);
        softly.assertThat(transportationTaskMapper.getById(1L).getStatus()).isEqualTo(TransportationTaskStatus.INVALID);
        softly.assertThat(transportationTaskMapper.getById(2L).getStatus()).isEqualTo(TransportationTaskStatus.INVALID);

        softly.assertThat(transportationTaskMapper.getById(1L).getUpdated()).isNotNull();
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    void persist() {
        transportationTaskMapper.persistAll(List.of(TRANSPORTATION_TASK, FULL_TRANSPORTATION_TASK));
        assertThatModelEquals(TRANSPORTATION_TASK, transportationTaskMapper.getById(1));
        assertThatModelEquals(FULL_TRANSPORTATION_TASK, transportationTaskMapper.getById(2), "transportationIds");
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void count() {
        TransportationTaskFilter filter = new TransportationTaskFilter().setStatus(AdminTransportationTaskStatus.NEW);
        softly.assertThat(transportationTaskMapper.count(filter)).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void getWithStatus() {
        List<Long> newTasks = transportationTaskMapper.getWithStatus(TransportationTaskStatus.NEW);
        softly.assertThat(newTasks).containsExactlyInAnyOrder(1L, 5L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/validation_errors.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_errors_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void clearErrors() {
        transportationTaskMapper.clearValidationErrors(4L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_errors_inserted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertErrors() {
        transportationTaskMapper.insertValidationErrors(1L, List.of("err1", "err2"));
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void getStatus() {
        softly.assertThat(transportationTaskMapper.getStatus(3L)).isEqualTo(TransportationTaskStatus.VALIDATING);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly.assertThat(transportationTaskMapper.switchStatusReturningCount(
            2L,
            TransportationTaskStatus.ENRICHING,
            TransportationTaskStatus.COMPLETED
        ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly.assertThat(transportationTaskMapper.switchStatusReturningCount(
            2L,
            null,
            TransportationTaskStatus.COMPLETED
        ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/transportation_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountSkip() {
        softly.assertThat(transportationTaskMapper.switchStatusReturningCount(
            2L,
            TransportationTaskStatus.NEW,
            TransportationTaskStatus.COMPLETED
        ))
            .isEqualTo(0L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void findByExternalIdAndClient() {
        Long taskId = transportationTaskMapper.findByExternalIdAndClient(1L, ClientName.MBOC);
        softly.assertThat(taskId).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void registerIdsByTask() {
        softly.assertThat(transportationTaskMapper.registerIdsByTask(List.of(1L, 2L, 1000L)))
            .containsExactlyInAnyOrder(
                new TransportationTaskRegisterIdDto().setTransportationTaskId(1L).setRegisterId(1L),
                new TransportationTaskRegisterIdDto().setTransportationTaskId(2L).setRegisterId(1L)
            );
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    void getWithCompletedRegisterRequestsAndStatus() {
        softly
            .assertThat(
                transportationTaskMapper.getWithCompletedRegisterRequestsAndStatus(
                    TransportationTaskStatus.STOCK_AVAILABILITY_CHECKING,
                    RegisterAxaptaRequestStatus.RECEIVED
                )
            )
            .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    void getWithExpiredRegisterRequestsEmpty() {
        softly
            .assertThat(
                transportationTaskMapper.getWithExpiredRegisterRequests(
                    TransportationTaskStatus.STOCK_AVAILABILITY_CHECKING,
                    RegisterAxaptaRequestStatus.RECEIVED,
                    LocalDateTime.of(2021, 5, 6, 12, 0).atZone(ZoneId.systemDefault()).toInstant()
                )
            ).isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    void getWithExpiredRegisterRequests() {
        softly
            .assertThat(
                transportationTaskMapper.getWithExpiredRegisterRequests(
                    TransportationTaskStatus.STOCK_AVAILABILITY_CHECKING,
                    RegisterAxaptaRequestStatus.RECEIVED,
                    LocalDateTime.of(2021, 5, 6, 12, 1).atZone(ZoneId.systemDefault()).toInstant()
                )
            ).containsExactly(1L);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks_width_axapta_requests.xml")
    void getByRegisterId() {
        softly
            .assertThat(transportationTaskMapper.getByRegisterId(1001L))
            .isEqualTo(new TransportationTask()
                .setId(1L)
                .setStatus(TransportationTaskStatus.STOCK_AVAILABILITY_CHECKING)
                .setLogisticPointFromId(11L)
                .setLogisticPointToId(12L)
                .setExternalId(1L)
                .setRegisterId(1001L)
                .setClientName(ClientName.MBOC)
            );
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/transportation_tasks_with_deny_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setDeniedRegister() {
        transportationTaskMapper.setDeniedRegister(1L, 1L);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/transportation_task/task_with_transportation.xml"
    )
    void getByTransportation() {
        TransportationTask task = transportationTaskMapper.getByTransportation(5L);
        TransportationTask expected = new TransportationTask()
            .setRegisterId(1L)
            .setLogisticPointFromId(1L)
            .setLogisticPointToId(2L)
            .setClientName(ClientName.MBOC)
            .setExternalId(1L)
            .setStatus(TransportationTaskStatus.NEW)
            .setCsvFileGroupingKey("xxx");

        assertThatModelEquals(expected, task);
    }

    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
    })
    @Test
    void findOutdatedTransportationTaskIds() {
        softly.assertThat(
                transportationTaskMapper.findOutdatedTransportationTaskIds(
                    Instant.parse("2022-03-18T20:00:00.0Z"),
                    1,
                    1
                )
            )
            .containsExactly(2L);
    }

    @DatabaseSetup(value = {
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/transportation_task_transportations.xml",
        "/repository/transportation_task/validation_errors.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void delete() {
        transportationTaskMapper.delete(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L));
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    void setComment() {
        String comment = "Some comment";
        transportationTaskMapper.setCommentById(1L, comment);
        TransportationTask task = transportationTaskMapper.getById(1L);
        softly.assertThat(task.getComment()).isEqualTo(comment);
    }
}

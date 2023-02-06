package ru.yandex.mail.cerberus.yt.staff;

import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentGroup;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffOffice;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffRoom;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;
import ru.yandex.mail.tvmlocal.junit_jupiter.WithLocalTvm;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@WithLocalTvm(TestTvmToolOptionsProvider.class)
@MicronautTest(transactional = false)
class StaffManagerTest {
    @Inject
    private StaffManager manager;

    @FunctionalInterface
    private interface StaffFetcher<T> {
        Flux<List<StaffEntity<T>>> fetch(StaffManager staffManager, int pageSize);
    }

    private static Stream<Arguments> staffFetchParameters() {
        return Stream.of(
            Arguments.of(
                "meetingRooms",
                (StaffFetcher<StaffRoom>) (manager, pageSize) -> manager.meetingRoomsRx(pageSize, Optional.empty())
            ),
            Arguments.of(
                "users",
                (StaffFetcher<StaffUser>) (manager, pageSize) -> manager.usersRx(pageSize, Optional.empty())
            ),
            Arguments.of(
                "departments",
                (StaffFetcher<StaffDepartmentGroup>) (manager, pageSize) -> manager.departmentsRx(pageSize, Optional.empty())
            ),
            Arguments.of(
                "offices",
                (StaffFetcher<StaffOffice>) (manager, pageSize) -> manager.officesRx(pageSize, Optional.empty())
            )
        );
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("staffFetchParameters")
    @DisplayName("Verify that 'fetch' method returns correct data from staff")
    void fetchTest(String testName, StaffFetcher<?> fetcher) {
        val pageSize = 10;
        final var chunk = fetcher.fetch(manager, pageSize)
            .log()
            .subscribeOn(Schedulers.immediate())
            .blockFirst();
        assertThat(chunk)
            .hasSize(pageSize);
        assertThat(chunk)
            .filteredOn(StaffEntity::isValid)
            .isNotEmpty();
    }
}

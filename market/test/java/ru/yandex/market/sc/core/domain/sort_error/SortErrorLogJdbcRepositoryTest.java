package ru.yandex.market.sc.core.domain.sort_error;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mors741
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortErrorLogJdbcRepositoryTest {

    private final SortErrorLogJdbcRepository jdbcRepository;
    private final TestFactory testFactory;
    private final Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void writeAndFix() {
        SortErrorLogEntry entry = SortErrorLogEntry.builder()
                .id(1)
                .scannedAt(Instant.now(clock))
                .errorType(ScErrorCode.ROUTE_FROM_ANOTHER_SC.name())
                .sortingCenterId(sortingCenter.getId())
                .dispatchPersonId(100)
                .dispatchPersonName("Иванов Дмитрий")
                .orderId(1)
                .orderExternalId("o1")
                .placeExternalId("p11")
                .scannedCellId(666)
                .scannedCellName("WRONG CELL 666")
                .scannedCellRoute("Syktyvkar")
                .expectedCellId(741L)
                .expectedCellName("MORSLAND CELL 741")
                .expectedCellRoute("Morsland")
                .currentCellId(31L)
                .currentCellName("PREVIOUS CELL 31")
                .currentCellRoute("PREVIOUS ROUTE")
                .fixedInFiveMinutes(null)
                .fixedAt(null)
                .build();

        jdbcRepository.insert(entry);

        List<SortErrorLogEntry> loaded = jdbcRepository.selectNotProcessed(Instant.now(clock).plusSeconds(1000));

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0)).isEqualTo(entry);

        jdbcRepository.setFixedInFiveMinutes(List.of(entry.getId()), true);

        loaded = jdbcRepository.selectNotProcessed(Instant.now(clock).plusSeconds(1000));
        assertThat(loaded).isEmpty();


        List<SortErrorLogEntry> notFixed = jdbcRepository.selectWithoutFixedAt(
                Instant.now(clock).minusSeconds(1000));

        assertThat(notFixed).hasSize(1);

        jdbcRepository.setFixedAt(List.of(notFixed.get(0).toBuilder().fixedAt(Instant.now(clock)).build()));

        notFixed = jdbcRepository.selectWithoutFixedAt(
                Instant.now(clock).minusSeconds(1000));
        assertThat(notFixed).isEmpty();
    }
}

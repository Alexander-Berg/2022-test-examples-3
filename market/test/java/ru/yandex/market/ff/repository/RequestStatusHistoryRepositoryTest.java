package ru.yandex.market.ff.repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.RequestStatusHistory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.Assert.assertEquals;

/**
 * Интеграционный тест для {@link RequestStatusHistoryRepository}.
 *
 * @author avetokhin 12/01/18.
 */
public class RequestStatusHistoryRepositoryTest extends IntegrationTest {
    @Autowired
    private RequestStatusHistoryRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/request-history/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request-history/after-save.xml", assertionMode = NON_STRICT)
    public void testSave() {
        repository.save(Arrays.asList(
                new RequestStatusHistory(1L, LocalDateTime.of(2017, 1, 1, 10, 10, 10), RequestStatus.SENT_TO_SERVICE),
                new RequestStatusHistory(2L, LocalDateTime.of(2018, 1, 1, 10, 10, 10), RequestStatus.VALIDATED)
        ));
    }

    @Test
    void checkSupplyCreationTimingOnEmptyDb() {
        Long interval = repository.getRequestMoveToStatusIntervalOnPercentile(LocalDateTime.now(),
            RequestStatus.ACCEPTED_BY_SERVICE.getId(), 0.95, Set.of(RequestType.SUPPLY.getId()));

        assertEquals(0L, interval.longValue());
    }

    @Test
    @DatabaseSetup("classpath:repository/request-history/sla-check/1.xml")
    void checkSupplyCreationTimingOnSingleSupply() {
        Long interval = repository.getRequestMoveToStatusIntervalOnPercentile(
            LocalDateTime.of(2019, 1, 1, 0, 0),
            RequestStatus.ACCEPTED_BY_SERVICE.getId(), 0.95, Set.of(RequestType.SUPPLY.getId()));

        assertEquals(240L, interval.longValue());
    }

    @Test
    @DatabaseSetup("classpath:repository/request-history/sla-check/2.xml")
    void checkSupplyCreationTimingOnManySupplies() {
        Long interval = repository.getRequestMoveToStatusIntervalOnPercentile(
            LocalDateTime.of(2019, 1, 1, 0, 0),
            RequestStatus.ACCEPTED_BY_SERVICE.getId(), 0.95, Set.of(RequestType.SUPPLY.getId()));

        assertEquals(480L, interval.longValue());
    }

    @Test
    @DatabaseSetup("classpath:repository/request-history/sla-check/1.xml")
    void checkDoesNotAnalyzeSuppliesCreatedBeforeCurrentHourInterval() {
        Long interval = repository.getRequestMoveToStatusIntervalOnPercentile(
            LocalDateTime.of(2019, 1, 1, 2, 0),
            RequestStatus.ACCEPTED_BY_SERVICE.getId(), 0.95, Set.of(RequestType.SUPPLY.getId()));

        assertEquals(0L, interval.longValue());
    }

    @Test
    @DatabaseSetup("classpath:repository/request-history/sla-check/3.xml")
    void checkSupplyCreationTimingOnManySuppliesWithDelaysInWindowSelection() {
        Long interval = repository.getRequestMoveToStatusIntervalOnPercentile(
            LocalDateTime.of(2019, 1, 1, 0, 0),
            RequestStatus.ACCEPTED_BY_SERVICE.getId(), 0.95, Set.of(RequestType.SUPPLY.getId()));

        assertEquals(7680, interval.longValue());
    }

    @Nested
    class GetRequestsWithSlowMoveToStatus extends IntegrationTest {
        private final LocalDateTime fromDateTime = LocalDateTime.of(2019, 1, 1, 0, 0);
        private final long targetStatusId = RequestStatus.ACCEPTED_BY_SERVICE.getId();
        private final Set<Integer> requestTypesIds = Set.of(RequestType.SUPPLY.getId());

        @Test
        @DatabaseSetup("classpath:empty.xml")
        void emptyDb() {
            Set<BigInteger> requestIds = repository.getRequestsWithSlowMoveToStatus(
                    fromDateTime,
                    targetStatusId,
                    30,
                    requestTypesIds
            );

            assertEquals(0, requestIds.size());
        }

        @Test
        @DatabaseSetup("classpath:repository/request-history/get-requests-with-slow-move-to-status/single-request.xml")
        void singleRequest() {
            Set<BigInteger> requestIds = repository.getRequestsWithSlowMoveToStatus(
                    fromDateTime,
                    targetStatusId,
                    30,
                    requestTypesIds
            );

            assertEquals(1, requestIds.size());
        }

        @Test
        @DatabaseSetup(
                "classpath:" +
                        "repository/request-history/get-requests-with-slow-move-to-status/single-request-with-slot.xml"
        )
        void singleRequestSearchTakesIntoAccountLinkedSlot() {
            Set<BigInteger> requestIds = repository.getRequestsWithSlowMoveToStatus(
                    fromDateTime,
                    targetStatusId,
                    30,
                    requestTypesIds
            );

            assertEquals(0, requestIds.size());
        }

        @Test
        @DatabaseSetup("classpath:repository/request-history/get-requests-with-slow-move-to-status/many-requests.xml")
        void requestsAreFilteredByFromCriteria() {
            Set<BigInteger> requestIds = repository.getRequestsWithSlowMoveToStatus(
                    fromDateTime,
                    targetStatusId,
                    30,
                    requestTypesIds
            );

            assertEquals(3, requestIds.size());
        }

        @Test
        @DatabaseSetup("classpath:repository/request-history/get-requests-with-slow-move-to-status/many-requests.xml")
        void requestsAreFilteredBySecondsToMoveCriteria() {
            Set<BigInteger> requestIds = repository.getRequestsWithSlowMoveToStatus(
                    fromDateTime,
                    targetStatusId,
                    301,
                    requestTypesIds
            );

            assertEquals(1, requestIds.size());
        }
    }

}

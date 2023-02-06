package ru.yandex.market.pvz.core.domain.returns;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.returns.history.ReturnRequestHistory;
import ru.yandex.market.pvz.core.domain.returns.history.ReturnRequestHistoryRepository;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnStatus;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplForbiddenException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author valeriashanti
 * @date 2/10/21
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReturnRequestCommandServiceTest {

    private static final String COMMENT = "Новый комменарий";

    private final TestableClock clock;

    private final TestReturnRequestFactory returnRequestFactory;
    private final ReturnRequestCommandService returnRequestCommandService;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestHistoryRepository returnRequestHistoryRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private ReturnRequestParams returnRequest;

    @BeforeEach
    public void setUp() {
        returnRequest = returnRequestFactory.createReturnRequest();
    }

    @Test
    void updateCommentsSuccessfully() {
        returnRequestCommandService.updateComment(
                returnRequest.getReturnId(), returnRequest.getPickupPointId(), COMMENT,
                returnRequest.getItems().get(0).getId());
        var updatedRequest = returnRequestRepository.findByReturnId(returnRequest.getReturnId());
        assertThat(updatedRequest.get().getItems().get(0).getOperatorComment()).isEqualTo(COMMENT);
    }

    @Test
    void updateCommentsWithWrongPickupPointId() {
        assertThatThrownBy(() -> returnRequestCommandService.updateComment(
                returnRequest.getReturnId(), returnRequest.getPickupPointId() + 123L, COMMENT,
                returnRequest.getItems().get(0).getId()))
                .isInstanceOf(TplForbiddenException.class).hasMessageContaining("Данные по возвратам другого ПВЗ " +
                        "недоступны");
    }

    @Test
    void updateCommentsWithWrongReturnItemId() {
        assertThatThrownBy(() -> returnRequestCommandService.updateComment(
                returnRequest.getReturnId(), returnRequest.getPickupPointId(), COMMENT,
                returnRequest.getItems().get(0).getId() + 123456L))
                .isInstanceOf(TplIllegalArgumentException.class).hasMessageContaining("Единицы возврата с таким " +
                        "идентификатором не существует");
    }

    @Test
    void updateCommentsWithWrongReturnId() {
        assertThatThrownBy(() -> returnRequestCommandService.updateComment(
                returnRequest.getReturnId() + 123L, returnRequest.getPickupPointId(), COMMENT,
                returnRequest.getItems().get(0).getId()))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void receiveReturn() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime receiveDate = LocalDateTime.of(2021, 4, 6, 14, 50, 0);
        clock.setFixed(receiveDate.toInstant(zone), zone);

        returnRequestCommandService.receive(returnRequest.getReturnId(), returnRequest.getPickupPointId());
        var updatedRequest = returnRequestRepository.findByReturnId(returnRequest.getReturnId());
        assertThat(updatedRequest).isNotEmpty();
        assertThat(updatedRequest.get().getStatus()).isEqualTo(ReturnStatus.RECEIVED);
        assertThat(updatedRequest.get().getArrivedAt()).isEqualTo(OffsetDateTime.of(receiveDate, zone));
    }

    @Test
    void receiveReturnOnWrongPickupPointId() {
        assertThatThrownBy(() -> returnRequestCommandService.receive(
                returnRequest.getReturnId(), returnRequest.getPickupPointId() + 123L))
                .isInstanceOf(TplForbiddenException.class).hasMessageContaining("Данные по возвратам другого ПВЗ " +
                        "недоступны");
    }

    @Test
    void cancelReturn() {
        returnRequestCommandService.cancel(returnRequest.getReturnId(), returnRequest.getPickupPointId());
        var updatedRequest = returnRequestRepository.findByReturnId(returnRequest.getReturnId());
        assertThat(updatedRequest).isNotEmpty();
        assertThat(updatedRequest.get().getStatus()).isEqualTo(ReturnStatus.CANCELLED);
    }

    @Test
    void cancelReturnOnWrongPickupPointId() {
        assertThatThrownBy(() -> returnRequestCommandService.cancel(
                returnRequest.getReturnId(), returnRequest.getPickupPointId() + 123L))
                .isInstanceOf(TplForbiddenException.class).hasMessageContaining("Данные по возвратам другого ПВЗ " +
                        "недоступны");
    }

    @ParameterizedTest(name = "updateReturnStatus_{index}")
    @MethodSource("updateReturnStatusMethodSource")
    void updateReturnStatus(int daysAddToRequestDate, ReturnStatus expectedStatus) {
        var requestDate = LocalDate.of(2020, 1, 1);

        var request = returnRequestFactory.createReturnRequest(TestReturnRequestFactory.CreateReturnRequestBuilder
                .builder()
                .params(TestReturnRequestFactory.ReturnRequestTestParams.builder()
                        .requestDate(requestDate)
                        .build())
                .build()
        );
        var today = requestDate.plusDays(daysAddToRequestDate);
        returnRequestCommandService.expireAllReturnsWhereExpirationDateBefore(today);
        var updatedRequest = returnRequestRepository.findById(request.getId()).get();
        assertThat(updatedRequest.getStatus()).isEqualTo(expectedStatus);
    }

    static Stream<Arguments> updateReturnStatusMethodSource() {
        return StreamEx.of(
                Arguments.of(15, ReturnStatus.EXPIRED),
                Arguments.of(1, ReturnStatus.NEW)
        );
    }

    @Test
    void notToExpireReceivedAndDispatchedReturns() {
        var requestDate = LocalDate.of(2020, 1, 1);

        var request1 = returnRequestFactory.createReturnRequest(TestReturnRequestFactory.CreateReturnRequestBuilder
                .builder()
                .params(TestReturnRequestFactory.ReturnRequestTestParams.builder()
                        .requestDate(requestDate)
                        .build())
                .build()
        );
        returnRequestFactory.receiveReturnRequest(request1.getReturnId());

        var request2 = returnRequestFactory.createReturnRequest(TestReturnRequestFactory.CreateReturnRequestBuilder
                .builder()
                .params(TestReturnRequestFactory.ReturnRequestTestParams.builder()
                        .requestDate(requestDate)
                        .build())
                .build()
        );
        returnRequestFactory.dispatchReturnRequest(request2.getReturnId());

        var today = requestDate.plusDays(4);
        returnRequestCommandService.expireAllReturnsWhereExpirationDateBefore(today);

        assertThat(returnRequestRepository.findByReturnId(request1.getReturnId()).get().getStatus())
                .isEqualTo(ReturnStatus.RECEIVED);
        assertThat(returnRequestRepository.findByReturnId(request2.getReturnId()).get().getStatus())
                .isEqualTo(ReturnStatus.DISPATCHED);
    }

    @Test
    void testOrderCreationLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW);

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testReceiveReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.RECEIVED);

        returnRequestCommandService.receive(returnRequest.getReturnId(), returnRequest.getPickupPointId());

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testCancelReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.CANCELLED);

        returnRequestCommandService.cancel(returnRequest.getReturnId(), returnRequest.getPickupPointId());

        verifyReturnRequestHistory(expectedStatuses);
    }

    @ParameterizedTest
    @EnumSource(ReturnStatus.class)
    void testUpdateStatusReturnRequestLogged(ReturnStatus status) {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, status);

        returnRequestCommandService.updateStatusWithoutPvzCheck(returnRequest.getReturnId(), status);

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testRevertReceiveReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.RECEIVED, ReturnStatus.NEW);

        returnRequestCommandService.receive(returnRequest.getReturnId(), returnRequest.getPickupPointId());
        returnRequestCommandService.revertReceive(returnRequest.getId());

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testExpireReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.EXPIRED);

        returnRequestCommandService.expire(returnRequest.getId());

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testExpireByDateReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.EXPIRED);

        returnRequestCommandService.expireAllReturnsWhereExpirationDateBefore(LocalDate.now());

        verifyReturnRequestHistory(expectedStatuses);
    }

    @Test
    void testDispatchReturnRequestLogged() {
        List<ReturnStatus> expectedStatuses = List.of(ReturnStatus.NEW, ReturnStatus.RECEIVED, ReturnStatus.DISPATCHED);

        returnRequestCommandService.receive(returnRequest.getReturnId(), returnRequest.getPickupPointId());
        returnRequestCommandService.dispatch(List.of(returnRequest.getId()));

        verifyReturnRequestHistory(expectedStatuses);
    }

    private void verifyReturnRequestHistory(List<ReturnStatus> expectedReturnStatuses) {
        List<ReturnRequestHistory> actualHistory =
                returnRequestHistoryRepository.findAllByReturnRequestIdOrderByCreatedAtAsc(returnRequest.getId());

        var actualStatuses = actualHistory.stream().map(ReturnRequestHistory::getStatus).collect(Collectors.toList());
        assertThat(actualStatuses).isEqualTo(expectedReturnStatuses);

        for (int i = 0, actualHistorySize = actualHistory.size(); i < actualHistorySize; i++) {
            ReturnRequestHistory actualRecord = actualHistory.get(i);

            assertThat(actualRecord.getCreatedAt()).isNotNull();
            assertThat(actualRecord.getUpdatedAt()).isNotNull();

            var queue = dbQueueTestUtil.getQueue(PvzQueueType.RETURN_REQUEST_HISTORY);
            dbQueueTestUtil.executeSingleQueueItem(PvzQueueType.RETURN_REQUEST_HISTORY);
            assertThat(queue.get(i)).isEqualTo(String.valueOf(actualRecord.getId()));
        }
    }
}

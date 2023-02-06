package ru.yandex.market.logistics.lom.service;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.processor.CancelOrderReturnsService;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.CancelReturnsRequest;
import ru.yandex.market.logistics.lrm.client.model.CancelReturnsResponse;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DatabaseSetup("/service/cancel_order_returns/before/setup.xml")
class CancelOrderReturnsServiceTest extends AbstractContextualTest {
    @Autowired
    private CancelOrderReturnsService cancelOrderReturnsService;

    @Autowired
    private ReturnsApi returnsApi;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(returnsApi);
    }

    @Test
    @DisplayName("Успешная отмена")
    @ExpectedDatabase(
        value = "/service/cancel_order_returns/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        Set<Long> returnIdsToCancel = Set.of(1L, 2L);
        mockRequest(returnIdsToCancel, List.of(1L, 2L), List.of());

        cancelOrderReturnsService.processPayload(createOrderIdPayload(1, 1));
        verifyAnswer(returnIdsToCancel);
    }

    @Test
    @DisplayName("Ошибка отмены")
    @ExpectedDatabase(
        value = "/service/cancel_order_returns/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void error() {
        Set<Long> returnIdsToCancel = Set.of(1L, 2L);
        String error = "Failed to find RETURN with ids [1, 2]";
        mockRequest(returnIdsToCancel, List.of(), List.of(new ValidationViolation().message(error)));

        cancelOrderReturnsService.processPayload(createOrderIdPayload(1, 1));
        verifyAnswer(returnIdsToCancel);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(error);
    }

    @Test
    @DisplayName("Один возврат отменился, второй - нет")
    @ExpectedDatabase(
        value = "/service/cancel_order_returns/after/partial_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void partialSuccess() {
        Set<Long> returnIdsToCancel = Set.of(1L, 2L);
        String error = "Failed to find RETURN with ids [2]";
        mockRequest(returnIdsToCancel, List.of(1L), List.of(new ValidationViolation().message(error)));

        cancelOrderReturnsService.processPayload(createOrderIdPayload(1, 1));
        verifyAnswer(returnIdsToCancel);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(error);
    }

    @Test
    @DisplayName("Нет ни одного активного возврата, нет похода в LRM")
    @DatabaseSetup(
        value = "/service/cancel_order_returns/before/order_return_cancelled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/cancel_order_returns/after/partial_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noActiveReturns() {
        cancelOrderReturnsService.processPayload(createOrderIdPayload(1, 1));
    }

    private void mockRequest(
        Set<Long> returnIdsToCancel,
        List<Long> successReturnIds,
        List<ValidationViolation> errors
    ) {
        when(returnsApi.cancelReturns(new CancelReturnsRequest().returnIds(returnIdsToCancel)))
            .thenReturn(new CancelReturnsResponse().returnIds(successReturnIds).errors(errors));
    }

    private void verifyAnswer(Set<Long> returnIdsToCancel) {
        verify(returnsApi).cancelReturns(new CancelReturnsRequest().returnIds(returnIdsToCancel));
    }
}

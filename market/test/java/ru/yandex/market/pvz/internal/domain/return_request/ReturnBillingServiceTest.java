package ru.yandex.market.pvz.internal.domain.return_request;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.billing.dto.BillingReturnDto;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReturnBillingServiceTest {
    public static final OffsetDateTime DISPATCHED_AT_1 = LocalDate.of(2021, 1, 1).atStartOfDay()
            .atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
    public static final OffsetDateTime DISPATCHED_AT_2 = LocalDate.of(2021, 1, 2).atStartOfDay()
            .atZone(ZoneOffset.systemDefault()).toOffsetDateTime();

    private final TestableClock clock;
    private final TestReturnRequestFactory returnRequestFactory;
    private final ReturnBillingService returnBillingService;


    @Test
    void getReturns() {
        var returnRequest = returnRequestFactory.createReturnRequest();
        clock.setFixed(DISPATCHED_AT_1.toInstant(), ZoneOffset.systemDefault());
        returnRequest = returnRequestFactory.dispatchReturnRequest(returnRequest.getReturnId());

        var returnRequest2 = returnRequestFactory.createReturnRequest();
        clock.setFixed(DISPATCHED_AT_2.toInstant(), ZoneOffset.systemDefault());
        returnRequestFactory.dispatchReturnRequest(returnRequest2.getReturnId());

        List<BillingReturnDto> returns = returnBillingService.getDispatchedReturns(
                DISPATCHED_AT_1.toLocalDate(),
                DISPATCHED_AT_1.toLocalDate()
        );
        BillingReturnDto expected = BillingReturnDto.builder()
                .pickupPointId(returnRequest.getPickupPointId())
                .dispatchedAt(returnRequest.getDispatchedAt())
                .externalOrderId(returnRequest.getOrderId())
                .returnId(returnRequest.getReturnId())
                .build();

        assertEquals(expected, returns.get(0), "returns");
    }
}

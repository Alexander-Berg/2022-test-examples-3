package ru.yandex.market.checkout.checkouter.cashback.model.details;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackPromoAccrualStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.cashback.model.details.CashbackPromoAccrualStatusResponse.findByCode;

public class CashbackPromoAccrualStatusResponseTest {

    @Test
    public void testFindByCode() {
        Map<StructuredCashbackPromoAccrualStatus, CashbackPromoAccrualStatusResponse> mapCashbackPromoStatus =
                Map.of(
                        StructuredCashbackPromoAccrualStatus.SUCCESS, CashbackPromoAccrualStatusResponse.SUCCESS,
                        StructuredCashbackPromoAccrualStatus.PENDING, CashbackPromoAccrualStatusResponse.PENDING,
                        StructuredCashbackPromoAccrualStatus.UNKNOWN, CashbackPromoAccrualStatusResponse.UNKNOWN
                );

        for (Map.Entry<StructuredCashbackPromoAccrualStatus, CashbackPromoAccrualStatusResponse> statusEntry :
                mapCashbackPromoStatus.entrySet()) {
            assertThat(findByCode(statusEntry.getKey().getCode()), is(statusEntry.getValue()));
        }
    }

}

package ru.yandex.market.global.checkout.domain.actualize;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.actualizer.base.ActualizeType;
import ru.yandex.market.global.checkout.domain.actualize.actualizer.base.OrderActualizer;

import static ru.yandex.market.global.common.util.StringFormatter.sf;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ActualizeConfigTest extends BaseFunctionalTest {

    private final ActualizeService actualizeService;

    @Test
    public void testOrderActualizersTransactionOutFirst() {
        testExternalFirst(actualizeService.getNewOrderActualizers());
    }

    @Test
    public void testCartActualizersExternalFirst() {
        testExternalFirst(actualizeService.getCartActualizers());
    }

    @Test
    public void testExistingOrderActualizersExternalFirst() {
        testExternalFirst(actualizeService.getExistingOrderActualizers());
    }

    private void testExternalFirst(List<OrderActualizer> actualizers) {
        boolean transactionInFound = false;
        for (OrderActualizer actualizer : actualizers) {
            if (actualizer.getActualizeType() == ActualizeType.TRANSACTION_OUT) {
                Assertions.assertThat(transactionInFound)
                        .withFailMessage(sf("{} is TRANSACTION_OUT but follows TRANSACTION_IN actualizer",
                                actualizer.getClass().getSimpleName())
                        )
                        .isFalse();
            }
            transactionInFound = transactionInFound || actualizer.getActualizeType() == ActualizeType.TRANSACTION_IN;
        }
    }
}

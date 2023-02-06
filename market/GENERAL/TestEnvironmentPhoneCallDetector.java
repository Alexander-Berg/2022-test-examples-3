package ru.yandex.market.antifraud.orders.detector;

import java.util.Set;

import org.springframework.stereotype.Component;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.misc.env.EnvironmentType;

import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.ROBOCALL;

/**
 * @author dzvyagin
 */
@Component
public class TestEnvironmentPhoneCallDetector implements LegacyOrderFraudDetector {

    private static final Set<Long> TEST_ITEM_MSKU =
        Set.of(100126173347L, 100438983455L, 100439155808L, 100126178518L);

    @Override
    public OrderDetectorResult detectFraud(OrderDataContainer orderDataContainer) {
        if (EnvironmentType.TESTING.equals(EnvironmentType.getActive()) && haveTestItem(orderDataContainer.getOrderRequest())) {
            return OrderDetectorResult.builder()
                .ruleName(getUniqName())
                .answerText("Вам перезвонят")
                .reason("Вас заметили")
                .actions(Set.of(AntifraudAction.ROBOCALL))
                .build();
        }
        return OrderDetectorResult.empty(getUniqName());
    }

    @Override
    public Set<AntifraudAction> detectorRestrictions() {
        return Set.of(ROBOCALL);
    }

    private boolean haveTestItem(MultiCartRequestDto orderRequest) {
        return orderRequest.getCarts().stream()
            .flatMap(cart -> cart.getItems().stream())
            .filter(i -> i.getMsku() != null)
            .anyMatch(i -> TEST_ITEM_MSKU.contains(i.getMsku()));
    }
}

package ru.yandex.market.fulfillment.wrap.marschroute.service.util;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.WaybillInfo;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusHistory;

import static org.assertj.core.api.Assertions.assertThat;

class InboundHistoryFactoryTest {

    private final InboundHistoryFactory inboundHistoryFactory = new InboundHistoryFactory();

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            InboundHistoryFactoryScenarios.firstScenario(),
            InboundHistoryFactoryScenarios.secondScenario(),
            InboundHistoryFactoryScenarios.thirdScenario(),
            InboundHistoryFactoryScenarios.fourthScenario(),
            InboundHistoryFactoryScenarios.fifthScenario(),
            InboundHistoryFactoryScenarios.sixthScenario(),
            InboundHistoryFactoryScenarios.seventhScenario(),
            InboundHistoryFactoryScenarios.eighthScenario()
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testHistoryConstruction(WaybillInfo waybillInfo, InboundStatusHistory expectedHistory) {
        InboundStatusHistory actualHistory = inboundHistoryFactory.construct(
            InboundHistoryFactoryScenarios.RESOURCE_ID,
            waybillInfo
        );

        assertThat(actualHistory).isEqualToComparingFieldByFieldRecursively(expectedHistory);
    }
}

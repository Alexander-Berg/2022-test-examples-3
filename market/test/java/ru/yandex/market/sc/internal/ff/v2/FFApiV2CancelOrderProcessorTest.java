package ru.yandex.market.sc.internal.ff.v2;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FFApiV2CancelOrderProcessorTest {

    private final TestFactory testFactory;
    private final ScOrderRepository scOrderRepository;
    private final MockMvc mockMvc;

    @Test
    void cancelOrder() {
        var sortingCenterPartner = testFactory.storedSortingCenterPartner();
        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());
        var order = testFactory.createOrder(sortingCenter);

        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, sortingCenterPartner.getToken(), "cancelOrder",
                "<orderId>\n" +
                        "            <yandexId>" + order.get().getExternalId() + "</yandexId>\n" +
                        "            <partnerId>" + order.get().getId() + "</partnerId>\n" +
                        "        </orderId>");

        var actual = scOrderRepository.findByIdOrThrow(order.get().getId());
        assertThat(actual.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
    }
}

package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.RecipientApiFacade;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Где курьер")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClientOrderCancellationTest {
    private final ApiFacade apiFacade;
    private final CourierApiFacade courierApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final RecipientApiFacade recipientApiFacade;

    @BeforeEach
    void before() {
        TestConfiguration configuration = AutoTestContextHolder.getContext().getTestConfiguration();
        configuration.setPaymentStatus(OrderPaymentStatus.UNPAID);
        configuration.setPaymentType(OrderPaymentType.CASH);
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Feature("Доставка")
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Отмена доставки клиентом"), @Story("Вернуть посылку в СЦ")})
    @DisplayName(value = "Тест отмены доставки клиентом")
    void test() {
        apiFacade.createAndPickupOrder();
        checkOrderEquals();
        recipientApiFacade.cancelOrderInTracking();
        checkOrderCancelled();
        publicApiFacade.updateDataButton();
        apiFacade.finishShiftInScWithOrders();
    }

    private void checkOrderEquals() {
        TrackingDto trackingDto = recipientApiFacade.getInternalTracking();
        OrderDeliveryTaskDto taskDto =
                (OrderDeliveryTaskDto) AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        assertThat(taskDto.getOrder().getExternalOrderId()).isEqualTo(trackingDto.getOrder().getId());
    }

    private void checkOrderCancelled() {
        TrackingDto trackingDto = recipientApiFacade.getInternalTracking();
        assertThat(trackingDto.getDelivery().getStatus()).isEqualTo(TrackingDeliveryStatus.NOT_DELIVERED);
        assertThat(trackingDto.getCourier()).isNull();
        assertThat(trackingDto.getOrder()).isNull();
    }
}

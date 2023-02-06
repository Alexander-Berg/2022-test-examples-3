package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import io.qameta.allure.Epic;
import io.qameta.allure.Epics;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContext;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epics({@Epic("Курьерское приложение"), @Epic("Приложение 'Где курьер'")})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryNearTheDoorWithPhotoTest {
    private final ApiFacade apiFacade;
    private final PublicApiFacade publicApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;

    @BeforeEach
    void before() {
        AutoTestContext context = AutoTestContextHolder.getContext();
        TestConfiguration configuration = context.getTestConfiguration();
        configuration.setOrderRecipientNotes("Оставить у двери");
        configuration.setDeliveryPhotoEnabled(true);
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Features({@Feature("Доставка"), @Feature("Near the door"), @Feature("Delivery Photo")})
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Оставить у двери"), @Story("Успешная доставка")})
    @DisplayName(value = "Тест успешной доставки посылки под дверь с приложенной фотографией")
    void test() {
        apiFacade.createAndPickupOrder();

        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.makePhoto();
        publicApiFacade.giveParcel();
        publicApiFacade.finishLastDeliveryTask();

        apiFacade.finishShiftInSc();
    }
}

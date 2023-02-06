package ru.yandex.market.deliveryintegrationtests.delivery.tests.checkouter;

import dto.requests.checkouter.CreateOrderParameters;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qatools.properties.Resource;

import java.util.function.Supplier;

@Slf4j

@Resource.Classpath({"delivery/checkouter.properties"})
@DisplayName("Test cart/checkout for CPA")
@Epic("Checkouter")

@Tag("CheckouterSmokeTest")
public class CheckoutTest extends AbstractCheckoutTest {

    @ParameterizedTest(name = "Создание {1}")
    @MethodSource("getParams")
    public void cartAndCheckout(Supplier<CreateOrderParameters> parameters, String caseName) {
        log.info("Trying to create checkouter order");

        order = ORDER_STEPS.createOrder(parameters.get());

        Allure.step("Номер заказа " + order.getId());
    }

}

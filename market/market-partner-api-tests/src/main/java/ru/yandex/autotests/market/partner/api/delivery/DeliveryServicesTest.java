package ru.yandex.autotests.market.partner.api.delivery;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.partner.api.steps.delivery.DeliverySteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.api.delivery.DeliveryServicesResponse;
import ru.yandex.autotests.market.partner.beans.api.delivery.DeliveryServicesResponse.DeliveryService;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.List;

/**
 * @author apershukov
 */
@Aqua.Test(title = "Тесты для ручки GET /delivery/services.[format]")
@Features("GET /delivery/services.[format]")
@Issue("https://st.yandex-team.ru/MBI-20938")
public class DeliveryServicesTest {

    private final DeliverySteps steps = new DeliverySteps();

    /**
     * Запрос на получение списка доступных служб с формате xml
     */
    @Test
    public void testGetServicesAsXml() {
        getAvailableServices(Format.XML);
    }

    /**
     * Запрос на получение списка доступных служб с формате json
     */
    @Test
    public void testGetServicesAsJson() {
        getAvailableServices(Format.JSON);
    }

    private void getAvailableServices(Format format) {
        DeliveryServicesResponse response = steps.getDeliveryServices(format);

        List<DeliveryService> services = response.getResult().getDeliveryService();

        steps.assertServiceCountMoreThan(services, 3);

        steps.assertServiceAvailable(new DeliveryService(1, "Почта России"), services);
        steps.assertServiceAvailable(new DeliveryService(8, "DHL"), services);
    }
}

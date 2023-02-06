package ru.yandex.autotests.market.checkouter.api.red;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.market.checkouter.beans.Status.CANCELLED;
import static ru.yandex.autotests.market.checkouter.beans.Status.DELIVERY;

/**
 * @author kukabara
 */
@Aqua.Test(title = "Обработка Красного заказа")
@Features("RED")
@Issue("https://st.yandex-team.ru/MARKETCHECKOUT-6729")
@RunWith(Parameterized.class)
public class ProcessRedMultiOrderTest extends AbstractRedOrderTest {
    @Test
    @Title("Обработка Красного мультизаказа, CANCELLED")
    public void processRedMultiOrderCancel() throws Exception {
        processRedMultiOrder(CANCELLED, CANCELLED);
    }

    @Test
    @Title("Обработка Красного мультизаказа, DELIVERY")
    public void processRedMultiOrderDelivery() throws Exception {
        processRedMultiOrder(DELIVERY, DELIVERY);
    }

    @Test
    @Title("Обработка Красного мультизаказа, CANCELLED + DELIVERY")
    public void processRedMultiOrderCancelDelivery() throws Exception {
        processRedMultiOrder(DELIVERY, CANCELLED);
    }
}

package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateOrderColorTest extends AbstractWebTestBase {

    @BeforeEach
    void setUp() {
        checkouterProperties.setSetOrderColorUsingReportOfferColor(true);
    }

    @Test
    public void shouldCreateBlueOrderUsingReportColor() throws Exception {
        Parameters blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        blueOrderParameters.setColor(Color.WHITE);
        List<FoundOffer> foundOffers = blueOrderParameters.getOrder().getItems().stream()
                .map(item -> FoundOfferBuilder.createFrom(item)
                        .color(ru.yandex.market.common.report.model.Color.BLUE))
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toList());
        blueOrderParameters.getReportParameters().setOffers(foundOffers);

        // сходим частью запросов в белый репорт, частью в синий и заказ создадим синий
        // тут напрямую мокируется синий репорт, а внутри createOrder замокируется белый (т.к. такой цвет запроса)
        orderCreateHelper.initializeMock(reportConfigurer, blueOrderParameters);
        Order order = orderCreateHelper.createOrder(blueOrderParameters);
        assertEquals(Color.BLUE, order.getRgb());
    }
}

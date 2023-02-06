package ru.yandex.market.checkout.checkouter.order;

import java.util.Collections;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeliveryPartnerTypeFromOfferInfoTest extends AbstractWebTestBase {

    @Test
    public void positiveTest() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getBuiltMultiCart().getCarts().get(0).setDelivery(new Delivery(213L));
        parameters.getReportParameters().setDeliveryPartnerTypes(Collections.singletonList("SHOP"));
        orderCreateHelper.cart(parameters);

        ServeEvent pushApiEvent = Iterables.getOnlyElement(pushApiMock.getServeEvents().getRequests());
        assertTrue(pushApiEvent.getRequest().getBodyAsString()
                .contains("delivery-partner-type=\"SHOP\""));
    }

    @Test
    public void checkRegSetTest() {
        checkouterProperties.setRegSet2ByDefault(true);

        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        orderCreateHelper.cart(parameters);

        ServeEvent pushApiEvent = reportMockWhite.getServeEvents().getRequests().stream().filter(it -> it.getRequest()
                .getQueryParams().get("place").containsValue(MarketReportPlace.OFFER_INFO.getId())).findFirst().get();
        assertTrue(pushApiEvent.getRequest().getQueryParams().get("regset").containsValue("2"));
    }
}

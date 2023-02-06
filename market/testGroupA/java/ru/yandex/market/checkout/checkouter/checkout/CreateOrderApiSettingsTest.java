package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.GenericMockHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @link https://testpalm.yandex-team.ru/testcase/checkouter-1
 */
public class CreateOrderApiSettingsTest extends AbstractWebTestBase {

    @ParameterizedTest
    @EnumSource(value = ApiSettings.class, names = {"PRODUCTION", "SANDBOX", "STUB"})
    public void shouldSaveAcceptMethod(ApiSettings apiSettings) {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setApiSettings(apiSettings);

        Order order = orderCreateHelper.createOrder(parameters);

        List<ServeEvent> servedEvents = Lists.newArrayList(GenericMockHelper.servedEvents(pushApiMock));

        assertThat(servedEvents, hasSize(greaterThanOrEqualTo(3)));

        ServeEvent firstCart = servedEvents.get(0);
        // для FBS /cart должен идти в STUB
        checkCart(apiSettings == ApiSettings.PRODUCTION ? ApiSettings.STUB : apiSettings, order, firstCart);

        ServeEvent secondCart = servedEvents.get(1);
        checkCart(apiSettings, order, secondCart);

        ServeEvent orderAccept = servedEvents.get(2);
        checkOrderAccept(apiSettings, order, orderAccept);
    }

    private void checkCart(ApiSettings apiSettings, Order order, ServeEvent firstCart) {
        UrlBuilder urlBuilder = UrlBuilder.fromString(firstCart.getRequest().getUrl());
        Assertions.assertEquals("/shops/" + order.getShopId() + "/cart", urlBuilder.path);
        Assertions.assertEquals(apiSettings.name(),
                Iterables.getOnlyElement(urlBuilder.queryParameters.get(CheckouterClientParams.API_SETTINGS)));
    }

    private void checkOrderAccept(ApiSettings apiSettings, Order order, ServeEvent orderAccept) {
        UrlBuilder urlBuilder3 = UrlBuilder.fromString(orderAccept.getRequest().getUrl());
        Assertions.assertEquals("/shops/" + order.getShopId() + "/order/accept", urlBuilder3.path);
        Assertions.assertEquals(apiSettings.name(),
                Iterables.getOnlyElement(urlBuilder3.queryParameters.get(CheckouterClientParams.API_SETTINGS)));
    }
}

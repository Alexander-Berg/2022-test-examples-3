package ru.yandex.autotests.market.push.cart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.Item;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.wiki.CartDeliveryTestDataFromWiki;
import ru.yandex.autotests.market.pushapi.steps.CartDeliveryDatesSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.events.AddParameterEvent;

import java.util.Collection;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.toArray;
import static ru.yandex.autotests.market.common.wiki.WikiProperties.WIKI_BASE_URL;
import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.wiki.CartDeliveryTestDataFromWiki.getCartDeliveryTestDataFromWiki;

/**
 * Created by zajic on 03.09.16.
 */
@Feature("Cart resource")
@Aqua.Test(title = "Проверка delivery dates и prices в ответе push-api")
@RunWith(Parameterized.class)
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-2052")
public class CartDeliveryDatesTest {
    private Cart cartRequest;
    private PushApiRequestData requestData;
    private CartDeliveryDatesSteps steps;

    public CartDeliveryDatesTest(CartDeliveryTestDataFromWiki wiki){
        Allure.LIFECYCLE.fire(new AddParameterEvent("Test data", WIKI_BASE_URL+wiki.getWikiPagePath()));

        cartRequest = CartRequestProvider.createRequestFromXML(wiki.getPushApiRequestFromWiki());
        requestData = requestWithBodyForCase(Long.valueOf(wiki.getShopId()),
                formatRequestAsXML(cartRequest),
                wiki.getShopId() + " " + cartRequest.getItems().getItems().stream()
                        .map(Item::getOfferId)
                        .reduce((i1, i2) -> i1 + " " +i2));
        steps = new CartDeliveryDatesSteps();
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return getCartDeliveryTestDataFromWiki()
                .stream()
                .map(p -> toArray(p))
                .collect(toList());
    }

    @Test
    public void checkDeliveryDatesAndPrices() {
        steps.compareDeliveryDates(steps.pushApiCartResponse(requestData),
                steps.getOffersFromReport(cartRequest));
    }
}

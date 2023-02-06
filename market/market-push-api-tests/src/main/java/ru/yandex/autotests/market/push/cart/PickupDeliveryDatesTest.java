package ru.yandex.autotests.market.push.cart;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.DeliveryType;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.push.api.beans.response.cart.PushApiCartResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.CartSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;
import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.bodies.ItemProvider.offerOnStockPI;
import static ru.yandex.autotests.market.pushapi.data.bodies.ItemProvider.offerOutOfStockPI;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;

/**
 * Created by poluektov on 24.08.16.
 */

@Aqua.Test(title = "Даты доставки самовывозом зависят от данных в репорте")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-2642")
@Feature("Cart resource")
public class PickupDeliveryDatesTest {
    private long shopId = ShopIdProvider.SHOP_ELP_JSON_PI;
    private final int REGION = 213;

    private DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");

    private PushApiResponse pushApiResponse;
    private Cart cartRequestBody;
    private PushApiRequestData cartRequest;
    private PushApiCartResponse cartResponse;
    @Parameter
    private String expectedToDate;
    @Parameter
    private String expectedFromDate;

    @Test
    @Title("offer with onStock=1")
    public void testOfferOnStock() {
        pushApiResponse = ShopResponse.makeShopResponseCartForPickup(offerOnStockPI());
        cartResponse = getCartResponse();

        expectedToDate = formatter.print(new DateTime().plusDays(4));
        expectedFromDate = formatter.print(new DateTime());

        checkDeliveryDates();
    }

    @Test
    @Title("offer with onStock=0")
    public void testOfferOutOfStock() {
        pushApiResponse = ShopResponse.makeShopResponseCartForPickup(offerOutOfStockPI());
        cartResponse = getCartResponse();

        expectedToDate = formatter.print(new DateTime().plusDays(31));
        expectedFromDate = expectedToDate;

        checkDeliveryDates();
    }

    @Test
    @Title("offers with onStock=1 & onStock=0")
    public void testCartWithDifferentOffers() {
        pushApiResponse = ShopResponse.makeShopResponseCartForPickup(offerOnStockPI(), offerOutOfStockPI());
        cartResponse = getCartResponse();

        expectedToDate = formatter.print(new DateTime().plusDays(31));
        expectedFromDate = expectedToDate;

        checkDeliveryDates();
    }

    private PushApiCartResponse getCartResponse() {
        new PushApiCompareSteps().setShopResponse(formatResponseAsJson(pushApiResponse), shopId, ShopResource.CART);
        cartRequestBody = new CartRequestProvider().createRequestForShopResponse(pushApiResponse, shopId, REGION, DeliveryType.PICKUP);
        cartRequest = requestWithBodyForCase(shopId, formatRequestAsXML(cartRequestBody), "");
        return new CartSteps().getCartResponse(cartRequest);
    }

    @Step
    private void checkDeliveryDates() {
        assertThat("null delivery options", cartResponse.getDeliveryOptions(), notNullValue());
        assertStep("toDate", cartResponse.getDeliveryOptions().getDeliveries().get(0).getDates().getToDate(),
                equalTo(expectedToDate));
        assertStep("fromDate", cartResponse.getDeliveryOptions().getDeliveries().get(0).getDates().getFromDate(),
                equalTo(expectedFromDate));
    }
}

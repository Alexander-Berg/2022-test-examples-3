package ru.yandex.autotests.market.push.settings;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.pushapi.beans.cart.CartBodiesProvider;
import ru.yandex.autotests.market.push.api.beans.response.cart.PaymentMethod;
import ru.yandex.autotests.market.push.api.beans.response.cart.PushApiCartResponse;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.steps.ShopAdminStubSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.market.common.matchers.collection.ContainsInAnyOrderMatcher.containsEqualItemsInAnyOrder;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;
import static ru.yandex.autotests.market.pushapi.beans.cart.CartBodiesProvider.getDefaultCartRequestBody;
import static ru.yandex.autotests.market.push.api.beans.response.cart.PaymentMethod.CASH_ON_DELIVERY;
import static ru.yandex.autotests.market.push.api.beans.response.cart.PaymentMethod.YANDEX;
import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBody;

/**
 * Created by poluektov on 04.08.16.
 */
@Feature("Shop admin stub")
@Aqua.Test(title = "Изменение настроек магазина и соответствие ответа /cart изменениям")
public class ShopAdminStubSettingsTest {
    private final Integer BASE_REGION = 213;
    private final Integer CHANGED_REGION = 2;
    private static final Long SHOP_ID = ShopIdProvider.SHOP_ADMIN_STUB_TEST;

    private List<Integer> currentRegions;
    private List<PaymentMethod> currentPaymentMethods;

    private ShopAdminStubSteps stubSteps = new ShopAdminStubSteps();


    @Test
    public void testChangeRegions() {
        stubSteps.postRegions(SHOP_ID, BASE_REGION);
        currentRegions = stubSteps.getShopRegions(SHOP_ID);
        assertStep("изменился регион", currentRegions, containsEqualItemsInAnyOrder(asList(BASE_REGION)));

        stubSteps.postRegions(SHOP_ID, CHANGED_REGION);
        currentRegions = stubSteps.getShopRegions(SHOP_ID);
        assertStep("изменился регион", currentRegions, containsEqualItemsInAnyOrder(asList(CHANGED_REGION)));
    }

    @Test
    public void testChangePaymentMethods() {
        stubSteps.postPaymentMethods(SHOP_ID, CASH_ON_DELIVERY);
        currentPaymentMethods = stubSteps.getShopPaymentMethods(SHOP_ID);
        assertStep("изменились способы оплаты", currentPaymentMethods, containsEqualItemsInAnyOrder(asList(CASH_ON_DELIVERY)));

        stubSteps.postPaymentMethods(SHOP_ID, YANDEX);
        currentPaymentMethods = stubSteps.getShopPaymentMethods(SHOP_ID);
        assertStep("изменились способы оплаты", currentPaymentMethods, containsEqualItemsInAnyOrder(asList(YANDEX)));
    }

    @Test
    public void testCompareRegionsWithCartResponse() {
        currentRegions = stubSteps.getShopRegions(SHOP_ID);

        PushApiRequestData request = requestWithBody(SHOP_ID, CartBodiesProvider.cartWithDeliveryForRegion(currentRegions.get(0)));

        PushApiCartResponse cartResponse = stubSteps.getCartResponse(request);

        assertThat(cartResponse.getItems(), notNullValue());
        assertStep("есть доставка в текущем регионе", cartResponse.getItems().getItems().get(0).getDelivery(), equalTo("true"));

    }

    @Test
    public void testComparePaymentMethodsWithCartResponse() {
        currentPaymentMethods = stubSteps.getShopPaymentMethods(SHOP_ID);
        assertThat(currentPaymentMethods, not(empty()));

        PushApiRequestData request = requestWithBody(SHOP_ID, getDefaultCartRequestBody());
        PushApiCartResponse cartResponse = stubSteps.getCartResponse(request);

        assertStep("методы оплаты совпадают", cartResponse.getPaymentMethods().getPaymentMethods(),
                containsEqualItemsInAnyOrder(currentPaymentMethods));
    }
}

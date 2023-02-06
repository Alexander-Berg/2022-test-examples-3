package ru.yandex.autotests.market.push.cart;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.DeliveryType;
import ru.yandex.autotests.market.push.api.beans.request.settings.Settings;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.CartSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.steps.SettingsSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;
import static ru.yandex.autotests.market.pushapi.utils.ApiDateFormatter.formatFromDateTime;

/**
 * Created by strangelet on 02.09.15.
 */
@Feature("Cart resource")
@Aqua.Test(title = "Проверка delivery в ответе push_api .")
@RunWith(Parameterized.class)
@Issues({@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1107"),
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1955")
})
public class CartDeliveryTest {

    private ShopResponse shopResponse = new ShopResponse(ShopResource.CART);
    private PushApiCompareSteps pushApiSteps = new PushApiCompareSteps();
    private CartRequestProvider cartRequestProvider = new CartRequestProvider();

    private PushApiResponse pushApiResponse;
    private PushApiRequestData requestData;
    @Parameterized.Parameter(0)
    public long shopId;
    @Parameterized.Parameter(1)
    public int region;
    @Parameterized.Parameter(2)
    public String shopType;
    private SettingsSteps settingsSteps = new SettingsSteps();
    private CartSteps cartSteps = new CartSteps();
    private static PushApiCompareSteps tester = new PushApiCompareSteps();
    private String defaultFrom;
    private String defaultTo;
    private Settings settings;


    @Parameterized.Parameters(name = "shop {0} {2} request region {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{SHOP_ELP_JSON, REGION_MSC, "spb partner-interface=false"},
                new Object[]{SHOP_ELP_JSON, REGION_SPB, "spb partner-interface=false"},
                new Object[]{SHOP_ELP_JSON_PI, REGION_MSC, "msc partner-interface=true"},
                new Object[]{SHOP_ELP_JSON_PI, REGION_SPB, "msc partner-interface=true"}
        );
    }

    @Before
    public void prepare() {
        DateTime now = new DateTime();
        defaultFrom = formatFromDateTime(now.plusDays(1));
        defaultTo = formatFromDateTime(now.plusDays(4));
        settings = settingsSteps.getSettingsByShopId(shopId);
    }

    @Test
    public void checkDelivery() {
        // Формируется ответ магазина
        pushApiResponse = settings.isPartnerInterface()
                ? shopResponse.cartPIWithDeliveryAndCount(1, true, true)
                : shopResponse.cartWithDeliveryAndCount(1, true, true);
        // установка ответа магазина
        pushApiSteps.setShopResponse(formatResponseAsJson(pushApiResponse), shopId, ShopResource.CART);
        // формирование запроса /cart
        Cart cartRequest = cartRequestProvider.createRequestForShopResponse(pushApiResponse, shopId, region, DeliveryType.DELIVERY);
        String requestBody = formatRequestAsXML(cartRequest);
        // запрос с формированием имени для еллиптикса
        requestData = requestWithBodyForCase(shopId, requestBody, shopId + region + shopType);
        // сохранить результат на эллиптикс
        //tester.saveExpectedToStorage(requestData);  // сохраналка  результатов на элиптикс
        // сравнить результат с ожидаемым ответом на эллиптиксе
        cartSteps.compareCartWithStorageWithoutDates(requestData);

    }

}

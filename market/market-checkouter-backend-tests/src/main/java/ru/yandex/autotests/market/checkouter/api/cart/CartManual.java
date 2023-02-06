package ru.yandex.autotests.market.checkouter.api.cart;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.market.checkouter.api.data.requests.cart.CartValidationRequests;
import ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutApiRequestFactory;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdRule;
import ru.yandex.autotests.market.checkouter.api.steps.CartSteps;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataMultiCart;
import ru.yandex.autotests.market.checkouter.client.body.request.cart.CartRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CartResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.cart.CartResponseMultiCart;
import ru.yandex.autotests.market.checkouter.client.body.utils.CheckouterDataUtils;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;

import static ru.yandex.autotests.market.checkouter.client.body.utils.BodyMapper.map;

/**
 * User: jkt
 * Date: 25.07.13
 * Time: 18:31
 */
@RunWith(Parameterized.class)
public class CartManual {

    private CartSteps cart = new CartSteps();

    @ClassRule
    public static final ShopIdRule SHOP_ID_RULE = new ShopIdRule(ShopTags.CHECKOUTER_PAY);
    @Rule
    public RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule();
    @Parameter
    private CheckoutApiRequestFactory requestFactory;
    @Parameter
    private  static long shopId;
    private CheckoutApiRequest<CartRequestBody, CartResponseBody> request;
    private TestDataMultiCart requestMultiCart ;
    private CartResponseMultiCart responseMultiCart;


    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return CheckouterDataUtils.asRequestWithCase(
                CartValidationRequests.validCartRequestFactory()
        );
    }

    public CartManual(CheckoutApiRequestFactory requestFactory, String caseName) {
        this.requestFactory = requestFactory;
    }

    @BeforeClass
    public static void setUpClass() {
        shopId = SHOP_ID_RULE.getTestShop();
    }

    @Before
    public void makeRequest() {
        request = requestFactory.getRequest(shopId);
        requestMultiCart = map(request.getBodyBean(), TestDataMultiCart.class);
        cart.setValidShopResponseFor(requestMultiCart);
    }

    @Test
    public void sendRequest() {
//        for (int i = 0; i < 500; i++)
            cart.getMultiCartByRequest(request);

//        for (int i = 0; i < 500; i++) {
//            cart.getMultiCartByRequest(request);
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
    }
}

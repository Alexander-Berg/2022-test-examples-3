package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.settings.DataType;

/**
 * @author mkasumov
 */
public class OrderItemsChangeJSONTest extends AbstractOrderItemsChangeTest {

    public OrderItemsChangeJSONTest() {
        super(DataType.JSON);
    }

    @Test
    public void test() throws Exception {
        performOrderItemsChange(SHOP_ID);
        assertShopAdminRequestHasItemChange();
    }

    private void assertShopAdminRequestHasItemChange() {
        shopadminStubMock.verify(
                RequestPatternBuilder.newRequestPattern()
                        .withUrl("/svn-shop/774/order/items")
                        .withRequestBody(new ContainsPattern("{\"id\":234,\"feedId\":200305173,\"offerId\":\"4\",\"feedCategoryId\":\"{{feedcategory}}\",\"offerName\":\"{{offername}}\",\"price\":100,\"subsidy\":0,\"count\":5}"))
                        .withRequestBody(new ContainsPattern("{\"id\":235,\"feedId\":200305173,\"offerId\":\"45\",\"feedCategoryId\":\"{{feedcategory}}\",\"offerName\":\"{{offername}}\",\"price\":200,\"subsidy\":0,\"count\":4}")));
    }

}

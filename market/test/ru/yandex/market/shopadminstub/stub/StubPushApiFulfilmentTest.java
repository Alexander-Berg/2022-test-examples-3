package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;

public class StubPushApiFulfilmentTest extends AbstractTestBase {
    @Autowired
    private CartHelper cartHelper;

    @Test
    public void testFulfilmentCart() throws Exception {
        Item fulfilmentItem = ItemProvider.buildFulfilmentItem();

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(fulfilmentItem);
        cartRequest.setFulfilment(true);

        CartParameters cartParameters = new CartParameters(cartRequest);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());

        checkItem(resultActions, 1, fulfilmentItem.getFeedId(), fulfilmentItem.getOfferId());
    }
}

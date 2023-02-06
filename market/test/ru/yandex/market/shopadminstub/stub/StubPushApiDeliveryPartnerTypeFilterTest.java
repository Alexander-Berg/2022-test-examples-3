package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOption;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;


public class StubPushApiDeliveryPartnerTypeFilterTest extends AbstractTestBase {

    @Autowired
    private CartHelper cartHelper;

    /**
     * Стаб должен отдавать доставку только для опций из репорта с partnerType = REGULAR или без partnerType
     */
    @ParameterizedTest
    @CsvSource({"market_delivery,false", "regular,true"})
    public void filtersDeliveryOptionsByPartnerType(String partnerType, boolean isDelivered) throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportParameters().setDeliveryOptionModifier(option -> {
            option.setPartnerType(partnerType);
        });
        ResultActions resultActions = cartHelper.cart(cartParameters);
        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID, isDelivered);
        if (isDelivered) {
            LocalDate today = LocalDate.now();
            checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID,
                    "Курьер", "DELIVERY", "100.00", 1, 2);
            checkDeliveryOption(resultActions, today, 2, StubPushApiTestUtils.ONLY_CASH, "Курьер",
                    "DELIVERY", "50.00", 14, 14);
            checkDeliveryOption(resultActions, today, 3, StubPushApiTestUtils.ONLY_CASH, "Курьер",
                    "DELIVERY", "0.00", 31, null);
            resultActions
                    .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                    .andExpect(
                            xpath("/cart/payment-methods/payment-method[1]/text()")
                                    .string(PaymentMethod.CASH_ON_DELIVERY.name())
                    )
                    .andExpect(
                            xpath("/cart/payment-methods/payment-method[2]/text()")
                                    .string(PaymentMethod.CARD_ON_DELIVERY.name())
                    );
        }
    }
}

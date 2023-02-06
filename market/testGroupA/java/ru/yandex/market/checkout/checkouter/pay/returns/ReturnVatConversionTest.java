package ru.yandex.market.checkout.checkouter.pay.returns;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.ReturnProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;

public class ReturnVatConversionTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private RefundHelper refundHelper;
    private Return ret;

    @BeforeEach
    public void createOrderAndReturn() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();

        order.set(orderServiceTestHelper.createDeliveredBluePostPaid1POrder());

        Return request = ReturnProvider.generateReturnWithoutCompensation(order.get());
        returnHelper.mockActualDelivery(request, order.get());
        ret = returnHelper.createReturn(order.get().getId(), request);
        trustMockConfigurer.resetRequests();
    }

    @Test
    public void postPaidOrderRefundVatConversion() {
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(order().getId());

        String createBasket = getCreateBasketRequestBody();
        assertThat(createBasket, containsString("nds_20"));
        assertThat(createBasket, not(containsString("nds_18")));
    }

    @Test
    public void postPaidOrderRefundWoVatConversion() {
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(order().getId());
        String createBasket = getCreateBasketRequestBody();
        assertThat(createBasket, containsString("nds_20"));
        assertThat(createBasket, not(containsString("nds_18")));
    }

    private String getCreateBasketRequestBody() {
        List<String> createBasketRequests = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());
        assertThat(createBasketRequests, hasSize(1));
        return createBasketRequests.get(0);
    }

}

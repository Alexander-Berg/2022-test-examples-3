package ru.yandex.market.checkout.checkouter.pay;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.ReturnHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author : poluektov
 * date: 15.01.2019.
 */
public class SearchPaymentOrderIdsTest extends AbstractPaymentTestBase {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReturnHelper returnHelper;

    private Order order;
    private Payment createdPayment;

    @BeforeEach
    public void createOrderAndPayment() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        order = orderServiceTestHelper.createDeliveredBlueOrder();
        createdPayment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
    }

    @Test
    public void testSearchByBasketId() {
        List<PaymentOrderIdsSearchResult> result =
                paymentService.findOrderIdsByPayment(searchRequestWithBasketId(createdPayment));
        checkResponse(result);
    }

    @Test
    public void testSearchByIdViaClient() {
        List<PaymentOrderIdsSearchResult> response =
                client.payments().findOrderIdsByPayment(searchRequestWithBasketId(createdPayment),
                        ClientRole.REFEREE, 123L);
        checkResponse(response);
    }

    @Test
    public void testSearchByCardViaClient() {
        List<PaymentOrderIdsSearchResult> response =
                client.payments().findOrderIdsByPayment(searchRequestWithCard(createdPayment),
                        ClientRole.REFEREE, 123L);
        checkResponse(response);
    }

    @Test
    public void testSearchByCardNumber() {
        List<PaymentOrderIdsSearchResult> result =
                paymentService.findOrderIdsByPayment(searchRequestWithCard(createdPayment));
        checkResponse(result);
    }

    private PaymentSearchRequest searchRequestWithBasketId(Payment payment) {
        PaymentSearchRequest request = new PaymentSearchRequest();
        request.setBasketId(payment.getBasketId());
        return request;
    }

    private PaymentSearchRequest searchRequestWithCard(Payment payment) {
        PaymentSearchRequest request = new PaymentSearchRequest();
        request.setCard(payment.getCardNumber());
        Date toDate = Date.from(getClock().instant());
        Date fromDate = DateUtil.addDay(toDate, -7);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setAmount(payment.getTotalAmount());
        return request;
    }

    private void checkResponse(List<PaymentOrderIdsSearchResult> result) {
        assertThat(result, not(empty()));
        PaymentOrderIdsSearchResult paymentInfo = result.get(0);
        assertThat(paymentInfo.getPaymentId(), equalTo(createdPayment.getId()));
        assertThat(paymentInfo.getOrderIds(), hasSize(1));
        assertThat(paymentInfo.getOrderIds().get(0), equalTo(order.getId()));
    }
}

package ru.yandex.market.notifier.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;

import static org.mockito.Mockito.mock;

public class NotifierSimpleContainerTest extends AbstractContainerTestBase {

    @Autowired
    private CheckouterClient checkoutClient;

    @Test
    public void jettyShouldStart() {
        ResponseEntity<String> forEntity = testRestTemplate.getForEntity("/ping", String.class);

        Assertions.assertNotEquals(HttpStatus.NOT_FOUND, forEntity.getStatusCode());
    }

    @Test
    public void importCheckoutEventShouldDoSomething() {
        CheckouterOrderHistoryEventsApi orderHistoryEventsApi = mock(CheckouterOrderHistoryEventsApi.class);
        Mockito.doReturn(null)
                .when(orderHistoryEventsApi).getOrderHistoryEvent(Mockito.anyLong());
        Mockito.doReturn(orderHistoryEventsApi)
                .when(checkoutClient).orderHistoryEvents();

        ResponseEntity<String> forEntity
                = testRestTemplate.postForEntity("/import-checkouter-event?orderId=1&orderId=3", null, String.class);

        Assertions.assertEquals(HttpStatus.OK, forEntity.getStatusCode());
    }

}

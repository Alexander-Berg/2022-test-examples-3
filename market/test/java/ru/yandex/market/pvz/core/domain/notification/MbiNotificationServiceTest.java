package ru.yandex.market.pvz.core.domain.notification;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MbiNotificationServiceTest {

    private MbiApiClient mbiApiClient;

    private MbiNotificationService mbiNotificationService;

    @BeforeEach
    void setup() {
        mbiApiClient = mock(MbiApiClient.class);

        mbiNotificationService = new MbiNotificationService(mbiApiClient);
        ReflectionTestUtils.setField(mbiNotificationService,
                "orderUrlTemplate", "https://order.url/tpl-outlet/{}/orders/{}");
    }

    @Test
    void testBuildClientNearPvzMessage() {
        List<OrderSimpleParams> orderSimpleParams = List.of(
                OrderSimpleParams.builder().id(1L).pvzMarketId(134L).externalId("11").build(),
                OrderSimpleParams.builder().id(2L).pvzMarketId(134L).externalId("22").build()
        );

        mbiNotificationService.sendClientNearPvzNotification(10, orderSimpleParams, "Сережа");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mbiApiClient).sendMessageToShop(any(), any(), messageCaptor.capture());


        String actual = messageCaptor.getValue();
        String expected = "" +
                "<message>К вам направляется Сережа за заказами: " +
                "<link url=\"https://order.url/tpl-outlet/134/orders/1\">11</link>" +
                ", " +
                "<link url=\"https://order.url/tpl-outlet/134/orders/2\">22</link>" +
                ". Уже скоро он будет у вас, подготовьте заказы!</message>";

        assertThat(actual).isEqualTo(expected);
    }
}

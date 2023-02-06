package ru.yandex.market.global.checkout.sms;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.mj.generated.client.sms.api.SmsApiClient;

@Disabled
public class SmsClientTest extends BaseLocalTest {
    @Autowired
    private SmsApiClient smsApiClient;

    @Test
    public void test() {
        String response = smsApiClient.sendsmsPost("global-market-checkout-testing", 19387632L, "bebebe")
                .schedule().join();

        System.out.println(response);
    }
}

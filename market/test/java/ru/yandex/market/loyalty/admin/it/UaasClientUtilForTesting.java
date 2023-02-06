package ru.yandex.market.loyalty.admin.it;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.loyalty.core.service.uaas.Split;
import ru.yandex.market.loyalty.core.service.uaas.UaasService;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

@Ignore("this test suite should be run manually because it uses real Avatar")
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class UaasClientUtilForTesting {
    private static final String SOME_IP6 = "2a02:6b8:0:408:a538:6daf:78d7:b9ad";
    private static final String SOME_YANDEX_ID = "asdasdasdas";
    @Autowired
    private UaasService uaasService;
    private String environmentBefore;

    @Before
    public void setUp() {
        environmentBefore = System.getProperty("testing");
        System.setProperty("environment", "testing");
    }

    @After
    public void cleanUp() {
        if (environmentBefore != null) {
            System.setProperty("environment", environmentBefore);
        } else {
            System.clearProperty("environment");
        }
    }

    @Test
    public void getSplits() {
        List<Split> split = uaasService.getSplitsForEvent(
                OrderStatusUpdatedEvent.builder()
                        .addPersistentData(OrderEventInfo.builder()
                                .setYandexUid(SOME_YANDEX_ID)
                                .setUserIp(SOME_IP6)
                                .setDeliveryRegion(213L)
                                .setUserAgent(
                                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, " +
                                                "like Gecko) Chrome/59.0.3071.115 Safari/537.36"
                                )
                                .setNoAuth(false)
                                .setUid(123123213L)
                                .build()
                        )
                        .build()
        );
        System.out.println(split);
    }
}

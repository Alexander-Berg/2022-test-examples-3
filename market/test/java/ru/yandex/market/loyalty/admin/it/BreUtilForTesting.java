package ru.yandex.market.loyalty.admin.it;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.trigger.restrictions.segments.UserSegmentsServiceClient;

import java.util.Collections;

@Ignore
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class BreUtilForTesting {
    @Autowired
    private UserSegmentsServiceClient userSegmentsServiceClient;

    @Test
    public void testRequestUserSegments() {
        userSegmentsServiceClient.getUserSegments(Identity.Type.UID.buildIdentity("69238496"), Collections.singleton(
                "seg_46bFSN"));
    }
}

package ru.yandex.direct.core.entity.campaign.service.accesschecker.api5;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.RequestSource;

import static org.junit.Assert.assertNotNull;

public class Api5CampaignAccessibilityCheckerTest {

    @Test
    public void testCheckerByRequestSource() throws Exception {
        StreamEx.of(RequestSource.values()).forEach(source -> assertNotNull(
                "Проверяем наличие Api5CampaignAccessibilityChecker для RequestSource " + source,
                Api5CampaignAccessibilityChecker.getApi5AccessibilityChecker(source)));
    }
}

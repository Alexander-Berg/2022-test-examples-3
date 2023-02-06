package ru.yandex.direct.grid.processing.service.campaign;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailEvent;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignEmailSettings;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.CampaignNotificationUtils.getAvailableEmailEvents;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignDataConverter.toGdCampaignCheckPositionInterval;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@ParametersAreNonnullByDefault
public class ExtractGdEmailSettingsTest {

    private GdiCampaign gdiCampaign;
    private GdCampaignEmailSettings expectedEmailSettings;

    @Before
    public void initTestData() {
        gdiCampaign = getGdiCampaign();
        expectedEmailSettings = getExpectedEmailSettings(gdiCampaign);
    }


    @Test
    public void checkExtractGdEmailSettings() {
        GdCampaignEmailSettings actualEmailSettings = CampaignDataConverter.extractGdEmailSettings(gdiCampaign);

        assertThat(actualEmailSettings)
                .is(matchedBy(beanDiffer(expectedEmailSettings)));
    }

    @Test
    public void checkExtractGdEmailSettings_whenDisableCheckPositionEvent() {
        gdiCampaign.setEnableCheckPositionEvent(false);
        expectedEmailSettings.setCheckPositionInterval(null);

        GdCampaignEmailSettings actualEmailSettings = CampaignDataConverter.extractGdEmailSettings(gdiCampaign);

        assertThat(actualEmailSettings)
                .is(matchedBy(beanDiffer(expectedEmailSettings)));
    }

    private static GdiCampaign getGdiCampaign() {
        return new GdiCampaign()
                .withWalletId(null)
                .withType(CampaignType.TEXT)
                .withManagerUserId(RandomNumberUtils.nextPositiveLong())
                .withEmail(RandomStringUtils.randomAlphabetic(7))
                .withWarningBalance(RandomNumberUtils.nextPositiveInteger())
                .withEnableSendAccountNews(RandomUtils.nextBoolean())
                .withEnableOfflineStatNotice(RandomUtils.nextBoolean())
                .withEnablePausedByDayBudgetEvent(RandomUtils.nextBoolean())
                .withEnableCheckPositionEvent(true)
                .withCheckPositionInterval(CampaignWarnPlaceInterval._30);
    }

    private static GdCampaignEmailSettings getExpectedEmailSettings(GdiCampaign campaign) {
        var allowedEvents =
                getAvailableEmailEvents(campaign.getWalletId(), campaign.getType(), campaign.getManagerUserId());
        return new GdCampaignEmailSettings()
                .withEmail(campaign.getEmail())
                .withAllowedEvents(mapSet(allowedEvents, GdCampaignEmailEvent::fromSource))
                .withWarningBalance(campaign.getWarningBalance())
                .withSendAccountNews(campaign.getEnableSendAccountNews())
                .withXlsReady(campaign.getEnableOfflineStatNotice())
                .withStopByReachDailyBudget(campaign.getEnablePausedByDayBudgetEvent())
                .withCheckPositionInterval(toGdCampaignCheckPositionInterval(campaign.getCheckPositionInterval()));
    }
}

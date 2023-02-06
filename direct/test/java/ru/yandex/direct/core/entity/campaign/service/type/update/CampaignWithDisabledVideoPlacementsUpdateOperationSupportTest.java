package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledVideoPlacements;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CampaignWithDisabledVideoPlacementsUpdateOperationSupportTest {
    private static final String DOMAIN = "domain.com";
    private static final String ANOTHER_DOMAIN = "anotherdomain.com";
    private static final String WWW = "www.";
    private static final String PARAM = "/param";
    private static final Long CID = RandomUtils.nextLong();

    @Test
    public void test() {
        HostingsHandler hostingsHandler = mock(HostingsHandler.class);
        when(hostingsHandler.stripWww(eq(DOMAIN + PARAM))).thenReturn(DOMAIN);
        when(hostingsHandler.stripWww(eq(WWW + ANOTHER_DOMAIN))).thenReturn(ANOTHER_DOMAIN);

        CampaignWithDisabledVideoPlacementsUpdateOperationSupport support =
                new CampaignWithDisabledVideoPlacementsUpdateOperationSupport(hostingsHandler);

        CampaignWithDisabledVideoPlacements campaign = new CpmBannerCampaign()
                .withId(CID)
                .withDisabledDomains(Collections.emptyList());

        ModelChanges<CampaignWithDisabledVideoPlacements> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithDisabledVideoPlacements.class);

        campaignModelChanges.process(List.of(DOMAIN + PARAM, WWW + ANOTHER_DOMAIN),
                CampaignWithDisabledVideoPlacements.DISABLED_VIDEO_PLACEMENTS);
        AppliedChanges<CampaignWithDisabledVideoPlacements> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        support.onChangesApplied(null, List.of(campaignAppliedChanges));

        List<String> domains =
                campaignAppliedChanges.getNewValue(CampaignWithDisabledVideoPlacements.DISABLED_VIDEO_PLACEMENTS);
        assertThat(domains).containsExactlyInAnyOrder(DOMAIN, ANOTHER_DOMAIN);
    }

}

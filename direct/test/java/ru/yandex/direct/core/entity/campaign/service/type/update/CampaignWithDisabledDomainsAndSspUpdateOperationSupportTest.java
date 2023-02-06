package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledDomainsAndSsp;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithDisabledDomainsAndSspUpdateOperationSupportTest {
    private static final String DOMAIN = "domain.com";
    private static final String ANOTHER_DOMAIN = "anotherdomain.com";
    private static final String WWW = "www.";
    private static final Long CID = RandomUtils.nextLong();

    @Mock
    private FeatureService featureService;

    @Autowired
    private DisableDomainValidationService disableDomainValidationService;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.PERFORMANCE}
        });
    }

    @Test
    public void test() {
        HostingsHandler hostingsHandler = mock(HostingsHandler.class);
        when(hostingsHandler.stripWww(eq(DOMAIN))).thenReturn(DOMAIN);
        when(hostingsHandler.stripWww(eq(WWW + ANOTHER_DOMAIN))).thenReturn(ANOTHER_DOMAIN);

        CampaignWithDisabledDomainsAndSspUpdateOperationSupport support =
                new CampaignWithDisabledDomainsAndSspUpdateOperationSupport(hostingsHandler,
                        disableDomainValidationService);

        CampaignWithDisabledDomainsAndSsp campaign =
                ((CampaignWithDisabledDomainsAndSsp) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(CID)
                        .withDisabledDomains(Collections.emptyList());

        ModelChanges<CampaignWithDisabledDomainsAndSsp> campaignModelChanges = new ModelChanges<>(CID,
                CampaignWithDisabledDomainsAndSsp.class);

        campaignModelChanges.process(List.of(DOMAIN, WWW + ANOTHER_DOMAIN),
                CampaignWithDisabledDomainsAndSsp.DISABLED_DOMAINS);
        AppliedChanges<CampaignWithDisabledDomainsAndSsp> campaignAppliedChanges =
                campaignModelChanges.applyTo(campaign);

        support.onChangesApplied(RestrictedCampaignsUpdateOperationContainer.create(
                0,
                null,
                null,
                null,
                null), List.of(campaignAppliedChanges));

        List<String> domains = campaignAppliedChanges.getNewValue(CampaignWithDisabledDomainsAndSsp.DISABLED_DOMAINS);
        assertThat(domains).containsExactlyInAnyOrder(DOMAIN, ANOTHER_DOMAIN);
    }
}

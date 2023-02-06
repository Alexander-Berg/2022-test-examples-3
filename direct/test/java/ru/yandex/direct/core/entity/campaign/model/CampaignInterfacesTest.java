package ru.yandex.direct.core.entity.campaign.model;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

public class CampaignInterfacesTest {

    private static final Set<Class<? extends BaseCampaign>> SPECIAL_TYPES = Set.of(CampaignStub.class,
            WalletTypedCampaign.class,
            BillingAggregateCampaign.class);

    private Set<Class<? extends BaseCampaign>> campaignTypes;

    @Before
    public void before() {
        Reflections reflections = new Reflections("ru.yandex.direct.core.entity.campaign");
        campaignTypes = StreamEx.of(reflections.getSubTypesOf(BaseCampaign.class))
                .filter(campaignType -> !campaignType.isInterface())
                .filter(campaignType -> !SPECIAL_TYPES.contains(campaignType))
                .toSet();
    }

    @Test
    public void checkRequireFiltrationByDontShowDomainsInterfaces() {
        campaignTypes.forEach(campaignType -> {
            boolean implementsPositive =
                    CampaignWithOptionalRequireFiltrationByDontShowDomains.class.isAssignableFrom(campaignType);
            boolean implementsNegative =
                    CampaignWithRequireFiltrationByDontShowDomainsForbidden.class.isAssignableFrom(campaignType);
            String message = format("Кампания типа %s неправильно реализует интерфейсы для поля"
                    + " RequireFiltrationByDontShowDomains", campaignType.getSimpleName());

            if (campaignType != ContentPromotionCampaign.class && campaignType != MobileContentCampaign.class
                    && campaignType != McBannerCampaign.class) {
                assertTrue(message, implementsPositive ^ implementsNegative);
            } else {
                // Костыль для продвижения, см. CampaignWithRequireFiltrationByDontShowDomainsForbiddenValidator
                assertTrue(message, implementsPositive && implementsNegative);
            }
        });
    }
}

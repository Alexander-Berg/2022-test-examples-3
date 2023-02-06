package ru.yandex.market.core.partner.onboarding.sender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.partner.model.PartnerTypeAwareInfo;
import ru.yandex.market.core.partner.onboarding.finder.PartnerBeingOnboarded;
import ru.yandex.market.core.partner.onboarding.sender.delay.OnboardingDelayCalculator;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class SenderTestUtils {

    public static long PARTNER_ID = 1010L;
    public static long CAMPAIGN_ID = 101010L;
    public static PartnerId PARTNER_ID_ENTITY = PartnerId.supplierId(PARTNER_ID);
    public static long CHILD_PARTNER_ID = 1020L;
    public static PartnerId CHILD_PARTNER_ID_ENTITY = PartnerId.supplierId(CHILD_PARTNER_ID);
    public static long RUSSIA_ID = 225;
    public static long MOSCOW_ID = 1;
    public static String PARTNER_MARKET_URL = "http://some-test-url.com";

    public static void assertTemplateContainsSections(NamedContainer namedContainer, List<String> templateSections) {
        assertThat(templateSections).containsExactlyInAnyOrderElementsOf((List<String>) namedContainer.getContent());
    }

    @SuppressWarnings("unchecked")
    public static NamedContainer getContainerByName(List data, String name) {
        return (NamedContainer) data.stream()
                .filter(o -> ((NamedContainer) o).name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, NamedContainer> getContainersByName(List<?> data) {
        return data.stream()
                .map(o -> (NamedContainer) o)
                .collect(Collectors.toMap(NamedContainer::name, Function.identity()));
    }

    public static PartnerOnboardingState createTestDropshipState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList
    ) {
        return createTestDropshipState(partnerId, partnerCreatedAt, stepDataList, true);
    }

    public static PartnerOnboardingState createTestDropshipState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList,
            boolean actualWizardStatus
    ) {
        return PartnerOnboardingState.builder()
                .withPartnerId(partnerId)
                .withCampaignId(CAMPAIGN_ID)
                .withPartnerName("dropship_partner")
                .withPartnerTypeAwareInfo(dropshipInfo())
                .withCountryId(RUSSIA_ID)
                .withLocalRegionId(MOSCOW_ID)
                .withPartnerCreatedAt(partnerCreatedAt)
                .withStepDataList(stepDataList)
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(actualWizardStatus)
                .build();
    }

    public static PartnerOnboardingState createTestDropshipBySellerState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList
    ) {
        return PartnerOnboardingState.builder()
                .withPartnerId(partnerId)
                .withCampaignId(CAMPAIGN_ID)
                .withPartnerName("dropship_by_seller_partner")
                .withPartnerTypeAwareInfo(dropshipBySellerInfo())
                .withCountryId(RUSSIA_ID)
                .withLocalRegionId(MOSCOW_ID)
                .withPartnerCreatedAt(partnerCreatedAt)
                .withStepDataList(stepDataList)
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(true)
                .build();
    }

    public static PartnerOnboardingState createTestCrossdockState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList
    ) {
        return PartnerOnboardingState.builder()
                .withPartnerId(partnerId)
                .withPartnerName("crossdock_partner")
                .withCampaignId(CAMPAIGN_ID)
                .withPartnerTypeAwareInfo(crossdockInfo())
                .withCountryId(RUSSIA_ID)
                .withLocalRegionId(MOSCOW_ID)
                .withPartnerCreatedAt(partnerCreatedAt)
                .withStepDataList(stepDataList)
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(true)
                .build();
    }

    public static PartnerOnboardingState createTestFulfillmentState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList
    ) {
        return PartnerOnboardingState.builder()
                .withPartnerId(partnerId)
                .withPartnerName("fulfillment_partner")
                .withCampaignId(CAMPAIGN_ID)
                .withPartnerTypeAwareInfo(fulfillmentInfo())
                .withCountryId(RUSSIA_ID)
                .withLocalRegionId(MOSCOW_ID)
                .withPartnerCreatedAt(partnerCreatedAt)
                .withStepDataList(stepDataList)
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(true)
                .build();
    }

    public static PartnerBeingOnboarded createOnboardingCandidate(
            PartnerId partnerId,
            String name,
            Instant createdAt
    ) {
        return new PartnerBeingOnboarded(
                partnerId,
                CAMPAIGN_ID,
                name,
                createdAt
        );
    }

    public static PartnerOnboardingState createTestNotDropshipState(
            PartnerId partnerId,
            Instant partnerCreatedAt,
            List<PartnerOnboardingState.WizardStepData> stepDataList
    ) {
        return PartnerOnboardingState.builder()
                .withPartnerId(partnerId)
                .withPartnerName("not_dropship_partner")
                .withCampaignId(CAMPAIGN_ID)
                .withPartnerTypeAwareInfo(notDropshipInfo())
                .withCountryId(RUSSIA_ID)
                .withLocalRegionId(MOSCOW_ID)
                .withPartnerCreatedAt(partnerCreatedAt)
                .withStepDataList(stepDataList)
                .withHasPublishError(false)
                .withActualWizardStepsCompleted(true)
                .build();
    }

    public static PartnerTypeAwareInfo notDropshipInfo() {
        return new PartnerTypeAwareInfo.Builder()
                .setDropship(false)
                .build();
    }

    public static PartnerTypeAwareInfo dropshipInfo() {
        return new PartnerTypeAwareInfo.Builder()
                .setDropship(true)
                .build();
    }

    public static PartnerTypeAwareInfo dropshipBySellerInfo() {
        return new PartnerTypeAwareInfo.Builder()
                .setDropshipBySeller(true)
                .build();
    }

    public static PartnerTypeAwareInfo crossdockInfo() {
        return new PartnerTypeAwareInfo.Builder()
                .setFulfillment(true)
                .setCrossdock(true)
                .build();
    }

    public static PartnerTypeAwareInfo fulfillmentInfo() {
        return new PartnerTypeAwareInfo.Builder()
                .setFulfillment(true)
                .setCrossdock(false)
                .build();
    }

    /**
     * Простой калькулятор задержки по дням, считающий просто расстояние по времени
     */
    public static OnboardingDelayCalculator simpleStateDelayCalculator() {
        return state -> Duration.between(state.getLastUpdateDate(), Instant.now());
    }
}

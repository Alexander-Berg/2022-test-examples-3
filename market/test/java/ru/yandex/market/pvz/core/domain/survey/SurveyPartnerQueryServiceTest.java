package ru.yandex.market.pvz.core.domain.survey;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.banner_information.BannerCampaignFeatures;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SurveyPartnerQueryServiceTest {

    private final TestSurveyFactory surveyFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final SurveyPartnerQueryService surveyPartnerQueryService;
    private final PickupPointQueryService pickupPointQueryService;

    @Test
    void whenSurveyPartnerEmpty() {
        SurveyParams surveyParams = surveyFactory.create();
        Map<Long, SurveyPartnerDbQueueParams> surveyPartnerDbQueueParams =
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams);

        assertThat(surveyPartnerDbQueueParams).hasSize(0);
    }

    @Test
    void findPartnerWithoutSettings() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        var legalPartnerNotBranded = createApprovedLegalPartnerWithNotBrandPickupPoint();
        var legalPartnerWithoutActivePickupPoint = createApprovedLegalPartnerWithNotActivePickupPoint();
        createNotApprovedLegalPartner();

        SurveyParams surveyParams = surveyFactory.create();
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).hasSize(3);
        assertThat(ids).containsAll(List.of(
                legalPartnerBranded.getId(), legalPartnerNotBranded.getId(),
                legalPartnerWithoutActivePickupPoint.getId()
        ));
    }

    @Test
    void findPickupPointWithoutSettings() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        addBrandedPickupPointToLegalPartner(legalPartnerBranded);
        var legalPartnerNotBranded = createApprovedLegalPartnerWithNotBrandPickupPoint();
        addBrandedPickupPointToLegalPartner(legalPartnerNotBranded);
        createApprovedLegalPartnerWithNotActivePickupPoint();
        createNotApprovedLegalPartner();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .surveyByPickupPoint(true)
                        .build()
        );
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).hasSize(4);

        var pickupPoints = pickupPointQueryService.getAllByLegalPartnerId(legalPartnerBranded.getId());
        pickupPoints.addAll(pickupPointQueryService.getAllByLegalPartnerId(legalPartnerNotBranded.getId()));
        var pickupPointIds = pickupPoints.stream()
                .map(PickupPointSimpleParams::getPvzMarketId)
                .collect(Collectors.toList());

        assertThat(ids).containsExactlyInAnyOrderElementsOf(pickupPointIds);
    }

    private LegalPartner createApprovedLegalPartnerWithBrandPickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory.forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));

        addBrandedPickupPointToLegalPartner(legalPartner);

        return legalPartner;
    }

    private LegalPartner createApprovedLegalPartnerWithNotBrandPickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory.forceApprove(
                legalPartner.getId(), LocalDate.of(2021, 1, 1)
        );
        pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        return legalPartner;
    }

    private LegalPartner createNotApprovedLegalPartner() {
        return legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .approvePreLegalPartner(false)
                        .build()
        );
    }

    private void addBrandedPickupPointToLegalPartner(LegalPartner legalPartner) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(
                                TestPickupPointFactory.PickupPointTestParams.builder()
                                        .name(RandomStringUtils.randomAlphanumeric(6))
                                        .build()
                        )
                        .build()
        );
        brandRegionFactory.createDefaults();
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());
    }

    @Test
    void findBrandPartner() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        createApprovedLegalPartnerWithNotBrandPickupPoint();
        createApprovedLegalPartnerWithNotActivePickupPoint();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .campaignFeatures(List.of(BannerCampaignFeatures.BRANDED.name()))
                        .build()
        );
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).isEqualTo(List.of(legalPartnerBranded.getId()));
    }

    @Test
    void findBrandPickupPoint() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        addBrandedPickupPointToLegalPartner(legalPartnerBranded);
        createApprovedLegalPartnerWithNotBrandPickupPoint();
        createApprovedLegalPartnerWithNotActivePickupPoint();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .surveyByPickupPoint(true)
                        .campaignFeatures(List.of(BannerCampaignFeatures.BRANDED.name()))
                        .build()
        );
        Set<Long> ids = surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet();

        assertThat(ids).hasSize(2);

        var pickupPointIds = pickupPointQueryService.getAllByLegalPartnerId(legalPartnerBranded.getId()).stream()
                .map(PickupPointSimpleParams::getPvzMarketId)
                .collect(Collectors.toList());

        assertThat(ids).containsExactlyInAnyOrderElementsOf(pickupPointIds);
    }

    private LegalPartner createApprovedLegalPartnerWithNotActivePickupPoint() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory.forceApprove(
                legalPartner.getId(), LocalDate.of(2021, 1, 1)
        );
        pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .activateImmediately(false)
                        .build()
        );
        return legalPartner;
    }

    @Test
    void findNotBrandAndDropOffPartner() {
        var legalPartnerDropOff = createApprovedLegalPartnerWithDropOff();
        var legalPartnerNotBranded = createApprovedLegalPartnerWithNotBrandPickupPoint();
        addBrandedPickupPointToLegalPartner(legalPartnerNotBranded);
        createApprovedLegalPartnerWithNotActivePickupPoint();
        createApprovedLegalPartnerWithBrandPickupPoint();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .campaignFeatures(List.of(
                                BannerCampaignFeatures.NOT_BRANDED.name(), BannerCampaignFeatures.DROP_OFF.name()
                        ))
                        .build()
        );
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).hasSize(2);
        assertThat(ids).containsAll(List.of(legalPartnerDropOff.getId(), legalPartnerNotBranded.getId()));
    }

    private LegalPartner createApprovedLegalPartnerWithDropOff() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory.forceApprove(
                legalPartner.getId(), LocalDate.of(2021, 1, 1)
        );
        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, legalPartner, null
        );
        return legalPartner;
    }

    @Test
    void findPartnersByIds() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        var legalPartnerNotApproved = createNotApprovedLegalPartner();
        createApprovedLegalPartnerWithNotBrandPickupPoint();
        createApprovedLegalPartnerWithNotActivePickupPoint();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .campaignIds(List.of(legalPartnerBranded.getId(), legalPartnerNotApproved.getId()))
                        .build()
        );
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).hasSize(1);
        assertThat(ids).isEqualTo(List.of(legalPartnerBranded.getId()));
    }

    @Test
    void findBrandPartnersAndByIds() {
        var legalPartnerBranded = createApprovedLegalPartnerWithBrandPickupPoint();
        var legalPartnerNotBranded = createApprovedLegalPartnerWithNotBrandPickupPoint();
        createNotApprovedLegalPartner();
        createApprovedLegalPartnerWithNotActivePickupPoint();

        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .campaignIds(List.of(legalPartnerNotBranded.getId()))
                        .campaignFeatures(List.of(BannerCampaignFeatures.BRANDED.name()))
                        .build()
        );
        List<Long> ids = new ArrayList<>(
                surveyPartnerQueryService.findCampaignBySurveySettings(surveyParams).keySet()
        );

        assertThat(ids).hasSize(2);
        assertThat(ids).containsAll(List.of(legalPartnerBranded.getId(), legalPartnerNotBranded.getId()));
    }

}

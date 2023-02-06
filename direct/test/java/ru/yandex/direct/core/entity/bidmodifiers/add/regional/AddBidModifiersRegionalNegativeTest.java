package ru.yandex.direct.core.entity.bidmodifiers.add.regional;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.geoBidModifiersNotSupportedOnAdGroups;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.geoRegionsIntersection;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.nonexistentRegionIds;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientGeoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления региональных корректировок ставок")
public class AddBidModifiersRegionalNegativeTest {

    private static final long NONEXISTENT_REGION_ID = 123456L;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    private AdGroupInfo adGroup;
    private Long campaignId;

    @Before
    public void before() {
        adGroup = adGroupSteps.createActiveTextAdGroup();
        campaignId = adGroup.getCampaignId();
    }

    @Test
    @Description("Добавим две одинаковые корректировки двумя запросами")
    public void sameRegionalAdjustmentsInSomeRequestTest() {
        addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaignId)
                                .withRegionalAdjustments(createDefaultClientGeoAdjustments())));
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaignId)
                                .withRegionalAdjustments(createDefaultClientGeoAdjustments())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("regionalAdjustments")),
                        geoRegionsIntersection())));
    }

    @Test
    @Description("Добавим две одинаковые корректировки одним запросом")
    public void sameRegionalAdjustmentsInOneRequestTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaignId)
                                .withRegionalAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierRegionalAdjustment()
                                                        .withRegionId(Region.RUSSIA_REGION_ID)
                                                        .withHidden(false)
                                                        .withPercent(110),
                                                new BidModifierRegionalAdjustment()
                                                        .withRegionId(Region.RUSSIA_REGION_ID)
                                                        .withHidden(false)
                                                        .withPercent(110)
                                        )
                                )));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("regionalAdjustments")),
                        geoRegionsIntersection())));
    }

    @Test
    @Description("Добавляем корректировку ставок на группу")
    public void regionalAdjustmentToGroupTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withRegionalAdjustments(createDefaultClientGeoAdjustments())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0)),
                        geoBidModifiersNotSupportedOnAdGroups())));
    }


    @Test
    @Description("Добавляем корректировку ставок с несуществующим id региона")
    public void add_WithNonexistentRegionId() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaignId)
                                .withRegionalAdjustments(createAdjustmentsWithRegionId(NONEXISTENT_REGION_ID))));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("regionalAdjustments")),
                        nonexistentRegionIds(String.valueOf(NONEXISTENT_REGION_ID)))));
    }

    @Test
    @Description("Добавляем корректировку ставок с отрицательным id региона")
    public void add_WithMinusRegionId() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaignId)
                                .withRegionalAdjustments(createAdjustmentsWithRegionId(-Region.RUSSIA_REGION_ID))));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("regionalAdjustments")),
                        nonexistentRegionIds(String.valueOf(-Region.RUSSIA_REGION_ID)))));
    }

    private List<BidModifierRegionalAdjustment> createAdjustmentsWithRegionId(long regionId) {
        return singletonList(createDefaultClientGeoAdjustment().withRegionId(regionId));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, adGroup.getClientId(), adGroup.getUid());
    }
}

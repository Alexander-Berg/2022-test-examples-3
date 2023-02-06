package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка допустимого диапазона корректировок ставок")
public class AddBidModifiersRangesWithMultiValuesTest {
    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    private TestContextManager testContextManager;

    @Parameterized.Parameter(0)
    public String adjustmentFieldName;

    @Parameterized.Parameter(1)
    public Function<List<BidModifierAdjustment>, BidModifier> itemFactory;

    @Parameterized.Parameter(2)
    public Supplier<BidModifierAdjustment> adjustmentSupplier;

    @Parameterized.Parameter(3)
    public int percentMin;

    @Parameterized.Parameter(4)
    public int percentMax;

    @Parameterized.Parameter(5)
    public Supplier<Defect> percentMinDefectSupplier;

    @Parameterized.Parameter(6)
    public Supplier<Defect> percentMaxDefectSupplier;

    @Parameterized.Parameter(7)
    public Function<CampaignSteps, CampaignInfo> campaignFactory;

    private static long retCondId;
    private CampaignInfo campaign;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        return cartesianProduct(
                ImmutableSet.of(
                        new Object[]{
                                "regionalAdjustments",
                                (Function<List<BidModifierRegionalAdjustment>, BidModifierGeo>)
                                        (adjustments) -> createEmptyClientGeoModifier()
                                                .withRegionalAdjustments(adjustments),
                                (Supplier<BidModifierRegionalAdjustment>)
                                        () -> new BidModifierRegionalAdjustment()
                                                .withRegionId(Region.RUSSIA_REGION_ID)
                                                .withHidden(false),
                                10,
                                1300,
                                (Supplier<Defect>) () -> NumberDefects.greaterThanOrEqualTo(10),
                                (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(1300),
                        },
                        new Object[]{
                                "demographicsAdjustments",
                                (Function<List<BidModifierDemographicsAdjustment>, BidModifierDemographics>)
                                        (adjustments) -> createEmptyClientDemographicsModifier()
                                                .withDemographicsAdjustments(adjustments),
                                (Supplier<BidModifierDemographicsAdjustment>)
                                        () -> new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._18_24),
                                0,
                                1300,
                                (Supplier<Defect>)
                                        BidModifiersDefects::invalidPercentShouldBePositive,
                                (Supplier<Defect>) () -> NumberDefects
                                        .lessThanOrEqualTo(1300),
                        },
                        new Object[]{
                                "retargetingAdjustments",
                                (Function<List<AbstractBidModifierRetargetingAdjustment>, BidModifierRetargeting>)
                                        (adjustments) -> createEmptyClientRetargetingModifier()
                                                .withRetargetingAdjustments(adjustments),
                                (Supplier<BidModifierRetargetingAdjustment>)
                                        () -> new BidModifierRetargetingAdjustment()
                                                .withRetargetingConditionId(retCondId),
                                0,
                                1300,
                                (Supplier<Defect>)
                                        BidModifiersDefects::invalidPercentShouldBePositive,
                                (Supplier<Defect>) () -> NumberDefects
                                        .lessThanOrEqualTo(1300),
                        }
                ),
                ImmutableSet.of(
                        new Object[]{
                                (Function<CampaignSteps, CampaignInfo>) CampaignSteps::createActiveTextCampaign
                        },
                        new Object[]{
                                (Function<CampaignSteps, CampaignInfo>) CampaignSteps::createActiveDynamicCampaign
                        }
                )).stream().map(e -> ArrayUtils.addAll(e.get(0), e.get(1))).collect(toList());
    }

    @Before
    public void before() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        this.campaign = campaignFactory.apply(campaignSteps);

        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo());
        retCondId = retCondition.getRetConditionId();
    }

    @Test
    public void percentMinTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        itemFactory.apply(
                                singletonList(adjustmentSupplier.get().withPercent(percentMin))
                        ).withCampaignId(campaign.getCampaignId())));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void percentLessThanMinTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        itemFactory.apply(
                                singletonList(adjustmentSupplier.get().withPercent(percentMin - 1))
                        ).withCampaignId(campaign.getCampaignId())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(adjustmentFieldName), index(0), field("percent")),
                percentMinDefectSupplier.get())));
    }

    @Test
    public void percentMaxTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        itemFactory.apply(
                                singletonList(adjustmentSupplier.get().withPercent(percentMax))
                        ).withCampaignId(campaign.getCampaignId())));
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void percentGreaterThanMaxTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        itemFactory.apply(
                                singletonList(adjustmentSupplier.get().withPercent(percentMax + 1))
                        ).withCampaignId(campaign.getCampaignId())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(adjustmentFieldName), index(0), field("percent")),
                percentMaxDefectSupplier.get())));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}

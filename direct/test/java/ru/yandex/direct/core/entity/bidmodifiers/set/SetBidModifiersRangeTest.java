package ru.yandex.direct.core.entity.bidmodifiers.set;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.PERCENT_GEO_MIN;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.PERCENT_MAX;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.PERCENT_MIN;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.PERCENT_MOBILE_VIDEO_MIN;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyVideoModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка допустимого диапазона корректировок ставок при модификации")
public class SetBidModifiersRangeTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Parameterized.Parameter(0)
    public String adjustmentFieldName;

    @Parameterized.Parameter(1)
    public Supplier<BidModifierAdjustment> adjustmentsSupplier;

    @Parameterized.Parameter(2)
    public Function<BidModifierAdjustment, BidModifier> bidModifierFactory;

    @Parameterized.Parameter(3)
    public int percentMin;

    @Parameterized.Parameter(4)
    public int percentMax;

    @Parameterized.Parameter(5)
    public Supplier<Defect> percentMinDefectSupplier;

    @Parameterized.Parameter(6)
    public Supplier<Defect> percentMaxDefectSupplier;

    private static long retCondId;
    private CampaignInfo campaignInfo;
    private Long bmId;
    private BidModifierAdjustment bidModifierAdjustment;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {
                        "mobileAdjustments",
                        (Supplier<BidModifierMobileAdjustment>) TestBidModifiers::createDefaultMobileAdjustment,
                        (Function<BidModifierMobileAdjustment, BidModifierMobile>)
                                (adjustment) -> createEmptyMobileModifier()
                                        .withMobileAdjustment(adjustment),
                        PERCENT_MIN,
                        PERCENT_MAX,
                        (Supplier<Defect>) () -> new Defect(
                                BidModifiersDefectIds.GeneralDefects.INVALID_PERCENT_SHOULD_BE_POSITIVE),
                        (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(PERCENT_MAX),
                },
                {
                        "demographicsAdjustments",
                        (Supplier<BidModifierDemographicsAdjustment>)
                                TestBidModifiers::createDefaultDemographicsAdjustment,
                        (Function<BidModifierDemographicsAdjustment, BidModifierDemographics>)
                                (adjustment) -> createEmptyDemographicsModifier()
                                        .withDemographicsAdjustments(singletonList(adjustment)),
                        PERCENT_MIN,
                        PERCENT_MAX,
                        (Supplier<Defect>)
                                () -> new Defect<>(
                                        BidModifiersDefectIds.GeneralDefects.INVALID_PERCENT_SHOULD_BE_POSITIVE),
                        (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(PERCENT_MAX),
                },
                {
                        "retargetingAdjustments",
                        (Supplier<BidModifierRetargetingAdjustment>)
                                () -> TestBidModifiers.createDefaultRetargetingAdjustment(retCondId),
                        (Function<BidModifierRetargetingAdjustment, BidModifierRetargeting>)
                                (adjustment) -> createEmptyRetargetingModifier()
                                        .withRetargetingAdjustments(singletonList(adjustment)),
                        PERCENT_MIN,
                        PERCENT_MAX,
                        (Supplier<Defect>)
                                () -> new Defect<>(
                                        BidModifiersDefectIds.GeneralDefects.INVALID_PERCENT_SHOULD_BE_POSITIVE),
                        (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(PERCENT_MAX),
                },
                {
                        "regionalAdjustments",
                        (Supplier<BidModifierRegionalAdjustment>)
                                TestBidModifiers::createDefaultGeoAdjustment,
                        (Function<BidModifierRegionalAdjustment, BidModifierGeo>)
                                (adjustments) -> createEmptyGeoModifier()
                                        .withRegionalAdjustments(singletonList(adjustments)),
                        PERCENT_GEO_MIN,
                        PERCENT_MAX,
                        (Supplier<Defect>) () -> NumberDefects.greaterThanOrEqualTo(PERCENT_GEO_MIN),
                        (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(PERCENT_MAX),
                },
                {
                        "videoAdjustments",
                        (Supplier<BidModifierVideoAdjustment>) TestBidModifiers::createDefaultVideoAdjustment,
                        (Function<BidModifierVideoAdjustment, BidModifierVideo>)
                                (adjustment) -> createEmptyVideoModifier()
                                        .withVideoAdjustment(adjustment),
                        PERCENT_MOBILE_VIDEO_MIN,
                        PERCENT_MAX,
                        (Supplier<Defect>) () -> NumberDefects
                                .greaterThanOrEqualTo(PERCENT_MOBILE_VIDEO_MIN),
                        (Supplier<Defect>) () -> NumberDefects.lessThanOrEqualTo(PERCENT_MAX),
                },
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {

        // Создаём кампанию
        campaignInfo = campaignSteps.createActiveTextCampaign();

        // Создаем условие ретаргетинга
        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = retCondition.getRetConditionId();

        // Создаем корректировку
        bidModifierAdjustment = adjustmentsSupplier.get();
        BidModifier bidModifier =
                bidModifierFactory.apply(bidModifierAdjustment).withCampaignId(campaignInfo.getCampaignId());

        MassResult<List<Long>> result = bidModifierService.add(singletonList(bidModifier),
                campaignInfo.getClientId(), campaignInfo.getUid());
        bmId = BidModifierService.getRealId(result.getResult().get(0).getResult().get(0));
    }

    @Test
    @Description("Процент минимальный")
    public void percentMinTest() {
        assertThat(setPercent(percentMin).getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Процент меньше минимального")
    public void percentLessThanMinTest() {
        assertThat(setPercent(percentMin - 1).getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0)), percentMinDefectSupplier.get())));
    }

    @Test
    @Description("Процент максимальный")
    public void percentMaxTest() {
        assertThat(setPercent(percentMax).getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Процент больше максимального")
    public void percentGreaterThanMaxTest() {
        assertThat(setPercent(percentMax + 1).getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0)), percentMaxDefectSupplier.get())));
    }

    private MassResult<Long> setPercent(int percent) {
        ModelChanges<BidModifierAdjustment> modelChanges = new ModelChanges<>(bmId,
                bidModifierAdjustment.getClass()).castModelUp(BidModifierAdjustment.class);
        modelChanges.process(percent, BidModifierAdjustment.PERCENT);

        return bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());
    }
}

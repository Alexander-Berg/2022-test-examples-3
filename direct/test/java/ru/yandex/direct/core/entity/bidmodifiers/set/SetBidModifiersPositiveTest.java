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
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyVideoModifier;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка позитивных сценариев обновления корректировок ставок")
public class SetBidModifiersPositiveTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    private static final Integer NEW_PERCENT = DEFAULT_PERCENT / 2;

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
    public Function<BidModifier, Integer> getPercent;

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
                        (Function<BidModifier, Integer>) (item) ->
                                ((BidModifierMobile) item).getMobileAdjustment().getPercent()
                },
                {
                        "demographicsAdjustments",
                        (Supplier<BidModifierDemographicsAdjustment>)
                                TestBidModifiers::createDefaultDemographicsAdjustment,
                        (Function<BidModifierDemographicsAdjustment, BidModifierDemographics>)
                                (adjustment) -> createEmptyDemographicsModifier()
                                        .withDemographicsAdjustments(singletonList(adjustment)),
                        (Function<BidModifier, Integer>) (item) ->
                                ((BidModifierDemographics) item).getDemographicsAdjustments().get(0).getPercent()
                },
                {
                        "retargetingAdjustments",
                        (Supplier<BidModifierRetargetingAdjustment>)
                                () -> TestBidModifiers.createDefaultRetargetingAdjustment(retCondId),
                        (Function<BidModifierRetargetingAdjustment, BidModifierRetargeting>)
                                (adjustment) -> createEmptyRetargetingModifier()
                                        .withRetargetingAdjustments(singletonList(adjustment)),
                        (Function<BidModifier, Integer>) (item) ->
                                ((BidModifierRetargeting) item).getRetargetingAdjustments().get(0).getPercent()
                },
                {
                        "regionalAdjustments",
                        (Supplier<BidModifierRegionalAdjustment>)
                                TestBidModifiers::createDefaultGeoAdjustment,
                        (Function<BidModifierRegionalAdjustment, BidModifierGeo>)
                                (adjustments) -> createEmptyGeoModifier()
                                        .withRegionalAdjustments(singletonList(adjustments)),
                        (Function<BidModifier, Integer>) (item) ->
                                ((BidModifierGeo) item).getRegionalAdjustments().get(0).getPercent()
                },
                {
                        "videoAdjustments",
                        (Supplier<BidModifierVideoAdjustment>) TestBidModifiers::createDefaultVideoAdjustment,
                        (Function<BidModifierVideoAdjustment, BidModifierVideo>)
                                (adjustment) -> createEmptyVideoModifier()
                                        .withVideoAdjustment(adjustment),
                        (Function<BidModifier, Integer>) (item) ->
                                ((BidModifierVideo) item).getVideoAdjustment().getPercent()
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
    public void setDifferentModifierTest() {
        ModelChanges<BidModifierAdjustment> modelChanges = new ModelChanges<>(bmId,
                bidModifierAdjustment.getClass()).castModelUp(BidModifierAdjustment.class);
        modelChanges.process(NEW_PERCENT, BidModifierAdjustment.PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());

        assertEquals(NEW_PERCENT, getPercent.apply(items.get(0)));
    }
}

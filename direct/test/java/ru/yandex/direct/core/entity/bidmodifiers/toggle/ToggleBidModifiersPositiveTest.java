package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultVideoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyVideoModifier;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка позитивных сценариев отключения наборов корректировок")
public class ToggleBidModifiersPositiveTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Parameterized.Parameter()
    public String adjustmentFieldName;

    @Parameterized.Parameter(1)
    public Supplier<BidModifier> bidModifier;

    private static long retCondId;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private List<UntypedBidModifier> items;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Mobile", (Supplier<BidModifier>) () -> createEmptyMobileModifier()
                        .withMobileAdjustment(createDefaultMobileAdjustment())},
                {"Demographic", (Supplier<BidModifier>) () -> createEmptyDemographicsModifier()
                        .withDemographicsAdjustments(createDefaultDemographicsAdjustments())},
                {"Retargeting", (Supplier<BidModifier>) () -> createEmptyRetargetingModifier()
                        .withRetargetingAdjustments(createDefaultRetargetingAdjustments(retCondId))},
                {"Regional", (Supplier<BidModifier>) () -> createEmptyGeoModifier()
                        .withRegionalAdjustments(createDefaultGeoAdjustments())},
                {"Video", (Supplier<BidModifier>) () -> createEmptyVideoModifier()
                        .withVideoAdjustment(createDefaultVideoAdjustment())},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        // Создаём кампанию и группу
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();

        //Создаем условие ретаргетинга
        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = retCondition.getRetConditionId();

        //Добавляем корректировку
        BidModifier bidModifier = this.bidModifier.get().withCampaignId(campaignInfo.getCampaignId());
        bidModifierService.add(singletonList(bidModifier),
                campaignInfo.getClientId(), campaignInfo.getUid());

        items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(campaignInfo.getCampaignId())
                .withType(bidModifier.getType())
                .withEnabled(false));
    }

    @Test
    public void toggleDifferentModifierTest() {
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(items, campaignInfo.getClientId(), campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());
        assertTrue("Флаг активации набора корректировок изменен", !items.get(0).getEnabled());
    }
}

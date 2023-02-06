package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.Arrays;
import java.util.Collection;
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
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds.GeneralDefects.ADJUSTMENT_SET_NOT_FOUND;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка сценариев отключения наборов корректировок для групп/кампаний, в которых отсутствуют наборы корректировок")
public class ToggleBidModifiersNoBidModifierTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AdGroupSteps adGroupSteps;
    @Autowired
    private BidModifierService bidModifierService;

    @Parameterized.Parameter()
    public String adjustmentFieldName;

    @Parameterized.Parameter(1)
    public Supplier<Long> idSupplier;

    @Parameterized.Parameter(2)
    public Function<Long, BidModifier> bidModifierFactory;

    private static Long adGroupId;
    private static Long campaignId;
    private AdGroupInfo adGroupInfo;
    private CampaignInfo campaignInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Demographic for adGroup", (Supplier<Long>) () -> adGroupId,
                        (Function<Long, BidModifier>) (id) -> new UntypedBidModifier().withAdGroupId(id)
                                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)},
                {"Retargeting for adGroup", (Supplier<Long>) () -> adGroupId,
                        (Function<Long, BidModifier>) (id) -> new UntypedBidModifier().withAdGroupId(id)
                                .withType(BidModifierType.RETARGETING_MULTIPLIER)},
                {"Demographic for campaign", (Supplier<Long>) () -> campaignId,
                        (Function<Long, BidModifier>) (id) -> new UntypedBidModifier().withCampaignId(id)
                                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)},
                {"Retargeting for campaign", (Supplier<Long>) () -> campaignId,
                        (Function<Long, BidModifier>) (id) -> new UntypedBidModifier().withCampaignId(id)
                                .withType(BidModifierType.RETARGETING_MULTIPLIER)},
                {"Geo for campaign", (Supplier<Long>) () -> campaignId,
                        (Function<Long, BidModifier>) (id) -> new UntypedBidModifier().withCampaignId(id)
                                .withType(BidModifierType.GEO_MULTIPLIER)},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();

        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void bidModifiersTypeTest() {
        UntypedBidModifier bidModifier =
                (UntypedBidModifier) bidModifierFactory.apply(idSupplier.get())
                        .withEnabled(false);
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(singletonList(bidModifier), campaignInfo.getClientId(),
                        campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), ADJUSTMENT_SET_NOT_FOUND)));
    }
}

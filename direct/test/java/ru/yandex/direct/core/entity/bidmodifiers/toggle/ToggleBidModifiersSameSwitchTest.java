package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
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
import static junit.framework.TestCase.assertTrue;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;

@CoreTest
@RunWith(Parameterized.class)
@Description("Переключение активности набора корректировок ставок в ранее установленное значение")
public class ToggleBidModifiersSameSwitchTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AdGroupSteps adGroupSteps;

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;

    @Parameterized.Parameter(value = 0)
    public Boolean enabled;

    @Autowired
    private BidModifierService bidModifierService;

    private List<UntypedBidModifier> items;
    private BidModifierDemographics bidModifier;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {false}, {true}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        // Создаём кампанию и группу
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();

        //Добавляем корректировку
        bidModifier =
                new BidModifierDemographics().withEnabled(true).withAdGroupId(adGroupInfo.getAdGroupId()).withType(
                        BidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withDemographicsAdjustments(createDefaultDemographicsAdjustments());
        bidModifierService.add(singletonList(bidModifier),
                campaignInfo.getClientId(), campaignInfo.getUid());

        items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withType(bidModifier.getType())
                .withEnabled(enabled));
    }

    @Test
    @Description("Переключение активности набора корректировок в ранее установленное значение")
    public void sameSwitchTest() {
        //Первоначально корректировка включена, поэтому выключаем ее два раза
        if (!enabled) {
            bidModifierService.toggle(items, campaignInfo.getClientId(), campaignInfo.getUid());
        }
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(items, campaignInfo.getClientId(), campaignInfo.getUid());
        assertTrue(result.getResult().get(0).isSuccessful());

        //Получаем корректировки по кампании для проверки
        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());
        // Проверяем что корректировка в нужном состоянии
        SoftAssertions.assertSoftly(softly -> {
            for (BidModifier item : items) {
                softly.assertThat(item.getEnabled()).isEqualTo(enabled);
            }
        });
    }
}

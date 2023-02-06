package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.NIZHNY_NOVGOROD_OBLAST_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка позитивных сценариев активации множественных наборов корректировок")
public class ToggleBidModifiersPositiveMultipleTest {
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private BidModifierService bidModifierService;

    private BidModifier twoDemographics;
    private BidModifier twoRegionals;
    private Long campaignId;
    private ClientId clientId;
    private Long uid;

    private CampaignInfo campaignInfo;

    @Before
    public void before() throws Exception {
        // Создаём кампанию и группу
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();
        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId();
        uid = campaignInfo.getUid();

        //Создаем корректировки
        twoDemographics = new BidModifierDemographics().withEnabled(true).withCampaignId(campaignId).withType(
                BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withDemographicsAdjustments(asList(
                        new BidModifierDemographicsAdjustment().withAge(AgeType._25_34).withPercent(DEFAULT_PERCENT),
                        new BidModifierDemographicsAdjustment().withAge(AgeType._35_44).withPercent(DEFAULT_PERCENT)
                ));

        twoRegionals = new BidModifierGeo().withEnabled(true).withCampaignId(campaignId)
                .withType(BidModifierType.GEO_MULTIPLIER).withRegionalAdjustments(
                        asList(
                                new BidModifierRegionalAdjustment().withRegionId(MOSCOW_REGION_ID)
                                        .withHidden(false).withPercent(DEFAULT_PERCENT),
                                new BidModifierRegionalAdjustment().withRegionId(NIZHNY_NOVGOROD_OBLAST_REGION_ID)
                                        .withHidden(false).withPercent(DEFAULT_PERCENT)
                        ));
    }

    private void test(List<BidModifier> bidModifiers) {
        //Добавляем корректировки
        bidModifierService.add(bidModifiers, clientId, uid);

        // Создаем модели изменений
        List<UntypedBidModifier> demographicItems = new ArrayList<>();
        for (BidModifier bidModifier : bidModifiers) {
            demographicItems.add((UntypedBidModifier) new UntypedBidModifier()
                    .withCampaignId(campaignInfo.getCampaignId())
                    .withType(bidModifier.getType())
                    .withEnabled(false));
        }

        // Изменяем корректировки
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(demographicItems, campaignInfo.getClientId(),
                        campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        //Получаем корректировки по кампании для проверки
        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());

        // Проверяем что корректировка выключена
        SoftAssertions.assertSoftly(softly -> {
            for (BidModifier item : items) {
                softly.assertThat(item.getEnabled()).isEqualTo(false);
            }
        });
    }

    @Test
    @Description("Модификация нескольких демографических корректировок ставок")
    public void toggleBidModifiersDemographicMultipleTest() {
        List<BidModifier> items = singletonList(twoDemographics.withCampaignId(campaignId));
        test(items);
    }

    @Test
    @Description("Модификация нескольких корректировок ставок по региону")
    public void toggleBidModifiersRegionalTypeTest() {
        List<BidModifier> items = singletonList(twoRegionals.withCampaignId(campaignId));
        test(items);
    }
}

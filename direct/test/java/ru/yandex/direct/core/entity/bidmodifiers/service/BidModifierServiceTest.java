package ru.yandex.direct.core.entity.bidmodifiers.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@CoreTest
@RunWith(SpringRunner.class)
public class BidModifierServiceTest {
    @Autowired
    private Steps steps;
    @Autowired
    private BidModifierService bidModifierService;

    @Test
    public void zeroAllBannerTypePercentAndThenAllInventoryPercentToZero_NotPass() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmBannerCampaign();
        MassResult<List<Long>> addResult1 = bidModifierService.add(singletonList(new BidModifierBannerType()
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                .withCampaignId(campaign.getCampaignId())
                .withBannerTypeAdjustments(asList(new BidModifierBannerTypeAdjustment()
                                .withBannerType(BannerType.CPM_BANNER)
                                .withPercent(0),
                        new BidModifierBannerTypeAdjustment()
                                .withBannerType(BannerType.CPM_OUTDOOR)
                                .withPercent(0)))
                .withEnabled(true)), campaign.getClientId(), campaign.getUid());
        assumeThat("обнуляем корректировки только по типу баннера - ок", addResult1, isFullySuccessful());

        MassResult<List<Long>> addResult2 = bidModifierService.add(singletonList(new BidModifierInventory()
                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                .withCampaignId(campaign.getCampaignId())
                .withInventoryAdjustments(asList(new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INPAGE)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INSTREAM_WEB)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INTERSTITIAL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INAPP)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INBANNER)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.REWARDED)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.PREROLL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.MIDROLL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.POSTROLL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.PAUSEROLL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.OVERLAY)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.POSTROLL_OVERLAY)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.POSTROLL_WRAPPER)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INROLL_OVERLAY)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INROLL)
                                .withPercent(0),
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.FULLSCREEN)
                                .withPercent(0)
                ))
                .withEnabled(true)), campaign.getClientId(), campaign.getUid());
        assertThat("все обнулили, должна быть ошибка валидации",
                addResult2.getValidationResult(), hasDefectDefinitionWith(
                        validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }
}

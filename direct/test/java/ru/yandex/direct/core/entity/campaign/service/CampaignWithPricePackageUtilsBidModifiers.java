package ru.yandex.direct.core.entity.campaign.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithPricePackageUtilsBidModifiers {
    @Autowired
    protected Steps steps;

    private PricePackage pricePackage;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        pricePackage = approvedPricePackage().withId(1L);
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
    }
    @Test
    public void emptyBidModifiers() {
        List<InventoryType> result = getResultInventoryTypes(emptyList());
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.INAPP,
                InventoryType.INBANNER,
                InventoryType.INPAGE,
                InventoryType.INSTREAM_WEB,
                InventoryType.REWARDED);
    }

    @Test
    public void wideTypesOnly() {
        List<InventoryType> result = getResultInventoryTypes(List.of(InventoryType.INAPP, InventoryType.INBANNER));
        assertThat(result).containsExactlyInAnyOrder(InventoryType.INPAGE,
                InventoryType.INSTREAM_WEB,
                InventoryType.REWARDED);
    }

    @Test
    public void oneNarrowType() {
        List<InventoryType> result = getResultInventoryTypes(List.of(InventoryType.MIDROLL, InventoryType.INBANNER));
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.INAPP,
                InventoryType.INPAGE,
                InventoryType.REWARDED,
                InventoryType.PREROLL,
                InventoryType.PAUSEROLL,
                InventoryType.POSTROLL,
                InventoryType.INROLL,
                InventoryType.INROLL_OVERLAY,
                InventoryType.OVERLAY,
                InventoryType.POSTROLL_OVERLAY,
                InventoryType.POSTROLL_WRAPPER);
    }

    @Test
    public void oneNarrowInTwoWideTypes() {
        List<InventoryType> result = getResultInventoryTypes(List.of(InventoryType.MIDROLL, InventoryType.FULLSCREEN));
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.INPAGE,
                InventoryType.REWARDED,
                InventoryType.INBANNER,
                InventoryType.PREROLL,
                InventoryType.PAUSEROLL,
                InventoryType.POSTROLL,
                InventoryType.INROLL,
                InventoryType.INROLL_OVERLAY,
                InventoryType.OVERLAY,
                InventoryType.POSTROLL_OVERLAY,
                InventoryType.POSTROLL_WRAPPER,
                InventoryType.INTERSTITIAL);
    }

    @Test
    public void allNarrowsInOneWideType() {
        List<InventoryType> result = getResultInventoryTypes(List.of(
                InventoryType.PREROLL,
                InventoryType.PAUSEROLL,
                InventoryType.POSTROLL,
                InventoryType.MIDROLL,
                InventoryType.INROLL,
                InventoryType.INROLL_OVERLAY,
                InventoryType.OVERLAY,
                InventoryType.POSTROLL_OVERLAY,
                InventoryType.POSTROLL_WRAPPER));
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.INPAGE,
                InventoryType.REWARDED,
                InventoryType.INBANNER,
                InventoryType.INAPP);
    }

    @Test
    public void allNarrowsInOneWideAndParentWideType() {
        List<InventoryType> result = getResultInventoryTypes(List.of(
                InventoryType.INTERSTITIAL,
                InventoryType.FULLSCREEN,
                InventoryType.INAPP));
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.INPAGE,
                InventoryType.REWARDED,
                InventoryType.INBANNER,
                InventoryType.INSTREAM_WEB);
    }

    @Test
    public void allNarrowsInOneWideAndParentAndAnotherWideType() {
        List<InventoryType> result = getResultInventoryTypes(List.of(
                InventoryType.INTERSTITIAL,
                InventoryType.FULLSCREEN,
                InventoryType.INAPP,
                InventoryType.INPAGE));
        assertThat(result).containsExactlyInAnyOrder(
                InventoryType.REWARDED,
                InventoryType.INBANNER,
                InventoryType.INSTREAM_WEB);
    }

    private List<InventoryType> getResultInventoryTypes(List<InventoryType> initInventoryTypes) {
        pricePackage.withBidModifiers(singletonList(createBidModifierByInventoryTypeForPackage(initInventoryTypes)));
        CpmPriceCampaign campaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withBidModifiers(singletonList(createBidModifierByInventoryTypeForCampaign(initInventoryTypes)));

        var result = CampaignWithPricePackageUtils.enrichBidModifierInventory(
                pricePackage, campaign).get(0);
        return mapList(((BidModifierInventory)result).getInventoryAdjustments(),
                BidModifierInventoryAdjustment::getInventoryType);
    }

    private BidModifier createBidModifierByInventoryTypeForPackage(List<InventoryType> inventoryTypes) {
        var adjs = mapList(inventoryTypes,
                inventoryType -> new BidModifierInventoryAdjustment()
                        .withInventoryType(inventoryType).withIsRequiredInPricePackage(true));
        return new BidModifierInventory().withType(BidModifierType.INVENTORY_MULTIPLIER).withInventoryAdjustments(adjs);
    }
    private BidModifier createBidModifierByInventoryTypeForCampaign(List<InventoryType> inventoryTypes) {
        var adjs = mapList(inventoryTypes,
                inventoryType -> new BidModifierInventoryAdjustment()
                        .withInventoryType(inventoryType).withPercent(0));
        return new BidModifierInventory().withType(BidModifierType.INVENTORY_MULTIPLIER).withInventoryAdjustments(adjs);
    }
}

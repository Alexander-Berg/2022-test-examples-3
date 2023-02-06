package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;

@DirectWebTest
@RunWith(Parameterized.class)
public class CpmAdGroupControllerAddFrontpagePricesTest extends CpmAdGroupControllerAddFrontpagePricesTestBase {

    @Parameterized.Parameters(name = "{4}")
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE), 20., "0", false, "empty targeting, no errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE), 1.2, String.valueOf(RUSSIA_REGION_ID), false,
                                "Russia targeting desktop, low price for moscow region, no errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE_MOBILE), 1.2, String.valueOf(RUSSIA_REGION_ID),
                                false, "Russia targeting mobile, low price for moscow region, no errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE), .8, String.valueOf(RUSSIA_REGION_ID), true,
                                "Russia targeting desktop, low price for Russia, error"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE_MOBILE), .8, String.valueOf(RUSSIA_REGION_ID),
                                false, "Russia targeting mobile, sufficient price for Russia, no errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE), 1.,
                                String.valueOf(GLOBAL_REGION_ID) + "," + String.valueOf(-RUSSIA_REGION_ID) +
                                        "," + String.valueOf(SAINT_PETERSBURG_REGION_ID),
                                true, "StPetersburg targeting, LenOblast low price, error"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE_MOBILE), 1., String.valueOf(RUSSIA_REGION_ID) + "," +
                                String.valueOf(-MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID) + "," + String.valueOf(MOSCOW_REGION_ID),
                                false, "Russia and Moscow minus MosRegion targeting mobile, no errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE), 1.,
                                String.valueOf(-MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID) + "," + String.valueOf(MOSCOW_REGION_ID),
                                true, "Russia and Moscow targeting desktop, errors"},
                        {singletonList(FrontpageCampaignShowType.FRONTPAGE_MOBILE), 2.,
                                String.valueOf(SAINT_PETERSBURG_REGION_ID) + "," + String.valueOf(MOSCOW_REGION_ID),
                                false, "StPetersburg and Moscow targeting desktop, low price for Moscow only, no errors"},
                        {ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE),
                                1.2, String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                                true, "Moscow region targeting mobile and desktop, mutual low price, error"},
                        {ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE),
                                1.0, String.valueOf(SAINT_PETERSBURG_REGION_ID),
                                true, "StPetersburg targeting mobile and desktop, mutual low price, error"},
                        {ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE),
                                1.3, String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                                false, "Moscow region targeting mobile and desktop, desktop low price, no error"},
                        {ImmutableList.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE),
                                1.1, String.valueOf(SAINT_PETERSBURG_REGION_ID),
                                false, "StPetersburg targeting mobile and desktop, desktop price, no error"},

                }
        );
    }

    @Test
    public void testAdd() {
        super.testPriceWithGeoTargetingAdd();
    }

    @Test
    public void testUpdate() {
        super.testPriceWithGeoTargetingUpdate();
    }
}

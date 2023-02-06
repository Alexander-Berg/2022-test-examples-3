package ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.currencies.CurrencyEur;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmYndxFrontpageAdGroupPriceRestrictionsTest {
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void testMergeLessStrict_DifferentRegions_Successful() {
        var restrictions1 =
                new CpmYndxFrontpageAdGroupPriceRestrictions(new BigDecimal("10"), new BigDecimal("40"))
                        .withRegionsById(geoTree.getRegions())
                        .withClientCurrency(CurrencyRub.getInstance())
                        .withMinPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("20"))))
                        .withMaxPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("30"))));

        var restrictions2 =
                new CpmYndxFrontpageAdGroupPriceRestrictions(new BigDecimal("5"), new BigDecimal("35"))
                        .withRegionsById(geoTree.getRegions())
                        .withClientCurrency(CurrencyRub.getInstance())
                        .withMinPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("20"),
                                        Region.MOSCOW_REGION_ID, new BigDecimal("120"))))
                        .withMaxPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("30"),
                                        Region.MOSCOW_REGION_ID, new BigDecimal("130"))));

        CpmYndxFrontpageAdGroupPriceRestrictions result =
                CpmYndxFrontpageAdGroupPriceRestrictions.mergeLessStrict(restrictions1, restrictions2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getRegionsById())
                    .isEqualTo(geoTree.getRegions());
            softly.assertThat(result.getCpmYndxFrontpageMinPrice())
                    .isEqualTo(restrictions2.getCpmYndxFrontpageMinPrice());
            softly.assertThat(result.getCpmYndxFrontpageMaxPrice())
                    .isEqualTo(restrictions1.getCpmYndxFrontpageMaxPrice());
            softly.assertThat(result.getMinPriceByRegion())
                    .isEqualTo(restrictions2.getMinPriceByRegion());
            softly.assertThat(result.getMaxPriceByRegion())
                    .isEqualTo(restrictions2.getMaxPriceByRegion());
        });
    }

    @Test
    public void testMergeLessStrict_DifferentShowTypes_Successful() {
        var restrictions1 = new CpmYndxFrontpageAdGroupPriceRestrictions(CurrencyRub.getInstance())
                        .withMinPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("20"))))
                        .withMaxPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("30"))));

        var restrictions2 = new CpmYndxFrontpageAdGroupPriceRestrictions(CurrencyRub.getInstance())
                        .withMinPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("20"))))
                        .withMaxPriceByRegion(Map.of(FrontpageCampaignShowType.FRONTPAGE,
                                Map.of(Region.RUSSIA_REGION_ID, new BigDecimal("30"))));

        Map<FrontpageCampaignShowType, Map<Long, BigDecimal>> expectedMinPriceByRegion =
                new HashMap<>(restrictions1.getMinPriceByRegion());
        expectedMinPriceByRegion.putAll(restrictions2.getMinPriceByRegion());
        Map<FrontpageCampaignShowType, Map<Long, BigDecimal>> expectedMaxPriceByRegion =
                new HashMap<>(restrictions1.getMaxPriceByRegion());
        expectedMaxPriceByRegion.putAll(restrictions2.getMaxPriceByRegion());

        CpmYndxFrontpageAdGroupPriceRestrictions result =
                CpmYndxFrontpageAdGroupPriceRestrictions.mergeLessStrict(restrictions1, restrictions2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getCpmYndxFrontpageMinPrice())
                    .isEqualTo(CurrencyRub.getInstance().getMinCpmPrice());
            softly.assertThat(result.getCpmYndxFrontpageMaxPrice())
                    .isEqualTo(CurrencyRub.getInstance().getMaxCpmPrice());
            softly.assertThat(result.getMinPriceByRegion())
                    .isEqualTo(expectedMinPriceByRegion);
            softly.assertThat(result.getMaxPriceByRegion())
                    .isEqualTo(expectedMaxPriceByRegion);
        });
    }

    @Test
    public void testMergeLessStrict_DifferentCurrencies_Error() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Restrictions have different client currencies");
        var restrictions1 = new CpmYndxFrontpageAdGroupPriceRestrictions(CurrencyRub.getInstance());
        var restrictions2 = new CpmYndxFrontpageAdGroupPriceRestrictions(CurrencyEur.getInstance());
        CpmYndxFrontpageAdGroupPriceRestrictions.mergeLessStrict(restrictions1, restrictions2);
    }
}

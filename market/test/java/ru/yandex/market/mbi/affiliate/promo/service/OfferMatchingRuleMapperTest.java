package ru.yandex.market.mbi.affiliate.promo.service;

import java.util.List;

import Market.DataCamp.DataCampPromo;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbi.affiliate.promo.TestResourceUtils;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoOffers;
import ru.yandex.market.mbi.affiliate.promo.common.AffiliatePromoException;
import ru.yandex.market.mbi.affiliate.promo.regions.Region;
import ru.yandex.market.mbi.affiliate.promo.regions.RegionService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OfferMatchingRuleMapperTest {

    private RegionService regionService;
    private OfferMatchingRuleMapper offerMatchingRuleMapper;

    @Before
    public void setUp() {
        regionService = mock(RegionService.class);
        when(regionService.getAvailableRegions()).thenReturn(List.of(
                new Region(1, "Регион1"),
                new Region(2, "Регион2"),
                new Region(1099, "Регион1099"),
                new Region(1110, "Регион1100"),
                new Region(2000, "Регион2000")
        ));
        when(regionService.getStandardRegions()).thenReturn(List.of(new Region(2000, "Регион2000")));
        offerMatchingRuleMapper = new OfferMatchingRuleMapper(regionService);
    }

    @Test
    public void testMatchingCategory() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_category_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_category_output.json");
    }

    @Test
    public void testMatchingBrand() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_brand_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_brand_output.json");
    }

    @Test
    public void testMatchingCategoryAndBrand() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_category_and_brand_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_category_and_brand_output.json");
    }

    @Test
    public void testMatchingMskus() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_msku_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_msku_output.json");
    }

    @Test
    public void testMatchingSuppliers() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_supplier_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_supplier_output.json");
    }

    @Test
    public void testMatchingRegionsFull() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_output.json");
    }

    @Test
    public void testMatchingRegionsExplicit() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_input_explicit.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_output_explicit.json");
    }

    @Test
    public void testMatchingRegionsStandard() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_input_standard.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_output_standard.json");
    }

    @Test
    public void testMatchingRegionsNoStandardList() throws Exception {
        when(regionService.getStandardRegions()).thenReturn(null);
        var offers = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_input_standard.json",
                PromoOffers.class);
        assertThrows(AffiliatePromoException.class, () -> {
            offerMatchingRuleMapper.toOfferMatchingRules(offers);
        });
    }

    @Test
    public void testMatchingRegionsBadInput() throws Exception {
        var offers = TestResourceUtils.loadDto(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_region_input_bad_explicit.json",
                PromoOffers.class);
        assertThrows(AffiliatePromoException.class, () -> {
            offerMatchingRuleMapper.toOfferMatchingRules(offers);
        });
    }

    @Test
    public void testMatchingAllWithExceptions() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_all_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_all_output.json");
    }

    @Test
    public void testComplexMatching() throws Exception {
        testMatching(
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_complex_input.json",
                "ru/yandex/market/mbi/affiliate/promo/service/offer_matching_complex_output.json");
    }

    private void testMatching(String inputFile, String outputFile) throws Exception {
        var offers = TestResourceUtils.loadDto(
                inputFile,
                PromoOffers.class);

        DataCampPromo.PromoConstraints.Builder builder = DataCampPromo.PromoConstraints.newBuilder();
        TestResourceUtils.loadProto(outputFile, builder);

        var result = offerMatchingRuleMapper.toOfferMatchingRules(offers);
        assertThat(result, is(builder.getOffersMatchingRulesList()));
    }
}
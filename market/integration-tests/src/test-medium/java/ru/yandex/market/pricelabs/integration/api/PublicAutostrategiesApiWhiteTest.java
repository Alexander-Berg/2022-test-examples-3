package ru.yandex.market.pricelabs.integration.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.MockMvcProxyHttpException;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyDrr;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.model.BlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.assertThrowsWithMessage;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;

public class PublicAutostrategiesApiWhiteTest extends AbstractAutostrategiesApiTest {

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.white, CoreTestUtils.emptyRunnable());
        // перезаливаем офферы, т.к. в некоторых тестах мы добавляем новые.
        var offers = List.of(offersSampleShop1(), offersSampleShop2NoAutostrategy())
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        executors.offers().insert(offers);
        executors.blueBidsRecommender().insert(new ArrayList<>(getBlueBidsRecommendations().values()));
    }

    @AfterEach
    void afterEach() {
        executors.blueBidsRecommender().clearTargetTable();
    }

    @Test
    void testEstimateCpaDrrNoFilter() {
        assertEquals(new AutostrategyDrr(),
                checkResponse(getPublicApi().autostrategyEstimateCpaDrrPost(SHOP1, null, getTarget(), null, null)));
    }

    @Test
    void testEstimateCpaDrrFilterHasNoOffers() {
        double expectedDrr = 0;

        // фильтр пуст, найдем все офферы и посчитаем ожидаемую ставку
        Map<Integer, BlueBidsRecommendation> blueBidsRecommendations = getBlueBidsRecommendations();
        double defaultDrr = blueBidsRecommendations.get(CATEGORY_DEFAULT).getCpa_rec_bid();
        List<Offer> offers = offersSampleShop1();
        for (Offer o : offers) {
            expectedDrr += blueBidsRecommendations.containsKey((int) o.getCategory_id())
                    ? blueBidsRecommendations.get((int) o.getCategory_id()).getCpa_rec_bid()
                    : defaultDrr;
        }

        expectedDrr /= offers.size();

        AutostrategyDrr actual = checkResponse(
                getPublicApi().autostrategyEstimateCpaDrrPost(
                        SHOP1,
                        autostrategyFilter(List.of()),
                        getTarget(),
                        null,
                        null
                )
        );

        Assertions.assertThat(actual.getRecommendations())
                .isNull();
        assertEquals(toDrr(expectedDrr), actual.getDrr(), 0.01);
    }

    @Test
    void testEstimateCpaDrrOffersFromSameCategory() {
        // берем офферы из одной категории
        List<Offer> offersInCategory = offersSampleShop1GroupedByCategories().get(CATEGORY1);
        List<String> offerIds = List.of(
                offersInCategory.get(0).getOffer_id(),
                offersInCategory.get(1).getOffer_id()
        );

        double category1Drr = getBlueBidsRecommendations().get(CATEGORY1).getCpa_rec_bid();
        double expectedDrr = (category1Drr * offerIds.size()) / offerIds.size();

        AutostrategyDrr actual = checkResponse(getPublicApi()
                .autostrategyEstimateCpaDrrPost(SHOP1, autostrategyFilter(offerIds), getTarget(), null, null));
        assertEquals(toDrr(expectedDrr), actual.getDrr());
    }

    @Test
    void testEstimateCpaDrrOffersFromDifferentCategories() {
        // берем офферы из разных категорий
        Map<Integer, List<Offer>> offersByCategories = offersSampleShop1GroupedByCategories();
        List<String> offerIdsCategory1 = List.of(
                offersByCategories.get(CATEGORY1).get(0).getOffer_id(),
                offersByCategories.get(CATEGORY1).get(1).getOffer_id()
        );
        List<String> offerIdsCategory2 = List.of(
                offersByCategories.get(CATEGORY2).get(0).getOffer_id()
        );

        List<String> offerIds = new ArrayList<>();
        offerIds.addAll(offerIdsCategory1);
        offerIds.addAll(offerIdsCategory2);

        Map<Integer, BlueBidsRecommendation> blueBidsRecommendations = getBlueBidsRecommendations();
        double recDr1 = blueBidsRecommendations.get(CATEGORY1).getCpa_rec_bid();
        double recDr2 = blueBidsRecommendations.get(CATEGORY2).getCpa_rec_bid();
        double expectedDrr = (recDr1 * offerIdsCategory1.size() + recDr2 * offerIdsCategory2.size()) / offerIds.size();

        AutostrategyDrr actual = checkResponse(getPublicApi()
                .autostrategyEstimateCpaDrrPost(SHOP1, autostrategyFilter(offerIds), getTarget(), null, null));
        assertEquals(toDrr(expectedDrr), actual.getDrr(), 0.01);
    }

    @Test
    void testEstimateCpaDrrOffersFromDifferentCategoriesOneOfWhichDoesntExist() {
        // берем офферы из разных категорий, одна из них не найдена, но вместо нее используется дефолтный cpa_rec_bid
        Map<Integer, List<Offer>> offersByCategories = offersSampleShop1GroupedByCategories();
        List<String> offerIds = List.of(
                offersByCategories.get(CATEGORY1).get(0).getOffer_id(),
                offersByCategories.get(CATEGORY3).get(0).getOffer_id() // такой категории нет
        );
        Map<Integer, BlueBidsRecommendation> blueBidsRecommendations = getBlueBidsRecommendations();
        double recDr1 = blueBidsRecommendations.get(CATEGORY1).getCpa_rec_bid();
        double recDr2 = blueBidsRecommendations.get(CATEGORY_DEFAULT).getCpa_rec_bid();
        double expectedDrr = (recDr1 + recDr2) / offerIds.size();

        AutostrategyDrr actual = checkResponse(getPublicApi()
                .autostrategyEstimateCpaDrrPost(SHOP1, autostrategyFilter(offerIds), getTarget(), null, null));
        assertEquals(toDrr(expectedDrr), actual.getDrr());
    }

    @Test
    public void testEstimateCpaDrrByParameters() {
        List<Offer> offersToFilter = getOffersToFilter();
        executors.offers().insert(offersToFilter);

        AutostrategyFilter filter = new AutostrategyFilter()
                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                .simple(new AutostrategyFilterSimple()
                        .vendors(List.of("Facebook", "Yandex", "Microsoft"))
                        .categories(List.of(-1L))
                        .priceFrom(1L)
                        .priceTo(1002L)
                );

        Map<Integer, BlueBidsRecommendation> blueBidsRecommendations = getBlueBidsRecommendations();
        double recDr1 = blueBidsRecommendations.get(CATEGORY1).getCpa_rec_bid(); //Samsung
        double recDr2 = blueBidsRecommendations.get(CATEGORY_DEFAULT).getCpa_rec_bid(); //Google
        double expectedDrr = (recDr1 + recDr2) / 2; //Будет только два товара, т.к. у Bosch price = 1006

        AutostrategyDrr actual = checkResponse(getPublicApi()
                .autostrategyEstimateCpaDrrPost(SHOP1, filter, getTarget(), null, null));
        assertEquals(toDrr(expectedDrr), actual.getDrr());
    }

    @Test
    void testLookupMaxBid() {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                        getPublicApi().autostrategyLookupMaxBidPost(SHOP1, null, getTarget()),
                "Unsupported autoStrategyTarget: white. Expect: [vendorBlue]");
    }

    @Test
    void testLookupMaxBids() {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                        getPublicApi().autostrategyLookupMaxBidsPost(SHOP1, null, getTarget()),
                "Unsupported autoStrategyTarget: white. Expect: [vendorBlue]");
    }

    private List<Offer> getOffersToFilter() {
        boolean sskuOffer = false;

        return List.of(
                offer("44", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name44");
                    o.setPrice(99800);
                    o.setModel_id(MODEL2);
                    o.setApp_autostrategy_id(4);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY1);
                    o.setVendor_name("Yandex");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                }),
                offer("45", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name45");
                    o.setPrice(100600);
                    o.setModel_id(MODEL2);
                    o.setApp_autostrategy_id(4);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY2);
                    o.setVendor_name("Yandex");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                }),
                offer("46", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED2);
                    o.setStatus(Status.ACTIVE);
                    o.setName("name46");
                    o.setPrice(100100);
                    o.setModel_id(MODEL2);
                    o.setApp_autostrategy_id(4);
                    o.setSsku_offer(sskuOffer);
                    o.setCategory_id(CATEGORY3);
                    o.setVendor_name("Microsoft");
                    o.setVendor_name_lower(o.getVendor_name().toLowerCase());
                })
        );
    }

}

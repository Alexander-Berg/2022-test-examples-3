package ru.yandex.market.pricelabs.integration.api;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.api.api.PublicApi;
import ru.yandex.market.pricelabs.api.api.PublicApiInterfaces;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchShopOffersResponse;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicTest;

import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.FEED_ID;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.FEED_ID_2;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_10;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

public class PublicApiSearchShopOffers extends AbstractApiTests {

    @Autowired
    private PublicApi publicApiBean;
    private PublicApiInterfaces publicApi;

    @BeforeEach
    void init() {
        publicApi = buildProxy(PublicApiInterfaces.class, publicApiBean);
        super.init();

        // добавим второй фид
        testControls.saveShop(shop(SHOP_ID, s -> {
            s.setFeeds(Set.of(
                    (long) FEED_ID,
                    (long) FEED_ID_2
            ));
        }));

        // и оффер с тем же offer_id в него
        List<Offer> existingOffers = executors.offersGen().selectTargetRows();
        existingOffers.add(getOfferFromAnotherFeed());
        executors.offersGen().insert(existingOffers);
    }

    private Offer getOfferFromAnotherFeed() {
        var offers = new OffersProcessorBasicTest().readTargetList();
        Offer fromFeed1 = offers.get(0);
        Offer fromAnotherFeed = new Offer();
        fromAnotherFeed.setShop_id(fromFeed1.getShop_id());
        fromAnotherFeed.setFeed_id(FEED_ID_2);
        fromAnotherFeed.setOffer_id(fromFeed1.getOffer_id());
        fromAnotherFeed.setName(fromFeed1.getName());
        fromAnotherFeed.setOffer_id_index(fromFeed1.getOffer_id_index());
        fromAnotherFeed.setName_index(fromFeed1.getName_index());
        fromAnotherFeed.setStatus(Status.ACTIVE);
        return fromAnotherFeed;
    }

    @Test
    void searchShopOffersDefaultPost() {
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID, null, null, null, new Filter().toJsonString());
        checkResponse(ret);
        List<SearchShopOffersResponse> body = ret.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(9, body.size());

        // проверяем что нет повторений offer_id
        Set<String> ids = body.stream().map(SearchShopOffersResponse::getOfferId).collect(Collectors.toSet());
        Assertions.assertEquals(ids.size(), body.size());
    }

    @Test
    void searchShopOffersWhitePost() {
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID, AutostrategyTarget.white.name(), null, null, new Filter().toJsonString());
        checkResponse(ret);
        List<SearchShopOffersResponse> body = ret.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertFalse(Objects.requireNonNull(ret.getBody()).isEmpty());
        // проверяем что нет повторений offer_id
        Set<String> ids = body.stream().map(SearchShopOffersResponse::getOfferId).collect(Collectors.toSet());
        Assertions.assertEquals(ids.size(), body.size());
    }

    @Test
    void searchShopOffersWhiteWithQueryPost() {
        Filter filter = new Filter();
        //Ищем "Установка монтаж кассетных кондиционеров 2.1-3.0 кВт", который есть в обоих фидах
        filter.setQuery("2.1-3.0 кВт");
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID, null, null, null, filter.toJsonString());
        checkResponse(ret);
        Assertions.assertEquals(1, Objects.requireNonNull(ret.getBody()).size());
    }

    @Test
    void searchShopOffersBluePost() {
        Filter filter = new Filter();
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID_10, AutostrategyTarget.blue.name(), null, null, filter.toJsonString());
        checkResponse(ret);
        Assertions.assertFalse(Objects.requireNonNull(ret.getBody()).isEmpty());
        Assertions.assertEquals(11, ret.getBody().size());
        Assertions.assertTrue(Objects.requireNonNull(ret.getBody()).get(0).getOfferName().contains("_blue"));
    }

    @Test
    void searchShopOffersBlueWithWhiteShopPost() {
        Filter filter = new Filter();
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID, AutostrategyTarget.blue.name(), null, null, filter.toJsonString());
        checkResponse(ret);
        Assertions.assertTrue(Objects.requireNonNull(ret.getBody()).isEmpty());
    }

    @Test
    void searchShopOffersBlueWithQueryPost() {
        Filter filter = new Filter();
        filter.setQuery("18й класс");
        var ret = publicApi.searchShopOffersPost(
                SHOP_ID_10, AutostrategyTarget.blue.name(), null, null, filter.toJsonString());
        checkResponse(ret);
        Assertions.assertEquals(1, Objects.requireNonNull(ret.getBody()).size());
        Assertions.assertEquals("Установка, монтаж кондиционера, сплит системы до 5,5 КВт(18й класс)_blue",
                ret.getBody().get(0).getOfferName());
    }


}

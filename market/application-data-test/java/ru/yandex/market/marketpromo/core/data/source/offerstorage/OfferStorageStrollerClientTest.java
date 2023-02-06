package ru.yandex.market.marketpromo.core.data.source.offerstorage;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.properties.OfferStorageProperties;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.utils.OfferStorageTestHelper;
import ru.yandex.market.marketpromo.model.DatacampOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;

public class OfferStorageStrollerClientTest extends ServiceTestBase {

    private static final List<String> OFFER_IDS = List.of(
            "some offer"
    );

    @Autowired
    private OfferStorageStrollerClient storageStrollerClient;
    @Autowired
    private OfferStorageTestHelper offerStorageTestHelper;
    @Autowired
    private OfferStorageProperties offerStorageProperties;

    @BeforeEach
    void configure() {
        offerStorageTestHelper.mockStrollerSearchOffersByShopResponse(
                offerStorageProperties.getShopId(), offerStorageProperties.getWarehouseId(), OFFER_IDS);
    }

    @Test
    void shouldLoadOffers() {
        List<DatacampOffer> offerList = storageStrollerClient.searchOffersByShop(
                offerStorageProperties.getShopId(),
                offerStorageProperties.getWarehouseId(),
                OFFER_IDS,
                null,
                null
        );
        assertThat(offerList, not(empty()));
        assertThat(offerList, everyItem(allOf(
                hasProperty("shopId"),
                hasProperty("feedId"),
                hasProperty("warehouseId"),
                hasProperty("categoryId"),
                hasProperty("marketSku"),
                hasProperty("shopSku"),
                hasProperty("name"),
                hasProperty("basePrice"),
                hasProperty("price"),
                hasProperty("createdAt"),
                hasProperty("updatedAt")
        )));
    }
}

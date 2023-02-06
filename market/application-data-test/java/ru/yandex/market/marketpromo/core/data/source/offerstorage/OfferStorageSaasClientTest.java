package ru.yandex.market.marketpromo.core.data.source.offerstorage;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.utils.OfferStorageTestHelper;
import ru.yandex.market.marketpromo.model.PagerList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

public class OfferStorageSaasClientTest extends ServiceTestBase {

    private static final List<String> OFFER_IDS = List.of(
            "some offer"
    );

    @Autowired
    private OfferStorageSaasClient storageSaasClient;
    @Autowired
    private OfferStorageTestHelper offerStorageTestHelper;

    @BeforeEach
    void configure() {
        offerStorageTestHelper.mockSaasSearchServiceResponse(OFFER_IDS);
    }

    @Test
    void shouldLoadOffers() {
        PagerList<String> offerList = storageSaasClient.searchOffersBy(SearchRequest.empty());
        assertThat(offerList.getList(), not(empty()));
    }
}

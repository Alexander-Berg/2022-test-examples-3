package ru.yandex.market.marketpromo.core.integration;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.marketpromo.core.data.source.offerstorage.OfferStorageStrollerClient;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.SearchOffersRequest;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.DatacampOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.OFFER_STORAGE_ACTIVE;

@ActiveProfiles(OFFER_STORAGE_ACTIVE)
@TestPropertySource(
        properties = {
                "market.marketpromo.ciface-promo.tvm.clientId=2021562",
                "market.marketpromo.ciface-promo.tvm.logbroker.clientId=2001059",
                "market.marketpromo.ciface-promo.tvm.stroller.clientId=2011472",
                "market.marketpromo.ciface-promo.offerstorage.dataCampUrl=" +
                        "http://datacamp.blue.tst.vs.market.yandex.net",
                "market.marketpromo.ciface-promo.offerstorage.dataCampUrl=" +
                        "http://datacamp.blue.tst.vs.market.yandex.net",
                "client_secret={secret-key}"
        }
)
@Disabled
public class OfferStorageStrollerClientIntegrationTest extends ServiceTestBase {

    @Autowired
    private OfferStorageStrollerClient storageStrollerClient;

    @Test
    void shouldLoadUnitedOffersByFilter() {
        List<DatacampOffer> offerList = storageStrollerClient.searchOffersByBusiness(SearchOffersRequest.builder()
                .shopId(10264169L)
                .businessId(10447296L)
                .shopSkus(Set.of(
                        "00065.00001.4567356735",
                        "00065.00001.uiuiii",
                        "00065.00008.2563456245626"
                ))
                .build());

        assertThat(offerList, hasSize(3));
    }
}

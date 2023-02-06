package ru.yandex.market.loyalty.admin.tms.stroller;

import Market.DataCamp.DataCampPromo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.service.datacamp.DataCampStrollerClient;

import static Market.DataCamp.SyncAPI.SyncGetPromo.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;


public class DataCampStrollerClientTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    @Qualifier("mockedDataCampClient")
    private DataCampStrollerClient client;

    @Test
    public void shouldGetPromo() {
        GetPromoBatchRequest request = GetPromoBatchRequest.newBuilder()
                .addEntries(getIdentifier())
                .build();

        GetPromoBatchResponse response = client.getPromo(
                request
        );

        assertThat(response, notNullValue());
    }

    private DataCampPromo.PromoDescriptionIdentifier getIdentifier() {
        DataCampPromo.PromoDescriptionIdentifier.Builder identifierBuilder = DataCampPromo
                .PromoDescriptionIdentifier
                .newBuilder();
        identifierBuilder.setBusinessId(0);
        identifierBuilder.setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN);
        identifierBuilder.setPromoId("PromoId");
        return identifierBuilder.build();
    }
}

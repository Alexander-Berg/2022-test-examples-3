package ru.yandex.market.global.checkout.datacamp;

import java.util.List;

import Market.DataCamp.SyncAPI.SyncCategory;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.common.datacamp.DataCampClient;

@Disabled
public class DataCampLocalTest extends BaseLocalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @Test
    public void testGetOffers() {
        SyncGetOffer.GetUnitedOffersResponse response = dataCampClient.getAvailableOffers(
                10432691L, 10426272L, List.of("01094666aa4f9b7c15fa", "0448b342d8925938f920")
        );

        Assertions.assertThat(response.getOffersCount()).isEqualTo(2);
    }

    @Test
    public void testGetCategories() {
        SyncCategory.PartnerCategoriesResponse response = dataCampClient.getCategories(10432691L);

        Assertions.assertThat(response.getCategories().getCategoriesCount()).isGreaterThan(0);
    }

}

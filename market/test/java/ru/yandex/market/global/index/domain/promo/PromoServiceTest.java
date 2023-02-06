package ru.yandex.market.global.index.domain.promo;

import java.util.List;

import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseFunctionalTest;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoServiceTest extends BaseFunctionalTest {
    private final PromoService promoService;

    @Test
    public void testFindPromos() {
        DataCampUnitedOffer.UnitedOffer.Builder united = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(Market.DataCamp.DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                            .setBusinessId(10286415)
                            .setOfferId("KATS0013").build()
                    )
                );

        List<String> promos = promoService.getPromos(united.build());
        Assertions.assertThat(promos).containsExactlyInAnyOrder(
                "WELCOME"
        );
    }

    @Test
    public void testNotFindPromos() {
        DataCampUnitedOffer.UnitedOffer.Builder united = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(Market.DataCamp.DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(3501908)
                                .setOfferId("123").build()
                        )
                );

        List<String> promos = promoService.getPromos(united.build());
        Assertions.assertThat(promos).isNull();
    }

}

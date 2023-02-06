package ru.yandex.market.mbo.db;

import com.google.common.base.Strings;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.Arrays;
import java.util.List;

public class OfferStorageServiceWrapperTest {

    @Test
    public void whenNoDescriptionShouldReturnUnchanged() {
        OffersStorage.GenerationDataOffer offer = OffersStorage.GenerationDataOffer.newBuilder()
            .setOffer("no description")
            .build();

        OfferStorageServiceWrapper wrapper = mockOfferStorageService(offer);

        List<OffersStorage.GenerationDataOffer> offersList = wrapper.getOffersByIds(Mockito.any()).getOffersList();
        Assertions.assertThat(offersList).isEqualTo(Arrays.asList(offer));
    }

    @Test
    public void whenSmallDescriptionShouldReturnUnchanged() {
        OffersStorage.GenerationDataOffer offer = OffersStorage.GenerationDataOffer.newBuilder()
            .setOffer("small description")
            .setDescription("less than 1000 symbols")
            .build();

        OfferStorageServiceWrapper wrapper = mockOfferStorageService(offer);

        List<OffersStorage.GenerationDataOffer> offersList = wrapper.getOffersByIds(Mockito.any()).getOffersList();
        Assertions.assertThat(offersList).isEqualTo(Arrays.asList(offer));
    }

    @Test
    public void whenLargeDescriptionShouldCut() {
        OffersStorage.GenerationDataOffer offer = OffersStorage.GenerationDataOffer.newBuilder()
            .setOffer("large description")
            .setDescription(Strings.repeat("more than 1000", OfferStorageServiceWrapper.MAX_STRING_LENGTH))
            .build();

        OfferStorageServiceWrapper wrapper = mockOfferStorageService(offer);

        List<OffersStorage.GenerationDataOffer> offersList = wrapper.getOffersByIds(Mockito.any()).getOffersList();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offersList).hasSize(1);
            OffersStorage.GenerationDataOffer wrappedOffer = offersList.get(0);

            s.assertThat(wrappedOffer.getOffer()).isEqualTo(offer.getOffer());
            s.assertThat(wrappedOffer.getDescription()).hasSize(OfferStorageServiceWrapper.MAX_STRING_LENGTH);
            s.assertThat(wrappedOffer.getDescription()).isSubstringOf(offer.getDescription());
        });
    }

    private OfferStorageServiceWrapper mockOfferStorageService(OffersStorage.GenerationDataOffer offer) {
        OffersStorage.GetOffersResponse response = OffersStorage.GetOffersResponse.newBuilder()
            .addOffers(offer)
            .build();
        OfferStorageService mockService = Mockito.mock(OfferStorageService.class);
        Mockito.when(mockService.getOffersByIds(Mockito.any())).thenReturn(response);
        return new OfferStorageServiceWrapper(mockService);
    }

}

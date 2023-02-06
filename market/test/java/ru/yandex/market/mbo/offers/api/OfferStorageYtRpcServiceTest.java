package ru.yandex.market.mbo.offers.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.mbo.http.OffersStorage.GenerationDataOffer;
import ru.yandex.market.mbo.offers.api.ytrpc.OfferStorageYtRpcServiceImpl;
import ru.yandex.market.mbo.offers.api.ytrpc.OfferStorageYtRpcWrapper;

import java.util.Arrays;
import java.util.List;

public class OfferStorageYtRpcServiceTest {

    private static final String TEST_TITLE = "Test offer title";

    protected OfferStorageYtRpcServiceImpl offerStorageYtRpcService;

    @Before
    public void setUp() throws Exception {
        OfferStorageYtRpcWrapper ytRpcWrapper = Mockito.mock(OfferStorageYtRpcWrapper.class);
        Mockito.when(ytRpcWrapper.getOffersByIds(Mockito.any()))
                .thenAnswer(invocation -> {
                    List<String> offerIds = invocation.getArgument(0);
                    GenerationDataOffer offer = GenerationDataOffer.newBuilder()
                            .setOffer(TEST_TITLE)
                            .setClassifierMagicId(offerIds.get(0))
                            .build();

                    return Arrays.asList(offer.toByteArray());
                });
        this.offerStorageYtRpcService = new OfferStorageYtRpcServiceImpl(ytRpcWrapper);
    }

    @Test
    public void getOffersByIdsTest() {

        List<String> ids = Arrays.asList(
                "fb0e131a44a071697c57d95d8799574b", "fb0e131a44a071697c57d95d87990000"
        );
        OffersStorage.GetOffersRequest request = OffersStorage.GetOffersRequest.newBuilder()
                .addAllClassifierMagicIds(ids)
                .build();

        OffersStorage.GetOffersResponse response = offerStorageYtRpcService.getOffersByIds(request);

        Assert.assertEquals(response.getOffersCount(), 1);

        GenerationDataOffer offer = response.getOffers(0);
        Assert.assertEquals(offer.getClassifierMagicId(), ids.get(0));
        Assert.assertEquals(offer.getOffer(), TEST_TITLE);
    }

}

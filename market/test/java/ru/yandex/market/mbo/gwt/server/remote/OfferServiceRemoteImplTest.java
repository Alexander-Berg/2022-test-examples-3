package ru.yandex.market.mbo.gwt.server.remote;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.mbo.db.OfferService;
import ru.yandex.market.mbo.gwt.client.services.OfferServiceRemote;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferDataMatchType;

import java.util.List;

public class OfferServiceRemoteImplTest {
    private OfferService offersServiceMock;
    private OfferServiceRemote modelOffersServiceRemote;

    @Before
    public void setUp() throws Exception {
        offersServiceMock = Mockito.mock(OfferService.class);
        modelOffersServiceRemote = new OfferServiceRemoteImpl();
        ReflectionTestUtils.setField(modelOffersServiceRemote, "offerService", offersServiceMock);
    }

    @Test
    public void whenGettingMatchedOffersShouldCallInternalService() {
        long testModelId = 1L;
        String testOffer = "test-offer";
        OfferData offerData = new OfferData();
        offerData.setOffer(testOffer);
        offerData.setOfferDataMatchType(OfferDataMatchType.DEEP_MATCH);
        Mockito.when(offersServiceMock.getOffersMatchedToModelId(Mockito.eq(testModelId), Mockito.any()))
            .thenReturn(ImmutableList.of(offerData));

        List<OfferData> matchedOffers = modelOffersServiceRemote.getModelMatchedOffers(testModelId, 1);

        Mockito.verify(offersServiceMock, Mockito.times(1))
            .getOffersMatchedToModelId(Mockito.eq(testModelId), Mockito.any());
        Assertions.assertThat(matchedOffers).hasSize(1);
        Assertions.assertThat(matchedOffers.get(0).getOffer()).isEqualTo(testOffer);
    }
}

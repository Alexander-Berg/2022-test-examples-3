package ru.yandex.market.partner.content.common.service;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.autogeneration_api.util.OkHttpResponseMock;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dergachevfv
 * @since 4/27/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class DataCampServiceTest {

    CloseableHttpClient httpClient;
    private DataCampService dataCampService;

    @Before
    public void setUp() {
        httpClient = mock(CloseableHttpClient.class);
        dataCampService = new DataCampServiceImpl(httpClient, "http://datacamp.white.tst.vs.market.yandex.net");
    }

    @Test
    public void test() throws IOException {
        SyncChangeOffer.ChangeOfferRequest request = SyncChangeOffer.ChangeOfferRequest.newBuilder()
                .addOffer(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setShopId(10252976)
                                .setOfferId("4187987"))
                )
                .build();

        when(httpClient.execute(any()))
                .thenReturn(new OkHttpResponseMock());

        SyncChangeOffer.FullOfferResponse offers = dataCampService.getOffersByShopId(10252976L, request);

        assertThat(offers).isNotNull();
        verify(httpClient).execute(
                argThat(x -> x.getMethod().equals(HttpGet.METHOD_NAME)
                        && x.getURI().equals(URI.create(
                        "http://datacamp.white.tst.vs.market.yandex.net/shops/10252976/offers"))
                ));
    }
}

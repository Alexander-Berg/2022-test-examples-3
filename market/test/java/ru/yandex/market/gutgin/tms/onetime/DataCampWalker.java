package ru.yandex.market.gutgin.tms.onetime;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.partner.content.common.service.DataCampService;
import ru.yandex.market.partner.content.common.service.DataCampServiceImpl;

@Ignore("Тест запускается руками для получения датакемп офера")
public class DataCampWalker {
    private DataCampService dataCampService;

    public DataCampWalker() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(1000)))
            .setSocketTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(1000)))
            .setConnectionRequestTimeout(Math.toIntExact(TimeUnit.MILLISECONDS.toMillis(1000)))
            .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setUserAgent("defaultUserAgent")
            .build();

        this.dataCampService = new DataCampServiceImpl(httpClient, "http://datacamp.white.tst.vs.market.yandex.net");
    }

    public void run() {
//        "offer_id": "126952", "shop_id": 10394787
        SyncChangeOffer.ChangeOfferRequest request = SyncChangeOffer.ChangeOfferRequest.newBuilder()
            .addOffer(DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setShopId(10394787)
                        .setOfferId("126952"))
                    .build()

            )
            .build();

        SyncChangeOffer.FullOfferResponse offers = dataCampService.getOffersByShopId(10394787, request);
        for (DataCampOffer.Offer offer : offers.getOfferList()) {
            DataCampOfferIdentifiers.OfferIdentifiers identifiers = offer.getIdentifiers();
            Path path = Paths.get("/home/a-shar/work/smb")
                .resolve(identifiers.getShopId() + "_" + identifiers.getOfferId() + ".pb");

            try (OutputStream out = Files.newOutputStream(path)) {
                offer.writeTo(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void doRun() {
        DataCampWalker dataCampWalker = new DataCampWalker();
        dataCampWalker.run();
    }
}

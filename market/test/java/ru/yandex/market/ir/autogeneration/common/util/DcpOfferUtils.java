package ru.yandex.market.ir.autogeneration.common.util;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPictures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class DcpOfferUtils {

    private DcpOfferUtils() {

    }

    public static DataCampOffer.Offer createDcpOfferWithPics(List<String> sourceList, Map<String, String> actualMap) {

        DataCampOfferPictures.SourcePictures.Builder origBuilder = DataCampOfferPictures.SourcePictures.newBuilder();
        sourceList.forEach(url -> origBuilder.addSource(DataCampOfferPictures.SourcePicture.newBuilder()
            .setUrl(url)
            .build()));
        Map<String, DataCampOfferPictures.MarketPicture> actualDcpMap = new HashMap<>();
        actualMap.forEach((url, actualUrl) -> actualDcpMap.put(url, DataCampOfferPictures.MarketPicture.newBuilder()
            .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                .setUrl(actualUrl)
                .build())
            .build()));
        DataCampOfferPictures.MarketPicture.newBuilder().build();
        return DataCampOffer.Offer.newBuilder()
            .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                .setPartner(DataCampOfferPictures.PartnerPictures.newBuilder()
                    .setOriginal(origBuilder.build())
                    .putAllActual(actualDcpMap)
                    .build()
                )
                .build())
            .build();
    }
}

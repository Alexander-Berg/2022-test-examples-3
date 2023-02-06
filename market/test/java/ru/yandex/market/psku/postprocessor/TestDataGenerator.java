package ru.yandex.market.psku.postprocessor;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class TestDataGenerator {

    private TestDataGenerator() {
    }

    public static UltraController.EnrichedOffer generateUCOfferWithoutMsku(PskuInfo pskuInfo) {
        return UltraController.EnrichedOffer.newBuilder()
            .setModelId(123)
            .setVendorId((int) pskuInfo.getVendorId())
            .build();

    }
    public static UltraController.EnrichedOffer generateUCOffer(PskuInfo pskuInfo, MemorizingLongGenerator idsGenerator) {
        return UltraController.EnrichedOffer.newBuilder()
            .setModelId(123)
            .setMarketSkuId(idsGenerator.next())
            .setMarketSkuName(pskuInfo.getTitle())
            .setVendorId((int) pskuInfo.getVendorId())
            .setMarketSkuPublishedOnBlueMarket(true)
            .build();
    }
}

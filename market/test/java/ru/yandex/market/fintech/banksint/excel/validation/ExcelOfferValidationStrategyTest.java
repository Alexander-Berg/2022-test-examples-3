package ru.yandex.market.fintech.banksint.excel.validation;

import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMapping;
import org.junit.jupiter.api.Test;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.excel.offers.ExcelOffer;
import ru.yandex.market.fintech.banksint.excel.offers.ExtractedExcelOffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelOfferValidationStrategyTest extends FunctionalTest {

    private static final Map<String, DataCampOffer.Offer> OFFERS_BY_SKU = Map.of(
            "offer_1", createOfferWithMsku(1234561L),
            "offer_2", createOfferWithMsku(1234562L),
            "offer_3", createOfferWithMsku(1234563L),
            "offer_4", createOfferWithMsku(1234564L),
            "offer_5", createOfferWithMsku(1234565L),
            "offer_6", createOfferWithMsku(1234566L)
    );

    private static final List<ExtractedExcelOffer> OFFERS_LIST = List.of(
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234561L)
                    .setSku("offer_1").setInstallment6(true).build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234562L)
                    .setSku("offer_2").setInstallment12(true).build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234563333L)
                    .setSku("offer_3").setInstallment6(true).build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234564L)
                    .setSku("offer_4").setInstallment12(true).setInstallment24(true).build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234565L)
                    .setSku("offer_5").build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234566L)
                    .setSku("offer_6").build()),
            new ExtractedExcelOffer(ExcelOffer.newBuilder().setMsku(1234567L)
                    .setSku("offer_88").build())
    );

    private static DataCampOffer.Offer createOfferWithMsku(long msku) {
        return DataCampOffer.Offer.newBuilder()
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setBinding(
                                        DataCampOfferMapping.ContentBinding.newBuilder()
                                                .setUcMapping(
                                                        DataCampOfferMapping.Mapping.newBuilder()
                                                                .setMarketSkuId(msku)
                                                                .build()
                                                ).build()
                                ).build()
                ).build();
    }

    @Test
    void testSimpleImpl() {
        var strategy = ExcelOfferValidationStrategy.getSimpleImpl();
        var updatedList = strategy.apply(OFFERS_LIST, OFFERS_BY_SKU);
        long correctCount = updatedList.stream().filter(o -> o.getErrors() == null).count();
        long correctSelectedCount = updatedList.stream()
                .filter(ExtractedExcelOffer::hasActiveInstallments)
                .filter(o -> o.getErrors() == null)
                .count();
        long invalidCount = updatedList.stream().filter(o -> o.getErrors() != null).count();

        assertEquals(5, correctCount);
        assertEquals(3, correctSelectedCount);
        assertEquals(2, invalidCount);

        assertTrue(updatedList.get(2).getErrors().contains(ExcelOfferValidationError.MARKET_SKU_INCORRECT));
        assertTrue(updatedList.get(6).getErrors().contains(ExcelOfferValidationError.SHOP_SKU_INCORRECT));
    }
}

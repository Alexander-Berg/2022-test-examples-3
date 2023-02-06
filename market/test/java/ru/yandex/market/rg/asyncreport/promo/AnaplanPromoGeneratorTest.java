package ru.yandex.market.rg.asyncreport.promo;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampOfferStockInfo;
import Market.DataCamp.DataCampPromo;
import NMarketIndexer.Common.Common;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.wrapper.PoiCell;
import ru.yandex.market.common.excel.wrapper.PoiRow;
import ru.yandex.market.common.excel.wrapper.PoiSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.supplier.promo.model.offer.xls.CheapestAsGiftXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.context.CheapestAsGiftTemplateContext;
import ru.yandex.market.core.supplier.promo.model.offer.xls.DiscountXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.context.DiscountTemplateContext;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationError;
import ru.yandex.market.core.supplier.promo.xlsx.SupplierPromoOffersXlsxProcessor;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.market.core.supplier.promo.service.PromoOffersFetcher.mapToDataCampPromoOffer;

public class AnaplanPromoGeneratorTest extends FunctionalTest {
    private static final String PROMO_NAME = "Гадкий утёнок";
    private static final String FULL_PROMO_NAME = "Вы настраиваете участие в акции: " + PROMO_NAME;
    @Autowired
    DiscountTemplateContext discountTemplateContext;
    @Autowired
    CheapestAsGiftTemplateContext cheapestAsGiftTemplateContext;

    @Test
    public void testSupplierPromoDiscountOffersXlsx() throws IOException {
        Path tempFilePath = Files.createTempFile("discount-promo-offers", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        List<DiscountXlsPromoOffer> promoOffers =
                generateDiscountDataCampOffers(() -> randomAlphanumeric(1, 100), RandomUtils::nextInt);

        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                new ClassPathResource("reports/marketplace-sales.xlsm"),
                reportFile,
                discountTemplateContext,
                promoOffers.stream(),
                PROMO_NAME
        );

        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(FULL_PROMO_NAME, promoNameCell.getFormattedCellValue().get());
        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        for (int rowNum = 0; rowNum <= 102; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum < 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse(null);
                Assertions.assertTrue(
                        StringUtils.isNotBlank(cellVal), "Blank cell at row: " + rowNum + ", col: " + colNum);
            }
        }
        FileUtils.deleteQuietly(reportFile);
    }

    @Test
    public void testSupplierPromoDiscountOffersXlsxWithBlanks() throws IOException {
        Path tempFilePath = Files.createTempFile("discount-promo-offers", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                new ClassPathResource("reports/marketplace-sales.xlsm"),
                reportFile,
                discountTemplateContext,
                generateDiscountDataCampOffers(() -> randomAlphanumeric(0, 100), RandomUtils::nextInt).stream(),
                PROMO_NAME
        );
        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);
        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(FULL_PROMO_NAME, promoNameCell.getFormattedCellValue().get());
        FileUtils.deleteQuietly(reportFile);
    }

    @Test
    public void testCheapestAsGiftPromoXlsx() throws IOException {
        Path tempFilePath = Files.createTempFile("cheapest-as-gift-promo-offers", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                new ClassPathResource("reports/marketplace-sales-three-as-two.xlsm"),
                reportFile,
                cheapestAsGiftTemplateContext,
                generateCheapestAsGiftDataCampOffers(
                        () -> randomAlphanumeric(1, 100),
                        RandomUtils::nextInt,
                        RandomUtils::nextBoolean
                ).stream(),
                PROMO_NAME
        );

        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);
        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(FULL_PROMO_NAME, promoNameCell.getFormattedCellValue().get());
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        for (int rowNum = 0; rowNum <= 102; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // header description may be blank
            }
            final PoiRow row = sheet.getRow(rowNum);
            for (int colNum = 0; colNum <= 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse(null);
                Assertions.assertTrue(
                        StringUtils.isNotBlank(cellVal), "Blank cell at row: " + rowNum + ", col: " + colNum);
            }
        }
        FileUtils.deleteQuietly(reportFile);
    }

    @Test
    public void testCheapestAsGiftPromoXlsxWithBlanks() throws IOException {
        Path tempFilePath = Files.createTempFile("cheapest-as-gift-promo-offers", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                new ClassPathResource("reports/marketplace-sales-three-as-two.xlsm"),
                reportFile,
                cheapestAsGiftTemplateContext,
                generateCheapestAsGiftDataCampOffers(
                        () -> randomAlphanumeric(0, 100),
                        RandomUtils::nextInt,
                        RandomUtils::nextBoolean
                ).stream(),
                PROMO_NAME
        );
        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);
        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(FULL_PROMO_NAME, promoNameCell.getFormattedCellValue().get());
        FileUtils.deleteQuietly(reportFile);
    }

    private List<CheapestAsGiftXlsPromoOffer> generateCheapestAsGiftDataCampOffers(
            Supplier<String> stringSupplier,
            Supplier<Integer> intSupplier,
            Supplier<Boolean> booleanSupplier
    ) {
        return generateDatacampOffers(RandomUtils::nextInt).stream()
                .map(dataCampOffer ->
                        mapToDataCampPromoOffer(dataCampOffer, "#0", DataCampPromo.PromoType.CHEAPEST_AS_GIFT))
                .map(dataCampPromoOffer ->
                        new CheapestAsGiftXlsPromoOffer.Builder()
                                .withShopSku(dataCampPromoOffer.getShopSku())
                                .withMarketSku(dataCampPromoOffer.getMarketSku())
                                .withName(dataCampPromoOffer.getName())
                                .withCount(dataCampPromoOffer.getCount())
                                .withPrice(dataCampPromoOffer.getPrice())
                                .withCategoryName(stringSupplier.get())
                                .withVendorName(stringSupplier.get())
                                .withErrors(generatePromoOfferValidationErrorSet())
                                .withMaxDiscount(BigDecimal.valueOf(intSupplier.get()).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN))
                                .withMinPromoPrice(Long.valueOf(intSupplier.get()))
                                .withParticipate(booleanSupplier.get())
                                .withActivePromosNames(stringSupplier.get())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<DiscountXlsPromoOffer> generateRandomCommentDiscountDataCampOffers(
            Supplier<String> stringSupplier,
            Supplier<Integer> intSupplier
    ) {
        return generateDatacampOffers(RandomUtils::nextInt).stream()
                .map(dataCampOffer ->
                        mapToDataCampPromoOffer(dataCampOffer, "#0", DataCampPromo.PromoType.DIRECT_DISCOUNT))
                .map(dataCampPromoOffer ->
                        new DiscountXlsPromoOffer.Builder()
                                .withShopSku(dataCampPromoOffer.getShopSku())
                                .withMarketSku(dataCampPromoOffer.getMarketSku())
                                .withName(dataCampPromoOffer.getName())
                                .withCount(dataCampPromoOffer.getCount())
                                .withPrice(dataCampPromoOffer.getPrice())
                                .withOldPrice(dataCampPromoOffer.getOldPrice())
                                .withPromoPrice(dataCampPromoOffer.getPromoPrice())
                                .withCategoryName(stringSupplier.get())
                                .withErrors(intSupplier.get() % 2 == 0 ? null : generatePromoOfferValidationErrorSet())
                                .withMinDiscount(intSupplier.get())
                                .withMaxPromoPrice(Long.valueOf(intSupplier.get()))
                                .withActivePromosNames(stringSupplier.get())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<DiscountXlsPromoOffer> generateDiscountDataCampOffers(
            Supplier<String> stringSupplier,
            Supplier<Integer> intSupplier
    ) {
        return generateDatacampOffers(RandomUtils::nextInt).stream()
                .map(dataCampOffer ->
                        mapToDataCampPromoOffer(dataCampOffer, "#0", DataCampPromo.PromoType.DIRECT_DISCOUNT))
                .map(dataCampPromoOffer ->
                        new DiscountXlsPromoOffer.Builder()
                                .withShopSku(dataCampPromoOffer.getShopSku())
                                .withMarketSku(dataCampPromoOffer.getMarketSku())
                                .withName(dataCampPromoOffer.getName())
                                .withCount(dataCampPromoOffer.getCount())
                                .withPrice(dataCampPromoOffer.getPrice())
                                .withOldPrice(dataCampPromoOffer.getOldPrice())
                                .withPromoPrice(dataCampPromoOffer.getPromoPrice())
                                .withCategoryName(stringSupplier.get())
                                .withVendorName(stringSupplier.get())
                                .withErrors(generatePromoOfferValidationErrorSet())
                                .withMinDiscount(intSupplier.get())
                                .withMaxPromoPrice(Long.valueOf(intSupplier.get()))
                                .withActivePromosNames(stringSupplier.get())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<DataCampOffer.Offer> generateDatacampOffers(Supplier<Integer> intSupplier) {
        DataCampOffer.OffersBatch.Builder batch = DataCampOffer.OffersBatch.newBuilder();
        for (int i = 0; i < 100; i++) {
            batch.addOffer(DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                            .setOfferId(randomAlphanumeric(1, 100)) // non null
                            .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                    .setMarketSkuId(intSupplier.get())
                                    .build())
                            .build()
                    )
                    .setContent(DataCampOfferContent.OfferContent.newBuilder()
                            .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                    .setCategoryId(intSupplier.get())
                                    .build())
                            .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                    .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                            .setMarketSkuId(intSupplier.get())
                                    )
                            )
                            .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                            .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                    .setValue(randomAlphanumeric(1, 100))
                                            )
                                    )
                            )
                            .build())
                    .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                            .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                    .setBinaryPrice(Common.PriceExpression.newBuilder()
                                            .setPrice(intSupplier.get())
                                            .build())
                                    .setBinaryOldprice(Common.PriceExpression.newBuilder()
                                            .setPrice(intSupplier.get())
                                            .build())
                                    .build())
                            .build())
                    .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                            .setMarketStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                    .setCount(intSupplier.get())
                                    .build())
                            .build())
                    .setPromos(
                            DataCampOfferPromos.OfferPromos.newBuilder()
                                    .setAnaplanPromos(
                                            createMarketPromos(
                                                    Arrays.asList(
                                                            createPromo("#0", intSupplier.get(), null),
                                                            createPromo("#1", intSupplier.get(), null)
                                                    ),
                                                    Arrays.asList(
                                                            createPromo("#0", null, intSupplier.get()),
                                                            createPromo("#1", null, intSupplier.get())
                                                    )
                                            )
                                    )
                    )
                    .build());
        }
        return batch.build().getOfferList();
    }

    private SortedSet<PromoOfferValidationError> generatePromoOfferValidationErrorSet() {
        SortedSet<PromoOfferValidationError> resultList = new TreeSet<>();
        for (int i = 0; i < RandomUtils.nextInt(1, 4); i++) {
            resultList.add(PromoOfferValidationError
                    .values()[RandomUtils.nextInt(0, PromoOfferValidationError.values().length)]);
        }
        return resultList;
    }

    private DataCampOfferPromos.Promo createPromo(String id, Integer price, Integer oldPrice) {
        DataCampOfferPromos.Promo.DirectDiscount.Builder directDiscountBuilder =
                DataCampOfferPromos.Promo.DirectDiscount.newBuilder();
        if (price != null) {
            directDiscountBuilder.setPrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(price))
                            )
                            .build()
            );
        }
        if (oldPrice != null) {
            directDiscountBuilder.setBasePrice(
                    Common.PriceExpression.newBuilder()
                            .setPrice(
                                    DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice))
                            )
                            .build()
            );
        }
        return DataCampOfferPromos.Promo.newBuilder()
                .setId(id)
                .setDirectDiscount(directDiscountBuilder)
                .build();
    }

    private DataCampOfferPromos.MarketPromos createMarketPromos(
            List<DataCampOfferPromos.Promo> activePromos,
            List<DataCampOfferPromos.Promo> allPromos
    ) {
        DataCampOfferPromos.MarketPromos.Builder anaplanPromosBuilder = DataCampOfferPromos.MarketPromos.newBuilder();
        if (CollectionUtils.isNotEmpty(activePromos)) {
            anaplanPromosBuilder.setActivePromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(activePromos)
                            .build()
            );
        }
        if (CollectionUtils.isNotEmpty(allPromos)) {
            anaplanPromosBuilder.setAllPromos(
                    DataCampOfferPromos.Promos.newBuilder()
                            .addAllPromos(allPromos)
                            .build()
            );
        }
        return anaplanPromosBuilder.build();
    }
}

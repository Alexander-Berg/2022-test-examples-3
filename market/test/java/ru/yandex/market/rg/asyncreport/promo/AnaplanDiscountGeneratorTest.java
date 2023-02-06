package ru.yandex.market.rg.asyncreport.promo;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.wrapper.PoiCell;
import ru.yandex.market.common.excel.wrapper.PoiRow;
import ru.yandex.market.common.excel.wrapper.PoiSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.rg.config.FunctionalTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.supplier.promo.service.PromoService.BUSINESS_ID_ANAPLAN;

@DbUnitDataSet(before = "supplierPromoDiscountOffersGeneratorTest/before.csv")
public class AnaplanDiscountGeneratorTest extends FunctionalTest {
    private static final String PREFIX_PROMO_NAME = "Вы настраиваете участие в акции: ";

    @Autowired
    private AnaplanDiscountGenerator anaplanPromoDiscountOffersGenerator;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    private AmazonS3 amazonS3;

    @DisplayName("Тест на то, что офферы из ОХ корректно преобразуются в потенциальный ассортимент для акции ПС.")
    @Test
    public void testSupplierPromoDiscountOfferGenerator() {
        List<List<String>> expectedRows = List.of(
                List.of("null", "hid.1000161", "Микроволновая печь Samsung PG838R-W", "Samsung", "Микроволновые печи", "633", "5000", "3100", "15", "null", "null"),
                List.of("null", "910-001794", "Мышь Logitech M90, черный", "Logitech", "Мыши", "100", "50000", "3100", "null", "30000", "null"),
                List.of("null", "06d", "Шоколадный букет из роз", "Florelli", "Шоколадные конфеты в коробках, подарочные наборы", "312", "3100", "3100", "20", "null", "null"),
                List.of("null", "0Р-00018614", "Гладильная доска NIKA БК3", "Nika", "Гладильные доски", "653", "50000", "3100", "15", "30000", "null")
        );

        generateAnaplanDiscountAssortmentAndDoAssertions(expectedRows);
    }

    @DisplayName("Проверка, что под флагом не предзаполняется колонка Старой цены для акции.")
    @Test
    @DbUnitDataSet(before = "supplierPromoDiscountOffersGeneratorTest/do-not-prefill-old-price.csv")
    public void testDoNotPrefillOldPrice() {
        List<List<String>> expectedRows = List.of(
                List.of("null", "hid.1000161", "Микроволновая печь Samsung PG838R-W", "Samsung", "Микроволновые печи", "633", "null", "3100", "15", "null", "null"),
                List.of("null", "910-001794", "Мышь Logitech M90, черный", "Logitech", "Мыши", "100", "null", "3100", "null", "30000", "null"),
                List.of("null", "06d", "Шоколадный букет из роз", "Florelli", "Шоколадные конфеты в коробках, подарочные наборы", "312", "null", "3100", "20", "null", "null"),
                List.of("null", "0Р-00018614", "Гладильная доска NIKA БК3", "Nika", "Гладильные доски", "653", "null", "3100", "15", "30000", "null")
        );

        generateAnaplanDiscountAssortmentAndDoAssertions(expectedRows);
    }

    @DisplayName("Проверка, что под флагом в файл добавляются офферы без msku.")
    @Test
    @DbUnitDataSet(before = "supplierPromoDiscountOffersGeneratorTest/anaplan-promo-msku-removed.csv")
    public void testRemovedMskuInPromos() {
        List<List<String>> expectedRows = List.of(
                List.of("null", "hid.1000161", "Микроволновая печь Samsung PG838R-W", "Samsung", "Микроволновые печи", "633", "5000", "3100", "15", "null", "null"),
                List.of("null", "910-001794", "Мышь Logitech M90, черный", "Logitech", "Мыши", "100", "50000", "3100", "null", "30000", "null", "null"),
                List.of("null", "06d", "Шоколадный букет из роз", "Florelli", "Шоколадные конфеты в коробках, подарочные наборы", "312", "3100", "3100", "20", "null", "null"),
                List.of("null", "0Р-00018614", "Гладильная доска NIKA БК3", "Nika", "Гладильные доски", "653", "50000", "3100", "15", "30000", "null"),
                List.of("null", "1957100", "Чехол Samsung Silicone Cover A71 розовый", "Samsung", "Чехлы для мобильных телефонов", "653", "50000", "1990", "15", "30000", "null", "null")
        );

        generateAnaplanDiscountAssortmentAndDoAssertions(expectedRows);
    }

    void generateAnaplanDiscountAssortmentAndDoAssertions(List<List<String>> expectedRows) {
        final String reportId = "reportId";
        final String flashPromoId = "#81456";
        final String discountPromoId = "#258445267";
        final long partnerId = 1234;
        final long businessId = 1;
        final String flashPromoName = "Молниеносненькая скидочка";
        final String discountPromoName = "Название акции ПС";
        final AnaplanPromoOffersParams reportParams = new AnaplanPromoOffersParams();
        reportParams.setPromoId(discountPromoId);
        reportParams.setSupplierId(partnerId);

        mockPromoStorageInfo(discountPromoId, flashPromoId, discountPromoName, flashPromoName);

        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId("hid.1000161")
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "supplierPromoDiscountOffersGeneratorTest/testSupplierPromoDiscountOfferGenerator/assortment.json",
                getClass()
        );
        List<OffersBatch.UnitedOffersBatchResponse.Entry> entries1 = getUnitedOffersResponse.getOffersList().stream()
                .map(unitedOffer -> {
                    OffersBatch.UnitedOffersBatchResponse.Entry.Builder entryBuilder =
                            OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                                    .setUnitedOffer(unitedOffer);
                    return entryBuilder.build();
                }).collect(Collectors.toList());
        OffersBatch.UnitedOffersBatchResponse response = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addAllEntries(entries1)
                .build();
        doReturn(response)
                .when(dataCampShopClient).getBusinessUnitedOffers(businessId, Set.of("hid.1000161"), partnerId);

        AtomicReference<PoiWorkbook> wb = new AtomicReference<>();
        when(amazonS3.putObject(anyString(), anyString(), any(File.class)))
                .then(a -> {
                    wb.set(PoiWorkbook.load((File) a.getArgument(2)));
                    return null;
                });

        anaplanPromoDiscountOffersGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(PREFIX_PROMO_NAME + discountPromoName, promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);

        assertEquals(headerRows.size() + expectedRows.size(), sheet.getLastRowNum() + 1);
        for (int rowNum = 0; rowNum < headerRows.size() + expectedRows.size(); rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);
            List<String> expectedRow = expectedRows.get(rowNum - headerRows.size());
            for (int colNum = 0; colNum < expectedRow.size(); colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");
                assertEquals(expectedRow.get(colNum), cellVal);
            }
        }
    }

    private void mockPromoStorageInfo(String discountPromoId, String flashPromoId, String discountPromoName, String flashPromoName) {
        GetPromoBatchRequestWithFilters requestForActivePromos =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(discountPromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(flashPromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        GetPromoBatchRequestWithFilters requestForDiscount =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(discountPromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        DataCampPromo.PromoConstraints constraints = DataCampPromo.PromoConstraints.newBuilder()
                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(90595)
                                        .setMinDiscount(15)
                                        .setName("Микроволновые печи")
                                )
                                .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(15714102)
                                        .setMinDiscount(20)
                                        .setName("Шоколадные конфеты в коробках, подарочные наборы")
                                )
                                .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(14333188)
                                        .setMinDiscount(15)
                                        .setName("Мыши")
                                )
                                .addPromoCategory(DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(1564519)
                                        .setMinDiscount(15)
                                        .setName("Гладильные доски")
                                )
                        )
                ).build();

        DataCampPromo.PromoDescription flashPromo = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(flashPromoId)
                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                .setSource(Promo.ESourceType.ANAPLAN))
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(flashPromoName)
                        .build())
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.BLUE_FLASH))
                .setConstraints(constraints)
                .build();

        DataCampPromo.PromoDescription discountPromo = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(discountPromoId)
                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                .setSource(Promo.ESourceType.ANAPLAN))
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName(discountPromoName)
                        .build())
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT))
                .setConstraints(constraints)
                .build();


        SyncGetPromo.GetPromoBatchResponse promosResponse =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(flashPromo)
                                .addPromo(discountPromo)
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse discountResponse =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(discountPromo)
                        )
                        .build();

        doReturn(promosResponse).when(dataCampShopClient).getPromos(requestForActivePromos);
        doReturn(discountResponse).when(dataCampShopClient).getPromos(requestForDiscount);
    }
}

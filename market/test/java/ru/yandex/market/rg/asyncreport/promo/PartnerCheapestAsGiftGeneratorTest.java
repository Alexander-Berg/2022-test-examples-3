package ru.yandex.market.rg.asyncreport.promo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.amazonaws.services.s3.AmazonS3;
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
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "supplierPromoCheapestAsGiftOffersGeneratorTest/before.csv")
public class PartnerCheapestAsGiftGeneratorTest extends FunctionalTest {
    private static final String PREFIX_PROMO_NAME = "Вы настраиваете участие в акции: ";

    @Autowired
    private PartnerCheapestAsGiftGenerator partnerCheapestAsGiftGenerator;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private SaasService saasDataCampShopService;

    @Test
    public void testCheapestAsGift() {
        String reportId = "reportId";
        String cheapestAsGiftPromoId = "#6666";
        String discountPromoId = "#6431";
        long partnerId = 1234;
        long businessId = 1;
        final int warehouseId = 48339;
        String cheapestAsGiftPromoName = "Самый дешевенький даром к вам в ручки";
        String discountPromoName = "Скидочка";
        final PartnerCheapestAsGiftPotentialAssortmentParams reportParams = new PartnerCheapestAsGiftPotentialAssortmentParams();
        reportParams.setSupplierId(partnerId);
        reportParams.setStartDate(LocalDateTime.of(2021, 1, 1, 0, 0, 0));
        reportParams.setEndDate(LocalDateTime.of(2021, 1, 13, 23, 59, 59));
        reportParams.setBundleSize(4);
        reportParams.setWarehouseId(warehouseId);

        mockDiscount(discountPromoId, discountPromoName);
        mockCheapestAsGift(cheapestAsGiftPromoId, cheapestAsGiftPromoName, warehouseId, businessId);


        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId("hid.1000161")
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "supplierPromoCheapestAsGiftOffersGeneratorTest/testCheapestAsGift/assortment.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        AtomicReference<PoiWorkbook> wb = new AtomicReference<>();
        when(amazonS3.putObject(anyString(), anyString(), any(File.class)))
                .then(a -> {
                    wb.set(PoiWorkbook.load((File) a.getArgument(2)));
                    return null;
                });

        partnerCheapestAsGiftGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        org.junit.jupiter.api.Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        org.junit.jupiter.api.Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        assertEquals(PREFIX_PROMO_NAME + "Новая акция", promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        List<String> rowsOfferId1 = List.of(
                "null", "hid.1000161", "Микроволновая печь Samsung PG838R-W", "Samsung", "Микроволновые печи", "633", "3100", "25", "2325", "null", "Скидочка"
        );
        for (int rowNum = 0; rowNum <= 3; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum <= 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");
                assertEquals(cellVal, rowsOfferId1.get(colNum));
            }
        }
    }

    /**
     * Проверка, что оффер без msku успешно выгружается в ассортимент при выставленном флаге partner.promos.msku.remove
     */
    @Test
    @DbUnitDataSet(before = "supplierPromoCheapestAsGiftOffersGeneratorTest/partnerMskuRemoved.before.csv")
    public void noMskuTest() {
        String reportId = "reportId";
        String cheapestAsGiftPromoId = "#6666";
        String discountPromoId = "#6431";
        long partnerId = 1234;
        long businessId = 1;
        final int warehouseId = 48339;
        String cheapestAsGiftPromoName = "Самый дешевенький даром к вам в ручки";
        String discountPromoName = "Скидочка";
        final PartnerCheapestAsGiftPotentialAssortmentParams reportParams = new PartnerCheapestAsGiftPotentialAssortmentParams();
        reportParams.setSupplierId(partnerId);
        reportParams.setStartDate(LocalDateTime.of(2021, 1, 1, 0, 0, 0));
        reportParams.setEndDate(LocalDateTime.of(2021, 1, 13, 23, 59, 59));
        reportParams.setBundleSize(4);
        reportParams.setWarehouseId(warehouseId);

        mockDiscount(discountPromoId, discountPromoName);
        mockCheapestAsGift(cheapestAsGiftPromoId, cheapestAsGiftPromoName, warehouseId, businessId);

        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId("hid.1000161")
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "supplierPromoCheapestAsGiftOffersGeneratorTest/testCheapestAsGift/noMskuAssortment.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        AtomicReference<PoiWorkbook> wb = new AtomicReference<>();
        when(amazonS3.putObject(anyString(), anyString(), any(File.class)))
                .then(a -> {
                    wb.set(PoiWorkbook.load((File) a.getArgument(2)));
                    return null;
                });

        partnerCheapestAsGiftGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        org.junit.jupiter.api.Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        org.junit.jupiter.api.Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        assertEquals(PREFIX_PROMO_NAME + "Новая акция", promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        List<String> rowsOfferId1 = List.of(
                "null", "0Р-00018614", "Гладильная доска NIKA БК3", "Nika", "Гладильные доски", "633", "3100", "25", "2325", "null", "Скидочка"
        );
        for (int rowNum = 0; rowNum <= 3; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum <= 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");
                assertEquals(rowsOfferId1.get(colNum), cellVal);
            }
        }
    }

    private void mockDiscount(String discountPromoId, String discountPromoName) {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(discountPromoId)
                                                .setBusinessId(0)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse marketPromocodePromo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setPrimaryKey(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(discountPromoId)
                                                        .setBusinessId(0)
                                                        .setSource(Promo.ESourceType.ANAPLAN))
                                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                .setName(discountPromoName)
                                                .build())
                                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                                .setStartDate(1)
                                                .setEndDate(2137307401)
                                                .setEnabled(true)
                                        )
                                )
                        )
                        .build();
        doReturn(marketPromocodePromo).when(dataCampShopClient).getPromos(requestForPromo);
    }

    private void mockCheapestAsGift(String promoId, String promoName, long warehouseId, long businessId) {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(promoId)
                                                .setBusinessId(Math.toIntExact(businessId))
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse promo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setPrimaryKey(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(promoId)
                                                        .setBusinessId(Math.toIntExact(businessId))
                                                        .setSource(Promo.ESourceType.ANAPLAN))
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName(promoName)
                                                        .build())
                                        .setPromoGeneralInfo(
                                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT))
                                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                                .setCheapestAsGift(DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                                        .setCount(4)))
                                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                                .setStartDate(1609479257)
                                                .setEndDate(1911120857)
                                                .setEnabled(true)
                                                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                        .setWarehouseRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                        .addId(warehouseId)))))
                                )
                        )
                        .build();
        doReturn(promo).when(dataCampShopClient).getPromos(requestForPromo);
    }
}

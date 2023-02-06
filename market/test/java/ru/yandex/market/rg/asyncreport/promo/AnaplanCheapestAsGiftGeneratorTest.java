package ru.yandex.market.rg.asyncreport.promo;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static ru.yandex.market.core.supplier.promo.service.PromoService.BUSINESS_ID_ANAPLAN;

@DbUnitDataSet(before = "supplierPromoCheapestAsGiftOffersGeneratorTest/before.csv")
public class AnaplanCheapestAsGiftGeneratorTest extends FunctionalTest {

    private static final String PREFIX_PROMO_NAME = "Вы настраиваете участие в акции: ";

    @Autowired
    private AnaplanCheapestAsGiftGenerator anaplanPromoCheapestAsGiftOffersGenerator;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private SaasService saasDataCampShopService;

    @Test
    public void testCheapestAsGift() {
        final String reportId = "reportId";
        final String promoId = "#6431";
        final long partnerId = 1234;
        final long businessId = 1;
        final int warehouseId = 48339;
        final String promoName = "Третий товар в подарок";
        final AnaplanPromoOffersParams reportParams = new AnaplanPromoOffersParams();
        reportParams.setPromoId(promoId);
        reportParams.setSupplierId(partnerId);

        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(promoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
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
                                                        .setBusinessId(BUSINESS_ID_ANAPLAN)
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
                                                .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                        .setWarehouseRestriction(DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                        .addId(warehouseId)))))
                                )
                        )
                        .build();
        doReturn(promo).when(dataCampShopClient).getPromos(requestForPromo);

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

        anaplanPromoCheapestAsGiftOffersGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(PREFIX_PROMO_NAME + promoName, promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        for (int rowNum = 0; rowNum <= 3; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum <= 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse(null);
                if (colNum == 0 || // столбец комментария
                        colNum == 10) { // или акция, в которой участвует другой товар
                    Assertions.assertNull(cellVal);
                } else {
                    Assertions.assertTrue(
                            StringUtils.isNotBlank(cellVal), "Blank cell at row: " + rowNum +
                                    ", col: " + colNum +
                                    "columnName: " + sheet.getRow(1).getCell(colNum).getFormattedCellValue());
                }
            }
        }
    }
}

package ru.yandex.market.rg.asyncreport.promo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import Market.DataCamp.DataCampPromo;
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
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyRestClientImpl;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyTariff;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyTariffResponse;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "before.csv")
public class PartnerCashbackGeneratorTest extends FunctionalTest {
    private static final String PREFIX_PROMO_NAME = "Вы настраиваете участие в акции: ";

    @Autowired
    private PartnerCashbackGenerator partnerCashbackGenerator;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    @Qualifier("loyaltyRestClientImpl")
    private LoyaltyRestClientImpl loyaltyClient;

    @Test
    public void testCashbackGenerator() {
        final String reportId = "reportId";
        final long partnerId = 10;
        final String promoName = "Кешбечик на офферочки";
        final PartnerCashbackPotentialAssortmentParams reportParams = new PartnerCashbackPotentialAssortmentParams();
        reportParams.setSupplierId(partnerId);
        reportParams.setPromoId("10_PCC_1634563510");

        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#6431")
                                                .setBusinessId(0)
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
                                                        .setPromoId("#6431")
                                                        .setBusinessId(0)
                                                        .setSource(Promo.ESourceType.ANAPLAN)
                                        )
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName("Прямая скидка!")
                                                        .build()
                                        )
                                        .setPromoGeneralInfo(
                                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                                        )
                                        .setConstraints(
                                                DataCampPromo.PromoConstraints.newBuilder()
                                                        .setStartDate(1)
                                                        .setEndDate(1928838815L)
                                        )
                                )
                        )
                        .build();
        doReturn(promo).when(dataCampShopClient).getPromos(requestForPromo);

        GetPromoBatchRequestWithFilters requestForCashback =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("10_PCC_1634563510")
                                                .setBusinessId(1)
                                                .setSource(Promo.ESourceType.PARTNER_SOURCE))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse cashbcakPromo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setPrimaryKey(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId("10_PCC_1634563510")
                                                        .setBusinessId(1)
                                                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                        )
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName(promoName)
                                                        .build()
                                        )
                                        .setPromoGeneralInfo(
                                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK)
                                        )
                                        .setConstraints(
                                                DataCampPromo.PromoConstraints.newBuilder()
                                                        .setStartDate(1)
                                                        .setEndDate(1928838815L)
                                        )
                                )
                        )
                        .build();
        doReturn(cashbcakPromo).when(dataCampShopClient).getPromos(requestForCashback);

        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId("hid.1000161")
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment.json",
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
        doReturn(new LoyaltyTariffResponse(0, List.of(new LoyaltyTariff(90595, 1.5, 30, 5))))
                .when(loyaltyClient).getActualLoyaltyTariffs(any());

        partnerCashbackGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и кешбэк").build());
        org.junit.jupiter.api.Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        org.junit.jupiter.api.Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(PREFIX_PROMO_NAME + promoName, promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        for (int rowNum = 0; rowNum <= 3; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum <= 8; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse(null);
                if (colNum == 0) { // столбец комментария
                    org.junit.jupiter.api.Assertions.assertNull(cellVal);
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

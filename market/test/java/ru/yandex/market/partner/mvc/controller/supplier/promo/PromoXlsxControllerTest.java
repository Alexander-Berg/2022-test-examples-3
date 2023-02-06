package ru.yandex.market.partner.mvc.controller.supplier.promo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.client.AdvPromoClient;
import ru.yandex.market.adv.promo.client.model.WaitingPromoDto;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.AsyncReports;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.supplier.promo.dao.PromoOffersValidationParamsDao;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PromoXlsxControllerTest extends FunctionalTest {
    S3Object s3Object = mock(S3Object.class);
    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AsyncReports<ReportsType> asyncReportsService;

    @Autowired
    private PromoOffersValidationParamsDao promoOffersValidationParamsDao;

    @Autowired
    private AdvPromoClient advPromoClient;

    @Test
    @DbUnitDataSet(
            before = "promoXlsxControllerTest/testUpload/uploadOffers_before.csv"
    )
    public void testUploadNewFormat() throws IOException {
        uploadFileWithName("promoXlsxControllerTest/testUpload/testUpload.xlsm");
    }

    @Test
    @DbUnitDataSet(
            before = "promoXlsxControllerTest/testUpload/uploadOffers_before.csv"
    )
    public void testUploadOldFormat() throws IOException {
        uploadFileWithName("promoXlsxControllerTest/testUpload/testUploadOldFormat.xls");
    }

    private void uploadFileWithName(String fileName) throws IOException {
        doReturn(ProtoTestUtil.getProtoMessageByJson(
                SyncChangeOffer.FullOfferResponse.class,
                "promoXlsxControllerTest/proto/emptyOffers.json",
                getClass()
        )).when(dataCampShopClient).getOffers(anyLong(), any(), any());
        String promoId = "promo-id-1"; // оставим без решетки,
        mockAnaplanPromoDescription(promoId);
        String key = "key";
        String bucketName = "bucket";
        when(resourceLocationFactory.createLocation(any()))
                .thenReturn(ResourceLocation.create(bucketName, key));
        URL url = getClass().getResource("promoXlsxControllerTest/testUpload/dummy.xlsx");
        when(amazonS3.getUrl(any(), any())).thenReturn(url);
        when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
        File file = new File(getClass()
                .getResource(fileName).getPath());
        InputStream inputStream = new FileInputStream(file);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));
        doNothing().when(advPromoClient).startWaitingForPromo(any());
        final FileSystemResource resource = new FileSystemResource(url.getPath());
        final HttpEntity<?> upload = FunctionalTestHelper.createMultipartHttpEntity("upload", resource, params -> {
        });
        String s = baseUrl + "/supplier/promo/offers/async/upload?campaign_id=1001&promo_type=CHEAPEST_AS_GIFT&promo_id=" + promoId;
        final ResponseEntity<String> resp = FunctionalTestHelper.post(s, upload);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        ReportInfo<ReportsType> reportInfo = asyncReportsService.getReportInfo("777");
        assertEquals(reportInfo.getReportRequest().getReportType(), ReportsType.PROMO_OFFERS_VALIDATION);
        assertEquals(reportInfo.getState(), ReportState.PENDING);
        assertEquals(reportInfo.getReportRequest().getParams().get("promoId").toString(), "promo-id-1");
        assertEquals(reportInfo.getReportRequest().getParams().get("promoMechanic").toString(), "CHEAPEST_AS_GIFT");
        inputStream.close();
    }

    @Test
    @DbUnitDataSet(
            before = "promoXlsxControllerTest/testCommit/commitOffers_before.csv"
    )
    public void testCommit() throws IOException {
        String promoId = "1_XXX_promo";
        long createdAt = 1111;
        long updatedAt = 2222;
        mockDataCampClientResponse(promoId, createdAt, updatedAt);

        String key = "eligibleS3Key";
        String bucketName = "bucket";
        when(resourceLocationFactory.createLocation(any()))
                .thenReturn(ResourceLocation.create(bucketName, key));
        URL url = getClass().getResource("promoXlsxControllerTest/testUpload/dummy.xlsx");
        when(amazonS3.getUrl(any(), any())).thenReturn(url);
        final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(amazonS3.getObject(anyString(), stringArgumentCaptor.capture())).thenReturn(s3Object);
        File file = new File(getClass()
                .getResource("promoXlsxControllerTest/testCommit/fileForParted.pbsn").getPath());
        InputStream inputStream = new FileInputStream(file);
        when(s3Object.getObjectContent()).thenReturn(
                new S3ObjectInputStream(inputStream, null));
        final ResponseEntity<String> resp = FunctionalTestHelper.post(baseUrl +
                "/supplier/promo/offers/async/commit?campaign_id=1001&upload_id=validation-id-1");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(key, stringArgumentCaptor.getValue());
        inputStream.close();

        ArgumentCaptor<WaitingPromoDto> waitingPromoCaptor = ArgumentCaptor.forClass(WaitingPromoDto.class);
        verify(advPromoClient, times(1)).startWaitingForPromo(waitingPromoCaptor.capture());
        WaitingPromoDto waitingPromoDto = waitingPromoCaptor.getValue();
        assertEquals(promoId, waitingPromoDto.getPromoId());
        assertEquals(1, waitingPromoDto.getPartnerId());

        ArgumentCaptor<DataCampPromo.PromoDescription> updatePromoDescriptionCaptor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampShopClient, times(1)).addPromo(updatePromoDescriptionCaptor.capture());
        DataCampPromo.PromoDescription updatedDescription = updatePromoDescriptionCaptor.getValue();
        assertEquals(promoId, updatedDescription.getPrimaryKey().getPromoId());
        assertEquals(createdAt, updatedDescription.getUpdateInfo().getCreatedAt());
        assertTrue(updatedAt < updatedDescription.getUpdateInfo().getUpdatedAt());
        assertTrue(promoOffersValidationParamsDao.getValidationStats("validation-id-1").getCommitted());
    }

    @Test
    @DbUnitDataSet(
            before = "promoXlsxControllerTest/testGetPromoOffersAsyncValidationInfo/validationInfo_before.csv"
    )
    public void testGetPromoOffersAsyncValidationInfo() throws IOException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/supplier/promo/offers/async/upload/info?campaign_id=1001&upload_id=id1");

        String result = ResponseJsonUtil.getResult(response);
        String expected = IOUtils.toString(
                getClass().getResourceAsStream(
                        "promoXlsxControllerTest/testGetPromoOffersAsyncValidationInfo/validationInfo_response.json"),
                UTF_8
        );

        JSONAssert.assertEquals(expected, result, false);
    }

    @Test
    @DbUnitDataSet(
            before = "promoXlsxControllerTest/testGetPromoOffersAsyncValidationInfo/" +
                    "validationInfo_multiPromo_before.csv"
    )
    public void testGetPromoOffersAsyncValidationInfo_multiPromo() throws IOException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/supplier/promo/offers/async/upload/info?campaign_id=1001&upload_id=id1&is_multi_promo=1");

        String result = ResponseJsonUtil.getResult(response);
        String expected = IOUtils.toString(
                getClass().getResourceAsStream(
                        "promoXlsxControllerTest/testGetPromoOffersAsyncValidationInfo/" +
                                "validationInfo_multiPromo_response.json"),
                UTF_8
        );

        JSONAssert.assertEquals(expected, result, false);
    }

    private void mockDataCampClientResponse(String promoId, long createdAt, long updatedAt) {
        doNothing().when(dataCampShopClient).addPromo(any(DataCampPromo.PromoDescription.class));

        DataCampPromo.PromoDescription promo = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .build()
                )
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(createdAt)
                                .setUpdatedAt(updatedAt)
                                .build()
                )
                .build();
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(promo)
                                        .build()
                        )
                        .build()
        ).when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    private void mockAnaplanPromoDescription(String promoId) {
        DataCampPromo.PromoDescription promo = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .setBusinessId(1)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo " + promoId)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addAllPromoCategory(
                                                        List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1)
                                                                        .setName("Category 1")
                                                                        .setMinDiscount(10)
                                                                        .build()))
                                                .build())
                                .build())
                        .setStartDate(1)
                        .setEndDate(15)
                        .build())
                .build();

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    @Test
    @DbUnitDataSet(before = "promoXlsxControllerTest/testUpload/uploadOffers_before.csv")
    public void partnerInitialPromoDescriptionTest() throws IOException {
        URL urlXls = getClass().getResource("promoXlsxControllerTest/testUpload/dummy.xlsx");
        URL urlJson = getClass().getResource("promoXlsxControllerTest/testUpload/promocodeCreationRequestDto.json");
        File fileXls = new File(getClass()
                .getResource("promoXlsxControllerTest/testUpload/testUpload.xlsm").getPath());
        File fileJson = new File(getClass()
                .getResource("promoXlsxControllerTest/testUpload/promocodeCreationRequestDto.json").getPath());
        InputStream inputStreamXls = new FileInputStream(fileXls);
        InputStream inputStreamJson = new FileInputStream(fileJson);
        String key = "key";
        String bucketName = "bucket";
        when(resourceLocationFactory.createLocation(any()))
                .thenReturn(ResourceLocation.create(bucketName, key));
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStreamXls, null));
        when(amazonS3.getUrl(any(), any())).thenReturn(urlXls);
        when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
        final FileSystemResource resourceXls = new FileSystemResource(urlXls.getPath());
        final FileSystemResource resourceJson = new FileSystemResource(urlJson.getPath());
        final HttpEntity<?> upload = FunctionalTestHelper.createMultipartHttpEntity(
                Map.of("upload", resourceXls,
                        "promocodeCreationRequestDto", resourceJson),
                params -> {
                }
        );
        String s = baseUrl + "/supplier/promo/offers/async/upload-promocode?campaign_id=1001";
        final ResponseEntity<String> resp = FunctionalTestHelper.post(s, upload);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        ReportInfo<ReportsType> reportInfo = asyncReportsService.getReportInfo("777");
        String promoDescriptionJson = reportInfo.getReportRequest().getParams().get("promoDescription").toString();
        var targetPromoDescriptionBuilder = DataCampPromo.PromoDescription.newBuilder();
        JsonFormat.parser().merge(promoDescriptionJson, targetPromoDescriptionBuilder);
        DataCampPromo.PromoDescription targetPromoDescription = targetPromoDescriptionBuilder.build();

        DataCampPromo.PromoDescription promoDescription = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(1)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setPromoId("1_pcd123")
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                )
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(1617483600)
                                .setEndDate(1617656399)
                )
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setName("Промокод pcd123 на скидку")
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setMarketPromocode(
                                        DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                                .setPromoCode("pcd123")
                                                .setValue(50)
                                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                                                .setApplyingType(DataCampPromo.PromoMechanics.MarketPromocode.ApplyingType.ONE_TIME)
                                )
                )
                .build();
        Assertions.assertEquals(promoDescription.getPrimaryKey(), targetPromoDescription.getPrimaryKey());
        Assertions.assertEquals(promoDescription.getPromoGeneralInfo().getPromoType(), targetPromoDescription.getPromoGeneralInfo().getPromoType());
        Assertions.assertEquals(promoDescription.getConstraints().getStartDate(), targetPromoDescription.getConstraints().getStartDate());
        Assertions.assertEquals(promoDescription.getConstraints().getEndDate(), targetPromoDescription.getConstraints().getEndDate());
        Assertions.assertEquals(promoDescription.getAdditionalInfo().getName(), targetPromoDescription.getAdditionalInfo().getName());
        Assertions.assertEquals(promoDescription.getMechanicsData().getMarketPromocode(), targetPromoDescription.getMechanicsData().getMarketPromocode());

        inputStreamXls.close();
        inputStreamJson.close();
    }

    @Test
    @DbUnitDataSet(before = "promoXlsxControllerTest/testUpload/uploadOffers_before.csv")
    public void partnerCheapestAsGiftInitialPromoDescriptionTest() throws IOException {
        URL urlXls = getClass().getResource("promoXlsxControllerTest/testUpload/dummy.xlsx");
        URL urlJson = getClass().getResource("promoXlsxControllerTest/testUpload/partnerCheapestAsGiftCreationRequestDto.json");
        File fileXls = new File(getClass()
                .getResource("promoXlsxControllerTest/testUpload/testUpload.xlsm").getPath());
        File fileJson = new File(getClass()
                .getResource("promoXlsxControllerTest/testUpload/partnerCheapestAsGiftCreationRequestDto.json").getPath());
        InputStream inputStreamXls = new FileInputStream(fileXls);
        InputStream inputStreamJson = new FileInputStream(fileJson);
        String key = "key";
        String bucketName = "bucket";
        when(resourceLocationFactory.createLocation(any()))
                .thenReturn(ResourceLocation.create(bucketName, key));
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStreamXls, null));
        when(amazonS3.getUrl(any(), any())).thenReturn(urlXls);
        when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
        final FileSystemResource resourceXls = new FileSystemResource(urlXls.getPath());
        final FileSystemResource resourceJson = new FileSystemResource(urlJson.getPath());
        final HttpEntity<?> upload = FunctionalTestHelper.createMultipartHttpEntity(
                Map.of("upload", resourceXls,
                        "partnerCheapestAsGiftCreationRequestDto", resourceJson),
                params -> {
                }
        );
        String s = baseUrl + "/supplier/promo/offers/async/upload-cheapest-as-gift?campaign_id=1001";
        final ResponseEntity<String> resp = FunctionalTestHelper.post(s, upload);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        ReportInfo<ReportsType> reportInfo = asyncReportsService.getReportInfo("777");
        String promoDescriptionJson = reportInfo.getReportRequest().getParams().get("promoDescription").toString();
        var targetPromoDescriptionBuilder = DataCampPromo.PromoDescription.newBuilder();
        JsonFormat.parser().merge(promoDescriptionJson, targetPromoDescriptionBuilder);
        DataCampPromo.PromoDescription targetPromoDescription = targetPromoDescriptionBuilder.build();

        DataCampPromo.PromoDescription promoDescription = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(1)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setPromoId("1_CAG_1626415324")
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                )
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .addOffersMatchingRules(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                                .setSupplierRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                                                                .setSuppliers(
                                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                                .addId(1)
                                                                )
                                                )
                                                .setWarehouseRestriction(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                                .setWarehouse(
                                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                                .addId(142)
                                                                )
                                                ))
                                .setStartDate(1617483600)
                                .setEndDate(1617656399)
                )
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setName("Самый дешевый товар в подарок 3=4")
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setCheapestAsGift(
                                        DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                                .setCount(4)
                                )
                )
                .build();
        Assertions.assertEquals(promoDescription.getPrimaryKey(), targetPromoDescription.getPrimaryKey());
        Assertions.assertEquals(promoDescription.getPromoGeneralInfo().getPromoType(), targetPromoDescription.getPromoGeneralInfo().getPromoType());
        Assertions.assertEquals(promoDescription.getConstraints().getStartDate(), targetPromoDescription.getConstraints().getStartDate());
        Assertions.assertEquals(promoDescription.getConstraints().getEndDate(), targetPromoDescription.getConstraints().getEndDate());
        Assertions.assertEquals(promoDescription.getConstraints().getOffersMatchingRules(0), targetPromoDescription.getConstraints().getOffersMatchingRules(0));
        Assertions.assertEquals(promoDescription.getAdditionalInfo().getName(), targetPromoDescription.getAdditionalInfo().getName());
        Assertions.assertEquals(promoDescription.getMechanicsData().getCheapestAsGift(), targetPromoDescription.getMechanicsData().getCheapestAsGift());

        inputStreamXls.close();
        inputStreamJson.close();
    }
}

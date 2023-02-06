package ru.yandex.vendor.modeleditor.feed.template;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.ProtocolMessage;
import ru.yandex.vendor.modeleditor.feed.template.model.FeedStatus;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeed;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeedDetails;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeedInfo;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeedMessage;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeedUpload;
import ru.yandex.vendor.modeleditor.mbo.MboAuthenticatedPartner;
import ru.yandex.vendor.partner.Partner;
import ru.yandex.vendor.partner.RequestorType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Instant;
import java.util.List;

@ParametersAreNonnullByDefault
class MboPartnerContentConversionsTest {
    @Test
    void testDataWithNullSKU() {
        List<XlsFeedMessage.Details> details =
                MboPartnerContentConversions.toXlsFeedMessageDetails(ProtocolMessage.Message.newBuilder()
                        .setCode("ir.partner_content.error.absent_mandatory_parameter")
                        .setTemplate(
                                "Missing mandatory parameter {{paramName}} for {{shopSKU}} shop SKU at {{rowIndex}} row")
                        .setParams("{\"shopSKU\": null, \"paramName\": \"Размер монитора\", \"rowIndex\": \"13\"}")
                        .build());
        Assertions.assertEquals(1, details.size());
        XlsFeedMessage.Details expectedDetails =
                new XlsFeedMessage.Details.Builder().setParamName("Размер монитора").setRowIndex(13).build();
        Assertions.assertEquals(expectedDetails, details.get(0));
    }

    @Test
    void testDataWithAllNull() {
        List<XlsFeedMessage.Details> details =
                MboPartnerContentConversions.toXlsFeedMessageDetails(ProtocolMessage.Message.newBuilder()
                        .setCode("ir.partner_content.error.absent_mandatory_parameter")
                        .setTemplate(
                                "Missing mandatory parameter {{paramName}} for {{shopSKU}} shop SKU at {{rowIndex}} row")
                        .setParams("{\"shopSKU\": null, \"paramName\": null, \"rowIndex\": null}")
                        .build());
        Assertions.assertEquals(1, details.size());
        XlsFeedMessage.Details expectedDetails =
                new XlsFeedMessage.Details.Builder().build();
        Assertions.assertEquals(expectedDetails, details.get(0));
    }

    @Test
    void testDataWithAllNotNull() {
        List<XlsFeedMessage.Details> details =
                MboPartnerContentConversions.toXlsFeedMessageDetails(ProtocolMessage.Message.newBuilder()
                        .setCode("ir.partner_content.error.absent_mandatory_parameter")
                        .setTemplate(
                                "Missing mandatory parameter {{paramName}} for {{shopSKU}} shop SKU at {{rowIndex}} row")
                        .setParams("{\"shopSKU\": \"ABBA123\", \"paramName\": \"Цвет\", \"rowIndex\": 23}")
                        .build());
        Assertions.assertEquals(1, details.size());
        XlsFeedMessage.Details expectedDetails =
                new XlsFeedMessage.Details.Builder()
                        .setShopSku("ABBA123")
                        .setParamName("Цвет")
                        .setRowIndex(23)
                        .build();
        Assertions.assertEquals(expectedDetails, details.get(0));
    }

    XlsFeedInfo mockFeedInfo() {
        return XlsFeedInfo.of(
                new XlsFeed.Builder()
                        .setFeedStatus(FeedStatus.ERROR)
                        .setId(1).setMboProcessId(1)
                        .setPartner(MboAuthenticatedPartner.of(Partner.of(10, RequestorType.SHOP), 10))
                        .setUpdatedAt(Instant.now()).setUploadId(100).build(),
                new XlsFeedUpload.Builder()
                        .setUploadId(100)
                        .setUploadTimestamp(Instant.now())
                        .setContentType("application/octet-stream")
                        .setFileName("file")
                        .setFileSizeInBytes(123)
                        .setUrl("url.ru")
                        .build(),
                Instant.now()
        );
    }

    @Test
    void testAllInvalidBuckets() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.ERROR, details.info().feedStatus());
    }

    @Test
    void testAllFinishedBuckets() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.OK, details.info().feedStatus());
    }

    @Test
    void testOneMixedBucket() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.MIXED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.MIXED, details.info().feedStatus());
    }

    @Test
    void testOnlyMixedBuckets() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.MIXED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.MIXED)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.MIXED, details.info().feedStatus());
    }

    @Test
    void testFinishedAndInvalidAndMixedBuckets() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.MIXED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.MIXED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.MIXED, details.info().feedStatus());
    }

    @Test
    void testInvalidAndFinishedBuckets() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.MIXED, details.info().feedStatus());
    }

    @Test
    void testInvalidRequest() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.INVALID)
                .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                        .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                        .build())
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.ERROR, details.info().feedStatus());
    }

    @Test
    void testProcessingRequest() {
        XlsFeedInfo feedInfo = mockFeedInfo();
        PartnerContent.FileInfoResponse response = PartnerContent.FileInfoResponse.newBuilder()
                .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.PROCCESSING)
                .build();
        XlsFeedDetails details = MboPartnerContentConversions.toXlsFeedDetails(feedInfo, response, Instant.now());
        Assertions.assertEquals(FeedStatus.PROCESSING, details.info().feedStatus());
    }
}

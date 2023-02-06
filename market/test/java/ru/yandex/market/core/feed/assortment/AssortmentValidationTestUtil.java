package ru.yandex.market.core.feed.assortment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import ru.yandex.market.Magics;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.protobuf.tools.MagicChecker;

public class AssortmentValidationTestUtil {
    public static URL mockTotalUrl() throws IOException {
        Path tempFile = Files.createTempFile("total", "pbuf");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            MboMappings.OfferExcelUpload.TotalResult totalResult =
                    MboMappings.OfferExcelUpload.TotalResult.newBuilder()
                            .setStatus(MboMappings.OfferExcelUpload.Status.WARNING)
                            .setUploadStatistics(MboMappings.OfferExcelUpload.UploadStatistics.newBuilder()
                                    .setErrorsSkipped(1)
                                    .setExistingOffers(MboMappings.OfferStatusStatistics.newBuilder()
                                            .setApproved(1)
                                            .setInWork(1)
                                            .setRejected(1)
                                            .build())
                                    .setNewOffers(1)
                                    .setTotal(5)
                                    .build())
                            .build();
            fileOutputStream.write(totalResult.toByteArray());
        }
        return tempFile.toUri().toURL();
    }

    public static URL mockTotalUrlWithError() throws IOException {
        Path tempFile = Files.createTempFile("total", "pbuf");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            MboMappings.OfferExcelUpload.TotalResult totalResult =
                    MboMappings.OfferExcelUpload.TotalResult.newBuilder()
                            .setStatus(MboMappings.OfferExcelUpload.Status.ERROR)
                            .setStatusMessage(MbocCommon.Message.newBuilder()
                                    .setMessageCode("message")
                                    .setMustacheTemplate("Template {}")
                                    .setJsonDataForMustacheTemplate("data")
                                    .build())
                            .build();
            fileOutputStream.write(totalResult.toByteArray());
        }
        return tempFile.toUri().toURL();
    }

    public static URL mockDetailsUrl() throws IOException {
        Path detailsFile = Files.createTempFile("details", "pbuf");
        try (FileOutputStream fileOutputStream = new FileOutputStream(detailsFile.toFile())) {
            fileOutputStream.write(MagicChecker.magicToBytes(Magics.MagicConstants.MPOM.name().toUpperCase()));
            MboMappings.OfferExcelUpload.ParsedOfferWithMessages offer =
                    MboMappings.OfferExcelUpload.ParsedOfferWithMessages.newBuilder()
                            .setExcelDataLineIndex(1)
                            .setOffer(SupplierOffer.Offer.newBuilder()
                                    .setTitle("145 147 true false")
                                    .setSupplierId(10101L)
                                    .setShopSkuId("145_147_true_false")
                                    .build())
                            .addItemMessages(MboMappings.OfferExcelUpload.LiteItemMessage.newBuilder()
                                    .setMessage(MbocCommon.Message.newBuilder()
                                            .setMessageCode("resp error code 1")
                                            .setMustacheTemplate("resp error template 1")
                                            .build())
                                    .setEmergency(MboMappings.OfferExcelUpload.Status.ERROR))
                            .build();
            offer.writeDelimitedTo(fileOutputStream);
        }
        return detailsFile.toUri().toURL();
    }

    public static URL mockDetailsUrlWithError() throws IOException {
        Path detailsFile = Files.createTempFile("details", "pbuf");
        try (FileOutputStream fileOutputStream = new FileOutputStream(detailsFile.toFile())) {
            fileOutputStream.write(MagicChecker.magicToBytes(Magics.MagicConstants.MPOM.name().toUpperCase()));
        }
        return detailsFile.toUri().toURL();
    }
}

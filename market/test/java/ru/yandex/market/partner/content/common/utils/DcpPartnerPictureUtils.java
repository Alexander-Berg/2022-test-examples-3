package ru.yandex.market.partner.content.common.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;

public class DcpPartnerPictureUtils {


    public static final String IDX_PREFIX = "idx_";
    public static final String MBO_PREFIX = "mbo_";

    public static DcpPartnerPicture mboUploadPictureResult(String baseName, boolean isCwValidationOk) {
        return mboUploadPictureResult(baseName, isCwValidationOk, null);
    }

    public static DcpPartnerPicture mboUploadPictureResult(String baseName, boolean isCwValidationOk, UUID md5) {
        return new DcpPartnerPicture(
                null,
                IDX_PREFIX + baseName,
                null,
                null,
                ModelStorage.Picture.newBuilder().setUrl(MBO_PREFIX + baseName)
                        .setOrigMd5(md5 != null ? md5.toString() : "")
                        .build(),
                null,
                Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                isCwValidationOk,
                false,
                md5);
    }

    public static DcpPartnerPicture mboUploadPictureForFastCard(String baseName) {
        return new DcpPartnerPicture(
                null,
                IDX_PREFIX + baseName,
                null,
                null,
                null,
                1L,
                Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                false,
                false,
                null
        );
    }

    public static DcpPartnerPicture mboUploadPictureError(String baseName) {
        return new DcpPartnerPicture(
                null,
                IDX_PREFIX + baseName,
                null,
                ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                                .setType(ModelStorage.ValidationErrorType.INVALID_IMAGE_FORMAT)
                                .build())
                        .build(),
                null,
                null,
                Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                true,
                false,
                null
        );
    }

    public static String testImgUrlForOffer(int bizId, String offerId) {
        return String.format("test-partner.market.yandex.ru/%d/%s.jpg", bizId, offerId);
    }
}

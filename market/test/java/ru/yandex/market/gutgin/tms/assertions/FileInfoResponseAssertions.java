package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContent.ProcessRequestStatus;

import java.util.Optional;

import static ru.yandex.market.ir.http.PartnerContent.FileInfoResponse;

/**
 * @author danfertev
 * @since 29.07.2019
 */
public class FileInfoResponseAssertions
    extends AbstractObjectAssert<FileInfoResponseAssertions, FileInfoResponse> {

    public FileInfoResponseAssertions(FileInfoResponse fileInfoResponse) {
        super(fileInfoResponse, FileInfoResponseAssertions.class);
    }

    public FileInfoResponseAssertions hasStatus(ProcessRequestStatus expectedStatus) {
        super.isNotNull();
        Assertions.assertThat(actual.getProcessRequestStatus()).isEqualTo(expectedStatus);
        return myself;
    }

    public DataBucketAssertions getDataBucket(long categoryId) {
        super.isNotNull();
        Optional<PartnerContent.BucketProcessInfo> dataBucketO = actual.getBucketProcessInfoList().stream()
            .filter(b -> b.getCategoryId() == categoryId)
            .findFirst();
        Assertions.assertThat(dataBucketO).isNotEmpty();
        return new DataBucketAssertions(dataBucketO.get());
    }
}

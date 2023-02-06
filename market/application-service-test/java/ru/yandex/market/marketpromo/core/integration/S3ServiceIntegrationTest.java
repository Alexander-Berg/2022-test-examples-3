package ru.yandex.market.marketpromo.core.integration;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.service.S3Service;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.S3_ACTIVE;

@ActiveProfiles(S3_ACTIVE)
@TestPropertySource(
        properties = {
                "market.marketpromo.ciface-promo.s3.endpoint=https://s3.mds.yandex.net",
                "market.marketpromo.ciface-promo.s3.accessKey={key}",
                "market.marketpromo.ciface-promo.s3.secretKey={secret}",
                "market.marketpromo.ciface-promo.s3.bucketName=market-promo",
        }
)
@Disabled
public class S3ServiceIntegrationTest extends ServiceTestBase {

    @Value("classpath:/proto/promo_description_proto.bin")
    private Resource resourceToUpload;

    @Autowired
    private S3Service s3Service;

    @Test
    void shouldUploadFileToS3Storage() throws IOException {
        String token = s3Service.uploadAsExportFile(requireNonNull(resourceToUpload.getFilename()),
                IOUtils.toByteArray(resourceToUpload.getURL()));

        assertThat(token, Matchers.not(Matchers.emptyString()));
    }
}

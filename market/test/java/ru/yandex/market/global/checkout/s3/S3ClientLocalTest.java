package ru.yandex.market.global.checkout.s3;

import java.io.ByteArrayInputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.config.properties.S3Properties;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class S3ClientLocalTest extends BaseLocalTest {

    private static final String TEST_FILE_KEY = "test_file.txt";

    private final AmazonS3 s3Client;
    private final S3Properties s3Properties;

    @Test
    void test() {
        PutObjectResult result = s3Client.putObject(new PutObjectRequest(
                s3Properties.getPaymentConfirmationBucket(),
                TEST_FILE_KEY,
                new ByteArrayInputStream("Hello world!\n".getBytes()),
                new ObjectMetadata()
        ));
        log.info("Request complete {}", result.getContentMd5());
    }

}

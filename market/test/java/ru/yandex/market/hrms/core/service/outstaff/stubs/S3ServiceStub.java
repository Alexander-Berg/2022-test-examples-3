package ru.yandex.market.hrms.core.service.outstaff.stubs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import lombok.Getter;

import ru.yandex.market.hrms.core.service.s3.S3Service;

public class S3ServiceStub extends S3Service {

    @Getter
    private int putObjectCalled = 0;

    @Getter
    private int getObjectCalled = 0;

    private Map<String, byte[]> testData = new HashMap<>();

    public S3ServiceStub(AmazonS3 s3Client) {
        super(s3Client);
    }

    public void resetCounters() {
        putObjectCalled = 0;
        getObjectCalled = 0;
    }

    public void withData(String bucketName, String key, byte[] object) {
        testData.put(bucketName + "_" + key, object);
    }

    @Override
    public void putObject(String bucketName, String key, byte[] object) {
        putObjectCalled++;
    }

    @Override
    public Optional<byte[]> getObject(String bucketName, String key) {
        getObjectCalled++;

        if (testData.containsKey(bucketName + "_" + key)) {
            return Optional.of(testData.get(bucketName + "_" + key));
        }

        return Optional.empty();
    }

    @Override
    public void deleteObject(String bucketName, String key) {
        getObjectCalled++;
    }
}

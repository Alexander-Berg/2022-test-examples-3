package ru.yandex.market.jmf.mds.test.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.jmf.mds.MdsLocation;
import ru.yandex.market.jmf.mds.MdsService;
import ru.yandex.market.jmf.mds.UploadResult;

@Primary
@Service
public class MockMdsService implements MdsService {

    private String mdsHost;
    private final List<Pair<String, String>> deletedBucketKeys = new ArrayList<>();
    private final List<Pair<String, String>> addedBucketKeys = new ArrayList<>();

    public void setMdsHost(String mdsHost) {
        this.mdsHost = mdsHost;
    }

    public boolean isDeletedBucketKey(String bucketName, String key) {
        return deletedBucketKeys.contains(new Pair<>(bucketName, key));
    }

    public boolean isAddedBucketKey(String bucketName, String key) {
        return addedBucketKeys.contains(new Pair<>(bucketName, key));
    }

    public int getDeletedCount() {
        return deletedBucketKeys.size();
    }

    @Override
    public UploadResult upload(InputStream stream) {
        long size = -1;
        try {
            size = stream.readAllBytes().length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        String url = "url";
        addedBucketKeys.add(new Pair<>(null, url));
        return new UploadResult(url, null, size);
    }

    @Override
    public UploadResult upload(InputStream stream, String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadResult upload(InputStream stream, Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadResult upload(InputStream stream, ResourceLocation resourceLocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadResult upload(URL url) {
        String bucket = mdsHost != null ? mdsHost : RandomStringUtils.randomAlphabetic(5);
        return new UploadResult(url.toString(), ResourceLocation.create(bucket, UUID.randomUUID().toString()), 0);
    }

    @Override
    public MdsLocation uploadStreamAndGetLocation(InputStream stream) {
        if (stream != null) {
            try {
                stream.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String bucket = mdsHost != null ? mdsHost : RandomStringUtils.randomAlphabetic(5);
        String key = UUID.randomUUID().toString();
        String url = "http://" + bucket + "/" + key;

        addedBucketKeys.add(new Pair<>(bucket, key));
        return new MdsLocation(ResourceLocation.create(bucket, key), url);
    }

    @Override
    public void delete(String... keys) {
        delete(null, keys);
    }

    @Override
    public void delete(String bucketName, String... keys) {
        for (String key : keys) {
            deletedBucketKeys.add(new Pair<>(bucketName, key));
        }
    }
}

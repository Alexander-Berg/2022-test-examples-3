package ru.yandex.market.checkout.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.checkout.checkouter.service.mds.MdsS3Service;

@TestComponent("mdsS3Service")
public class StubMdsS3ServiceImpl implements MdsS3Service {

    public static final String STUB_MDS_URL_PREFIX = "http://blablabla/";

    private final Map<String, byte[]> savedFiles = new HashMap<>();

    @Override
    public String uploadFile(String key, MultipartFile file) {
        return keyToUrl(key);
    }

    @Override
    public String uploadFile(String key, InputStream fileStream) {
        try {
            byte[] file = IOUtils.toByteArray(fileStream);
            savedFiles.put(keyToUrl(key), file);
            return keyToUrl(key);
        } catch (Exception ignore) {
            throw new RuntimeException();
        }
    }

    @Override
    public void downloadFileWithUrl(String url, OutputStream outputStream) {
        try {
            outputStream.write(savedFiles.get(url));
        } catch (Exception ignore) {

        }
    }

    @Override
    public void downloadFileWithKey(String key, OutputStream outputStream) {
        try {
            outputStream.write(savedFiles.get(keyToUrl(key)));
        } catch (Exception ignore) {

        }
    }

    @Nonnull
    private String keyToUrl(String key) {
        return STUB_MDS_URL_PREFIX + key;
    }
}

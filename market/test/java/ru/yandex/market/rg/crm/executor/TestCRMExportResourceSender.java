package ru.yandex.market.rg.crm.executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.util.io.ResourceSender;

/**
 * @author otedikova
 */
public class TestCRMExportResourceSender implements ResourceSender {
    private String uploadedData;

    @Override
    public void sendResource(@Nonnull Resource resource) {
        GZIPInputStream gzis = null;
        try {
            gzis = new GZIPInputStream(resource.getInputStream());
            InputStreamReader reader = new InputStreamReader(gzis);
            BufferedReader in = new BufferedReader(reader);
            StringBuilder stringBuilder = new StringBuilder();
            String readed;
            while ((readed = in.readLine()) != null) {
                stringBuilder.append(readed);
            }
            uploadedData = stringBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(gzis);
        }
    }

    public String getUploadedData() {
        return uploadedData;
    }

}

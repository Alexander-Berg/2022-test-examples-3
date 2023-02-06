package ru.yandex.market.mbo.cms.exporter.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;

public class ExporterTestUtils {
    private ExporterTestUtils() {
    }

    @SuppressWarnings("magicnumber")
    public static HttpResponse makeOkHttpResponse(String content) {
        BasicHttpResponse response = new BasicHttpResponse(
                new ProtocolVersion("1", 2, 3),
                200, "");
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream is;
        try {
            is = IOUtils.toInputStream(content, "UTF-8");
            entity.setContent(is);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
        response.setEntity(entity);
        return response;
    }
}

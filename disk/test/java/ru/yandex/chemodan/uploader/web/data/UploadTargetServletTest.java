package ru.yandex.chemodan.uploader.web.data;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.uploader.web.AbstractWebTestSupport;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class UploadTargetServletTest extends AbstractWebTestSupport {

    @Autowired
    private UploadTargetServlet servlet;

    @Test
    public void urlEncodeDocumentPath() {
        String path = "123:/тест/папка/dir/файл 2.jpg";
        String location = UploadTargetServlet.urlEncodeDocumentPath(path);
        Assert.equals(
                "/%D1%82%D0%B5%D1%81%D1%82/%D0%BF%D0%B0%D0%BF%D0%BA%D0%B0/dir/%D1%84%D0%B0%D0%B9%D0%BB%202.jpg",
                location);
    }

}

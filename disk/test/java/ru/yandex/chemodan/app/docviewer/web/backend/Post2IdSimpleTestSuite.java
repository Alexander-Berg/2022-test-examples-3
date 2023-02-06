package ru.yandex.chemodan.app.docviewer.web.backend;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.chemodan.app.docviewer.AbstractSpringAwareTest;
import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.FileUtils;
import ru.yandex.misc.io.IoFunction1V;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

@RunWith(Parameterized.class)
public class Post2IdSimpleTestSuite extends AbstractSpringAwareTest {

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (URL resource : new URL[] {
                //
                TestResources.Adobe_Acrobat_1_3_001p, //
                TestResources.Adobe_Acrobat_1_4_001p_limited, //
                TestResources.Adobe_Acrobat_1_4_004p, //
                TestResources.Adobe_Acrobat_1_5_114p, //

                TestResources.Microsoft_Excel_97_001p, //
                TestResources.Microsoft_Excel_97_001p_bug, //
//              too huge for CI
//                TestResources.Microsoft_Excel_97_565p, //
//                TestResources.Microsoft_Excel_97_582p, //

                TestResources.Microsoft_Word_95_001p, //
                TestResources.Microsoft_Word_97_001p, //
                TestResources.Microsoft_Word_97_002p, //
                TestResources.Microsoft_Word_97_046p, //
//                TestResources.Microsoft_Word_97_102p, //
                TestResources.Microsoft_Word_12_001p, //
        })
        {
            data.add(new Object[] { resource, "post2html", MimeTypes.MIME_HTML + ";charset=UTF-8" });
            data.add(new Object[] { resource, "post2pdf", MimeTypes.MIME_PDF});
            data.add(new Object[] { resource, "post2txt", MimeTypes.MIME_TEXT_PLAIN });
        }
        return data;
    }

    private final String actionPath;

    private final String expectedResultContentType;

    private final URL resource;

    public Post2IdSimpleTestSuite(URL resource, String actionPath, String expectedResultContentType) {
        super();
        this.resource = resource;
        this.actionPath = actionPath;
        this.expectedResultContentType = expectedResultContentType;
    }

    @Test
    public void test() {
        FileUtils.withTemporaryFile("test", ".bin", new UrlInputStreamSource(resource),
                (IoFunction1V<File2>) temporaryInputFile -> {
                    HttpPost httpPost = new HttpPost("http://localhost:32405/" + actionPath);

                    FileEntity reqEntity = new FileEntity(temporaryInputFile.getFile(),
                            MimeTypes.MIME_UNKNOWN);
                    httpPost.setEntity(reqEntity);

                    ApacheHttpClient4Utils.execute(httpPost, response -> {
                        HttpEntity httpEntity = response.getEntity();
                        Assert.A.equals(expectedResultContentType.toLowerCase().trim(), httpEntity
                                .getContentType().getValue().toLowerCase().trim());
                        return null;
                    }, Timeout.seconds(120));
                });
    }

}

package ru.yandex.chemodan.app.docviewer.web.backend;

import java.awt.image.RenderedImage;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.app.docviewer.test.handlers.ReadContentTypeHandler;
import ru.yandex.chemodan.app.docviewer.utils.ImageUtils;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

/**
 * @author vlsergey
 * @author akirakozov
 *
 */
public class PreviewActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private UriHelper uriHelper;
    @Autowired
    private TestManager testManager;

    @Test
    public void getPreview() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.Microsoft_Word_97_001p.toString());
        byte[] preview = ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&width=1024&url="
                + TestResources.Microsoft_Word_97_001p);

        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
        Assert.equals(1024, image.getWidth());
    }

    @Test
    @Ignore("Not stable")
    public void checkMaxBound() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.Microsoft_Word_97_001p.toString());
        byte[] preview = ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&url="
                + TestResources.Microsoft_Word_97_001p);

        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
        Assert.equals(PreviewAction.MAX_PREVIEW_EDGE, image.getHeight());
    }

    @Test
    public void getTextFilePreview() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.TXT.toString());
        byte[] preview = ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&width=1024&url="
                + TestResources.TXT);

        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
        Assert.equals(1024, image.getWidth());
        Assert.lt(image.getWidth(), image.getHeight());
    }

    @Test
    @Ignore("Not stable")
    public void getFb2ImagePreview() {
        testManager.cleanupUri(PassportUidOrZero.zero(), TestResources.FB2_EXAMPLE.toString());
        byte[] preview = ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&url="
                + TestResources.FB2_EXAMPLE);

        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
        Assert.equals(120, image.getWidth()); // 120 via ZIPPED_HTML, 251 via PDF
    }

    @Test
    @Ignore("Not stable")
    public void checkUnsafeParam() {
        uriHelper.setDisableOriginalUrlCheck(false);
        String originalUrl = UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.TXT));

        try {
            ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&url=" + originalUrl);
            Assert.fail("403 error is expected");
        } catch (HttpException exc) {
            Assert.assertTrue(exc.statusCodeIs(403));
        }

        byte[] preview = ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&unsafe=true&url=" + originalUrl);
        RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
        Assert.isTrue(image.getWidth() > 0);
    }

    @Test
    public void previewFileFromMulca() {
        // Check that content-type parameter used
        ByteArrayInputStreamSource baiss = new ByteArrayInputStreamSource(new String("lala").getBytes());

        testManager.withUploadedToMulcaFile(baiss, false, mulcaId -> {
            String url = UrlUtils.addParameters("http://localhost:32405/preview",
                    Cf.<String, Object>map("uid", 0)
                            .plus1("unsafe", true)
                            .plus1("url", mulcaId.asMulcaUri())
                            .plus1("content-type", "text/plain"));
            byte[] preview = ApacheHttpClientUtils.download(url);
            RenderedImage image = ImageUtils.read(new ByteArrayInputStreamSource(preview));
            Assert.isTrue(image.getWidth() > 0);
        });
    }

    @Test
    public void checkUnsupportedMediaTypeError() {
        ByteArrayInputStreamSource baiss = new ByteArrayInputStreamSource(
                Random2.R.nextAlnum(10).getBytes());

        testManager.withUploadedToMulcaFile(baiss, false, mulcaId -> {
            String url = UrlUtils.addParameters("http://localhost:32405/preview",
                    Cf.<String, Object>map("uid", 0)
                            .plus1("unsafe", true)
                            .plus1("url", mulcaId.asMulcaUri())
                            .plus1("content-type", "audio/mp3"));
            try {
                ApacheHttpClientUtils.download(url);
            } catch (HttpException e) {
                Assert.isTrue(e.statusCodeIs(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
            }
        });
    }

    @Test
    @Ignore("Not stable")
    public void checkForbiddenError() {
        uriHelper.setDisableOriginalUrlCheck(false);
        String originalUrl = UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.Adobe_Acrobat_1_4_001p_password));
        testManager.cleanupUri(PassportUidOrZero.zero(), originalUrl);

        try {
            ApacheHttpClientUtils.download("http://localhost:32405/preview?uid=0&unsafe=true&url=" + originalUrl);
        } catch (HttpException e) {
            Assert.isTrue(e.statusCodeIs(HttpStatus.SC_FORBIDDEN));
        }
    }

    @Test
    public void shouldGeneratePngPreview() {
        uriHelper.setDisableOriginalUrlCheck(false);
        String url = UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.Adobe_Acrobat_1_4_001p));
        testManager.cleanupUri(PassportUidOrZero.zero(), url);

        String contentType = ApacheHttpClient4Utils.execute(
                new HttpGet("http://localhost:32405/preview?uid=0&unsafe=true&url=" + url),
                new ReadContentTypeHandler());

        Assert.equals(MimeTypes.MIME_IMAGE_PNG, contentType);
    }
}

package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.FileUtils;
import ru.yandex.chemodan.app.docviewer.utils.pdf.PdfUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

public class Post2IdTest extends DocviewerWebSpringTestBase {

    private static final class StripTextFromHtmlResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) {
            try {
                HttpEntity httpEntity = response.getEntity();
                Assert.A.equals(MimeTypes.MIME_HTML + ";charset=utf-8", httpEntity.getContentType()
                        .getValue());
                return InputStreamSourceUtils.wrap(httpEntity.getContent()).readText();
            } catch (Exception exc) {
                throw ExceptionUtils.translate(exc);
            }
        }
    }

    private static final class StripTextFromPdfResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) {
            try {
                HttpEntity httpEntity = response.getEntity();
                Assert.A.equals(MimeTypes.MIME_PDF, httpEntity.getContentType()
                        .getValue());

                return PdfUtils.withExistingDocument(
                        InputStreamSourceUtils.wrap(httpEntity.getContent()), true,
                        PdfUtils::stripText);
            } catch (Exception exc) {
                throw ExceptionUtils.translate(exc);
            }
        }
    }

    void test(final String reportedContentType, final String uri,
            final ResponseHandler<String> stripTextHandler)
    {
        FileUtils.withFile(new UrlInputStreamSource(TestResources.Microsoft_Word_97_001p),
                file -> {
                    HttpPost httpPost = new HttpPost(uri);

                    FileEntity reqEntity = new FileEntity(file.getFile(), reportedContentType);
                    httpPost.setEntity(reqEntity);

                    String pdfText = ApacheHttpClient4Utils.execute(httpPost, stripTextHandler,
                            Timeout.seconds(120));

                    Assert.assertContains(pdfText, "3db63cb0-92ee-4853-9f7d-1832a0da125d");
                });
    }

    @Test
    public void testToHtmlWithCorrectMimeType() {
        test(MimeTypes.MIME_MICROSOFT_WORD, "http://localhost:32405/post2html",
                new StripTextFromHtmlResponseHandler());
    }

    @Test
    public void testToHtmlWithUnknownMimeType() {
        test(MimeTypes.MIME_UNKNOWN, "http://localhost:32405/post2html",
                new StripTextFromHtmlResponseHandler());
    }

    @Test
    public void testToPdfWithCorrectMimeType() {
        test(MimeTypes.MIME_MICROSOFT_WORD, "http://localhost:32405/post2pdf",
                new StripTextFromPdfResponseHandler());
    }

    @Test
    public void testToPdfWithUnknownMimeType() {
        test(MimeTypes.MIME_UNKNOWN, "http://localhost:32405/post2pdf",
                new StripTextFromPdfResponseHandler());
    }
}

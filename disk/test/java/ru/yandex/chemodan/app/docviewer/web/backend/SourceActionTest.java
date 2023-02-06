package ru.yandex.chemodan.app.docviewer.web.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.crypt.TokenManager;
import ru.yandex.chemodan.app.docviewer.utils.HttpUtils2;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.utils.pdf.PdfUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.lang.Check;
import ru.yandex.misc.test.Assert;

public class SourceActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;
    @Autowired
    private TokenManager tokenManager;

    private static final class StripTextFromPdfResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) {
            try {
                HttpEntity httpEntity = response.getEntity();
                Assert.A.equals(MimeTypes.MIME_PDF, httpEntity.getContentType()
                        .getValue());

                byte[] doc = InputStreamSourceUtils.wrap(httpEntity.getContent()).readBytes();
                return PdfUtils.withExistingDocument(new ByteArrayInputStreamSource(doc),
                        true, PdfUtils::stripText);
            } catch (Exception exc) {
                throw ExceptionUtils.translate(exc);
            }
        }
    }

    @Test
    public void testContent() {
        String fileId = convertAndGetFileId(TestResources.Adobe_Acrobat_1_4_001p);

        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty());
        String pdfText = executeRequest(url, new StripTextFromPdfResponseHandler());

        Assert.assertContains(pdfText, "6108283e-a09e-4f78-84cf-c06e6193264");
    }

    @Test
    public void shouldReturnFileContentFromUrl() {
        String downloadUrl = UriUtils.toUrlString(TestResources.Adobe_Acrobat_1_4_001p);

        String url = getSourceUrl(PassportUidOrZero.zero(), Option.empty(), Option.of(downloadUrl));
        String pdfText = executeRequest(url, new StripTextFromPdfResponseHandler());

        Assert.assertContains(pdfText, "6108283e-a09e-4f78-84cf-c06e6193264");
    }

    @Test
    public void checkContentDispositionAttachment() {
        String contentDisposition = getContentDisposition(false);
        Assert.equals("attachment; filename*=UTF-8''PDF_1.4_001p.pdf", contentDisposition);
    }

    @Test
    public void checkContentDispositionInline() {
        String contentDisposition = getContentDisposition(true);
        // inline because file:// scheme is not supported by any url providers and considered internal
        Assert.equals("inline; filename*=UTF-8''PDF_1.4_001p.pdf", contentDisposition);
    }

    @Test
    public void checkContentDispositionForForbiddenContentType() {
        String contentDisposition = getContentDisposition(TestResources.TXT, true);
        // attachement because text/plain is forbidden for inlining
        Assert.equals("attachment; filename*=UTF-8''en.txt", contentDisposition);
    }

    @Test
    public void testContentDispositionRus() {
        String fileId = convertAndGetFileId(TestResources.Adobe_Acrobat_Rus);

        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty());
        String contentDisposition = executeAndGetHeader(url, HttpUtils2.HEADER_CONTENT_DISPOSITION);

        Assert.A.equals(
                "attachment; filename*=UTF-8''" +
                "welcome.pdf",
                contentDisposition);
    }

    @Test
    public void checkContentDispositionWithOverridedFileName() {
        String fileId = convertAndGetFileId(TestResources.Adobe_Acrobat_1_4_001p);

        String fileName = "fileName.pdf";
        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty(), Option.of(fileName));

        String contentDisposition = executeAndGetHeader(url, HttpUtils2.HEADER_CONTENT_DISPOSITION);
        Assert.equals("attachment; filename*=UTF-8''" + fileName, contentDisposition);
    }

    @Test
    public void checkContentDispositionWithEmptyFileName() {
        String fileId = convertAndGetFileId(TestResources.Adobe_Acrobat_1_4_001p);

        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty(), Option.of(""));

        String contentDisposition = executeAndGetHeader(url, HttpUtils2.HEADER_CONTENT_DISPOSITION);
        Assert.equals("attachment; filename*=UTF-8''PDF_1.4_001p.pdf", contentDisposition);
    }

    @Test
    public void doNotCheckRightsInsideArchiveFile() {
        String fileUrl = UriUtils.toUrlString(TestResources.ZIP_WITH_DOCX);
        convertAndGetFileId(TestResources.ZIP_WITH_DOCX, TargetType.HTML_WITH_IMAGES);

        String sourceUrl = getSourceUrl(PassportUidOrZero.zero(),
                Option.empty(), Option.of(fileUrl), Option.of("//Word_12_001p.docx"),
                Option.empty(), false);

        executeRequest(sourceUrl, response -> null);
    }

    @Test
    public void checkFileTooBigError() {
        File zip = generateZipWithFile("file", 420 << 20);
        try {
            URL url = zip.toURI().toURL();
            convertAndGetFileId(url, TargetType.HTML_WITH_IMAGES);
            String sourceUrl = getSourceUrl(PassportUidOrZero.zero(),
                    Option.empty(), Option.of(UriUtils.toUrlString(url)), Option.of("//file"),
                    Option.empty(), false);

            executeRequest(sourceUrl, response -> {
                Check.equals(500, response.getStatusLine().getStatusCode());
                Check.equals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<file-too-big><limit-length>94371840</limit-length></file-too-big>",
                        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));
                return null;
            });
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        } finally {
            Check.isTrue(zip.delete());
        }
    }

    @Test
    public void testRequestWithoutToken() {
        String fileId = convertAndGetFileId(TestResources.Adobe_Acrobat_1_4_001p);
        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty(), Option.empty(),
                Option.empty(), false, false);

        int status = executeAndGetStatus(url);
        Assert.assertEquals(status, 403);
    }

    private String getContentDisposition(boolean inline) {
        return getContentDisposition(TestResources.Adobe_Acrobat_1_4_001p, inline);
    }

    private String getContentDisposition(URL source, boolean inline) {
        String fileId = convertAndGetFileId(source);

        String url = getSourceUrl(PassportUidOrZero.zero(), Option.of(fileId), Option.empty(), inline);
        return executeAndGetHeader(url, HttpUtils2.HEADER_CONTENT_DISPOSITION);
    }

    private String getSourceUrl(PassportUidOrZero uid, Option<String> fileId, Option<String> url) {
        return getSourceUrl(uid, fileId, url, Option.empty(), Option.empty(), false);
    }

    private String getSourceUrl(
            PassportUidOrZero uid, Option<String> fileId,
            Option<String> url, Option<String> fileName)
    {
        return getSourceUrl(uid, fileId, url, Option.empty(), fileName, false);
    }

    private String getSourceUrl(PassportUidOrZero uid, Option<String> fileId, Option<String> url, boolean inline) {
        return getSourceUrl(uid, fileId, url, Option.empty(), Option.empty(), inline);
    }

    private String getSourceUrl(
            PassportUidOrZero uid, Option<String> fileId, Option<String> url, Option<String> archivePath,
            Option<String> fileName, boolean inlineDisposition)
    {
        return getSourceUrl(uid, fileId, url, archivePath, fileName, inlineDisposition, true);
    }

    private String getSourceUrl(
            PassportUidOrZero uid, Option<String> fileId, Option<String> url, Option<String> archivePath,
            Option<String> fileName, boolean inlineDisposition, boolean useToken)
    {
        Tuple2<String, String> tokenAndTs = tokenManager.getTokenAndTs(fileId, url, archivePath, uid);

        MapF<String, Object> params = Cf.<String, Object>map("id", fileId.getOrNull())
                .plus1("url", url.getOrNull())
                .plus1("archive-path", archivePath.getOrNull())
                .plus1("uid", uid.getUid())
                .plus1("ts", tokenAndTs.get2());

        if (useToken) {
            params = params.plus1("token", tokenAndTs.get1());
        }

        if (fileName.isPresent()) {
            params = params.plus1("name", fileName.get());
        }

        if (inlineDisposition) {
            params = params.plus1("inline", true);
        }

        return UrlUtils.addParameters("http://localhost:32405/source", params);
    }

    private <T> T executeRequest(String url, ResponseHandler<T> handler) {
        return ApacheHttpClient4Utils.execute(new HttpGet(url), handler, Timeout.seconds(30));
    }

    private String executeAndGetHeader(String url, String header) {
        return executeRequest(url, HttpUtils2.getFirstHeaderValueResponseHander(header));
    }

    private int executeAndGetStatus(String url) {
        return executeRequest(url, response -> response.getStatusLine().getStatusCode());
    }

    private String convertAndGetFileId(URL source) {
        return convertAndGetFileId(source, TargetType.PDF);
    }

    private String convertAndGetFileId(URL source, TargetType target) {
        return convertAndGetFileId(source, target, Option.empty());
    }

    private String convertAndGetFileId(URL source, TargetType target, Option<String> archivePath) {
        return testManager.makeAvailable(PassportUidOrZero.zero(), UriUtils.toUrlString(source), archivePath, target);
    }

    private static File generateZipWithFile(String name, int size) {
        try {
            final byte[] block = new byte[1 << 20];
            File zip = File.createTempFile("dv-test-", ".zip");
            try (ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zip))) {
                stream.putNextEntry(new ZipEntry(name));
                while (size >= block.length) {
                    stream.write(block);
                    size -= block.length;
                }
                if (size > 0) {
                    stream.write(block, 0, size);
                }
                stream.closeEntry();
            } catch (Exception e) {
                zip.delete();
                throw e;
            }
            return zip;
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

}

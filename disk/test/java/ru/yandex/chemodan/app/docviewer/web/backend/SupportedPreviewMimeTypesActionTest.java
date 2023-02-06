package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.misc.bender.Bender;
import ru.yandex.misc.bender.parse.BenderJsonParser;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.docviewer.web.backend.SupportedPreviewMimeTypesAction.ConvertationInfo;

/**
 * @author Vsevolod Tolstopyatov (qwwdfsad)
 */
public class SupportedPreviewMimeTypesActionTest extends DocviewerWebSpringTestBase {

    @Value("${copier.maxFileLength}")
    private DataSize ds;

    private static final BenderJsonParser<ConvertationInfo> parser = Bender.jsonParser(ConvertationInfo.class);

    @Test
    public void testAvailableMimeTypes() {
        HttpGet post = new HttpGet("http://localhost:32405/preview-supported");
        byte[] serializedResult = ApacheHttpClient4Utils.execute(post, new ReadBytesResponseHandler());

        ConvertationInfo result = parser.parseJson(serializedResult);
        Assert.equals(ds, result.maxFileSize);
        Assert.isTrue(result.previewMimeTypes.containsAllTs(MimeTypes.SYNONYMS_ADOBE_ACROBAT));
        Assert.isTrue(result.previewMimeTypes.containsAllTs(MimeTypes.SYNONYMS_DJVU));
        Assert.isFalse(result.previewMimeTypes.containsTs(MimeTypes.MIME_UNKNOWN));
        Assert.isFalse(result.previewMimeTypes.containsTs(MimeTypes.MIME_ARCHIVE_ZIP));
    }
}

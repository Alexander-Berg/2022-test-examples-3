package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.storages.mulca.MulcaFileLink;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.chemodan.app.docviewer.utils.pdf.image.PdfHelper;
import ru.yandex.chemodan.app.docviewer.utils.pdf.image.PdfRenderTargetTypeHolder;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.chemodan.util.json.JsonNodeUtils;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;

public class RenderImageActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Autowired
    PdfHelper pdfHelper;

    @Autowired
    PdfRenderTargetTypeHolder pdfRenderTargetTypeHolder;

    @Test
    public void testPdfBackgroundNoText() {
        int width = 1024;
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                TestResources.Adobe_Acrobat_1_5_114p, TargetType.HTML_WITH_IMAGES);
        pdfHelper.removeHtmlBackground(fileId, 1, DimensionO.cons(width));

        MapF<String, Object> parameters = Cf.<String, Object>map()
                .plus1("id", fileId)
                .plus1("width", width)
                .plus1("index", 1)
                .plus1("json", 1);
        HttpUriRequest request = new HttpGet(UrlUtils.addParameters("http://localhost:32405" + RenderImageAction.URL, parameters));
        String res = ApacheHttpClientUtils.executeReadString(request);
        Assert.assertContains(res, "link");
        String link = JsonNodeUtils.getNode(res).get("link").textValue();
        Assert.notNull(new MulcaFileLink(link));
    }
}

package ru.yandex.chemodan.app.docviewer.web.backend;

import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.commune.bazinga.BazingaBender;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.bender.parse.BenderParser;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class PreviewUrlActionTest extends DocviewerWebSpringTestBase {

    private static final String PREVIEW_URL = "http://localhost:32405/preview-url";

    private final static BenderParser<PreviewUrlAction.PreviewUrlResponse> parser =
            BazingaBender.mapper.createParser(PreviewUrlAction.PreviewUrlResponse.class);
    @Test
    @Ignore("Depends on remote resource")
    public void getPreviewUrlForPrivateYaDiskFile() {
        getPreviewUrl(PREVIEW_URL, Cf.map("url", YaDiskIntegrationTest.TEST_URI_PDF,
                "uid", TestUser.PROD.uid,
                "size", "800x800"));
    }

    @Test
    @Ignore("Depends on remote resource")
    public void getPreviewUrlForPublicYaDiskFile() {
        getPreviewUrl(PREVIEW_URL, Cf.map("url", YaDiskIntegrationTest.TEST_URI_PUBLIC,
                "uid", PassportUidOrZero.zero(),
                "size", "800x800"));

    }

    private void getPreviewUrl(String downloadUrlBase, MapF<String, Object> params) {
        String urlWithParams = UrlUtils.addParameters(downloadUrlBase, params);
        HttpGet httpGet = new HttpGet(urlWithParams);
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        List<PreviewUrlAction.PreviewUrlResponse> res = parser.parseListJson(response);

        Assert.notEmpty(res);
    }

}

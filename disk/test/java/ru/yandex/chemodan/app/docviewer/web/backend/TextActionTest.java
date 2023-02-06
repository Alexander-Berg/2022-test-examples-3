package ru.yandex.chemodan.app.docviewer.web.backend;

import java.net.URL;
import java.util.Collection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.AbstractSpringAwareTest;
import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestSuites;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

@RunWith(Parameterized.class)
public class TextActionTest extends AbstractSpringAwareTest {

    @Autowired
    private TestManager testManager;

    private static final class GetTextResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) {
            try {
                HttpEntity httpEntity = response.getEntity();
                Assert.assertContains(httpEntity.getContentType().getValue(),
                        MimeTypes.MIME_TEXT_PLAIN);

                byte[] doc = InputStreamSourceUtils.wrap(httpEntity.getContent()).readBytes();
                String text = new String(doc, "utf-8");
                return text;
            } catch (Exception exc) {
                throw ExceptionUtils.translate(exc);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        return TestSuites.TEXTS_ARRAY;
    }

    private final String expectedText;
    private final URL source;

    public TextActionTest(URL source, String expectedText) {
        super();
        this.source = source;
        this.expectedText = expectedText;
    }

    @Test
    public void testDoPost() {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(), UriUtils.toUrlString(source), TargetType.PLAIN_TEXT);

        HttpGet httpGet = new HttpGet("http://localhost:32405/text?uid=0&id=" + fileId);
        String text = ApacheHttpClient4Utils.execute(httpGet, new GetTextResponseHandler(),
                Timeout.seconds(30));

        Assert.assertContains(text, expectedText);
    }

}

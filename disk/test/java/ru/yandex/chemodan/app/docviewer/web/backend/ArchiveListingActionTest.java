package ru.yandex.chemodan.app.docviewer.web.backend;

import java.net.URL;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ReadRootDom4jElementResponseHandler;
import ru.yandex.misc.io.http.apache.v4.ReadStringResponseHandler;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class ArchiveListingActionTest extends DocviewerWebSpringTestBase {

    @Autowired
    private TestManager testManager;


    // XXX ssytnik: make it work for other TargetType as well, for now it is not supported
    @Test
    public void testArchive() {
        testArchiveBase(false);
    }

    @Test
    public void testArchiveMobile() {
        testArchiveBase(true);
    }

    private void testArchiveBase(boolean mobile) {
        String response = getListing(TestResources.ZIP, new ReadStringResponseHandler(), mobile);

        Assert.isTrue(response.startsWith(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<archive>\n" +
                "  <folder name=\"a\">\n" +
                "    <folder name=\"b\">\n" +
                "      <file name=\"lala.txt\" path=\"a/b/lala.txt\" size=\"5\" mime-type=\"text/plain\" viewable=\"1\"/>\n"
                ));
    }

    @Test
    @Ignore("Not stable")
    public void doubleFolder_dv1259() {
        Element response = getListing(TestResources.DV_1259_ZIP, new ReadRootDom4jElementResponseHandler(), false);
        Assert.hasSize(1, response.selectNodes("/archive/folder[@name='name']"));
    }

    private <T> T getListing(URL resource, ResponseHandler<T> handler, boolean mobile) {
        String fileId = testManager.makeAvailable(PassportUidOrZero.zero(),
                UriUtils.toUrlString(resource), getConvertTargetMobileIncluded(mobile));

        HttpGet httpGet = new HttpGet("http://localhost:32405/archive-listing?uid=0"
                + "&id=" + fileId
                + getMobileParameter(mobile));

        T response = ApacheHttpClient4Utils.execute(httpGet, handler, Timeout.seconds(30));
        return response;
    }

}

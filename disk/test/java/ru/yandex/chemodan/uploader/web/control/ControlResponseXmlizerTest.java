package ru.yandex.chemodan.uploader.web.control;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.dom4j.Element;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.UploaderHosts;
import ru.yandex.chemodan.uploader.UploaderPorts;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.web.AbstractWebTestSupport;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRecord;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.misc.ip.Host;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
public class ControlResponseXmlizerTest extends AbstractWebTestSupport {
    private static final Host HOST = Host.parse("uploader23o.disk.yandex.net");

    private static final String REQUEST_ID = "requestid";
    private static final String POST_TARGET_ATTR = "post-target";
    private static final String POST_TARGET_ACTION = "/upload-target";

    @Autowired
    private UploaderHosts uploaderHosts;
    @Autowired
    private UploaderPorts uploaderPorts;

    @Value("${data.ssl.urls}")
    private boolean useSsl;

    private ControlResponseXmlizer controlResponseXmlizer;


    @Before
    public void init() {
        controlResponseXmlizer = new ControlResponseXmlizer(
                uploaderHosts, uploaderPorts, new DummySettings(), useSsl);
    }

    private RequestRecord consFakeRecord() {
        Instant now = new Instant();
        HostInstant localNow = new HostInstant(HOST, now);
        return new MpfsRequestRecord.UploadToDefault(
                new RequestMeta(UploadRequestId.valueOf(REQUEST_ID), now),
                localNow,
                RequestRevision.initial(localNow),
                null);
    }

    private Element getXmlizedResponse(MapF<String, String> attrs, Option<Boolean> useHttps) {
        return getXmlizedResponse(attrs, useHttps, Option.empty());
    }

    private Element getXmlizedResponse(MapF<String, String> attrs, Option<Boolean> useHttps, Option<String> tld) {
        RequestRecord r = consFakeRecord();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        controlResponseXmlizer.write(baos, "test", r, attrs, useHttps, "", tld);

        return Dom4jUtils.readRootElement(baos.toByteArray());
    }

    private void checkPollUrl(URI uri) {
        Assert.equals(HostnameUtils.localhost().toString(), uri.getHost());
        Assert.equals(uploaderPorts.getControlPort(), uri.getPort());
        Assert.equals("/request-status/" + REQUEST_ID, uri.getPath());
        Assert.equals("http", uri.getScheme());
    }

    private void checkUploadUrl(URI uri, boolean https) {
        Assert.equals(HOST.toString(), uri.getHost());
        Assert.equals(https ? uploaderPorts.getDataHttpsPort() : uploaderPorts.getDataPort(), uri.getPort());
        Assert.equals(POST_TARGET_ACTION + "/" + REQUEST_ID, uri.getPath());
        Assert.equals(https ? "https" : "http", uri.getScheme());
    }

    @Test
    public void xmlizeOrdinaryResponse() throws Exception {
        Element root = getXmlizedResponse(Cf.map(), Option.empty());
        checkPollUrl(new URI(root.attribute("poll-result").getText()));
    }

    @Test
    public void xmlizeUploadResponse() throws Exception {
        Element root = getXmlizedResponse(Cf.map(POST_TARGET_ATTR, POST_TARGET_ACTION), Option.empty());
        checkPollUrl(new URI(root.attribute("poll-result").getText()));
        checkUploadUrl(new URI(root.attribute(POST_TARGET_ATTR).getText()), useSsl);
    }

    @Test
    public void xmlizeUploadResponseEnableSsl() throws Exception {
        Element root = getXmlizedResponse(Cf.map(POST_TARGET_ATTR, POST_TARGET_ACTION), Option.of(true));
        checkPollUrl(new URI(root.attribute("poll-result").getText()));
        checkUploadUrl(new URI(root.attribute(POST_TARGET_ATTR).getText()), true);
    }

    @Test
    public void xmlizeUploadResponseDisableSsl() throws Exception {
        Element root = getXmlizedResponse(Cf.map(POST_TARGET_ATTR, POST_TARGET_ACTION), Option.of(false));
        checkPollUrl(new URI(root.attribute("poll-result").getText()));
        checkUploadUrl(new URI(root.attribute(POST_TARGET_ATTR).getText()), false);
    }

    @Test
    public void xmlizeUploadResponseForBannedUaDomain() throws Exception {
        Element root = getXmlizedResponse(Cf.map(POST_TARGET_ATTR, POST_TARGET_ACTION),
                Option.empty(), Option.of("ua"));
        int port = useSsl ? uploaderPorts.getDataHttpsPort() : uploaderPorts.getDataPort();

        checkPollUrl(new URI(root.attribute("poll-result").getText()));
        Assert.equals("http://disk.yandex.ua:" + port + "/upload/uploader23o/upload-target/requestid",
                root.attribute(POST_TARGET_ATTR).getText());
    }

    private static class DummySettings implements BannedDomainsProxySettings {
        @Override
        public boolean isBannedDomain(String tld) {
            return tld.equals("ua");
        }

        @Override
        public String getProxyHost() {
            return "disk.yandex.ua";
        }
    }
}

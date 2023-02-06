package ru.yandex.chemodan.uploader.registry.record;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Element;
import org.joda.time.Instant;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.ChemodanFile;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.av.AntivirusResult;
import ru.yandex.chemodan.uploader.mulca.MulcaUploadInfo;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord.UploadToDefault;
import ru.yandex.chemodan.uploader.registry.record.status.ExportPhotoStatus;
import ru.yandex.chemodan.uploader.registry.record.status.GenerateImageOnePreviewResult;
import ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus;
import ru.yandex.chemodan.uploader.registry.record.status.PostProcessStatus;
import ru.yandex.chemodan.uploader.social.SocialClientErrException;
import ru.yandex.chemodan.uploader.social.SocialProxyException;
import ru.yandex.chemodan.uploader.social.SocialTaskInfo.FailReason;
import ru.yandex.commune.image.ImageFormat;
import ru.yandex.commune.image.RotateAngle;
import ru.yandex.commune.uploader.registry.RequestLease;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.State;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.image.Dimension;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.ip.Host;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;
import ru.yandex.misc.xml.stream.builder.XmlBuilder;
import ru.yandex.misc.xml.stream.builder.XmlBuilders;

/**
 * @author akirakozov
 * @author alexm
 */
public class Xmlizer2Test extends TestBase {

    @BeforeClass
    public static void beforeClass() {
        MpfsRequestStatus.registerStatuses();
    }

    @Test
    public void xmlizeTestAntivirus() {
        XmlBuilder xw = XmlBuilders.defaultBuilder();
        PostProcessStatus pp = new PostProcessStatus();

        pp.antivirusResult2.complete(AntivirusResult.HEALTHY);

        xw.startElement("root");
        Xmlizer2.xmlizePostProcessStatus(xw, pp);
        xw.endElement();

        Element root = Dom4jUtils.readRootElement(new StringReader(xw.toValidXmlString()));
        Assert.equals("true", root.element("antivirus").element("result").attributeValue("result"));
        Assert.equals("healthy", root.element("antivirus").element("result").attributeValue("result-ext"));
    }

    @Test
    public void xmlizeAll() throws Exception {
        MpfsRequestRecord.UploadToDefault record = uploadRecord();

        XmlBuilder xw = XmlBuilders.defaultBuilder();
        record.xmlize(xw);

        String ethalon = ClassLoaderUtils.streamSourceForResource(
                Xmlizer2Test.class, "uploadToDefault.xml").readText();
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        Diff diff = XMLUnit.compareXML(ethalon, xw.toString());
        XMLAssert.assertXMLIdentical(diff, true);
    }

    private MpfsRequestRecord.UploadToDefault uploadRecord() throws URISyntaxException {
        Instant instant = new Instant(1234567890123L);
        RequestMeta meta = new RequestMeta(UploadRequestId.valueOf("reqId"), instant);
        Host localhost = Host.parse("somehost");
        RequestRevision rev = new RequestRevision(123, new HostInstant(localhost, instant));
        Option<RequestLease> leaseO = Option.of(new RequestLease(localhost, instant, Option.of(instant)));
        ChemodanFile chemodanFile = new ChemodanFile(UidOrSpecial.special("special"), "uniqF", "/a/b/c");
        MpfsRequest.UploadToDefault req = new MpfsRequest.UploadToDefault(ApiVersion.V_0_2,
                chemodanFile,
                Option.of(new URI("callback/uri")),
                Option.of(DataSize.fromBytes(123)),
                Option.of("cloud-123"));
        MpfsRequestStatus.UploadToDefault status = new MpfsRequestStatus.UploadToDefault();
        MpfsRequestRecord.UploadToDefault record = new UploadToDefault(meta, rev, leaseO, req, status);
        return record;
    }

    @Test
    public void xmlizeExportPhotoStatus() {
        XmlBuilder xw = XmlBuilders.defaultBuilder();
        MpfsRequestStatus.ExportPhotos status = new MpfsRequestStatus.ExportPhotos(2);
        ExportPhotoStatus exportPhotoStatus = status.multiplePhotoExport.queue.get(0);
        exportPhotoStatus.commitUploadMeta.complete(true);
        xw.startElement("root");
        status.xmlize(xw);
        xw.endElement();

        Element root = Dom4jUtils.readRootElement(xw.toByteArray());

        Element photo1 = root.element("export-photo-0");
        Assert.equals("success", photo1.attributeValue("status"));

        Element photo2 = root.element("export-photo-1");
        Assert.equals("initial", photo2.attributeValue("status"));
    }

    @Test
    public void xmlizeExportPhotoStatusWithTokenError() {
        XmlBuilder xw = XmlBuilders.defaultBuilder();
        MpfsRequestStatus.ExportPhotos status = new MpfsRequestStatus.ExportPhotos(1);
        ExportPhotoStatus exportPhotoStatus = status.multiplePhotoExport.queue.get(0);
        SocialProxyException invalidTokenException = new SocialProxyException(
                new FailReason("invalid_token", "", "", ""));
        SocialClientErrException uploadException = new SocialClientErrException(null, true);

        exportPhotoStatus.uploadInfo.failPermanently(State.initial(), invalidTokenException);
        exportPhotoStatus.uploadToSocialNet.failPermanently(State.initial(), uploadException);
        exportPhotoStatus.commitUploadMeta.failPermanently(State.initial(), invalidTokenException);

        xw.startElement("root");
        status.xmlize(xw);
        xw.endElement();

        Element root = Dom4jUtils.readRootElement(xw.toByteArray());
        Element photo1 = root.element("export-photo-0");
        Assert.equals("permanent-failure", photo1.attributeValue("status"));
        Assert.equals("true", photo1.element("details").attributeValue("invalid_token"));

        Element uploadPhoto = root.element("export-photo-upload-0");
        Assert.equals("permanent-failure", uploadPhoto.attributeValue("status"));
        Assert.equals("true", uploadPhoto.element("details").attributeValue("invalid_token"));

        Element photoUrl = root.element("export-photo-url-0");
        Assert.equals("permanent-failure", photoUrl.attributeValue("status"));
        Assert.equals("true", photoUrl.element("details").attributeValue("invalid_token"));
    }

    @Test
    public void xmlizeImagePreviewResult() {
        XmlBuilder xw = XmlBuilders.defaultBuilder();
        PostProcessStatus pp = new PostProcessStatus();

        GenerateImageOnePreviewResult result = new GenerateImageOnePreviewResult(
                new File2("preview.txt"), ImageFormat.JPEG,
                Option.of(new Dimension(2560, 1600)), new Dimension(1280, 800), Option.of(RotateAngle.D270));

        pp.previewImageStatus.previewMulcaUploadInfo.complete(MulcaUploadInfo.fromMulcaId(MulcaId.fromSerializedString("qwe")));
        pp.previewImageStatus.generateOnePreview.complete(result);

        xw.startElement("root");
        Xmlizer2.xmlizePostProcessStatus(xw, pp);
        xw.endElement();

        Element root = Dom4jUtils.readRootElement(new StringReader(xw.toValidXmlString()));
        Element resultEl = root.element("generate-image-one-preview").element("result");
        Assert.equals("jpeg", resultEl.attributeValue("format"));
        Assert.equals("1280", resultEl.attributeValue("width"));
        Assert.equals("800", resultEl.attributeValue("height"));
        Assert.equals("2560", resultEl.attributeValue("original-width"));
        Assert.equals("1600", resultEl.attributeValue("original-height"));
        Assert.equals("270", resultEl.attributeValue("rotate-angle"));
    }
}

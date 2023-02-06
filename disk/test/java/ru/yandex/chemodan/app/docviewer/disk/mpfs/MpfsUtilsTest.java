package ru.yandex.chemodan.app.docviewer.disk.mpfs;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsUtils.MpfsDownloadUrlResponse;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MpfsUtilsTest {

    @Test
    public void parseMpfsUrlResponse() {
        String json =
                "{\"status\": 1," +
                "\"result\":" +
                "   {\"digest\": \"https://downloader-tst.disk.yandex.ru/disk/b1ae8048b92fc51ffd063b2e4d1bdc11/5061c962/" +
                        "MTAwMDAwNi55YWRpc2s6MTM0MTkyNjIwLjM5ODMyOTYzODQ2MTIxNTA4NjUxOTIyOTU1NDc5NQ==" +
                        "?uid=134192620&filename=file.pdf&content_type=application/octet-stream&disposition=attachment" +
                        "&hash=&limit=0\"," +
                "   \"file\": \"https://downloader-tst.disk.yandex.ru/disk/9fd4e9863f24cef160afe1575d1073cd/" +
                        "5061c962/MTAwMDAwNy55YWRpc2s6MTM0MTkyNjIwLjM5ODMyOTYzODQyMDI1NDEzOTc0MTU5OTUyODc2NTk=" +
                        "?uid=134192620&filename=file.pdf&content_type=application/pdf&disposition=attachment" +
                        "&hash=&limit=0\"}}";

        MpfsDownloadUrlResponse response = MpfsUtils.parseMpfsDownloadUrl(json);
        Assert.equals(1, response.status);
        Assert.equals(
                "https://downloader-tst.disk.yandex.ru/disk/9fd4e9863f24cef160afe1575d1073cd/" +
                "5061c962/MTAwMDAwNy55YWRpc2s6MTM0MTkyNjIwLjM5ODMyOTYzODQyMDI1NDEzOTc0MTU5OTUyODc2NTk=" +
                "?uid=134192620&filename=file.pdf&content_type=application/pdf&disposition=attachment&hash=&limit=0",
                response.result.file);
        Assert.isTrue(response.result.digest.isPresent());
    }

    @Test
    public void parseMpfsUrlResponseForMailAttach() {
        String json =
            "{\"status\": 1," +
            " \"result\": {" +
                "\"file\": \"http://mail.yandex.ru/message_part/bla-bla-bla\"}}";

        MpfsDownloadUrlResponse response = MpfsUtils.parseMpfsDownloadUrl(json);
        Assert.equals(1, response.status);
        Assert.equals("http://mail.yandex.ru/message_part/bla-bla-bla", response.result.file);
        Assert.isEmpty(response.result.digest);
    }

    @Test
    public void parseMpfsPublicInfo() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "publicInfo.js");
        Assert.equals("https://front.tst.clk.yandex.net/i/-yO65m2lRer7", MpfsUtils.getShortUrlFromPublicInfo(response));
    }

    @Test
    public void parseMpfsInfoShortUrl() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "info.js");
        Assert.equals("https://front.tst.clk.yandex.net/i/iqHq0LNfRez6", MpfsUtils.getShortUrlFromInfo(response));
    }

    @Test
    public void parseMpfsSetPublicResponse() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "setPublic.js");
        Assert.equals(
                "https://front.tst.clk.yandex.net/i/iqHq0LNfRez6",
                MpfsUtils.getShortUrlFromSetPublicResponse(response));
    }

    @Test
    public void parseMpfsAsyncCopy() {
        String json = "{\"status\": 1, \"result\": {\"oid\": \"7e5daaca4815b85\", \"type\": \"copy\"}}";
        MpfsUtils.MpfsAsyncCopyResponse resp =  MpfsUtils.parseMpfsAsyncCopy(json);
        Assert.equals(1, resp.status);
        Assert.equals("7e5daaca4815b85", resp.result.oid);
    }

    @Test
    public void parseMpfsAsyncStoreExternal() {
        String json = "{\"at_version\":1489152746002309,\"oid\":\"7e5daaca4815b85\",\"type\":\"external_copy\"}";
        MpfsUtils.MpfsAsyncStoreExternalResponse resp =  MpfsUtils.parseMpfsAsyncStoreExternal(json);
        Assert.equals("7e5daaca4815b85", resp.oid);
        Assert.equals("external_copy", resp.type);
    }

    @Test
    public void getOperationStatus() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "async-copy-status-ok.js");
        Assert.equals(MpfsOperationStatus.DONE, MpfsUtils.getOperationStatus(response));
    }

    @Test
    public void getCopiedResourcePathFromStatus() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "async-copy-status-ok.js");
        Assert.equals("/disk/Загрузки/new (4).pdf", MpfsUtils.getCopiedResourcePathFromStatus(response));
    }

    @Test
    public void getCopiedResourceShortUrlFromStatus() {
        String response = ClassLoaderUtils.loadText(MpfsUtils.class, "async-copy-status-ok.js");
        Assert.equals(
                "https://front.tst.clk.yandex.net/d/ks9FPNoCRnnF",
                MpfsUtils.getCopiedResourceShortUrlFromStatus(response));
    }

    @Test
    public void getOfficeActionCheckResponse() {
        String json = "{\"office_online_url\":\"https://disk.dst.yandex.ru/edit/disk/disk%2Ffile.doc\","
                + "\"office_online_editor_type\":\"microsoft_online\"}";
        MpfsUtils.MpfsOfficeActionCheckResponse resp =  MpfsUtils.getOfficeActionCheckResponse(json);
        Assert.equals("https://disk.dst.yandex.ru/edit/disk/disk%2Ffile.doc", resp.officeOnlineUrl);
        Assert.equals("microsoft_online", resp.officeOnlineEditorType.get());
    }

    @Test
    public void getOfficeActionCheckResponseWithoutEditorType() {
        String json = "{\"office_online_url\":\"https://disk.dst.yandex.ru/edit/disk/disk%2Ffile.doc\"}";
        MpfsUtils.MpfsOfficeActionCheckResponse resp =  MpfsUtils.getOfficeActionCheckResponse(json);
        Assert.equals("https://disk.dst.yandex.ru/edit/disk/disk%2Ffile.doc", resp.officeOnlineUrl);
        Assert.isEmpty(resp.officeOnlineEditorType);
    }

}

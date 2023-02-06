package ru.yandex.chemodan.app.docviewer.copy;

import java.net.URI;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.copy.provider.FemidaUrlProvider;
import ru.yandex.chemodan.app.docviewer.copy.provider.MulcaUrlProvider;
import ru.yandex.chemodan.app.docviewer.copy.provider.OtrsUrlProvider;
import ru.yandex.chemodan.app.docviewer.copy.provider.ProcuUrlProvider;
import ru.yandex.chemodan.app.docviewer.copy.provider.YaBroUrlProvider;
import ru.yandex.chemodan.app.docviewer.disk.resource.DiskPrivateFileId;
import ru.yandex.chemodan.app.docviewer.disk.resource.DiskPublicFileId;
import ru.yandex.chemodan.app.docviewer.disk.resource.WebDocumentId;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author vlsergey
 * @author akirakozov
 *
 */
public class UriHelperTest extends DocviewerSpringTestBase {
    private static final String OTRS_URL =
            "http://storage-int.mdst.yandex.net/get-otrs/858848/9746fff1-1d73-4cd1-bca0-fc9cd65faf93";
    private static final String MULCA_URL =
            "http://storagetest.mail.yandex.net:10010/gate/get/1000003.98553867488156822122183803121?gettype=part&part=1.2&service=docviewer&ns=disk";

    @Autowired
    private UriHelper uriHelper;

    @Value("${mpfs.host}")
    private String mpfsHost;

    @Autowired
    private OtrsUrlProvider otrsUrlProvider;
    @Autowired
    private MulcaUrlProvider mulcaUrlProvider;
    @Autowired
    private FemidaUrlProvider femidaUrlProvider;
    @Autowired
    private YaBroUrlProvider yaBroUrlProvider;
    @Autowired
    private ProcuUrlProvider procuUrlProvider;

    @Test
    public void testFindProviderByActualUri() {
        ActualUri diskPrivate1 = new ActualUri("https://" + mpfsHost + "/service/direct_url?uid=666&path=somefileid");
        ActualUri diskPrivate2 = new ActualUri("https://" + mpfsHost + "/service/direct_url?uid=123456789&path=%2Fpath%2Ffile.txt%26uid%3D777");
        ActualUri diskPrivate3 = new ActualUri("https://" + mpfsHost + "/service/direct_url?uid=128729656&path=%2Fdisk%2Fone.txt");
        ActualUri diskPublic = new ActualUri("https://" + mpfsHost + "/service/public_direct_url?private_hash=hash12345");
        ActualUri otrs = new ActualUri(OTRS_URL);
        ActualUri browser = new ActualUri("http://storage-int.mdst.yandex.net/get-browser-clouddoc/4231/some_mds_key_value/part2");
        ActualUri mulca = new ActualUri(MULCA_URL);
        ActualUri serp = new ActualUri("http://docviewer.yandex.net/yandbtm?url=https%3A%2F%2Fsome.ru%2Fa.pdf&noconv=1");
        ActualUri femida = new ActualUri("https://femida.test.yandex-team.ru/api/attachments/12345");
        ActualUri real = new ActualUri("https://some.ru/some/path");

        ActualUri procu = new ActualUri("https://procu.test.yandex-team.ru/api/attachments/doc?attachment=10&uid=858848");

        DocumentSourceInfo documentSourceInfo = DocumentSourceInfo.builder().originalUrl("").uid(PassportUidOrZero.fromUid(123)).build();
        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, diskPrivate1).get() instanceof DiskPrivateFileId);
        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, diskPrivate2).get() instanceof DiskPrivateFileId);
        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, diskPrivate3).get() instanceof DiskPrivateFileId);
        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, diskPublic).get() instanceof DiskPublicFileId);

        Assert.isTrue(mulcaUrlProvider.isSupportedActualUri(mulca));
        Assert.isTrue(otrsUrlProvider.isSupportedActualUri(otrs));
        Assert.isTrue(uriHelper.findProviderByActualUri(otrs).get() instanceof OtrsUrlProvider);
        Assert.isTrue(yaBroUrlProvider.isSupportedActualUri(browser));
        Assert.isTrue(uriHelper.findProviderByActualUri(browser).get() instanceof YaBroUrlProvider);

        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, serp).get() instanceof WebDocumentId);
        Assert.isTrue(uriHelper.getDiskResourceId(documentSourceInfo, real).get() instanceof WebDocumentId);

        Assert.isTrue(femidaUrlProvider.isSupportedActualUri(femida));

        Assert.isTrue(procuUrlProvider.isSupportedActualUri(procu));
    }

    @Test
    public void checkRightsForWikiUrls() {
        // ya-team wiki
        Assert.equals(UriCheckResult.OK,
                uriHelper.checkOriginalUrlAllowedInner(
                        "ya-wiki://wiki-api.test.yandex-team.ru/page_supertag/file_url", TestUser.YA_TEAM_AKIRAKOZOV.uid,
                        Option.empty()));
        Assert.equals(UriCheckResult.FORBIDDEN,
                uriHelper.checkOriginalUrlAllowedInner(
                        "ya-wiki://wiki-api.test.yandex-team.ru/page_supertag/file_url", TestUser.TEST.uid,
                        Option.empty()));

        // public wiki
        Assert.equals(UriCheckResult.OK,
                uriHelper.checkOriginalUrlAllowedInner(
                        "ya-wiki://wiki-api.test.yandex.ru/page_supertag/file_url", TestUser.TEST.uid,
                        Option.empty()));
    }

    @Test
    public void testCheck() {
// XXX: disabled until UriHelper.isSafeUrl fix for cache and HEAD requests
//        Assert.equals(UriCheckResult.FORBIDDEN,
//                uriHelper.checkOriginalUrlAllowedInner("http://webattachXXX.mail.yandex.net/query",
//                PassportUidOrZero.zero()));
        Assert.equals(UriCheckResult.BAD_REQUEST,
                uriHelper.checkOriginalUrlAllowedInner("", PassportUidOrZero.zero(), Option.empty()));
    }

    @Test
    public void testRewrite() {
        URI result1 = uriHelper.rewriteForTests("ya-disk://somefileid", PassportUidOrZero.fromUid(666)).getUri();
        Assert.equals(
                "https://" + mpfsHost + "/service/direct_url?uid=666&path=somefileid",
                UriUtils.toUriString(result1));

        URI result2 = uriHelper.rewriteForTests("ya-disk:///path/file.txt&uid=777",
                PassportUidOrZero.fromUid(123456789)).getUri();
        Assert.equals(
                "https://" + mpfsHost + "/service/direct_url?uid=123456789&path=%2Fpath%2Ffile.txt%26uid%3D777",
                UriUtils.toUriString(result2));
    }

    @Test
    public void testCheck2() {
        Assert.equals(UriCheckResult.OK,
                uriHelper.checkOriginalUrlAllowedInner("ya-disk:///disk/one.txt", PassportUidOrZero.zero(),
                        Option.empty()));
        Assert.equals(UriCheckResult.FORBIDDEN,
                uriHelper.checkOriginalUrlAllowedInner("ya-temp:///disk/one.txt", PassportUidOrZero.zero(),
                        Option.empty()));
        Assert.equals(UriCheckResult.NOT_IMPLEMENTED,
                uriHelper.checkOriginalUrlAllowedInner("temp@tempuri.org", PassportUidOrZero.zero(),
                        Option.empty()));
        Assert.equals(UriCheckResult.BAD_REQUEST,
                uriHelper.checkOriginalUrlAllowedInner("", PassportUidOrZero.zero(),
                        Option.empty()));
    }

    @Test
    public void testRewrite2() {
        ActualUri actualUri = uriHelper.rewriteForTests("ya-disk:///disk/one.txt",
                PassportUidOrZero.fromUid(128729656));

        Assert.equals(
                "https://" + mpfsHost + "/service/direct_url?uid=128729656&path=%2Fdisk%2Fone.txt",
                actualUri.getUriString());
    }

    @Test
    public void testRewrite3() {
        Assert.equals(
                "https://" + mpfsHost + "/service/public_direct_url?private_hash=hash12345",
                uriHelper.rewriteForTests("ya-disk-public://hash12345", PassportUidOrZero.zero()).getUriString()
                );
    }

    @Test
    public void rewriteWithSpaces() {
        Assert.equals(
                UrlUtils.uri("https://" + mpfsHost + "/service/direct_url?uid=123&path=%2Fdisk%2FWhite+-+Hayek%27s+Monetary+Theory.pdf"),
                uriHelper.rewriteForTests("ya-disk:///disk/White - Hayek's Monetary Theory.pdf", PassportUidOrZero.fromUid(123)).getUri());
    }

    @Test
    public void rewriteWiki() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(123456789);
        Assert.equals(
                "https://wiki-api.test.yandex-team.ru/_api/docviewer/v2/download/?fileid=page_supertag%2Ffile_url",
                uriHelper.rewriteForTests("ya-wiki://wiki-api.test.yandex-team.ru/page_supertag/file_url", uid).getUriString());
        Assert.equals(
                "https://wiki-api.test.yandex.ru/_api/docviewer/v2/download/?fileid=page_supertag%2Ffile_url",
                uriHelper.rewriteForTests("ya-wiki://wiki-api.test.yandex.ru/page_supertag/file_url", uid).getUriString());
        Assert.equals(
                "https://wiki-api.school.test.yandex.ru/_api/docviewer/v2/download/?fileid=page_supertag%2Ffile_url",
                uriHelper.rewriteForTests("ya-wiki://wiki-api.school.test.yandex.ru/page_supertag/file_url", uid).getUriString());
        Assert.equals(
                "https://wiki-api.eva.test.yandex-team.ru/_api/docviewer/v2/download/?fileid=page_supertag%2Ffile_url",
                uriHelper.rewriteForTests("ya-wiki://wiki-api.eva.test.yandex-team.ru/page_supertag/file_url", uid).getUriString());
    }

    @Test
    public void checkOtrs() {
        String url = "otrs://MDS:858848/9746fff1-1d73-4cd1-bca0-fc9cd65faf93/";
        Assert.equals(UriCheckResult.FORBIDDEN,
                uriHelper.checkOriginalUrlAllowedInner(url, PassportUidOrZero.zero(), Option.empty()));
        Assert.equals(UriCheckResult.FORBIDDEN,
                uriHelper.checkOriginalUrlAllowedInner(url, PassportUidOrZero.fromUid(123), Option.empty()));
        Assert.equals(UriCheckResult.OK,
                uriHelper.checkOriginalUrlAllowedInner(url, TestUser.YA_TEAM_AKIRAKOZOV.uid, Option.empty()));
    }

    @Test
    public void rewriteOtrs() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(123456789);
        Assert.equals(OTRS_URL,
                uriHelper.rewriteForTests("otrs://MDS:858848/9746fff1-1d73-4cd1-bca0-fc9cd65faf93/", uid).getUriString());
    }

    @Test
    public void rewriteProcu() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(123456789);
        Assert.equals(
                "https://procu.test.yandex-team.ru/api/attachments/doc?attachment=10&uid=123456789",
                uriHelper.rewriteForTests("ya-procu://10", uid).getUriString());
    }

    @Test
    public void rewriteMulca() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(123456789);
        Assert.equals(MULCA_URL,
                uriHelper.rewriteForTests("mulca://1000003.98553867488156822122183803121/1.2", uid).getUriString());
    }

    @Test
    public void rewriteYaMail() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(123456789);
        DocumentSourceInfo source = DocumentSourceInfo.builder().originalUrl("ya-mail://mid123/1.2").uid(uid).build();
        Assert.equals(
                "https://web-tst1j.yandex.ru:443/dv-mimes?uid=123456789&mid=mid123&with_mulca=yes&with_inline=yes&part=1.2",
                uriHelper.rewrite(source).getUriString());

        DocumentSourceInfo source2 = DocumentSourceInfo.builder().originalUrl("ya-mail://mid123/1.2").uid(TestUser.YA_TEAM_AKIRAKOZOV.uid).build();
        Assert.equals(
                "https://web-tst1j.yandex.ru:443/dv-mimes?uid=1120000000000744&mid=mid123&with_mulca=yes&with_inline=yes&part=1.2",
                uriHelper.rewrite(source2).getUriString());
    }

    @Test
    public void checkYaSerp() {
        Assert.equals(UriCheckResult.OK, uriHelper.checkOriginalUrlAllowedInner(
                "ya-serp://some.ru/a.pdf", PassportUidOrZero.zero(), Option.empty()));
    }
}

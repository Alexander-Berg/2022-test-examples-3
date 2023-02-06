package ru.yandex.autotests.innerpochta.webattach;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import ru.yandex.autotests.innerpochta.beans.hound.AttachSidRequest;
import ru.yandex.autotests.innerpochta.beans.hound.Download;
import ru.yandex.autotests.innerpochta.beans.hound.HoundResponse;
import ru.yandex.autotests.innerpochta.beans.mbody.Attachment;
import ru.yandex.autotests.innerpochta.beans.mbody.BinaryTransformerResult;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UpdateHCFieldRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UtilsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.NO_VDIRECT_LINKS_WRAP;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi.apiMbody;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiHound;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.BufferedImageMatcher.hasSameTypeAndSize;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.LogCollectRule.withGrepAllLogsFor;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

public abstract class BaseWebattachTest {
    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = auth().withAnnotation().lock(lock);

    @ClassRule
    public static RuleChain chainAuth = RuleChain.outerRule(lock).around(authClient);

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public TestRule chainRule = RuleChain
            .outerRule(new LogConfigRule())
            .around(new UpdateHCFieldRule(authClient, this, "hc"));

    @Rule
    public UtilsRule utils = new UtilsRule(authClient);

    public static FolderList folderList = new FolderList(authClient);

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();


    protected DefaultHttpClient hc;


    protected final Logger logger = LogManager.getLogger(this.getClass());


    /**
     * Получаем URL для скачивания аттача
     *
     * @param mid     мид письма
     * @param attName имя аттача URL для скачивания которого хотим получить
     * @return
     * @throws Exception
     */
    protected static String urlOfAttach(String mid, String attName) {
        return urlOfAttach(mid, attName, props().houndUri());
    }

    protected static String urlOfAttach(String mid, String attName, String houndUri) {
        Mbody resp = apiMbody(authClient.account().userTicket()).message()
                .withFlags(NO_VDIRECT_LINKS_WRAP.toString())
                .withMid(mid)
                .withUid(authClient.account().uid())
                .get(identity()).peek().as(Mbody.class);

        List<String> hids = resp.getAttachments().stream()
                .map(Attachment::getBinaryTransformerResult)
                .filter(att -> att.getTypeInfo().getName().equals(attName))
                .map(BinaryTransformerResult::getHid)
                .collect(Collectors.toList());
        assertThat("Хидов нет для аттача с заданным именем", hids.size(), greaterThanOrEqualTo(1));

        List<String> sids = getAttachSid(mid, hids, houndUri);
        assertEquals("Неожиданное кол-во сидов", sids.size(), 1);

        String url = api(MessagePartReal.class)
                .setHost(props().webattachHost())
                .params(emptyObj().setSid(sids.get(0)))
                .getRequest();

        return url;
    }

    protected static String urlOfAllAttachesZipArchive(String mid, String attName) {
        Mbody resp = apiMbody(authClient.account().userTicket()).message()
                .withFlags(NO_VDIRECT_LINKS_WRAP.toString())
                .withMid(mid)
                .withUid(authClient.account().uid())
                .get(identity()).peek().as(Mbody.class);

        List<String> hids = resp.getAttachments().stream()
                .map(Attachment::getBinaryTransformerResult)
                .map(BinaryTransformerResult::getHid)
                .collect(Collectors.toList());

        String hid = Joiner.on(",").join(hids);
        assertThat(hid, not(isEmptyOrNullString()));

        List<String> sids = getAttachSid(mid, hids);
        assertEquals("Неожиданное кол-во сидов", sids.size(), 1);

        String url = api(MessagePartReal.class)
                .setHost(props().webattachHost())
                .params(emptyObj().setSid(sids.get(0)))
                .getRequest();

        return url;
    }

    protected static void shouldSeeImageFile(String url, File expected) throws IOException {
        File preview = downloadFile(url, expected.getName(), authClient.authHC());
        BufferedImage previewImage = ImageIO.read(preview);
        BufferedImage expectedImage = ImageIO.read(expected);
        assertThat(previewImage, hasSameTypeAndSize(expectedImage));
    }

    protected static void shouldSeeFile(String url, File expected) throws IOException {
        File preview = downloadFile(url, expected.getName(), authClient.authHC());
        assertThat("MD5 хэши файлов не совпали ", preview, hasSameMd5As(expected));
    }

    protected static List<String> getAttachSid(String mid, List<String> hids) {
        return getAttachSid(mid, hids, props().houndUri());
    }

    private static List<String> getAttachSid(String mid, List<String> hids, String houndHost) {
        ArrayList<Download> downloads = new ArrayList<Download>() {{
            add(new Download().withMid(mid).withHids(hids));
        }};

        AttachSidRequest sr = new AttachSidRequest().withUid(authClient.account().uid()).withDownloads(downloads);

        String body = new Gson().toJson(Collections.singletonList(sr));

        return apiHound(authClient.account().userTicket(), houndHost).attachSid()
                .withUid(authClient.account().uid())
                .withReq(req -> req.setContentType("application/json")
                        .setBody(body))
                .post(Function.identity())
                .as(HoundResponse.class)
                .getResult().get(0).getSids();
    }
}

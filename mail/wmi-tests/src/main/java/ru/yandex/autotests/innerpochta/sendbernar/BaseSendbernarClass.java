package ru.yandex.autotests.innerpochta.sendbernar;


import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.beans.hound.AttachSidRequest;
import ru.yandex.autotests.innerpochta.beans.hound.Download;
import ru.yandex.autotests.innerpochta.beans.hound.HoundResponse;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.beans.mailsend.PartsJson;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.WriteAttachmentResponse;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.autotests.innerpochta.wmi.core.rules.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.callback.noanswerremind.ApiNoAnswerRemind;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.callback.senddelayedmessage.ApiSendDelayedMessage;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.callback.sendundomessage.ApiSendUndoMessage;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.cancelsenddelayed.ApiCancelSendDelayed;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.cancelsendundo.ApiCancelSendUndo;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.generateoperationid.ApiGenerateOperationId;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.listunsubscribe.ApiListUnsubscribe;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.savedraft.ApiSaveDraft;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.savetemplate.ApiSaveTemplate;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendbarbetmessage.ApiSendBarbetMessage;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.senddelayed.ApiSendDelayed;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendshare.ApiSendShare;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendundo.ApiSendUndo;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.writeattachment.ApiWriteAttachment;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitForMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiHound;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiSendbernar;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.FileCompareMatcher.hasSameMd5As;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.attachOk200;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

public class BaseSendbernarClass {

    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = auth().withAnnotation().lock(lock);

    @ClassRule
    public static RuleChain chainAuth = RuleChain.outerRule(lock).around(authClient);

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @ClassRule
    public static IgnoreSshTestRule beforeSshTestClass = newIgnoreSshTestRule();

    @Rule
    public IgnoreSshTestRule beforeSshTest = newIgnoreSshTestRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public TestRule chainRule = RuleChain
            .outerRule(new LogConfigRule())
            .around(new UpdateHCFieldRule(authClient, this, "hc"));


    public static FolderList folderList = new FolderList(authClient);

    public WaitForMessage waitWith = new WaitForMessage(authClient);

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS)
            .times(1);


    protected DefaultHttpClient hc;

    protected final Logger logger = LogManager.getLogger(this.getClass());

    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    String subj;
    private String userTicket = null;

    static final String IMG_URL_JPG = "http://img-fotki.yandex.ru/get/9300/219421176.0/0_d2a24_8d0e0fea_orig";
    static final String caller = "test_qa";
    static final String remindMessageLabelName = "SystMetka:remindme_about_message";
    static final String noAnswerReminderLabelName = "remindme_threadabout:mark";

    String getUserTicket() {
        if (userTicket == null) {
            userTicket = authClient.account().userTicket();
        }
        return userTicket;
    }

    String unexistingUid() {
        return UNEXISTING_UID;
    }

    String getUid() {
        return authClient.account().uid();
    }

    @Before
    public void prepareBase() throws Exception {
        subj = getRandomString();
        getUserTicket();
        waitWith = new WaitForMessage(authClient);
    }

    String randomAddress() {
        return Util.getRandomAddress().toLowerCase();
    }

    ApiSaveDraft saveDraft() {
        return apiSendbernar(getUserTicket()).saveDraft()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSaveTemplate saveTemplate() {
        return apiSendbernar(getUserTicket()).saveTemplate()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendMessage sendMessage() {
        return apiSendbernar(getUserTicket()).sendMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendShare sendShare() {
        return apiSendbernar(getUserTicket()).sendShare()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiGenerateOperationId generateOperationId() {
        return apiSendbernar(getUserTicket()).generateOperationId()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiNoAnswerRemind noAnswerRemind() {
        return apiSendbernar(getUserTicket()).noAnswerRemind()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendDelayed sendDelayed() {
        return apiSendbernar(getUserTicket()).sendDelayed()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendUndo sendUndo() {
        return apiSendbernar(getUserTicket()).sendUndo()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiCancelSendDelayed cancelSendDelayed() {
        return apiSendbernar(getUserTicket()).cancelSendDelayed()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiCancelSendUndo cancelSendUndo() {
        return apiSendbernar(getUserTicket()).cancelSendUndo()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendDelayedMessage sendDelayedMessage() {
        return apiSendbernar(getUserTicket()).sendDelayedMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendUndoMessage sendUndoMessage() {
        return apiSendbernar(getUserTicket()).sendUndoMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiWriteAttachment writeAttachment() {
        return apiSendbernar(getUserTicket()).writeAttachment()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiListUnsubscribe listUnsubscribe() {
        return apiSendbernar(getUserTicket()).listUnsubscribe()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendBarbetMessage sendBarbetMessage() {
        return apiSendbernar(getUserTicket()).sendBarbetMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    String lidByTitle(String title) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(getUid()))
                .setHost(props().houndUri()).get().via(authClient).lidByTitle(title);
    }

    String lidBySymbol(LabelSymbol symbol) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(getUid()))
                .setHost(props().houndUri()).get().via(authClient).lidBySymbol(symbol);
    }

    static String lidBySymbol(LabelSymbol symbol, HttpClientManagerRule rule) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(rule.account().uid()))
                .setHost(props().houndUri()).get().via(rule).lidBySymbol(symbol);
    }

    String lidByName(String name) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(getUid()))
                .setHost(props().houndUri()).get().via(authClient).lidByName(name);
    }

    static String lidByName(String name, HttpClientManagerRule rule) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(rule.account().uid()))
                .setHost(props().houndUri()).get().via(rule).lidByName(name);
    }

    String lidByNameAndType(String name, String soName) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(getUid()))
                .setHost(props().houndUri()).get().via(authClient).lidByNameAndType(name, soName);
    }

    String uploadedId() {
        byte[] content;

        try {
            content = FileUtils.readFileToByteArray(downloadFile(IMG_URL_JPG, getRandomString(), hc));
        } catch (IOException ex) {
            return null;
        }

        return writeAttachment()
                .withFilename("not_rotate.jpg")
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(ok200()))
                .as(WriteAttachmentResponse.class)
                .getId();
    }

    Message byMid(String mid) {
        return new Message(mid, authClient);
    }

    Message byMid(String mid, HttpClientManagerRule rule) {
        return new Message(mid, rule);
    }

    void shouldSeeImageInMessage(File attach, String mid, HttpClientManagerRule rule) throws Exception {
        shouldSeeImageInMessage(attach, mid, rule, attach.getName());
    }

    void shouldSeeImageInMessage(File attach, String mid, HttpClientManagerRule rule, String filename) throws Exception {
        Message resp = byMid(mid);

        String hid = resp.getAttachHidByName(filename);

        ArrayList<Download> downloads = new ArrayList<Download>() {{
            add(new Download().withMid(mid).withHids(
                    new ArrayList<String>() {{
                        add(hid);
                    }}
            ));
        }};

        AttachSidRequest sr = new AttachSidRequest().withUid(getUid()).withDownloads(downloads);

        List<AttachSidRequest> sidRequests = new ArrayList<>();
        sidRequests.add(sr);

        String body = new Gson().toJson(sidRequests);

        List<String> sids = apiHound(getUserTicket())
                .attachSid()
                .withUid(rule.account().uid())
                .withReq((req) -> req.setContentType("application/json")
                        .setBody(body)).post(Function.identity()).as(HoundResponse.class)
                .getResult().get(0).getSids();

        String url = api(MessagePartReal.class)
                .params(MessagePartRealObj
                            .emptyObj()
                            .setSid(sids.get(0)))
                .setHost(props().webattachHost())
                .getRequest();

        File deliveredAttach = downloadFile(url, attach.getName(), rule.authHC());
        assertThat("MD5 хэши файлов не совпали", deliveredAttach, hasSameMd5As(attach));
    }

    String getPartsJson(File imageJpeg) throws Exception {
        byte[] content = FileUtils.readFileToByteArray(imageJpeg);

        String id = writeAttachment()
                .withUid(getUid())
                .withFilename(imageJpeg.getName())
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(attachOk200()))
                .as(WriteAttachmentResponse.class)
                .getId();


        SaveDraftResponse resp = saveDraft()
                .withTo(authClient.acc().getSelfEmail())
                .withUploadedAttachStids(id)
                .withSubj(getRandomString())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class);


        PartsJson p = new PartsJson()
                .withMid(Long.parseLong(resp.getStored().getMid()))
                .withHid(Double.parseDouble(resp.getAttachments().get(0).getHid()));


        return new Gson().toJson(p);
    }
}

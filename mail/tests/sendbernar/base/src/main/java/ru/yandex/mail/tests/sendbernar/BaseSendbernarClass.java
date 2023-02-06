package ru.yandex.mail.tests.sendbernar;


import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.rules.IgnoreRule;
import ru.yandex.mail.common.rules.IgnoreSshTestRule;
import ru.yandex.mail.common.rules.ObjectLogger;
import ru.yandex.mail.common.rules.RetryRule;
import ru.yandex.mail.common.rules.WriteAllureParamsRule;
import ru.yandex.mail.common.rules.XRequestIdRule;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.Labels;
import ru.yandex.mail.tests.hound.generated.ApiHound;
import ru.yandex.mail.tests.hound.generated.AttachSidRequest;
import ru.yandex.mail.tests.hound.generated.Download;
import ru.yandex.mail.tests.hound.generated.HoundResponse;
import ru.yandex.mail.tests.hound.generated.LabelSymbol;
import ru.yandex.mail.tests.sendbernar.generated.ApiSendbernar;
import ru.yandex.mail.tests.sendbernar.generated.PartsJson;
import ru.yandex.mail.tests.sendbernar.generated.SaveDraftResponse;
import ru.yandex.mail.tests.sendbernar.generated.WriteAttachmentResponse;
import ru.yandex.mail.tests.sendbernar.generated.callback.noanswerremind.ApiNoAnswerRemind;
import ru.yandex.mail.tests.sendbernar.generated.callback.remindmessage.ApiRemindMessage;
import ru.yandex.mail.tests.sendbernar.generated.callback.senddelayedmessage.ApiSendDelayedMessage;
import ru.yandex.mail.tests.sendbernar.generated.callback.sendundomessage.ApiSendUndoMessage;
import ru.yandex.mail.tests.sendbernar.generated.cancelsenddelayed.ApiCancelSendDelayed;
import ru.yandex.mail.tests.sendbernar.generated.cancelsendundo.ApiCancelSendUndo;
import ru.yandex.mail.tests.sendbernar.generated.listunsubscribe.ApiListUnsubscribe;
import ru.yandex.mail.tests.sendbernar.generated.savedraft.ApiSaveDraft;
import ru.yandex.mail.tests.sendbernar.generated.savetemplate.ApiSaveTemplate;
import ru.yandex.mail.tests.sendbernar.generated.senddelayed.ApiSendDelayed;
import ru.yandex.mail.tests.sendbernar.generated.sendmessage.ApiSendMessage;
import ru.yandex.mail.tests.sendbernar.generated.sendundo.ApiSendUndo;
import ru.yandex.mail.tests.sendbernar.generated.setmsgreminder.ApiSetMsgReminder;
import ru.yandex.mail.tests.sendbernar.generated.writeattachment.ApiWriteAttachment;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.mail.things.utils.DeleteLabelsMopsRule;
import ru.yandex.mail.things.utils.FolderList;
import ru.yandex.mail.things.utils.WaitForMessage;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.common.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.mail.common.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.mail.common.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.mail.common.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.mail.common.utils.Files.downloadFile;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.attachOk200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.FileCompareMatcher.hasSameMd5As;


@Features("SENDBERNAR")
public abstract class BaseSendbernarClass {
    abstract AccountWithScope mainUser();

    UserCredentials authClient = new UserCredentials(mainUser());

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @ClassRule
    public static IgnoreSshTestRule beforeSshTestClass = newIgnoreSshTestRule();

    @Rule
    public IgnoreSshTestRule beforeSshTest = newIgnoreSshTestRule();

    @Rule
    public ObjectLogger logger = ObjectLogger.objectLogger(props(), SendbernarProperties.properties());

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    public FolderList folderList = new FolderList(authClient);

    public WaitForMessage waitWith = new WaitForMessage(authClient);

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure(SendbernarProperties.properties().sendbernarUri(), props().scope());

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry()
            .ifException(Exception.class)
            .every(1, TimeUnit.SECONDS)
            .times(1);

    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).outbox().inbox().draft();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    String subj;

    static final String NO_SUBJECT_TITLE = "No subject";
    static final String IMG_URL_JPG = "http://img-fotki.yandex.ru/get/9300/219421176.0/0_d2a24_8d0e0fea_orig";
    static final String caller = "test_qa";
    static final String remindMessageLabelName = "SystMetka:remindme_about_message";
    static final String noAnswerReminderLabelName = "remindme_threadabout:mark";

    String unexistingUid() {
        return "4611686018427387904";
    }

    String getUid() {
        return authClient.account().uid();
    }

    @Before
    public void prepareBase() {
        subj = Random.string();
        waitWith = new WaitForMessage(authClient);
    }

    String randomAddress() {
        return Random.address().toLowerCase();
    }

    ApiSendbernar apiSendbernar() {
        return SendbernarApi.apiSendbernar(new RequestTraits()
                .withUrl(SendbernarProperties.properties().sendbernarUri())
                .withXRequestId(props().getCurrentRequestId())
        );
    }

    ApiSaveDraft saveDraft() {
        return apiSendbernar().saveDraft()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSaveTemplate saveTemplate() {
        return apiSendbernar().saveTemplate()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendMessage sendMessage() {
        return apiSendbernar().sendMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSetMsgReminder setMsgReminder() {
        return apiSendbernar().setMsgReminder()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiRemindMessage remindMessage() {
        return apiSendbernar().remindMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiNoAnswerRemind noAnswerRemind() {
        return apiSendbernar().noAnswerRemind()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendDelayed sendDelayed() {
        return apiSendbernar().sendDelayed()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendUndo sendUndo() {
        return apiSendbernar().sendUndo()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiCancelSendDelayed cancelSendDelayed() {
        return apiSendbernar().cancelSendDelayed()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiCancelSendUndo cancelSendUndo() {
        return apiSendbernar().cancelSendUndo()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendDelayedMessage sendDelayedMessage() {
        return apiSendbernar().sendDelayedMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiSendUndoMessage sendUndoMessage() {
        return apiSendbernar().sendUndoMessage()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiWriteAttachment writeAttachment() {
        return apiSendbernar().writeAttachment()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiListUnsubscribe listUnsubscribe() {
        return apiSendbernar().listUnsubscribe()
                .withUid(getUid())
                .withCaller(caller);
    }

    ApiHound houndApi() {
        return HoundApi.apiHound(HoundProperties.properties().houndUri(), props().getCurrentRequestId());
    }

    String lidByTitle(String title) {
        return Labels.labels(
                houndApi().labels()
                        .withUid(getUid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidByTitle(title);
    }

    String lidByName(String name) {
        return Labels.labels(
                houndApi().labels()
                        .withUid(getUid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidByName(name);
    }

    String lidByNameAndType(String name, String soName) {
        return Labels.labels(
                houndApi().labels()
                        .withUid(getUid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidByNameAndType(name, soName);
    }

    String seenLid() {
        return Labels.labels(
                houndApi().labels()
                        .withUid(getUid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidBySymbol(LabelSymbol.SEEN_LABEL);
    }

    String attachedLid() {
        return Labels.labels(
                houndApi().labels()
                        .withUid(getUid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidBySymbol(LabelSymbol.ATTACHED_LABEL);
    }

    String uploadedId() {
        byte[] content;

        try {
            content = FileUtils.readFileToByteArray(downloadFile(IMG_URL_JPG, Random.string()));
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

    Message byMid(String mid, UserCredentials rule) {
        return new Message(mid, rule);
    }

    void shouldSeeImageInMessage(File attach, String mid) throws Exception {
        Message resp = byMid(mid);

        String hid = resp.getAttachHidByName(attach.getName());

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

        List<String> sids = houndApi().attachSid()
                .withReq(
                        (req) -> req.setContentType("application/json").setBody(body)
                )
                .post(Function.identity())
                .as(HoundResponse.class)
                .getResult().get(0).getSids();

        String url = SendbernarProperties.properties().messagePartReal();
        String sid = URLDecoder.decode(sids.get(0), "UTF-8");

        File deliveredAttach = downloadFile(url, sid, attach.getName());
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
                .withTo(authClient.account().email())
                .withUploadedAttachStids(id)
                .withSubj(Random.string())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class);


        PartsJson p = new PartsJson()
                .withMid(Long.parseLong(resp.getStored().getMid()))
                .withHid(Double.parseDouble(resp.getAttachments().get(0).getHid()));


        return new Gson().toJson(p);
    }
}

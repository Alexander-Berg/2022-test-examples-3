package ru.yandex.autotests.innerpochta.mops;

import com.jayway.restassured.response.Response;
import com.tngtech.java.junit.dataprovider.DataProvider;
import lombok.val;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.mops.complexmove.ApiComplexMove;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.create.ApiFoldersCreate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.createhiddentrash.ApiFoldersCreateHiddenTrash;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.delete.ApiFoldersDelete;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.update.ApiFoldersUpdate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.updatepop3.ApiFoldersUpdatePop3;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.updateposition.ApiFoldersUpdatePosition;
import ru.yandex.autotests.innerpochta.wmi.core.mops.folders.updatesymbol.ApiFoldersUpdateSymbol;
import ru.yandex.autotests.innerpochta.wmi.core.mops.label.ApiLabel;
import ru.yandex.autotests.innerpochta.wmi.core.mops.labels.create.ApiLabelsCreate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.labels.delete.ApiLabelsDelete;
import ru.yandex.autotests.innerpochta.wmi.core.mops.labels.update.ApiLabelsUpdate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.mops.purge.ApiPurge;
import ru.yandex.autotests.innerpochta.wmi.core.mops.purgehiddentrash.ApiPurgeHiddenTrash;
import ru.yandex.autotests.innerpochta.wmi.core.mops.remove.ApiRemove;
import ru.yandex.autotests.innerpochta.wmi.core.mops.replylater.create.ApiReplyLaterCreate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.replylater.remove.ApiReplyLaterRemove;
import ru.yandex.autotests.innerpochta.wmi.core.mops.replylater.reset.ApiReplyLaterReset;
import ru.yandex.autotests.innerpochta.wmi.core.mops.replylater.update.ApiReplyLaterUpdate;
import ru.yandex.autotests.innerpochta.wmi.core.mops.spam.ApiSpam;
import ru.yandex.autotests.innerpochta.wmi.core.mops.unlabel.ApiUnlabel;
import ru.yandex.autotests.innerpochta.wmi.core.mops.unspam.ApiUnspam;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.Source;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitForMessage;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSyncOrAsync;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

public class MopsBaseTest {

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

    @Before
    public void updateFoldersAndLabels() {
        folderList = new FolderList(authClient);
        labels_ = Labels.labels(
                LabelsObj.empty()
                        .setUid(authClient.account().uid())
        ).get().via(authClient);
    }

    @Rule
    public LogConfigRule logConfigRule = new LogConfigRule();

    public static FolderList folderList = new FolderList(authClient);

    public WaitForMessage waitWith = new WaitForMessage(authClient);

    public static FolderList updatedFolderList() {
        return new FolderList(authClient);
    }

    public static Labels updatedLabels() {
        return Labels.labels(
                LabelsObj.empty()
                        .setUid(authClient.account().uid())
        ).get().via(authClient);
    }

    private static Labels labels_ = null;

    public static Labels labels() {
        if (labels_ == null) {
            labels_ = Labels.labels(
                    LabelsObj.empty()
                            .setUid(authClient.account().uid())
            ).get().via(authClient);
        }

        return labels_;
    }

    static String priorityHigh() {
        return labels().lidByName(LabelSymbol.PRIORITY_HIGH.toString());
    }

    static String answered() {
        return labels().lidByName(LabelSymbol.ANSWERED.toString());
    }

    static String forwarded() {
        return labels().lidBySymbol(LabelSymbol.FORWARDED_LABEL);
    }

    static String attached() { return labels().lidBySymbol(LabelSymbol.ATTACHED_LABEL); }

    static Long countByName(String name) {
        return labels().countByName(name);
    }

    static String mention_label() {
        return labels().lidByName(LabelSymbol.MENTION_LABEL.toString());
    }

    static String mention_unvisited_label() {
        return labels().lidByName(LabelSymbol.MENTION_UNVISITED_LABEL.toString());
    }

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            //эксперимент
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

    protected final Logger logger = LogManager.getLogger(this.getClass());

    static final String EMPTY_TASKS = "{\"tasks\":[]}";

    static SendContext sendMail() {
        return new SendContext(SendbernarUtils.sendWith(authClient).send().waitDeliver(), authClient);
    }

    static SendContext sendMail(String subject) {
        return new SendContext(SendbernarUtils.sendWith(authClient).subj(subject).send().waitDeliver(), authClient);
    }

    static SendContext sendMail(String subject, String fid) {
        return new SendContext(SendbernarUtils.sendWith(authClient).subj(subject).send().fid(fid).waitDeliver(), authClient);
    }

    static SendContext sendMail(int messagesCount, String subject) {
        return new SendContext(SendbernarUtils.sendWith(authClient).subj(subject).count(messagesCount).send().waitDeliver(), authClient);
    }

    static SendContext sendMail(int messagesCount) {
        return new SendContext(SendbernarUtils.sendWith(authClient).count(messagesCount).send().waitDeliver(), authClient);
    }

    static SendContext sendMail(File attach) {
        return new SendContext(SendbernarUtils.sendWith(authClient).addAttaches(attach).send().waitDeliver(), authClient);
    }

    static SendContext sendMailWithMention(String mention) {
        return new SendContext(SendbernarUtils.sendWith(authClient).mentions(mention).send().waitDeliver(), authClient);
    }

    void fillFolder(int messagesCount, String destFid, String subject) throws Exception {
        val mids = sendMail(messagesCount, subject).mids();
        if (!destFid.equals(folderList.defaultFID())) {
            complexMove(destFid, new MidsSource(mids)).withWithSent(ApiComplexMove.WithSentParam._0)
                    .post(shouldBe(okSyncOrAsync()));
            new WaitForMessage(authClient).subj(subject).count(messagesCount).fid(destFid).waitDeliver();
        }
    }

    static String createInnerFolderStructure(String parentFid, String name, int count) {
        String currParentFid = parentFid;

        for (int i = 0; i < count; i++) {
            currParentFid = Mops.newFolder(authClient, name, currParentFid);
        }
        return currParentFid;
    }

    static String createInnerFolderStructure(String parentFid, int count) {
        return createInnerFolderStructure(parentFid, getRandomString(), count);
    }

    String receiveLid(String name) {
        return updatedLabels().lidByName(name);
    }

    static void assertEmptyTasks(Response response) {
        assertThat("Есть непустые задачи", response.asString(), containsString(EMPTY_TASKS));
    }

    static void assertTaskWithType(Response response, String expectedType) {
        assertThat("Неверное значение у аттрибута <tasks>", response.asString(),
                containsString(String.format("\"type\":\"%s\"", expectedType)));
    }

    static void assertTaskWithGroupId(Response response, String expectTaskGroupId) {
        assertThat("Неверное значение аттрибута <taskGroupId>", response.asString(),
                containsString(String.format("\"taskGroupId\":\"%s\"", expectTaskGroupId)));
    }

    ApiComplexMove complexMove(String destFid, Source source) throws Exception {
        return Mops.complexMove(authClient, destFid, source);
    }

    ApiComplexMove complexMove(String destFid, String destTab, Source source) throws Exception {
        return Mops.complexMove(authClient, destFid, destTab, source);
    }

    ApiFoldersCreate createFolder(String name) {
        return Mops.createFolder(authClient, name);
    }

    String newFolder(String name) {
        return Mops.newFolder(authClient, name);
    }

    String newFolder(String name, String parentFid) {
        return Mops.newFolder(authClient, name, parentFid);
    }

    ApiFoldersDelete deleteFolder(String fid) {
        return Mops.deleteFolder(authClient, fid);
    }

    ApiFoldersUpdate updateFolder(String fid) {
        return Mops.updateFolder(authClient, fid);
    }

    ApiFoldersUpdate renameFolder(String fid, String name) {
        return Mops.renameFolder(authClient, fid, name);
    }

    ApiFoldersUpdatePop3 updatePop3(String... fids) {
        return Mops.updatePop3(authClient, fids);
    }

    ApiFoldersUpdatePosition updateFolderPosition(String fid) {
        return Mops.updateFolderPosition(authClient, fid);
    }

    ApiFoldersUpdateSymbol updateFolderSymbol(String fid) {
        return Mops.updateFolderSymbol(authClient, fid);
    }

    ApiLabelsCreate createLabel() {
        return Mops.createLabel(authClient);
    }

    String newLabelByName(String name) {
        return Mops.newLabelByName(authClient, name);
    }

    String newLabelByName(String name, String color) {
        return Mops.newLabelByName(authClient, name, color);
    }

    ApiLabelsDelete deleteLabel(String lid) {
        return Mops.deleteLabel(authClient, lid);
    }

    ApiLabelsUpdate updateLabel(String lid) {
        return Mops.updateLabel(authClient, lid);
    }

    ApiLabelsUpdate changeLabelColor(String lid, String color) {
        return Mops.changeLabelColor(authClient, lid, color);
    }

    ApiLabel label(Source source, String... lids) throws Exception {
        return Mops.label(authClient, source, asList(lids));
    }

    ApiLabel label(Source source, List<String> lids) throws Exception {
        return Mops.label(authClient, source, lids);
    }

    ApiUnlabel unlabel(Source source, String... lids) throws Exception {
        return Mops.unlabel(authClient, source, asList(lids));
    }

    ApiUnlabel unlabel(Source source, List<String> lids) throws Exception {
        return Mops.unlabel(authClient, source, lids);
    }

    ApiMark mark(Source source, StatusParam status) throws Exception {
        return Mops.mark(authClient, source, status);
    }

    ApiPurge purge(Source source) throws Exception {
        return Mops.purge(authClient, source);
    }

    ApiRemove remove(Source source) throws Exception {
        return Mops.remove(authClient, source);
    }

    ApiSpam spam(Source source) throws Exception {
        return Mops.spam(authClient, source);
    }

    ApiUnspam unspam(Source source) throws Exception {
        return Mops.unspam(authClient, source);
    }

    ApiFoldersCreateHiddenTrash createHiddenTrash() {
        return Mops.createHiddenTrash(authClient);
    }

    ApiPurgeHiddenTrash purgeHiddenTrash() {
        return Mops.purgeHiddenTrash(authClient);
    }

    ApiReplyLaterCreate createReplyLaterSticker(String mid, Long date) {
        return Mops.createReplyLaterSticker(authClient, mid, date);
    }

    ApiReplyLaterUpdate updateReplyLaterSticker(String mid, Long date) {
        return Mops.updateReplyLaterSticker(authClient, mid, date);
    }

    ApiReplyLaterReset resetReplyLaterSticker(String mid, Long date) {
        return Mops.resetReplyLaterSticker(authClient, mid, date);
    }

    ApiReplyLaterRemove removeReplyLaterSticker(String mid) {
        return Mops.removeReplyLaterSticker(authClient, mid);
    }

    @DataProvider
    public static Object[][] existingTabs() {
        return Tabs.existingTabs();
    }
}
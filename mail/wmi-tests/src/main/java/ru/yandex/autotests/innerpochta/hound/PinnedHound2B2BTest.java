package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderWithoutLabelObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolderWithoutLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThreadWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Title("[HOUND2] ????????. ???????????????? ?????????????? ???????????????????? ?????????? ?? ????????????")
@Description("?????????????? ???????????? ?????????? ?????????????? ?? ???????????? ????????????")
@Credentials(loginGroup = "PinsTest")
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.LABELS, MyStories.SYSTEM_FOLDERS, MyStories.FOLDERS})
@Issues({@Issue("DARIA-44825"), @Issue("DARIA-46008"), @Issue("DARIA-46010"), @Issue("DARIA-46510")})
public class PinnedHound2B2BTest extends BaseHoundTest {

    private static final String PIN_SUBJ = "PINNED_";
    private static final String NOTPIN_SUBJ = "NOTPINNED_";

    @Rule
    public CleanMessagesMopsRule clean = with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    public static final int COUNT_OF_LETTERS = 2;
    private static String b2bUri = props().houndB2bUri();

    private static MessagesByFolderObj pinsYwmiObj;

    private static MessagesByFolderWithoutLabelObj withoutLabelYwmiObj;
    private static String labelId;

    @BeforeClass
    public static void getInit() {
        pinsYwmiObj = new MessagesByFolderObj().setUid(uid()).setFirst("0").setCount("50").
                setFid(folderList.defaultFID())
                .setSortType("date1");

        labelId = Hound.getLidBySymbolTitle(authClient, PINNED);

        assertThat("?? ???????????????????????? ?????? ?????????? <pinned>", labelId, not(equalTo("")));

        withoutLabelYwmiObj = new MessagesByFolderWithoutLabelObj().setUid(uid())
                .setFirst("0").setCount("50").setFid(folderList.defaultFID())
                .setSortType("date1").setLid(labelId);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Title("?????????? *pins ???? ???????????? ??????????")
    @Description("?????????????????? ?????? ?????????? ???????? ??????????, ???? ???????????? ??????????.\n" +
            "???????????? ?????????????? ???????????? ???????????? ?? ????????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdWithoutLetterInMailbox() throws IOException {
        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Title("?????????? *pins ???? ?????????? ?? ???????????????? ????????????????")
    @Description("???????????????? ????????????. ?????????????????? ?????? ?????????? ???????? ?????????? ?????? ?????????????? ??????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdWithoutPinnedLetterInMailbox() throws Exception {
        String tid = sendWith(authClient).viaProd()
                .subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS)
                .send().waitDeliver().getTid();

        compareWithProd(folderList.defaultFID());
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issue("DARIA-46008")
    @Title("?????????? *pins ???? ?????????? ?? ?????????????? ???????????? \"pinned\" ????????????????")
    @Description("???????????????? ???????????? ?? ???????????????? ?????????????????? ???????????? _pinned_.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdWithOnlyPinnedLettersInMailbox() throws Exception {
        List<Envelope> envelopes = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getEnvelopes();

        List<String> mids = envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
        String tid = envelopes.get(0).getThreadId();

        // ???????????????? ?????????????????? ???????????? ?????? ?????????????????????????? ????????
        Mops.label(authClient, new MidsSource(mids), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(labelId, authClient, hasMsgsWithLid(mids, labelId));
        compareWithProd(folderList.defaultFID());

        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issues({@Issue("DARIA-46010"), @Issue("DARIA-46008")})
    @Title("?????????? *pins ???? ?????????? ?? \"pinned\" ?? ???????????????? ????????????????")
    @Description("???????????????? ????????????. ???????????????? ?????????? ?????????? ?????????????????? ???????????? _pinned_ .\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdWithPinnedLettersInMailbox() throws Exception {
        List<String> mids = sendWith(authClient).viaProd()
                .subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS)
                .send().waitDeliver().getMids();

        List<String> midsWithoutPins = sendWith(authClient).viaProd()
                .subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS)
                .send().waitDeliver().getMids();

        Mops.label(authClient, new MidsSource(mids), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(mids, labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issues({@Issue("DARIA-46010"), @Issue("DARIA-46008")})
    @Title("?????????? *pins ???? ?????????? ?? \"pinned\" ?? ???????????????? ????????????????")
    @Description("???????????????? ????????????. ???????????????? ???????????? ?????????????????? ???????????? _pinned_. ?????????? ?????? ???????????????? ????????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdWithMiddlePinnedLettersInMailbox() throws Exception {
        List<String> midsWithoutPins0 = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        List<String> mids = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        List<String> midsWithoutPins1 = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();


        // ???????????????? ?????????????????? ???????????? ?????? ?????????????????????????? ????????
        Mops.label(authClient, new MidsSource(mids), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(mids, labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins0, labelId)));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins1, labelId)));

        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.THREADS, MyStories.PINS})
    @Title("?????????? ???????????? ???? ??????????")
    @Description("???????????????? ???????????? _pinned_ 1 ???????????? ???? ??????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void pinnedOneNewMessageFromThread() throws Exception {
        List<Envelope> envelopes = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getEnvelopes();

        List<String> mids = envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
        String tid = envelopes.get(0).getThreadId();

        List<String> midsWithoutPins = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        // ???????????????? 1 ???????????? ???? ??????????
        Mops.label(authClient, new MidsSource(mids.get(0)), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgWithLid(mids.get(0), labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.THREADS, MyStories.PINS})
    @Title("1 ???????????? (???? ??????????) ???? ??????????")
    @Issue("DARIA-52999")
    @Description("???????????????? ???????????? _pinned_ 1 ???????????? (???? ??????????) ???? ??????????. ???????? ???????????? ?? ??????????????\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void pinnedOneMessageFromThread() throws Exception {
        List<Envelope> envelopes = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS + 1).send().waitDeliver().getEnvelopes();

        List<String> mids = envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
        String tid = envelopes.get(0).getThreadId();

        List<String> midsWithoutPins = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        // ???????????????? 1 ???????????? ???? ??????????
        Mops.label(authClient, new MidsSource(mids.get(1)), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgWithLid(mids.get(1), labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.FOLDERS, MyStories.PINS})
    @Title("?????????????? ???????????? ?? ???????????????????????????????? ????????????")
    @Description("?????????????????? ?????????????? ?? ???????????????????????????????? ??????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdPinnedLettersInUserFolder() throws Exception {
        String fid = Mops.newFolder(authClient, Util.getRandomString());

        //???????????????? ??????????:
        compareWithProd(fid);

        //???????????????? ???????????? ?? ???????????????? ?????????? _pinned_
        List<Envelope> envelopes = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
               .count(COUNT_OF_LETTERS).send().waitDeliver().getEnvelopes();

        List<String> mids = envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
        String tid = envelopes.get(0).getThreadId();

        List<String> midsWithoutPins = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
               .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();


        // ???????????????? ?????????????????? ???????????? ?????? ?????????????????????????? ????????
        Mops.label(authClient, new MidsSource(mids), asList(labelId))
               .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(mids, labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        //???????????????? ??????????:
        compareWithProd(fid);
        compareWithProdMessagesByThreadsWithPins(fid, tid);
    }

    @Test
    @Stories({MyStories.SYSTEM_FOLDERS, MyStories.PINS})
    @Title("?????????????? ???????????? ?? ?????????????????? ????????????")
    @Description("?????????????????? ?????????????? ???????????? ?? ?????????????????? ????????????.\n" +
            "???????????????????? ?? ?????????????????????? ???????????? ?????????? *pins")
    public void compareWithProdPinnedLettersInSystemFolders() throws Exception {
        //???????????????? ???????????? ?? ???????????????? ?????????? _pinned_
        List<String> mids = sendWith(authClient).subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        List<String> midsWithoutPins = sendWith(authClient).subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();

        // ???????????????? ?????????????????? ???????????? ?????? ?????????????????????????? ????????
        Mops.label(authClient, new MidsSource(mids), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(mids, labelId));
        assertThat(authClient, not(hasMsgsWithLid(midsWithoutPins, labelId)));
        for (String fid : Hound.folders(authClient).keySet()) {
            //???????????????? ??????????:
            compareWithProd(fid);
        }
    }

    private void compareWithProd(String fid) {
        compareWithProdWithoutLabel(fid);
        compareWithProdMessagesInFolderWithPins(fid);
        compareWithProdThreadsInFolderWithPins(fid);
    }

    private void compareWithProdWithoutLabel(String fid) {
        String respNew = api(MessagesByFolderWithoutLabel.class)
                .params(withoutLabelYwmiObj.setFid(fid)).setHost(props().houndUri())
                .get().via(authClient).toString();
        String respBase = api(MessagesByFolderWithoutLabel.class)
                .params(withoutLabelYwmiObj.setFid(fid)).setHost(b2bUri)
                .get().via(authClient).toString();

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    private void compareWithProdMessagesInFolderWithPins(String fid) {
        String respNew = api(MessagesInFolderWithPins.class)
                .params(pinsYwmiObj.setFid(fid)).setHost(props().houndUri())
                .get().via(authClient).toString();
        String respBase = api(MessagesInFolderWithPins.class)
                .params(pinsYwmiObj.setFid(fid)).setHost(b2bUri)
                .get().via(authClient).toString();

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    private void compareWithProdThreadsInFolderWithPins(String fid) {
        String respNew = api(ThreadsInFolderWithPins.class)
                .params(pinsYwmiObj.setFid(fid)).setHost(props().houndUri())
                .get().via(authClient).toString();
        String respBase = api(ThreadsInFolderWithPins.class)
                .params(pinsYwmiObj.setFid(fid)).setHost(b2bUri)
                .get().via(authClient).toString();
        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    private void compareWithProdMessagesByThreadsWithPins(String fid, String tid) {
        String respNew = api(MessagesByThreadWithPins.class)
                .params(pinsYwmiObj.setFid(fid).setTid(tid)).setHost(props().houndUri())
                .get().via(authClient).toString();
        String respBase = api(MessagesByThreadWithPins.class)
                .params(pinsYwmiObj.setFid(fid).setTid(tid)).setHost(b2bUri)
                .get().via(authClient).toString();;

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }
}

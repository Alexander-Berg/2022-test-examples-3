package ru.yandex.autotests.innerpochta.wmi.labels;

import gumi.builders.UrlBuilder;
import org.eclipse.jetty.http.HttpSchemes;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.HoundB2BTest;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.DariaMessagesObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderWithoutLabelObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolderWithoutLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThreadWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.15
 * Time: 15:28
 * <p/>
 * DARIA-46010
 * DARIA-46008
 * DARIA-44825
 */
@Aqua.Test
@Title("[PINS] Пины. Проверка нужного количества писем в выдаче")
@Description("Проверяем выдачу ручек ywmi_api ручек и wmi-айных")
@Credentials(loginGroup = "PinsTest")
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.LABELS, MyStories.SYSTEM_FOLDERS, MyStories.FOLDERS})
@Issues({@Issue("DARIA-44825"), @Issue("DARIA-46008"), @Issue("DARIA-46010"), @Issue("DARIA-46510")})
public class PinnedB2BTest extends BaseTest {

    public static final String PIN_SUBJ = "PINNED_";
    public static final String NOTPIN_SUBJ = "NOTPINNED_";

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox().deleted();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    public static final int COUNT_OF_LETTERS = 2;

    public static MessagesByFolderObj pinsYwmiObj;

    public static MessagesByFolderWithoutLabelObj withoutLabelYwmiObj;
    private static String labelId;

    @BeforeClass
    public static void getInit() throws IOException {
        pinsYwmiObj = new MessagesByFolderObj().setUid(composeCheck.getUid()).setFirst("0").setCount("50").
                setFid(folderList.defaultFID())
                .setSortType("date1");


        labelId = labels.pinned();

        assertThat("У пользователя нет метки <pinned>", labelId, not(equalTo("")));

        withoutLabelYwmiObj = new MessagesByFolderWithoutLabelObj().setUid(composeCheck.getUid())
                .setFirst("0").setCount("50").setFid(folderList.defaultFID())
                .setSortType("date1").setLid(labelId);

    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Title("Ручки *pins на пустом ящике")
    @Description("Проверяем как ведут себя ручки, на пустом ящике.\n" +
            "Должны увидеть пустую выдачу в ручках.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdWithoutLetterInMailbox() throws IOException {
        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Title("Ручки *pins на ящике с обычными письмами")
    @Description("Отсылаем письма. Проверяем как ведут себя ручки без пиновых писем.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdWithoutPinnedLetterInMailbox() throws Exception {
        String tid = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString()).viaProd().count(COUNT_OF_LETTERS).waitDeliver().send().getTid();

        compareWithProd(folderList.defaultFID());
        compareWithProdDariaMessagesThreadsAndPins(folderList.defaultFID(), tid);
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issue("DARIA-46008")
    @Title("Ручки *pins на ящике в котором только \"pinned\" письмами")
    @Description("Отсылаем письма и помечаем системной меткой _pinned_.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdWithOnlyPinnedLettersInMailbox() throws Exception {
        SendUtils sendUtils = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send();
        List<String> mids = sendUtils.getMids();
        String tid = sendUtils.getTid();

        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(labels.pinned(), hc, hasMsgsWithLid(mids, labelId));
        compareWithProd(folderList.defaultFID());

        compareWithProdDariaMessagesThreadsAndPins(folderList.defaultFID(), tid);
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);

    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issues({@Issue("DARIA-46010"), @Issue("DARIA-46008")})
    @Title("Ручки *pins на ящике с \"pinned\" и обычными письмами")
    @Description("Отсылаем письма. Помечаем часть писем системной меткой _pinned_ .\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdWithPinnedLettersInMailbox() throws Exception {
        List<String> mids = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        List<String> midsWithoutPins = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();


        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgsWithLid(mids, labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.PINS})
    @Issues({@Issue("DARIA-46010"), @Issue("DARIA-46008")})
    @Title("Ручки *pins на ящике с \"pinned\" и обычными письмами")
    @Description("Отсылаем письма. Помечаем письма системной меткой _pinned_. Затем еще отсылаем письма.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdWithMiddlePinnedLettersInMailbox() throws Exception {
        List<String> midsWithoutPins0 = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        List<String> mids = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        List<String> midsWithoutPins1 = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();


        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgsWithLid(mids, labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins0, labelId)));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins1, labelId)));

        compareWithProd(folderList.defaultFID());
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.THREADS, MyStories.PINS})
    @Title("Новое письмо из треда")
    @Description("Помечаем меткой _pinned_ 1 письмо из треда.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void pinnedOneNewMessageFromThread() throws Exception {
        SendUtils sendUtils = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send();

        List<String> mids = sendUtils.getMids();
        String tid = sendUtils.getTid();

        List<String> midsWithoutPins = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        // Помечаем 1 письмо из треда
        MessageToLabel.messageToLabel(MessageToLabelUnlabelObj.labelOne(mids.get(0), labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgWithLid(mids.get(0), labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());

        compareWithProdDariaMessagesThreadsAndPins(folderList.defaultFID(), tid);
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.LETTERS, MyStories.THREADS, MyStories.PINS})
    @Title("1 письмо (НЕ НОВОЕ) из треда")
    @Issue("DARIA-52999")
    @Description("Помечаем меткой _pinned_ 1 письмо (НЕ НОВОЕ) из треда. Была ошибка в запросе\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void pinnedOneMessageFromThread() throws Exception {
        SendUtils sendUtils = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS + 1).waitDeliver().send();

        List<String> mids = sendUtils.getMids();
        String tid = sendUtils.getTid();

        List<String> midsWithoutPins = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        // Помечаем 1 письмо из треда
        MessageToLabel.messageToLabel(MessageToLabelUnlabelObj.labelOne(mids.get(1), labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgWithLid(mids.get(1), labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        compareWithProd(folderList.defaultFID());

        compareWithProdDariaMessagesThreadsAndPins(folderList.defaultFID(), tid);
        compareWithProdMessagesByThreadsWithPins(folderList.defaultFID(), tid);
    }

    @Test
    @Stories({MyStories.FOLDERS, MyStories.PINS})
    @Title("Пиновые письма в пользовательских папках")
    @Description("Проверяем пиновые в пользовательской папке.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdPinnedLettersInUserFolder() throws Exception {
        String folderName = Util.getRandomString();
        newFolder(folderName).post().via(hc);
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folderName);

        //проверки ручек:
        compareWithProd(fid);

        //отсылаем письма и помечаем часть _pinned_
        SendUtils sendUtils = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send();

        List<String> mids = sendUtils.getMids();
        String tid = sendUtils.getTid();

                List<String> midsWithoutPins = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();


        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgsWithLid(mids, labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins, labelId)));

        //проверки ручек:
        compareWithProd(fid);
        compareWithProdDariaMessagesThreadsAndPins(fid, tid);
        compareWithProdMessagesByThreadsWithPins(fid, tid);
    }

    @Test
    @Stories({MyStories.SYSTEM_FOLDERS, MyStories.PINS})
    @Title("Пиновые письма в системных папках")
    @Description("Проверяем пиновые письма в системных папках.\n" +
            "Сравниваем с продакшеном выдачу ручек *pins")
    public void compareWithProdPinnedLettersInSystemFolders() throws Exception {
        List<String> fids = api(FolderList.class).post().via(hc).getAllFolderIds();
        //отсылаем письма и помечаем часть _pinned_
        List<String> mids = sendWith.subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        List<String> midsWithoutPins = sendWith.subj(NOTPIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        // Помечаем созданной меткой все вышенайденные миды
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgsWithLid(mids, labelId));
        assertThat(hc, not(hasMsgsWithLid(midsWithoutPins, labelId)));
        for (String fid : fids) {
            //проверки ручек:
            compareWithProd(fid);
        }
    }

    public void compareWithProd(String fid) throws IOException {
        compareWithProdWithoutLabel(fid);
        compareWithProdDariaMessages(fid);
        compareWithProdDariaMessagesThreads(fid);
        compareWithProdMessagesInFolderWithPins(fid);
        compareWithProdThreadsInFolderWithPins(fid);
    }

    public void compareWithProdWithoutLabel(String fid) throws IOException {
        MessagesByFolderWithoutLabel oper = api(MessagesByFolderWithoutLabel.class)
                .params(withoutLabelYwmiObj.setFid(fid)).setHost(props().houndUri());

        String respNew = oper.get().via(hc).toString();
        String respBase = oper.setHost(UrlBuilder.empty().withScheme(HttpSchemes.HTTP).
                withPort(props().houndPort()).withHost(props().b2bUri().getHost()).toString()).get().via(hc).toString();

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }


    public void compareWithProdDariaMessages(String fid) throws IOException {
        Oper respNew = jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(fid).withPins())
                .post().via(hc);

        Oper respBase = jsx(DariaMessages.class).setHost(props().b2bUri().toString()).params(DariaMessagesObj.getObjCurrFolder(fid).withPins())
                .post().via(hc);

        MatcherAssert.assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }

    public void compareWithProdDariaMessagesThreads(String fid) throws IOException {
        Oper respNew = jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(fid).withPins().threaded()).post().via(hc);

        Oper respBase = jsx(DariaMessages.class).setHost(props().b2bUri().toString())
                .params(DariaMessagesObj.getObjCurrFolder(fid).withPins().threaded()).post().via(hc);

        MatcherAssert.assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }

    public void compareWithProdMessagesInFolderWithPins(String fid) throws IOException {
        MessagesInFolderWithPins oper = api(MessagesInFolderWithPins.class).params(pinsYwmiObj.setFid(fid)).setHost(props().houndUri());

        String respNew = oper.get().via(hc).toString();
        String respBase = oper.setHost(UrlBuilder.empty().withScheme(HttpSchemes.HTTP).
                withPort(props().houndPort()).withHost(props().b2bUri().getHost()).toString()).get().via(hc).toString();

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    public void compareWithProdThreadsInFolderWithPins(String fid) throws IOException {
        ThreadsInFolderWithPins oper = api(ThreadsInFolderWithPins.class).params(pinsYwmiObj.setFid(fid)).setHost(props().houndUri());

        String respNew = oper.get().via(hc).toString();
        String respBase = oper.setHost(UrlBuilder.empty().withScheme(HttpSchemes.HTTP).
                withPort(props().houndPort()).withHost(props().b2bUri().getHost()).toString()).get().via(hc).toString();
        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }

    //DARIA-47992
    public void compareWithProdDariaMessagesThreadsAndPins(String fid, String tid) throws IOException {
        Oper respNew = jsx(DariaMessages.class).params(DariaMessagesObj.getObjCurrFolder(fid).withPins()
                .setThreadId(tid)).post().via(hc);

        Oper respBase = jsx(DariaMessages.class).setHost(props().b2bUri().toString())
                .params(DariaMessagesObj.getObjCurrFolder(fid).withPins().setThreadId(tid)).post().via(hc);

        MatcherAssert.assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }


    public void compareWithProdMessagesByThreadsWithPins(String fid, String tid) throws IOException {
        MessagesByThreadWithPins oper = api(MessagesByThreadWithPins.class)
                .params(pinsYwmiObj.setFid(fid).setTid(tid)).setHost(props().houndUri());

        String respNew = oper.get().via(hc).toString();
        String respBase = oper.setHost(UrlBuilder.empty().withScheme(HttpSchemes.HTTP).
                withPort(props().houndPort()).withHost(props().b2bUri().getHost()).toString()).get().via(hc).toString();

        Map expectedMap = JsonUtils.getObject(respBase, Map.class);
        Map actualMap = JsonUtils.getObject(respNew, Map.class);

        MatcherAssert.assertThat(actualMap, beanDiffer(expectedMap).fields(HoundB2BTest.IGNORE_DEFAULT));
    }
}

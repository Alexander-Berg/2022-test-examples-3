package ru.yandex.autotests.innerpochta.wmi.labels;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static ch.lambdaj.collection.LambdaCollections.with;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.ToObjectConverter.wrap;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 03.04.15
 * Time: 17:15
 */
@Aqua.Test
@Title("[LABELS] Действия с системные метками. В основном простые кейсы. Параметризованный тест")
@Description("Проверяем что не можем удалить/создать/переименовать системные метки")
@Features(MyFeatures.WMI)
@Stories({MyStories.LABELS, MyStories.PINS})
@Credentials(loginGroup = "SystemLabelsTest")
@RunWith(Parameterized.class)
@Issue("MAILPG-316")
public class SystemLabelsTest extends BaseTest {

    public static final int COUNT_OF_LETTERS = 2;

    private String lid;
    private String subject;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<LabelSymbol> data() {
        return GroupOperationsWithSystemLabels.data();
    }

    @Parameterized.Parameter
    public LabelSymbol labelSymbol;

    private String systemLabelName;

    @Before
    public void prepare() throws Exception {
        lid = labels.get().lidBySymbol(labelSymbol);
        systemLabelName = labels.get().nameBySymbol(labelSymbol);
        subject = Util.getRandomString();
    }

    @Test
    @Title("Простая пометка письма меткой")
    @Description("Посылка 1 письма, получение его мид\n" +
            "Создание метки со случайным именем и получение lid\n" +
            "Пометка сообщения меткой\n" +
            "- Проверка что письмо пометилось меткой\n" +
            "Чистка")
    public void messageToSystemLabel() throws Exception {
        logger.warn("Простая пометка письма меткой");
        String mid = sendWith.viaProd().waitDeliver().send().getMid();
        jsx(MessageToLabel.class)
                .params(MessageToLabelUnlabelObj.labelOne(mid, lid))
                .post().via(hc).errorcodeShouldBeEmpty();
        assertThat("Письмо с mid:" + mid + " не пометилось меткой:" + lid, hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("Пометка меткой целикового треда")
    @Description("Отправка случайного небольшого количества писем\n" +
            "Создание метки со случайным именем\n" +
            "Пометка всех писем треда меткой\n" +
            "- Проверка что все письма пометились")
    public void messagesToSystemLabel() throws Exception {
        logger.warn("Пометка меткой целикового треда");
        List<String> mids = sendWith.subj(subject).count(COUNT_OF_LETTERS).viaProd().waitDeliver().send().getMids();

        api(MessageToLabel.class)
                .params(MessageToLabelUnlabelObj.labelMessages(mids, lid))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat("Не все письма с темой:" + subject + " пометились меткой:" + lid, hc,
                hasMsgsWithLid(mids, lid));
    }

    @Test
    @Title("Удаление системных меток")
    @Description("Пробуем удалить(с force и без) системные метки.\n" +
            "Ожидаемый результат: ошибка с кодом 5001")
    public void deleteSystemLabelShouldSee5001() throws IOException {
        jsx(SettingsLabelDelete.class)
                .params(SettingsLabelDeleteObj.oneLid(lid))
                .post().via(authClient.authHC()).errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);

        // Удаляем метку с force параметром
        jsx(SettingsLabelDelete.class)
                .params(SettingsLabelDeleteObj.oneLid(lid).setForce("yes"))
                .post().via(hc).errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);
    }


    @Test
    @Title("Создание системных меток")
    @Description("Пробуем создать системные метки.\n" +
            "Ожидаемый результат: ошибка с кодом 1003")
    public void createSystemLabelShouldSee1003() throws IOException {
        jsx(SettingsLabelCreate.class)
                .params(SettingsLabelCreateObj.empty().setLabelName(systemLabelName))
                .post().via(hc).errorcode(WmiConsts.WmiErrorCodes.DB_UNKNOWN_ERROR_1000);
    }

    @Test
    @Title("Переименование системных меток")
    @Description("Пробуем переименовать системные метки.\n" +
            "Ожидаемый результат: ошибка с кодом 5001")
    public void renameSystemLabelShouldSee5001() throws IOException {
        jsx(SettingsLabelRename.class)
                .params(SettingsLabelRenameObj.getEmptyObj().setLid(lid).setLabelName(Util.getRandomString()))
                .post().via(hc).errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);
    }

    @Test
    @Title("Пометка меткой в папке \"Удаденные\"")
    @Description("Помечаем письмо которое находится в удаленных.\n" +
            "Ожидаемый результат: ошибка с кодом 31")
    public void markLetterInDeletedFolder() throws Exception {
       String mid = sendWith.viaProd().waitDeliver().send().getMid();

       jsx(MailboxOper.class).params(MailboxOperObj.moveOneMsg(mid, folderList.deletedFID(), folderList.defaultFID())).post().via(hc).resultIdOk();

       api(MessageToLabel.class)
                .params(MessageToLabelUnlabelObj.labelOne(mid, lid))
                .post().via(hc);

        assertThat("Письмо с mid:" + mid + " в Удаленных не пометилось меткой:" + lid, hc,
                hasMsgWithLidInFolder(mid, folderList.deletedFID(), lid));
    }


    @Test
    @Title("Дважды помечаем письмо систеной меткой")
    @Description("Дважлы помечаем письмо меткой. Ожидаемый результат: Ок")
    public void doubleMarkSystemLabel() throws Exception {
        logger.warn("Простая пометка письма меткой");
        String mid = sendWith.viaProd().waitDeliver().send().getMid();
        jsx(MessageToLabel.class)
                .params(MessageToLabelUnlabelObj.labelOne(mid, lid))
                .post().via(hc).errorcodeShouldBeEmpty();
        assertThat("Письмо с mid:" + mid + " не пометилось меткой:" + lid, hc, hasMsgWithLid(mid, lid));

        jsx(MessageToLabel.class).params(MessageToLabelUnlabelObj.labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        assertThat("У письма с mid:" + mid + " при повторном помечании, исчезла метка:" + lid, hc, hasMsgWithLid(mid, lid));
    }

    @Test
    @Title("Проверка снятия метки")
    @Description("Отправка одного письма\n" +
            "Пометка его новосозданной меткой\n" +
            "Снятие этой метки\n" +
            "- Проверка что метка снялась")
    public void messageToUnlabel() throws Exception {
        logger.warn("Проверка снятия метки");
        String mid = sendWith.subj(subject).viaProd().waitDeliver().send().getMid();

        api(MessageToLabel.class)
                .params(MessageToLabelUnlabelObj.labelOne(mid, lid))
                .post().via(hc).errorcodeShouldBeEmpty();

        api(MessageToUnlabelOneLabel.class)
                .params(MessageToLabelUnlabelObj.labelOne(mid, lid))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat("Письмо с mid:" + mid + " не сбросило метку:" + lid, hc,
                not(hasMsgWithLid(mid, lid)));
    }

}

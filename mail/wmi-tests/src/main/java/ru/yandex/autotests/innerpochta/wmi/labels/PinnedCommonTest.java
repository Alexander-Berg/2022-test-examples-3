package ru.yandex.autotests.innerpochta.wmi.labels;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.DariaMessagesObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.DariaMessages;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 22.04.15
 * Time: 19:39
 */
@Aqua.Test
@Title("[PINS] Пины. Проверка нужного количества писем в выдаче")
@Description("Проверяем выдачу ручек ywmi_api ручек и wmi-айных")
@Credentials(loginGroup = "PinnedCommonTest")
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.LABELS})
public class PinnedCommonTest extends BaseTest {

    public static final String PIN_SUBJ = "PINNED_";
    public static final String NOTPIN_SUBJ = "NOTPINNED_";

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox().deleted();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    public static final int COUNT_OF_LETTERS = 2;

    private static String labelId;

    @BeforeClass
    public static void getInit() throws IOException {
        labelId = labels.pinned();
        assertThat("У пользователя нет метки <pinned>", labelId, not(equalTo("")));
    }

    @Test
    @Title("Количество тредов и писем в выдаче")
    @Description("На корпах была бага в том что выдавали количество писем всегда 0")
    public void testCountWithPins() throws Exception {
        List<String> mids = sendWith.viaProd().subj(PIN_SUBJ + Util.getRandomString())
                .count(COUNT_OF_LETTERS).waitDeliver().send().getMids();

        sendWith.viaProd().subj(NOTPIN_SUBJ + Util.getRandomString()).count(COUNT_OF_LETTERS)
                .waitDeliver().send().getMids();

        // Помечаем тред меткой пиннед
        MessageToLabel.messageToLabel(labelMessages(mids, labelId))
                .post().via(hc).errorcodeShouldBeEmpty();

        assertThat(hc, hasMsgsWithLid(mids, labelId));

        DariaMessages dariaMessages = jsx(DariaMessages.class).params(DariaMessagesObj
                .getObjCurrFolder(folderList.defaultFID()).withPins().threaded()).post().via(hc);

        assertThat("Ожидалось другое количество писем в выдаче", dariaMessages.getCount(), equalTo(COUNT_OF_LETTERS * 2));
    }
}

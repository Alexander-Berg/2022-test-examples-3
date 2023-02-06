package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.NOT_EXIST_LID;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.IsThereLabel.hasLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 03.12.15
 * Time: 18:09
 *
 * https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface/#post/labels/delete
 */
@Aqua.Test
@Title("[MOPS] Удаление меток")
@Description("Удаляем метку, проверяем что метка снимается со всех писем")
@Features(MyFeatures.MOPS)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "LabelsDeleteMopsTest")
public class LabelsDeleteTest extends MopsBaseTest {
    private static final int COUNT_OF_LETTERS = 3;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).inbox().outbox();

    @Test
    @Title("Простое удаление метки")
    @Issue("DARIA-53251")
    public void labelsDeleteMopsTest() throws Exception {
        val labelName = getRandomString();
        val lid = newLabelByName(labelName);
        deleteLabel(lid).post(shouldBe(okSync()));
        assertThat("Метка не удалилась", authClient, not(hasLabel(lid)));
    }

    @Test
    @Title("Должны снять метку с сообщений, при удалении метки")
    @Issue("DARIA-53251")
    public void labelsDeleteMopsWithMessagesTest() throws Exception {
        val labelName = getRandomString();
        val lid = newLabelByName(labelName);
        val mids = sendMail(COUNT_OF_LETTERS).mids();


        label(new MidsSource(mids), lid).post(shouldBe(okSync()));
        deleteLabel(lid).post(shouldBe(okSync()));


        assertThat("Метка не удалилась", authClient, not(hasLabel(lid)));
        assertThat(authClient, withWaitFor(not(hasLabel(lid))));
    }


    @Test
    @Title("Удаление метки с несуществующим lid")
    @Description("В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    @Issue("DARIA-53251")
    public void labelsDeleteMopsWithNotExitLidTest() throws Exception {
        deleteLabel(NOT_EXIST_LID).post(shouldBe(okSync()));
    }
}

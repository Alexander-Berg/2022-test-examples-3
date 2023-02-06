package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj.empty;

@Aqua.Test
@Title("[HOUND] Ручка labels")
@Description("Тесты на ручку labels")
@Features(MyFeatures.HOUND)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = "HoundLabelsTest")
public class LabelsTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @Test
    @Title("Ручка labels с системной и пользовательской метками")
    @Description("Создаём метку." +
            "Проверяем, что ручка возвращает созданную метку и хотя бы одну системную.")
    public void testLabelsHandlerWithUserAndSystemLabel() {
        String labelName = Util.getRandomString();
        Mops.newLabelByName(authClient, labelName);

        Labels labels = api(Labels.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid()))
                .get()
                .via(authClient);

        assertTrue("Не нашли ни одной системной метки", labels.labels().entrySet().stream()
                .anyMatch(lbl -> lbl.getValue().getIsSystem()));
        assertThat("Не нашли созданную метку", labels.lidByName(labelName), notNullValue());
    }
}

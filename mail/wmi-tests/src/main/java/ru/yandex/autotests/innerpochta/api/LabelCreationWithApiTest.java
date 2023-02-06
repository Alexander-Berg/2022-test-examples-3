package ru.yandex.autotests.innerpochta.api;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.LabelCreatedMatcher.hasLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsLabelCreateObj.empty;

@Aqua.Test
@Title("[API] Создание метки API ручкой")
@Description("Создание метки апи ручкой.")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = LabelCreationWithApiTest.LOGIN_GROUP)
public class LabelCreationWithApiTest extends BaseTest {
    public static final String DEFAULT_COLOR = "3126463";
    public static final String LOGIN_GROUP = "LabelApiCreationTest";

    @Rule
    public RuleChain clearLabels = new LogConfigRule()
            .around(DeleteLabelsRule.with(authClient).all());

    @Test
    @Title("Должны создать метку через API метод")
    public void createAndDeleteLabel() throws Exception {
        logger.warn("Простое создание метки");
        String labelName = Util.getRandomString();
        String labelColor = DEFAULT_COLOR;
        api(SettingsLabelCreate.class).params(empty().setLabelName(labelName).setLabelColor(labelColor))
                .post().via(hc);
        assertThat(hc, withWaitFor(hasLabel(labelName, labelColor)));
    }
}

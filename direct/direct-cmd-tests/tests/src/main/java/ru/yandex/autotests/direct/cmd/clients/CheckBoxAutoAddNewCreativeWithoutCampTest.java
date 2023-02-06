package ru.yandex.autotests.direct.cmd.clients;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск: testirt-9730
@Aqua.Test
@Description("проверка значения is_agreed_on_creatives_autogeneration при отсутсвии кампании")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.USER_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class CheckBoxAutoAddNewCreativeWithoutCampTest {
    private static final String CLIENT = "at-direct-auto-add-creative-1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();


    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Test
    @Description("при отсутствии смарт-кампаний поле не передается")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9571")
    public void withPerfomanceCampsTurnOn() {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("значение is_agreed_on_creatives_autogeneration соответствует ожиданиям",
                userSettings.getIsAgreedOnCreativesAutogeneration(),
                nullValue()
        );
    }
}

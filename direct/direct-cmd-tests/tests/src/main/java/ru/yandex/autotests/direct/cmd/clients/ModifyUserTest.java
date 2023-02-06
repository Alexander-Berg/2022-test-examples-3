package ru.yandex.autotests.direct.cmd.clients;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Редактирование/отображение настроек пользователя (контроллер modifyUser)")
@Stories(TestFeatures.Client.MODIFY_USER)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class ModifyUserTest {

    protected static final String TEMPLATE_1_NAME = "cmd.modifyUser.model.forModifyUserTest-1";
    protected static final String TEMPLATE_2_NAME = "cmd.modifyUser.model.forModifyUserTest-2";
    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-backend-modifyuser-1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private ModifyUserModel modifyUserModel;

    @Before
    public void before() {
        resetUserSettings();
        modifyUserModel = BeanLoadHelper.loadCmdBean(TEMPLATE_2_NAME, ModifyUserModel.class);
        modifyUserModel.setUlogin(CLIENT);
    }

    @Test
    @Description("Редактирование/отображение настроек пользователя (контроллер modifyUser)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9572")
    public void testSaveAtModifyUserModel() {
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);

        ModifyUserModel actual = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT);
        ModifyUserModel expected = modifyUserModel.toResponse();

        assertThat("настройки пользователя не соответствуют выставленным",
                actual, beanDiffer(expected).useCompareStrategy(buildCompareStrategy()));
    }

    private void resetUserSettings() {
        ModifyUserModel reset = BeanLoadHelper.loadCmdBean(TEMPLATE_1_NAME, ModifyUserModel.class);
        reset.withUlogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(reset);

        ModifyUserModel expected = reset.toResponse();
        ModifyUserModel actual = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT);
        assumeThat("не удалось сбросить настройки перед тестом",
                actual, beanDiffer(expected).useCompareStrategy(buildCompareStrategy()));
    }

    private CompareStrategy buildCompareStrategy() {
        return DefaultCompareStrategies.allFieldsExcept(
                newPath("clientID"),
                newPath("uid"),
                newPath("currency"),
                newPath("defaultFeedCountLimit"),
                newPath("defaultFeedMaxFileSize"),
                newPath("forceMulticurrencyTeaser"),
                newPath("modifyConvertAllowed"),
                newPath("noDisplayHrefs"));
    }
}

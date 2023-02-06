package ru.yandex.autotests.direct.cmd.clients.greenurl;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Редактирование/отображение настройки пользователя no_display_hrefs (контроллер modifyUser)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class NoDisplayHrefsParamAtModifyUserTest {

    private static final String CLIENT = "at-direct-backend-modifyuser-3";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String resetValue;
    @Parameterized.Parameter(1)
    public String newValue;

    @Parameterized.Parameters(name = "no_display_hrefs: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"", "1"},
                {"1", ""}
        });
    }

    @Before
    public void before() {
        resetOption();
    }

    @Test
    @Description("Редактирование/отображение настройки пользователя no_display_hrefs (контроллер modifyUser)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9590")
    public void testNoDisplayHrefsParamAtModifyUser() {
        String actual = setAndGetOption(newValue);
        assertThat("настройка пользователя no_display_hrefs соответствует ожидаемой",
                actual, getExpectedMatcher(newValue));
    }

    private void resetOption() {
        String actual = setAndGetOption(resetValue);
        assumeThat("перед тестом удалось сбросить настройку пользователя no_display_hrefs",
                actual, getExpectedMatcher(resetValue));
    }

    private String setAndGetOption(String value) {
        ModifyUserModel modifyUserModel = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT);
        modifyUserModel.withPhone("+79999999999").withNoDisplayHrefs(value).withUlogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);
        return cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT).getNoDisplayHrefs();
    }

    private Matcher getExpectedMatcher(String value) {
        return "".equals(value) ? anyOf(nullValue(), equalTo("0")) : equalTo("1");
    }
}

package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Пересохранение текстовых дополнений")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_BANNERS_ADDITIONS)
public class SaveAndUpdateClientCalloutsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-5";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CalloutsTestHelper helper;

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), null);

        helper.clearCalloutsForClient();
    }

    @Test
    @Description("Повторное сохранение дополнений с добавлением нового")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9084")
    public void saveCalloutsAddExistingAndNew() {
        String[] expectedCallouts = new String[]{"callout1", "callout2"};
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, expectedCallouts);
        expectedCallouts = ArrayUtils.add(expectedCallouts, "callout3");
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, expectedCallouts);

        List<String> actual = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(ulogin);

        assertThat("у клиента " + expectedCallouts.length + " дополнения", actual, hasSize(expectedCallouts.length));

        assertThat("у клиента присутствуют ожидаемые дополнения", actual, containsInAnyOrder(expectedCallouts));
    }


    @Test
    @Description("Сохранение нового уточнения если уже есть сохраненние")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9085")
    public void saveCalloutsAddNew() {
        String[] expectedCallouts = new String[]{"callout1", "callout2"};
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, expectedCallouts);
        expectedCallouts = ArrayUtils.add(expectedCallouts, "callout3");
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, "callout3");

        List<String> actual = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(ulogin);

        assertThat("у клиента присутствуют дополнения", actual, containsInAnyOrder(expectedCallouts));
    }

}

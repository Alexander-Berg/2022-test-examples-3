package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Сохранение уточнений на клиента")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@RunWith(Parameterized.class)
@Tag(CmdTag.SAVE_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class SaveClientCalloutsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-4";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public List<String> callouts;

    @Parameterized.Parameters(name = "Уточнения: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Collections.singletonList("c")},
                {Collections.singletonList(RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH))},
                {Arrays.asList("Callout", "expectedCallout")},
                {Collections.singletonList("with spaces")},
                {Arrays.asList("english", "русское")},
                {Collections.singletonList("[ё,./;'\"#№$%&*_]")}
        });
    }

    @Before
    public void setUp() {
        CalloutsTestHelper helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9087")
    public void saveCallouts() {
        String[] expectedCallouts = callouts.toArray(new String[callouts.size()]);
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin, expectedCallouts);
        List<String> actual = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(ulogin);
        assertThat("у клиента присутствуют дополнения", actual, containsInAnyOrder(expectedCallouts));
    }
}

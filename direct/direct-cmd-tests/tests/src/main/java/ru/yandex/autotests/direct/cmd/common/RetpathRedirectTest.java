package ru.yandex.autotests.direct.cmd.common;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CommonTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка редиректа (метод redirect(req, retpath)) для параметра retpath")
@Stories(TestFeatures.Common.RETPATH)
@Features(TestFeatures.COMMON)
@RunWith(Parameterized.class)
@Tag(CommonTag.YES)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class RetpathRedirectTest {
    private static final String CLIENT = "at-direct-cmd-retpath-c1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameter(0)
    public String retPath;
    @Parameterized.Parameter(1)
    public Matcher<String> locationMatcher;

    @Parameterized.Parameters(name = "retpath = {0}, location = {1}")
    public static Collection<Object[]> getParameters() {
        String url = "http://yandex.ru";
        String relativePath = "main.pl?cmd=showCamp&cid=123";
        return Arrays.asList(new Object[][]{
                {url, equalTo(url)},
                {relativePath, equalTo(relativePath)}
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        cmdRule.getApiStepsRule().as(CLIENT);
    }

    @Test
    @Description("Метод redirect(req, retpath) на ручке cmd_stopBanner")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9595")
    public void testRetpathSimple() {
        RedirectResponse response = cmdRule.cmdSteps().
                stopResumeBannerSteps().postStopBanner(bannersRule.getCampaignId(), bannersRule.getBannerId(), retPath);

        String actualLocation = response.getLocation();
        assertThat("редирект соответствует параметру retPath", actualLocation, locationMatcher);
    }
}

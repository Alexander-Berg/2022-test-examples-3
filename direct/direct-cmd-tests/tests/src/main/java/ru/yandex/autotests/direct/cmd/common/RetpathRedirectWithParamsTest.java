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
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CommonTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка редиректа (метод redirect(req, retpath, opt)) для параметра retpath")
@Stories(TestFeatures.Common.RETPATH)
@Features(TestFeatures.COMMON)
@RunWith(Parameterized.class)
@Tag(CommonTag.YES)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class RetpathRedirectWithParamsTest {

    private static final String login = "at-direct-cmd-retpath-c1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static DirectTestRunProperties properties = DirectTestRunProperties.getInstance();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(login);
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
                {relativePath, equalTo(relativePath)},
                {null, allOf(startsWith("https://" + properties.getDirectCmdHost() + "/registered/main"),
                        containsString("cmd=showCamp"), containsString("cid="))}
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(login));
        cmdRule.getApiStepsRule().as(login);
    }

    @Test
    @Description("Метод redirect(req, retpath, hash) на ручке cmd_saveCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9596")
    public void testRetPathWithHash() {

        SaveCampRequest saveCampRequest =
                BeanLoadHelper.loadCmdBean("saveCamp.request.default", SaveCampRequest.class);
        saveCampRequest.setCid(String.valueOf(bannersRule.getCampaignId()));
        saveCampRequest.setRetPath(retPath);

        RedirectResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);

        String actualLocation = response.getLocation();
        assertThat("редирект соответствует параметру retPath", actualLocation, locationMatcher);
    }
}

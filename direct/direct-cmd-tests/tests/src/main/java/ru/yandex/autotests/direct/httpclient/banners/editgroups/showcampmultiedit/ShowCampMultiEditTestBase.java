package ru.yandex.autotests.direct.httpclient.banners.editgroups.showcampmultiedit;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.emptyMap;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.apiGroupsSetter.getAdditionalSiteLinks;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * Created by shmykov on 29.04.15.
 * TESTIRT-4974
 */
public abstract class ShowCampMultiEditTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    protected final String CLIENT = "at-direct-b-showcampmultiedit";

    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected Long campaignId;
    protected DirectResponse response;
    protected GroupsParameters requestParams;
    protected GroupsCmdBean expectedGroups;

    abstract void init();

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        requestParams = new ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters();
        requestParams.setUlogin(CLIENT);
        requestParams.setCid(String.valueOf(campaignId));
        init();
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
    }

    @Description("Проверка ответа контроллера showCampMultiEdit")
    public void showCampMultiEditResponseTest() {
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, requestParams);
        checkParameters();
    }

    @Description("Проверка ответа контроллера showCampMultiEditLight")
    public void showCampMultiEditLightResponseTest() {
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEditLight(csrfToken, requestParams);
        checkParameters();
    }

    @Description("Проверка ответа контроллера showCampMultiEdit при возвращении со второго шага на первый")
    public void goBackShowCampMultiEditResponseTest() {
        for (GroupCmdBean group : expectedGroups.getGroups()) {
            group.setCampaignID(null);
        }
        requestParams.setJsonGroups(expectedGroups.toJson());
        response = cmdRule.oldSteps().groupsSteps().goBackShowCampMultiEdit(csrfToken, requestParams);
        checkParameters();
    }

    private void checkParameters() {
        GroupsCmdBean actualResponse = JsonPathJSONPopulater
                .eval(response.getResponseContent().asString(), new GroupsCmdBean(), BeanType.RESPONSE);
        actualResponse.getGroups()
                .stream().forEach(g -> g.getBanners()
                .stream().forEach(b -> b.setHref(b.getHref() == null ? null : b.getHref().replace("http://", ""))));
        actualResponse.getGroups().stream().forEach(t -> t.setTags(emptyMap()));
        actualResponse.getGroups().stream().forEach(t -> t.getBanners().stream()
                .filter(b -> b.getSitelinks().size() < 4)
                .forEach(b -> b.getSitelinks().addAll(getAdditionalSiteLinks(4 - b.getSitelinks().size()))));
        expectedGroups.getGroups().stream().forEach(g -> {
            ArrayList<String> minusKeywordsList = new ArrayList<>();
            g.getMinusKeywords().stream()
                    .forEach(m -> minusKeywordsList.add(m));

            g.getMinusKeywords().clear();
            g.setMinusKeywords(minusKeywordsList);
        });
        assertThat("баннер в ответе контроллера соответствует сохраненному черeз апи", actualResponse,
                beanEquivalent(expectedGroups));
    }

}

package ru.yandex.autotests.direct.httpclient.banners.editgroups.savetextadgroups;

import java.util.Collections;

import ch.lambdaj.Lambda;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.Retargeting;
import ru.yandex.autotests.directapi.common.api45.RetargetingCondition;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранения с различными параметрами фраз в контроллере saveTextAdGroups")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class SaveTextAdGroupsPhrasesPositiveTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private final String CLIENT_LOGIN = "at-direct-b-bannersmultisave";
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long campaignId;
    private GroupsParameters requestParams;
    private CSRFToken csrfToken;
    private Long bannerId;
    private GroupsCmdBean expectedGroups;
    private Currency currency;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        bannerId = bannersRule.getBannerId();
        expectedGroups = new PropertyLoader<>(GroupsCmdBean.class).getHttpBean("groupWithTwoPhrasesAndRetargeting2");
        expectedGroups.getGroups().get(0).setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        expectedGroups.getGroups().get(0).getBanners().get(0).setBannerID(String.valueOf(bannerId));

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT_LOGIN).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT_LOGIN).getClientID()));
        Integer retargetingId = extract(cmdRule.apiSteps().retargetingSteps().addConditionsForUser(CLIENT_LOGIN, 1),
                on(RetargetingCondition.class).getRetargetingConditionID()).get(0);
        expectedGroups.getGroups().get(0).getRetargetings().get(0).setRetargetingConditionID(retargetingId.toString());

        requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setAdgroupIds(String.valueOf(bannersRule.getGroupId()));
        requestParams.setJsonGroups(expectedGroups.toJson());
        currency = User.get(CLIENT_LOGIN).getCurrency();

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
    }

    @Test
    @Description("Проверка допустимых комбинаций спецсимволов в фразах")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10133")
    public void allowedCombinationsTest() {
        String first_phrase = "\"!one [!two] [+three]\"";
        String second_phrase = "\"[!four] five\"";
        expectedGroups.getGroups().get(0).getPhrases().get(0).setPhrase(first_phrase);
        expectedGroups.getGroups().get(0).getPhrases().get(1).setPhrase(second_phrase);
        requestParams.setJsonGroups(expectedGroups.toJson());
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        first_phrase = first_phrase.replaceAll("\\+", ""); //+ обрезается после сохранения
        assertThat("фразы корректно сохранились", Lambda.extract(bannersRule.getCurrentGroup().getPhrases(),
                on(Phrase.class).getPhrase()),
                containsInAnyOrder(first_phrase, second_phrase));
    }

    @Test
    @Description("Проверка вычитания стоп слова, содержащегося в основной фразе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10134")
    public void minusStopWordsTest() {
        String first_phrase = "колеса на авто -на";
        expectedGroups.getGroups().get(0).getPhrases().get(0).setPhrase(first_phrase);
        requestParams.setJsonGroups(expectedGroups.toJson());
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        assertThat("фразы корректно сохранились", Lambda.extract(bannersRule.getCurrentGroup().getPhrases(),
                on(Phrase.class).getPhrase()),
                containsInAnyOrder(first_phrase.replaceAll("-", "-!"),
                        expectedGroups.getGroups().get(0).getPhrases().get(1).getPhrase()));
    }

    @Test
    @Description("Корректное сохранение группы только с условием ретаргетинга, без фраз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10135")
    public void saveGroupOnlyWithRetargetingTest() {
        expectedGroups.getGroups().get(0).setPhrases(Collections.emptyList());
        requestParams.setJsonGroups(expectedGroups.toJson());
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        assertThat("у сохраненного баннера отстутсвуют фразы", bannersRule.getCurrentGroup().getPhrases(), hasSize(0));
        Retargeting[] rets = cmdRule.apiSteps().retargetingSteps().getAdsRetargeting(CLIENT_LOGIN, currency, bannerId);//(CLIENT_LOGIN, bannerId);
        assertThat("баннер содержит ожидаемое условие ретаргетинга",
                String.valueOf(rets[0].getRetargetingConditionID()),
                equalTo(expectedGroups.getGroups().get(0).getRetargetings().get(0).getRetargetingConditionID()));
    }
}

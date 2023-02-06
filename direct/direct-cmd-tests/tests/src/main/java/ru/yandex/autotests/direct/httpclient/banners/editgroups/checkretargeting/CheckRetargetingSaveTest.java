package ru.yandex.autotests.direct.httpclient.banners.editgroups.checkretargeting;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.util.JsonContainer;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.directapi.common.api45.Retargeting;
import ru.yandex.autotests.directapi.common.api45.RetargetingCondition;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.banners.EditGroupsRequestParamsSetter.setJsonGroupParamsFromBanners;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сохранения условий ретаргетинга при сохранении группы объявлений")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class CheckRetargetingSaveTest {
    private String clientLogin = "at-direct-b-groups-ret-save";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(clientLogin);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private LogSteps log = LogSteps.getLogger(this.getClass());
    private Long campaignId;
    private Long bannerId;
    private CSRFToken csrfToken;
    private DirectResponse response;
    private GroupsParameters requestParams;
    private int[] expectedRetIds;
    private List<Float> expectedRetPrices;
    private JsonContainer jsonGroups;
    private JsonArray jsonGroupsArray;
    private Currency currency;

    @Before
    public void before() {

        cmdRule.getApiStepsRule().as(clientLogin);
        campaignId = bannersRule.getCampaignId();
        bannerId = bannersRule.getBannerId();

        cmdRule.oldSteps().onPassport().authoriseAs(clientLogin, User.get(clientLogin).getPassword());
        TestEnvironment.newDbSteps().useShardForLogin(clientLogin).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(clientLogin).getClientID()));
        currency = User.get(clientLogin).getCurrency();

        PropertyLoader<RetargetingCondition> loader = new PropertyLoader<>(RetargetingCondition.class);
        cmdRule.getApiStepsRule().as(clientLogin);
        expectedRetIds = cmdRule.apiSteps().retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(loader.getHttpBean("retWithSingleCondAndLoginAndGoalsForSave")),
                new RetargetingConditionMap(loader.getHttpBean("retWithMultipleCondAndLoginAndGoalsForSave")));

        requestParams = new GroupsParameters();
        requestParams.setBids(String.valueOf(bannerId));
        requestParams.setCid(campaignId.toString());
        requestParams.setUlogin(clientLogin);
        requestParams.setAdgroupIds(String.valueOf(bannersRule.getGroupId()));

        PropertyLoader<JsonContainer> jsonGroupsLoader = new PropertyLoader<>(JsonContainer.class);
        jsonGroups = jsonGroupsLoader.getHttpBean("jsonGroupsWithRetargetingForSave2");
        jsonGroupsArray = new JsonParser().parse(jsonGroups.toString()).getAsJsonArray();
        JsonObject group = jsonGroupsArray.get(0).getAsJsonObject();
        setJsonGroupParamsFromBanners(group, bannersRule.getCurrentGroup());
        JsonObject firstRetargeting = group.get("retargetings").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject secondRetargeting = group.get("retargetings").getAsJsonArray().get(1).getAsJsonObject();
        firstRetargeting.addProperty("ret_cond_id", String.valueOf(expectedRetIds[0]));
        secondRetargeting.addProperty("ret_cond_id", String.valueOf(expectedRetIds[1]));
        requestParams.setJsonGroups(jsonGroupsArray.toString());

        expectedRetPrices = new ArrayList<>();
        expectedRetPrices.add(firstRetargeting.get("price_context").getAsFloat());
        expectedRetPrices.add(secondRetargeting.get("price_context").getAsFloat());
        csrfToken = getCsrfTokenFromCocaine(User.get(clientLogin).getPassportUID());
    }

    @Test
    @Description("Проверка через апи, что условия ретаргетинга становятся используемыми в группе при сохранении группы с id условий")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10082")
    public void checkRetargetingsBecomeUsedInGroupWithApiTest() {
        log.info("Сохраним группу");
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        Retargeting[] retargetings = cmdRule.apiSteps().retargetingSteps().getAdsRetargeting(clientLogin, currency, bannerId);
        assumeThat("получили некорректный набор ретараргетингов", retargetings, arrayWithSize(2));

        Integer[] expectedBannerRetIds = ArrayUtils.toObject(expectedRetIds);
        List<Integer> acquiredBannerRetIds = extract(cmdRule.apiSteps().retargetingSteps()
                .getAdsRetargeting(clientLogin, currency, bannerId), on(Retargeting.class).getRetargetingConditionID());
        assertThat("условия ретаргетинга для баннера соответствут ожиданиям",
                acquiredBannerRetIds, containsInAnyOrder(expectedBannerRetIds));
    }

    @Test
    @Description("Проверка через апи что условия ретаргетинга становятся неиспользуемыми в группе при сохранении группы без id условий")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10083")
    public void checkRetargetingsBecomeUnusedInGroupWithApiTest() {
        cmdRule.apiSteps().retargetingSteps().addRetargetingToBanner(clientLogin, bannerId);
        jsonGroupsArray.get(0).getAsJsonObject().get("retargetings").getAsJsonArray().remove(1);
        jsonGroupsArray.get(0).getAsJsonObject().get("retargetings").getAsJsonArray().remove(0);
        requestParams.setJsonGroups(jsonGroupsArray.toString());

        response = cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        Retargeting[] retargetings = cmdRule.apiSteps().retargetingSteps().getAdsRetargeting(clientLogin, currency, bannerId);
        assertThat("получили пустой набор ретаргетингов для баннера", retargetings, emptyArray());
    }

    @Test
    @Description("Проверка через апи сохранения цен условий ретаретинга при сохранении группы объявлений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10084")
    public void checkRetargetingPriceSaveWithApiTest() {
        log.info("Сохраним группу");
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, requestParams);
        Retargeting[] retargetings = cmdRule.apiSteps().retargetingSteps().getAdsRetargeting(clientLogin, currency, bannerId);
        assumeThat("получили некорректный набор ретараргетингов", retargetings, arrayWithSize(2));

        List<Float> acquiredBannerRetPrice = extract(retargetings, on(Retargeting.class).getContextPrice());
        acquiredBannerRetPrice.set(0, Money.valueOf(acquiredBannerRetPrice.get(0)).floatValue());
        acquiredBannerRetPrice.set(1, Money.valueOf(acquiredBannerRetPrice.get(1)).floatValue());

        assertThat("условия ретаргетинга для баннера соответствут ожиданиям",
                acquiredBannerRetPrice, containsInAnyOrder(expectedRetPrices.toArray()));
    }
}

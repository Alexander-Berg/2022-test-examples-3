package ru.yandex.autotests.direct.httpclient.banners.editgroups.checktags;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.tags.BannerTagCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.tags.TagsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.SaveAdGroupTagsParameters;
import ru.yandex.autotests.direct.httpclient.util.JsonContainer;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater.eval;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by shmykov on 12.03.15.
 * TESTIRT-4018
 */
@Aqua.Test
@Description("Проверка тэгов баннера после сохранения")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.TAGS)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
public class CheckAdGroupTagsFirstStepTest {
    private String CLIENT = "at-direct-b-grouptags";
    private Long campaignId;
    private DirectResponse response;
    private List<BannerTagCmdBean> expectedTags;
    private GroupsParameters groupsParams;
    private JsonObject tags;
    private JsonArray jsonGroupsArray;
    private CSRFToken csrfToken;
    private final String WRONG_TAG_ID = "0";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        expectedTags = new ArrayList<>();
        PropertyLoader<BannerTagCmdBean> tagLoader = new PropertyLoader<>(BannerTagCmdBean.class);
        expectedTags.add(tagLoader.getHttpBean("firstTag"));
        expectedTags.add(tagLoader.getHttpBean("secondTag"));

        campaignId = bannersRule.getCampaignId();
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        Long adGroupId = bannersRule.getGroupId();

        //сохраняем аджаксово тэги
        SaveAdGroupTagsParameters saveTagsParams = new SaveAdGroupTagsParameters();
        saveTagsParams.setUlogin(CLIENT);
        saveTagsParams.setCid(campaignId.toString());
        saveTagsParams.setAdgroupIds(adGroupId.toString());
        saveTagsParams.setNewTags(join(extract(expectedTags, on(BannerTagCmdBean.class).getValue()), ","));
        List<String> tagIds = cmdRule.oldSteps().ajaxSaveAdGroupTagsSteps().addNewTagsAndGetTagIds(saveTagsParams, csrfToken);
        //закончили сохранять
        for (int i = 0; i < expectedTags.size(); i++) {
            expectedTags.get(i).setTagId(tagIds.get(i));
            expectedTags.get(i).setUsesCount(null);
        }

        PropertyLoader<JsonContainer> loader = new PropertyLoader<>(JsonContainer.class);
        JsonContainer alternativeJsonGroups = loader.getHttpBean("jsonGroupsForSave2");

        jsonGroupsArray = new JsonParser().parse(alternativeJsonGroups.toString()).getAsJsonArray();
        JsonObject group = jsonGroupsArray.get(0).getAsJsonObject();
        tags = group.getAsJsonObject("tags");
        tags.addProperty(expectedTags.get(0).getTagId(), 1);

        groupsParams = new GroupsParameters();
        groupsParams.setCid(String.valueOf(campaignId));
        groupsParams.setUlogin(CLIENT);
        groupsParams.setAdgroupIds(String.valueOf(adGroupId));
        groupsParams.setJsonGroups(jsonGroupsArray.toString());

        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, groupsParams);

    }

    @Test
    @Description("Проверка тэгов на первом шаге мультиредактирования")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10087")
    public void checkTagsOnShowCampMultiEditPageTest() {
        groupsParams.setJsonGroups(null);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, groupsParams);
        getTagsFromResponse(response);
        assertThat("список тэгов соответствует ожиданиям", getTagsFromResponse(response),
                containsInAnyOrder(beanDiffer(expectedTags.get(0)), beanDiffer(expectedTags.get(1))));
    }

    @Test
    @Description("Проверка тэгов на странице редактирования только текстов объявлений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10088")
    public void checkTagsOnShowCampMultiEditLightPageTest() {
        groupsParams.setJsonGroups(null);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEditLight(csrfToken, groupsParams);
        assertThat("список тэгов соответствует ожиданиям", getTagsFromResponse(response),
                containsInAnyOrder(beanDiffer(expectedTags.get(0)), beanDiffer(expectedTags.get(1))));
    }

    @Test
    @Description("Проверка тэгов на странице кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10089")
    public void checkTagsOnShowCampPageTest() {
        response = cmdRule.oldSteps().campaignsSteps().openShowCamp(CLIENT, String.valueOf(campaignId));
        assertThat("список тэгов соответствует ожиданиям", getTagsFromResponse(response),
                containsInAnyOrder(beanDiffer(expectedTags.get(0)), beanDiffer(expectedTags.get(1))));
    }

    @Test
    @Description("Проверки при возвращении со второго шага на первый")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10091")
    public void checkTagsOnGoBackShowCampMultiEditPageTest() {
        response = cmdRule.oldSteps().groupsSteps().goBackShowCampMultiEdit(csrfToken, groupsParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(
                response, ".groups.tags." + expectedTags.get(0).getTagId(), equalTo(Arrays.asList(1)));
        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(
                response, ".groups.tags." + expectedTags.get(1).getTagId(), equalTo(Collections.emptyList()));
    }

    @Test
    @Description("Сохранение группы с неверным tag id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10092")
    public void saveGroupsWithWrongTagIdest() {
        tags.addProperty(WRONG_TAG_ID, 1);
        groupsParams.setJsonGroups(jsonGroupsArray.toString());
        cmdRule.oldSteps().groupsSteps().saveGroups(csrfToken, groupsParams);
        groupsParams.setJsonGroups(null);
        response = cmdRule.oldSteps().groupsSteps().openShowCampMultiEdit(csrfToken, groupsParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(response, ".campaign.tags." + WRONG_TAG_ID,
                equalTo(Collections.emptyList()));
    }

    private List<BannerTagCmdBean> getTagsFromResponse(DirectResponse response) {
        TagsCmdBean acquiredTags;
        acquiredTags = eval(response.getResponseContent().asString(), new TagsCmdBean(), BeanType.RESPONSE);
        acquiredTags.getTags().forEach(t -> t.setUsesCount(null));
        return acquiredTags.getTags();
    }
}

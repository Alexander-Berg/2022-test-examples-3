package ru.yandex.autotests.direct.cmd.campaigns;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.groups.dynamic.DtoBaseTest;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сохранения динамической кампании")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Tag("sb_test")
public class SaveDynamicCampTest extends DtoBaseTest {

    @Parameterized.Parameter(0)
    public List<DynamicCondition> conditions;

    @Parameterized.Parameter(1)
    public String conditionName;

    @Parameterized.Parameters(name = "Условия нацелевания : {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {getDynamicCondition(), "Проверка сохранения группы с уcловием на все страницы"},
                {getBigDynamicCondition(), "Проверка сохранения группы с большим набором уловий"},
        });
    }

    public static Group getExpectedResponse(Group expectedGroup) {
        ContactInfo contactInfo = expectedGroup.getBanners().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в группе есть баннер"))
                .getContactInfo();

        contactInfo.withOrgDetails(new OrgDetails(null, expectedGroup.getBanners().get(0).getContactInfo().getOGRN()));
        contactInfo.withOGRN(null);

        expectedGroup.setHrefParams(null);
        expectedGroup.getDynamicConditions().forEach(x -> x.setDynId(null));
        expectedGroup.getBanners().forEach(x -> x.withImage(null).withBid(null).withHref(null).withUrlProtocol(null));
        expectedGroup.setAdGroupID(null);
        expectedGroup.setTags(null);
        return expectedGroup;
    }

    public static List<DynamicCondition> getDynamicCondition() {
        List<DynamicCondition> dynamicConditions = new ArrayList<>();

        Condition conditionCmdBean = new Condition();
        conditionCmdBean.setType("any");

        DynamicCondition dynamicCondition = new DynamicCondition();
        dynamicCondition.setDynamicConditionName("Все страницы");
        dynamicCondition.setDynId("");
        dynamicCondition.setPrice(new Float(0.3));
        dynamicCondition.setConditions(new ArrayList<>());
        dynamicCondition.getConditions().add(conditionCmdBean);

        dynamicConditions.add(dynamicCondition);
        return dynamicConditions;
    }

    public static List<DynamicCondition> getBigDynamicCondition() {
        return Collections.singletonList(BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class));
    }

    @Ignore("DIRECT-58262")
    @Test
    @Description("Проверяем создание кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9373")
    public void saveCamp() {
        List<Group> actualGroups = getCreatedGroup().getCampaign().getGroups();
        assertThat("Вернулось корректное число групп", actualGroups, hasSize(1));
        assertThat("Группы баннеров соответвуют ожиданиям", actualGroups.get(0),
                beanDiffer(getExpectedResponse(savingGroup)).useCompareStrategy(onlyExpectedFields()));
        ShowCampResponse actualCamp = getCreatedCamp();
        assertThat("Кампания соотвествует ожиданиям", actualCamp, beanDiffer(getExpectedCampResponse())
                .useCompareStrategy(onlyExpectedFields()));
    }

    protected Group getDynamicGroup() {
        Group dynamicGroup = super.getDynamicGroup();
        dynamicGroup.setMainDomain("www.ulmart.ru");
        dynamicGroup.setDynamicConditions(conditions);
        return dynamicGroup;
    }

    protected ShowCampResponse getExpectedCampResponse() {
        return BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_RESPONSE_SHOW_CAMP_DYNAMIC_FULL2, ShowCampResponse.class
        );
    }

}


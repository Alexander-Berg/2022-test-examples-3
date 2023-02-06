package ru.yandex.autotests.directintapi.tests.ppccampgetgoals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.MetrikaGoalsRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppccampgetgoals.OrderCounterGoalStatItemBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppccampgetgoals.PpcCampGetGoalsResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 10/03/2017.
 * https://st.yandex-team.ru/TESTIRT-11095
 */
@Aqua.Test
@Tag(TagDictionary.TRUNK)
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.PPC_CAMP_GET_GOALS)
@Issue("https://st.yandex-team.ru/DIRECT-58065")
@Description("Вызов скрипта ppcCampGetGoals.pl, основные проверки")
@RunWith(Parameterized.class)

public class PpcCampGetGoalsEcommerceTest {
    private static final String LOGIN = Logins.CHECK_GOALS_CLIENT;
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String DATE_FROM = "2017-03-02";
    private static final String DATE_TO = "2017-03-06";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    public static Long orderId;

    @Parameterized.Parameter()
    public Long[] goalIds;

    @Parameterized.Parameter(1)
    public int expectedAmountOfRecords;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        //Выбираем значения целей таким образом, чтобы они не пересекались между тестами
        //берем ближе к максимуму, чтобы минимизировать возможность затронуть существующие данные
        return Arrays.asList(new Object[][]{
                {new Long[]{3000000000L}, 0},
                {new Long[]{4000000001L}, 0},
                {new Long[]{3000000001L}, 1},
                {new Long[]{4000000000L}, 1},
                {new Long[]{3000000001L, 4000000000L}, 2},
                {new Long[]{3000000000L, 3000000001L, 4000000000L, 4000000001L}, 2},
        });
    }

    @BeforeClass
    public static void beforeClass() {
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = (long) api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void init() {
        List<OrderCounterGoalStatItemBean> goalStatItemBeanList = new ArrayList<>();
        for (Long goalId : goalIds) {
            goalStatItemBeanList.add(new OrderCounterGoalStatItemBean(orderId, goalId, 1, 1, 1));
        }
        HELPER.addMongoBean(new FakeBSProxyLogBean().withObjectIds(
                DATE_FROM.replace("-", "") + ":" + DATE_TO.replace("-", ""))
                .withResponseEntity(new PpcCampGetGoalsResponseBean(goalStatItemBeanList).toString()));
    }

    @Test
    public void checkDbAfterScriptPpcCampGetGoals() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcCampGetGoals(DATE_FROM, DATE_TO, FakeBsProxyConfig.getExportDirectGoalStatUrl());

        List<MetrikaGoalsRecord> metrikaGoals =
                api.userSteps.getDirectJooqDbSteps().metrikaGoalsSteps().getMetrikaGoalsList(goalIds);
        assertThat("В базу записалиcь только e-commerce цели", metrikaGoals, iterableWithSize(expectedAmountOfRecords));

    }

    @After
    public void remove() {
        HELPER.deleteFakeBSProxyLogBeansById(DATE_FROM.replace("-", "") + ":" + DATE_TO.replace("-", ""));
        api.userSteps.getDirectJooqDbSteps().metrikaGoalsSteps().deleteMetrikaGoals(goalIds);
    }
}

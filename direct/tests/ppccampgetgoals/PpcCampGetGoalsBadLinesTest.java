package ru.yandex.autotests.directintapi.tests.ppccampgetgoals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.MetrikaGoalsRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppccampgetgoals.OrderCounterGoalStatItemBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.ppccampgetgoals.PpcCampGetGoalsResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.datacontainers.DarksideTextConstants;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.hamcrest.Matchers.emptyIterable;
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
@Description("Вызов скрипта ppcCampGetGoals.pl, обработка \"плохих\" строк")
public class PpcCampGetGoalsBadLinesTest {
    private static final String LOGIN = Logins.CHECK_GOALS_CLIENT;
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();
    private static final String DATE_FROM = "2017-03-09";
    private static final String DATE_TO = "2017-03-10";
    //3 — количество служебных строк(код ответа, заголовки, #End), они тоже учитываются в подсчете
    private static final int LINES_AMOUNT = 997;
    //Выбираем значения целей таким образом, чтобы они не пересекались между тестами
    //берем ближе к максимуму, чтобы минимизировать возможность затронуть существующие данные
    private static final long MAX_ECOMMERCE_GOAL_ID = 3999999990L;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    public static Long orderId;
    public List<Long> goalsList = new ArrayList<>();

    @BeforeClass
    public static void beforeClass() {
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        orderId = (long) api.userSteps.campaignFakeSteps().setRandomOrderID(cid);
    }

    @Test
    @Description("Количество 'плохих' строк = 0.1%")
    public void checkSuccessWithBadLines() {
        List<OrderCounterGoalStatItemBean> goalStatItemBeanList = new ArrayList<>();
        for (int i = 0; i < LINES_AMOUNT - 1; i++) {
            goalStatItemBeanList.add(new OrderCounterGoalStatItemBean(orderId, MAX_ECOMMERCE_GOAL_ID - i, 1, 1, 1));
            goalsList.add(MAX_ECOMMERCE_GOAL_ID - i);
        }
        goalStatItemBeanList
                .add(new OrderCounterGoalStatItemBean(orderId, MAX_ECOMMERCE_GOAL_ID - LINES_AMOUNT, 1, -1, 1));
        goalsList.add(MAX_ECOMMERCE_GOAL_ID - LINES_AMOUNT);
        HELPER.addMongoBean(new FakeBSProxyLogBean().withObjectIds(
                DATE_FROM.replace("-", "") + ":" + DATE_TO.replace("-", ""))
                .withResponseEntity(new PpcCampGetGoalsResponseBean(goalStatItemBeanList).toString()));
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcCampGetGoals(DATE_FROM, DATE_TO, FakeBsProxyConfig.getExportDirectGoalStatUrl());

        List<MetrikaGoalsRecord> metrikaGoals =
                api.userSteps.getDirectJooqDbSteps().metrikaGoalsSteps()
                        .getMetrikaGoalsList(goalsList.toArray(new Long[]{}));
        assertThat("В базу записалиcь только e-commerce цели", metrikaGoals, iterableWithSize(LINES_AMOUNT - 1));

    }

    @Test
    @Description("Количество 'плохих' строк > 0.1%")
    public void checkErrorWithBadLines() {
        List<OrderCounterGoalStatItemBean> goalStatItemBeanList = new ArrayList<>();
        for (int i = 0; i < LINES_AMOUNT - 2; i++) {
            goalStatItemBeanList.add(new OrderCounterGoalStatItemBean(orderId, MAX_ECOMMERCE_GOAL_ID - i, 1, 1, 1));
            goalsList.add(MAX_ECOMMERCE_GOAL_ID - i);
        }
        goalStatItemBeanList
                .add(new OrderCounterGoalStatItemBean(orderId, MAX_ECOMMERCE_GOAL_ID - LINES_AMOUNT, 1, -1, 1));
        goalsList.add(MAX_ECOMMERCE_GOAL_ID - LINES_AMOUNT);

        HELPER.addMongoBean(new FakeBSProxyLogBean().withObjectIds(
                DATE_FROM.replace("-", "") + ":" + DATE_TO.replace("-", ""))
                .withResponseEntity(new PpcCampGetGoalsResponseBean(goalStatItemBeanList).toString()));
        //3 — количество служебных строк(код ответа, заголовки, #End)
        api.userSteps.getDarkSideSteps().getRunScriptSteps()
                .runPpcCampGetGoalsExpectError(DATE_FROM, DATE_TO, FakeBsProxyConfig.getExportDirectGoalStatUrl(), 255,
                        String.format(DarksideTextConstants.PPC_CAMP_GET_GOALS_MANY_INVALID_LINES.getText(), 1,
                                LINES_AMOUNT - 1 + 3));
        List<MetrikaGoalsRecord> metrikaGoals =
                api.userSteps.getDirectJooqDbSteps().metrikaGoalsSteps()
                        .getMetrikaGoalsList(goalsList.toArray(new Long[]{}));
        assertThat("в базу цели не записалиcь ", metrikaGoals, emptyIterable());
    }

    @After
    public void remove() {
        HELPER.deleteFakeBSProxyLogBeansById(DATE_FROM.replace("-", "") + ":" + DATE_TO.replace("-", ""));
        api.userSteps.getDirectJooqDbSteps().metrikaGoalsSteps().deleteMetrikaGoals(goalsList.toArray(new Long[]{}));
    }
}

package ru.yandex.direct.core.entity.retargeting.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionMappings.conditionTypeToDb;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.OPPOSITE_TO_DEFAULT_TYPE;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;

@CoreTest
@RunWith(Parameterized.class)
public class RetargetingConditionRepositoryGetTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFieldsExcept(
            newPath("deleted"), newPath("lastChangeTime"), newPath("available"));
    private static final String WRONG_NAME = "wrong name";
    private static final int LIMIT = 2; // Лимит больше 1, чтобы проверить, что нет задвоения записей

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingConditionRepository repoUnderTest;

    @Autowired
    private Steps steps;

    private RetargetingCondition retargetingCondition;
    private AdGroupInfo adGroupInfo;
    private ClientId clientId;
    private int shard;

    //Ожидаемое количество записей
    @Parameterized.Parameter()
    public int expectedCount;

    //id указан верно, если null, то ids = null
    @Parameterized.Parameter(1)
    public Boolean correctId;

    //idGroup указан верно, если null, то idGroups = null
    @Parameterized.Parameter(2)
    public Boolean correctIdAdGroup;

    //name указано верно, если null, то name = null
    @Parameterized.Parameter(3)
    public Boolean correctName;

    //type указан верно, если null, то type = null
    @Parameterized.Parameter(4)
    public Boolean correctType;

    @Parameterized.Parameters(name = "{0}: {1} {2} {3} {4} {5}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, true, true, true, true},
                {0, false, true, true, true},
                {0, true, false, true, true},
                {0, true, true, false, true},
                {0, true, true, true, false},
                {1, null, null, null, null},
                {0, false, null, null, null},
                {0, null, false, null, null},
                {0, null, null, false, null},
                {0, null, null, null, false},
        });
    }

    @Before
    public void before() {
        RetConditionInfo retConditionInfo = retConditionSteps.createDefaultRetCondition();
        shard = retConditionInfo.getShard();
        clientId = retConditionInfo.getClientId();
        retargetingCondition = retConditionInfo.getRetCondition();

        // 2 ретаргетинга, чтобы убедиться, что нет задвоений
        RetargetingInfo retargeting1 =
                steps.retargetingSteps().createRetargeting(
                        TestRetargetings.defaultTargetInterest(),
                        retConditionInfo.getClientInfo(),
                        retConditionInfo);

        adGroupInfo = retargeting1.getAdGroupInfo();

        RetargetingInfo retargeting2 =
                steps.retargetingSteps().createRetargeting(
                        TestRetargetings.defaultTargetInterest(),
                        adGroupInfo,
                        retConditionInfo);
    }

    @Test
    public void get() throws Exception {
        List<Long> ids = correctId == null ? null : singletonList(
                correctId ? retargetingCondition.getId() : retargetingCondition.getId() + 1);
        List<Long> adGroupIds = correctIdAdGroup == null ? null : singletonList(
                correctIdAdGroup ? adGroupInfo.getAdGroupId() : adGroupInfo.getAdGroupId() + 1);
        String name = correctName == null ? null : (correctName ? retargetingCondition.getName() : WRONG_NAME);
        Set<RetargetingConditionsRetargetingConditionsType> type = correctType == null ? null
                : singleton(conditionTypeToDb(correctType ? retargetingCondition.getType() : OPPOSITE_TO_DEFAULT_TYPE));

        List<RetargetingCondition> retargetingConditions = repoUnderTest.get(shard, clientId, ids, adGroupIds,
                name, type, limited(LIMIT, 0));

        assertEquals(expectedCount, retargetingConditions.size());
        if (expectedCount == 1) {
            assertThat(retargetingConditions, contains(
                    beanDiffer(retargetingCondition).useCompareStrategy(COMPARE_STRATEGY)));
        }
    }
}

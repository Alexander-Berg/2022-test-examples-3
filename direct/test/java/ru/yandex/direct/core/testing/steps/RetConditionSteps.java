package ru.yandex.direct.core.testing.steps;

import java.util.List;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterCryptaGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterLalSegmentGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterMetrikaGoals;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultBrandSafetyRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultIndoorRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;

public class RetConditionSteps {

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private RetargetingConditionRepository retConditionRepository;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    TestLalSegmentRepository testLalSegmentRepository;

    public RetConditionInfo createBigRetCondition() {
        return createRetCondition(bigRetCondition(null));
    }

    public RetConditionInfo createBigRetCondition(ClientInfo clientInfo) {
        return createRetCondition(bigRetCondition(clientInfo.getClientId()), clientInfo);
    }

    public RetConditionInfo createDefaultRetCondition() {
        return createRetCondition((RetargetingCondition) null);
    }

    public RetConditionInfo createDefaultRetCondition(ClientInfo clientInfo) {
        return createRetCondition(null, clientInfo);
    }

    public RetConditionInfo createRetCondition(RetargetingCondition retCondition) {
        return createRetCondition(new RetConditionInfo().withRetCondition(retCondition));
    }

    public RetConditionInfo createDefaultRetCondition(List<Goal> retargetingConditionGoals, ClientInfo clientInfo) {
        return createDefaultRetCondition(retargetingConditionGoals, clientInfo, ConditionType.metrika_goals,
                RuleType.ALL);
    }

    public RetConditionInfo createDefaultRetCondition(List<Goal> retargetingConditionGoals,
                                                      ClientInfo clientInfo, ConditionType conditionType,
                                                      RuleType ruleType) {
        RetargetingCondition defaultRetCondition = defaultRetCondition(clientInfo.getClientId());
        Rule rule = new Rule();
        rule.withGoals(retargetingConditionGoals)
                .withType(ruleType);

        defaultRetCondition.setRules(singletonList(rule));
        defaultRetCondition.setType(conditionType);
        return createRetCondition(new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(defaultRetCondition)
        );
    }

    public RetConditionInfo createRetCondition(RetargetingCondition retCondition, ClientInfo clientInfo) {
        return createRetCondition(new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(retCondition));
    }

    public RetConditionInfo createRetCondition(RetConditionInfo retConditionInfo) {
        if (retConditionInfo.getRetCondition() == null) {
            retConditionInfo.withRetCondition(defaultRetCondition(null));
        }
        saveRetConditionToRepository(retConditionInfo);

        return retConditionInfo;
    }

    public RetConditionInfo createDefaultInterestRetCondition(ClientInfo clientInfo) {
        RetargetingCondition retCondition = (RetargetingCondition) defaultRetCondition(null)
                .withType(ConditionType.interests)
                .withRules(List.of(
                        defaultRule(List.of(defaultGoalByType(GoalType.INTERESTS)), CryptaInterestType.short_term)
                ));
        return createRetCondition(new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(retCondition));
    }

    public RetConditionInfo createDefaultABSegmentRetCondition(ClientInfo clientInfo) {
        return createABSegmentRetCondition(null, clientInfo);
    }

    public RetConditionInfo createABSegmentRetCondition(RetargetingCondition retCondition, ClientInfo clientInfo) {
        return createABSegmentRetCondition(new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(retCondition));
    }

    public RetConditionInfo createABSegmentRetCondition(RetConditionInfo retConditionInfo) {
        if (retConditionInfo.getRetCondition() == null) {
            retConditionInfo.withRetCondition(defaultABSegmentRetCondition(null));
        }
        saveRetConditionToRepository(retConditionInfo);
        return retConditionInfo;
    }

    public RetConditionInfo createDefaultBrandSafetyRetCondition(ClientInfo clientInfo) {
        RetConditionInfo retConditionInfo = new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(defaultBrandSafetyRetCondition(clientInfo.getClientId()));
        saveRetConditionToRepository(retConditionInfo);
        return retConditionInfo;
    }

    private RetConditionInfo saveRetConditionToRepository(RetConditionInfo retConditionInfo) {
        if (retConditionInfo.getRetCondition() == null) {
            return null;
        }
        if (retConditionInfo.getRetConditionId() == null) {
            if (retConditionInfo.getClientInfo() == null
                    || retConditionInfo.getClientInfo().getClientId() == null) {
                clientSteps.createClient(retConditionInfo.getClientInfo());
            }
            retConditionInfo.getRetCondition().setClientId(retConditionInfo.getClientId().asLong());
            createRetConditionInfo(retConditionInfo.getClientInfo(), retConditionInfo);
            retConditionRepository.add(retConditionInfo.getShard(), singletonList(retConditionInfo.getRetCondition()));
        }
        return retConditionInfo;
    }

    public RetConditionInfo createCpmRetCondition(ClientInfo clientInfo) {
        RetConditionInfo retCondition = createRetCondition(defaultCpmRetCondition(), clientInfo);
        return createRetConditionInfo(clientInfo, retCondition);
    }

    public RetConditionInfo createIndoorRetCondition(ClientInfo clientInfo) {
        RetConditionInfo retCondition = createRetCondition(defaultIndoorRetCondition(), clientInfo);
        return createRetConditionInfo(clientInfo, retCondition);
    }

    private RetConditionInfo createRetConditionInfo(ClientInfo clientInfo, RetConditionInfo retCondition) {
        List<Goal> goals = StreamEx.of(retCondition.getRetCondition().getRules())
                .toFlatList(Rule::getGoals);
        List<Goal> metrikaGoals = filterMetrikaGoals(goals);
        metrikaHelperStub.addGoals(clientInfo.getUid(), metrikaGoals);
        testCryptaSegmentRepository.addAll(filterCryptaGoals(goals));
        testLalSegmentRepository.addAll(filterLalSegmentGoals(goals));
        return retCondition;
    }

    public Long deleteRetCondition(ClientInfo clientInfo, Long retConditionId) {
        return retConditionRepository.delete(clientInfo.getShard(), clientInfo.getClientId(), List.of(retConditionId)).get(0);
    }
}

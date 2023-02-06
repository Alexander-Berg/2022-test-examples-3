package ru.yandex.direct.teststeps.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;

import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;

@Service
@ParametersAreNonnullByDefault
public class RetargetingConditionStepsService {

    private final InfoHelper infoHelper;
    private final RetConditionSteps retConditionSteps;

    @Autowired
    public RetargetingConditionStepsService(InfoHelper infoHelper,
                                            RetConditionSteps retConditionSteps) {
        this.infoHelper = infoHelper;
        this.retConditionSteps = retConditionSteps;
    }

    public RetConditionInfo createRetargetingConditionWithNoRules(
            String login,
            ConditionType conditionType
    ) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        RetargetingCondition defaultRetCondition = defaultRetCondition(clientInfo.getClientId());
        defaultRetCondition.setRules(List.of());
        defaultRetCondition.setType(conditionType);
        return retConditionSteps.createRetCondition(new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(defaultRetCondition)
        );
    }

    public RetConditionInfo createRetargetingConditionWithSingleGoal(
            String login,
            ConditionType conditionType,
            RuleType ruleType,
            Long goalId,
            GoalType goalType,
            @Nullable Integer period
    ) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());
        Goal goal = defaultGoalWithId(goalId, goalType, period);
        return retConditionSteps.createDefaultRetCondition(List.of(goal), clientInfo, conditionType, ruleType);
    }

    public Long deleteRetargetingCondition(String login, Long retConditionId) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        return retConditionSteps.deleteRetCondition(clientInfo, retConditionId);
    }


}

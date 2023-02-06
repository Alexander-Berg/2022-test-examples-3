package ru.yandex.market.jmf.module.toloka.utils;

import java.time.Duration;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.toloka.AssessmentPool;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.model.TolokaServer;

@Component
public class AssessmentTicketUtils {

    @Inject
    BcpService bcpService;

    public AssessmentRule createAssessmentRule(TolokaServer serverId) {
        return bcpService.create(AssessmentRule.FQN, Map.of(
                AssessmentRule.SERVER_ID, serverId,
                AssessmentRule.PROJECT_ID, Randoms.string(),
                AssessmentRule.POOL_ID, Randoms.string(),
                AssessmentRule.PENDING_TIME, Duration.ofMinutes(60),
                AssessmentRule.ASSESSMENT_TIME, Duration.ofMinutes(60),
                AssessmentRule.TITLE, Randoms.string(),
                AssessmentRule.INPUT_FIELD_NAME, Randoms.string()
        ));
    }

    public AssessmentPool createAssessmentPool(TolokaServer serverId) {
        return bcpService.create(AssessmentPool.FQN, Map.of(
                AssessmentPool.ASSESSMENT_RULE, createAssessmentRule(serverId)
        ));
    }
}

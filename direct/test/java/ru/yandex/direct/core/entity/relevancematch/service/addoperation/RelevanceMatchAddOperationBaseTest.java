package ru.yandex.direct.core.entity.relevancematch.service.addoperation;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchOperationBaseTest;

import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;

public abstract class RelevanceMatchAddOperationBaseTest extends RelevanceMatchOperationBaseTest {
    protected AdGroup getAdGroup() {
        return draftTextAdgroup(activeCampaign.getCampaignId());
    }

    RelevanceMatch getInvalidRelevanceMatch() {
        return new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.valueOf(1.12D))
                .withAutobudgetPriority(111);
    }
}

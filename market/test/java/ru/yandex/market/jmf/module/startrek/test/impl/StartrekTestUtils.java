package ru.yandex.market.jmf.module.startrek.test.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.Instant;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.module.startrek.StartrekIssue;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class StartrekTestUtils {

    @Inject
    private BcpService bcpService;

    public StartrekIssue createIssue(Map<String, Object> additionalAttributes) {
        Map<String, Object> requiredAttributes = Maps.of(
                StartrekIssue.STATUS, "Открыт",
                HasId.ID, Randoms.string(),
                StartrekIssue.STATUS_KEY, "open",
                StartrekIssue.CREATED_AT, Instant.now()
        );
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.putAll(requiredAttributes);
        attributes.putAll(CrmCollections.nullToEmpty(additionalAttributes));
        return bcpService.create(StartrekIssue.FQN, attributes);
    }
}

package ru.yandex.market.abo.core.storage.json.premod.antifraud;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 20.02.2020
 */
class JsonPremodAntiFraudResultServiceTest extends EmptyTest {

    private static final long TICKET_ID = 123L;

    @Autowired
    private JsonPremodAntiFraudResultService jsonPremodAntiFraudResultService;
    @Autowired
    private JsonPremodAntiFraudResultRepo jsonPremodAntiFraudResultRepo;

    @Test
    void saveAntiFraudRulesTest() {
        var antiFraudRules = List.of(AntiFraudRule.NO_REGION_RULE, AntiFraudRule.BUY_OOO_HOSTS_RULE);
        jsonPremodAntiFraudResultService.saveIfNecessary(TICKET_ID, antiFraudRules);
        flushAndClear();
        assertEquals(antiFraudRules, jsonPremodAntiFraudResultService.loadAntiFraudRules(TICKET_ID));
    }

    @Test
    void saveEmptyAntiFraudRulesTest() {
        jsonPremodAntiFraudResultService.saveIfNecessary(TICKET_ID, Collections.emptyList());
        flushAndClear();
        assertEquals(Collections.emptyList(), jsonPremodAntiFraudResultService.loadAntiFraudRules(TICKET_ID));
        assertEquals(0, jsonPremodAntiFraudResultRepo.count());
    }
}

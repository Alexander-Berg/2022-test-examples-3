package ru.yandex.market.pers.notify.external.sender.model;

import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         07.02.17
 */
public class SenderTemplateTest extends MarketMailerMockedDbTest {
    @Test
    public void getCampaign() throws Exception {
        for (SenderTemplate template : SenderTemplate.values()) {
            assertNotNull(template.getCampaign());
            assertFalse(template.getCampaign().isEmpty());
        }
    }
}

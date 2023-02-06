package ru.yandex.market.wms.common.spring.service.identities.rule;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dto.IdentityFrontInfoDto;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.service.identities.rule.component.EmptyRuleComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnRuleTest {

    private final SnRule snRule = new SnRule();

    @Test
    public void identityRuleMOD10test() {
        assertFalse(applyRule("123456789012347"), "SN rule check should fail");
        assertFalse(applyRule("490154203237518"), "SN rule check should fail");
        assertTrue(applyRule("79927398713"), "SN rule check should succeed");
        assertTrue(applyRule("79927398715"), "SN rule check should succeed");

        assertFalse(applyRule("359130333484790"), "SN rule check should fail");

        assertFalse(applyRule("12345678901234569"), "SN rule check should fail");
        assertFalse(applyRule("74389634752034027"), "SN rule check should fail");
        assertFalse(applyRule("23547320067844674"), "SN rule check should fail");
        assertFalse(applyRule("00000000000000000"), "SN rule check should fail");
        assertTrue(applyRule("00000000001000000"), "SN rule check should succeed");
        assertTrue(applyRule("95697889478389436"), "SN rule check should succeed");
    }

    private boolean applyRule(String identityValue) {
        return snRule.apply(
                IdentityFrontInfoDto.builder()
                        .type(TypeOfIdentity.SN).identity(identityValue).process("DEFAULT").build(),
                EmptyRuleComponent.INSTANCE)
                .getIsSuccess();
    }
}

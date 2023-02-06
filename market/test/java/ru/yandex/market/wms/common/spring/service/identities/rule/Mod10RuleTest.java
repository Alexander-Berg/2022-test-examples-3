package ru.yandex.market.wms.common.spring.service.identities.rule;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dto.IdentityFrontInfoDto;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.service.identities.rule.component.EmptyRuleComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mod10RuleTest {

    private final Mod10Rule mod10rule = new Mod10Rule();

    @Test
    public void identityRuleMOD10test() {
        assertTrue(applyRule("123456789012347"), "IMEI rule check should succeed");
        assertTrue(applyRule("490154203237518"), "IMEI rule check should succeed");
        assertTrue(applyRule("79927398713"), "IMEI rule check should succeed");
        assertFalse(applyRule("79927398715"), "IMEI rule check should fail");

        assertTrue(applyRule("359130333484790"), "IMEI rule check should succeed");

        //check for 17 digit IMEI
        assertTrue(applyRule("12345678901234569"), "IMEI rule check should succeed");
        assertTrue(applyRule("74389634752034027"), "IMEI rule check should succeed");
        assertTrue(applyRule("23547320067844674"), "IMEI rule check should succeed");
        assertTrue(applyRule("00000000000000000"), "IMEI rule check should succeed");
        assertFalse(applyRule("00000000001000000"), "IMEI rule check should fail");
        assertFalse(applyRule("95697889478389436"), "IMEI rule check should fail");
    }

    private boolean applyRule(String identityValue) {
        return mod10rule.apply(
                IdentityFrontInfoDto.builder()
                        .type(TypeOfIdentity.IMEI).identity(identityValue).process("DEFAULT").build(),
                EmptyRuleComponent.INSTANCE)
                .getIsSuccess();
    }
}

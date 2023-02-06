package ru.yandex.market.wms.common.spring.service.identities.rule;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.wms.common.spring.dao.entity.IdentityType;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.IdentityDao;
import ru.yandex.market.wms.common.spring.dto.IdentityFrontInfoDto;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.service.identities.IdentityCheckResult;
import ru.yandex.market.wms.common.spring.service.identities.rule.component.SkuRuleComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegexRuleTest {

    final IdentityDao identityDaoMock = mock(IdentityDao.class);
    final SkuId skuId = SkuId.of("123456", "ROVSKU");

    {
        final String testUser = "test user";
        final IdentityType identityType = IdentityType.builder()
                .type(TypeOfIdentity.SN.toString())
                .regex("test")
                .description("test identity")
                .addWho(testUser)
                .editWho(testUser)
                .build();
        when(identityDaoMock.getSkuIdentityTypeOrDefault(TypeOfIdentity.SN, skuId))
                .thenReturn(Optional.of(identityType));
    }

    @Test
    public void identityRuleRegexTestSuccess() {
        final IdentityCheckRule<SkuRuleComponent> rule = new RegexRule(identityDaoMock);
        assertFalse(rule.apply(IdentityFrontInfoDto.builder()
                                        .type(TypeOfIdentity.SN).identity("bad test").process("DEFAULT").build(),
                                new SkuRuleComponent(skuId))
                        .getIsSuccess(),
                "rule should fail for not matched value"
        );
        assertTrue(rule.apply(IdentityFrontInfoDto.builder()
                                        .type(TypeOfIdentity.SN).identity("test").process("DEFAULT").build(),
                                new SkuRuleComponent(skuId))
                        .getIsSuccess(),
                "rule should succeed for matched value"
        );
    }

    @Test
    public void unknownType() {
        final IdentityDao emptyDB = mock(IdentityDao.class);
        when(emptyDB.getIdentityType(Mockito.any())).thenReturn(Optional.empty());
        final IdentityCheckResult checkResult = new RegexRule(emptyDB).apply(
                IdentityFrontInfoDto.builder()
                        .type(TypeOfIdentity.CIS)
                        .identity("Sylvanas")
                        .process("bfa")
                        .build(),
                new SkuRuleComponent(skuId)
        );
        assertFalse(checkResult.getIsSuccess(), "rule should fail for not matched value");
        assertEquals("Regex of CIS identity type is not found for SKU SkuId(storerKey=123456, sku=ROVSKU)",
                checkResult.getReason());
    }
}

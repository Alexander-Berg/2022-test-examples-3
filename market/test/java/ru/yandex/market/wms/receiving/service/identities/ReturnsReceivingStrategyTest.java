package ru.yandex.market.wms.receiving.service.identities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.IdentityDao;
import ru.yandex.market.wms.common.spring.dao.implementation.InstanceIdentityDAO;
import ru.yandex.market.wms.common.spring.dto.IdentityFrontInfoDto;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.service.identities.IdentityCheckResult;
import ru.yandex.market.wms.common.spring.service.identities.rule.CisCryptotailRule;
import ru.yandex.market.wms.common.spring.service.identities.rule.CisGtinRule;
import ru.yandex.market.wms.common.spring.service.identities.rule.Mod10Rule;
import ru.yandex.market.wms.common.spring.service.identities.rule.RegexRule;
import ru.yandex.market.wms.common.spring.service.identities.rule.SnRule;
import ru.yandex.market.wms.common.spring.service.identities.rule.UniquenessRule;
import ru.yandex.market.wms.common.spring.service.identities.rule.component.ReceivingRuleComponent;
import ru.yandex.market.wms.common.spring.service.identities.strategy.BaseStrategy;
import ru.yandex.market.wms.common.spring.utils.CisParser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReturnsReceivingStrategyTest extends BaseTest {

    @Mock
    private BaseStrategy baseStrategy;
    private ReturnsReceivingStrategy strategy;

    @BeforeEach
    public void setUp() {
        strategy = new ReturnsReceivingStrategy(baseStrategy, mock(IdentityDao.class),
                mock(InstanceIdentityDAO.class), mock(CisParser.class));
    }

    @Test
    void checkIdentityValidatesSn() {
        IdentityFrontInfoDto identity = IdentityFrontInfoDto.builder()
                .identity("")
                .type(TypeOfIdentity.SN)
                .process("").build();
        ReceivingRuleComponent receivingRuleComponent =
                new ReceivingRuleComponent(SkuId.of("123", "123"), "123", ReceiptType.CUSTOMER_RETURN, null);
        strategy.checkIdentity(identity, InventoryHoldStatus.CIS_QUAR, receivingRuleComponent, false);
        verify(baseStrategy).validate(eq(identity), eq(RegexRule.class), any());
        verify(baseStrategy).validate(eq(identity), eq(Mod10Rule.class), any());
        verify(baseStrategy).validate(eq(identity), eq(UniquenessRule.class), any());
        verify(baseStrategy).validate(eq(identity), eq(SnRule.class), any());
    }

    @Test
    void checkCisValidatesCis() {
        IdentityFrontInfoDto identity = IdentityFrontInfoDto.builder()
                .identity("")
                .type(TypeOfIdentity.CIS)
                .process("").build();
        IdentityCheckResult checkResult = IdentityCheckResult.builder()
                .identity(identity)
                .build();
        ReceivingRuleComponent receivingRuleComponent =
                new ReceivingRuleComponent(SkuId.of("123", "123"), "123", ReceiptType.CUSTOMER_RETURN, null);
        strategy.checkCis(checkResult, InventoryHoldStatus.CIS_QUAR, receivingRuleComponent, false);
        verify(baseStrategy).validate(eq(identity), eq(CisGtinRule.class), any());
        verify(baseStrategy).validate(eq(identity), eq(CisCryptotailRule.class), any());
    }
}

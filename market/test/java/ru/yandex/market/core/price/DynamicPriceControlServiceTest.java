package ru.yandex.market.core.price;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;

@ParametersAreNonnullByDefault
class DynamicPriceControlServiceTest extends FunctionalTest {
    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private DynamicPriceControlService dynamicPriceControlService;

    @Test
    void testInitialParam() {
        DynamicPriceControlConfig config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertTrue(config.isDisabled());
    }

    @Test
    void testReadWrite() {
        writeConfig(774, 66);
        DynamicPriceControlConfig config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertEquals(66, config.maxDiscountPercent().intValueExact());

        writeConfig(774, 0);
        config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertEquals(0, config.maxDiscountPercent().intValueExact());
    }

    @Test
    void testReadWriteWithStrategy() {
        writeConfig(774, 66);
        DynamicPriceControlConfig config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertEquals(66, config.maxDiscountPercent().intValueExact());
        Assertions.assertEquals(DynamicPricingStrategy.BUYBOX, config.strategy());

        writeConfig(774, 10, DynamicPricingStrategy.REFERENCE);
        config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertEquals(10, config.maxDiscountPercent().intValueExact());
        Assertions.assertEquals(DynamicPricingStrategy.REFERENCE, config.strategy());
    }

    /**
     * Смотрим, что успешно выполняется, а не падает с NPE.
     */
    @Test
    void testWriteDisableConfigWhenAlreadyDisabled() {
        writeConfig(774, DynamicPriceControlConfig.disabled());
    }

    @Test
    void testReadWriteAndReset() {
        writeConfig(774, 33);
        DynamicPriceControlConfig config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertEquals(33, config.maxDiscountPercent().intValueExact());

        writeConfig(774, DynamicPriceControlConfig.disabled());
        config = dynamicPriceControlService.getDynamicPriceConfig(774);
        Assertions.assertTrue(config.isDisabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.01", "0.1", "1", "7.5", "99", "99.9", "100"})
    void testIsValidDynamicPriceControlConfigUpdate(String valueString) {
        writeConfig(774, new BigDecimal(valueString));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100", "-1", "-0.01", "-0.1", "100.1", "100.01", "100.001", "101", "105", "200", "1000", "10000"})
    void testIsInvalidDynamicPriceControlConfigUpdate(String valueString) {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> writeConfig(774, new BigDecimal(valueString))
        );
    }

    private void writeConfig(long partnerId, int maxDiscountPercent) {
        writeConfig(partnerId, BigDecimal.valueOf(maxDiscountPercent));
    }


    private void writeConfig(long partnerId, int maxDiscountPercent, DynamicPricingStrategy strategy) {
        writeConfig(partnerId, DynamicPriceControlConfig.of(BigDecimal.valueOf(maxDiscountPercent), strategy));
    }

    private void writeConfig(long partnerId, BigDecimal maxDiscountPercent) {
        DynamicPriceControlConfig config = DynamicPriceControlConfig.ofMaxAllowedDiscountPercent(maxDiscountPercent);
        writeConfig(partnerId, config);
    }

    private void writeConfig(long partnerId, DynamicPriceControlConfig config) {
        ActionContext actionContext = new UIDActionContext(ActionType.CHANGE_DYNAMIC_PRICE_CONTROL_CONFIG, 28938572);
        protocolService.operationInTransaction(actionContext, (transactionStatus, actionId) -> {
            dynamicPriceControlService.setDynamicPriceConfig(partnerId, config, actionId);
        });
    }
}

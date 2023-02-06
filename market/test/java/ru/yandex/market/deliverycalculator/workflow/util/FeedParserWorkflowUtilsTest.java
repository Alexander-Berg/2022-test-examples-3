package ru.yandex.market.deliverycalculator.workflow.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.DeliveryServicePriceCalculationRule;
import ru.yandex.market.deliverycalculator.model.DeliveryService;
import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для {@link FeedParserWorkflowUtils}.
 */
class FeedParserWorkflowUtilsTest {

    /**
     * Тест для {@link FeedParserWorkflowUtils#getProtoDeliveryService(DeliveryService)}.
     * Случай: маппинг услуги, чья цена фиксирована.
     */
    @Test
    void testGetProtoDeliveryService_serviceWithFixPrice() {
        DeliveryService service = new DeliveryService();
        service.setCode("INSURANCE");
        service.setEnabled(false);
        service.setPriceSchemaType(DeliveryServicePriceSchemaType.FIX);
        service.setPriceSchemaValue(150.25);
        service.setMinPrice(150.00);
        service.setMaxPrice(1200.00);

        DeliveryCalcProtos.DeliveryService actualResult = FeedParserWorkflowUtils.getProtoDeliveryService(service);

        assertNotNull(actualResult);
        assertEquals(service.getCode(), actualResult.getCode());
        assertEquals(service.getEnabled(), actualResult.getEnabledByDefault());
        assertEquals(DeliveryServicePriceCalculationRule.FIX, actualResult.getPriceCalculationRule());
        assertEquals(15025.00, actualResult.getPriceCalculationParameter());
        assertEquals(150L, actualResult.getMinValue());
        assertEquals(1200L, actualResult.getMaxValue());
    }

    /**
     * Тест для {@link FeedParserWorkflowUtils#getProtoDeliveryService(DeliveryService)}.
     * Случай: маппинг услуги, чья цена вычисляется как процент от суммы, оплаченной покупателем.
     */
    @Test
    void testGetProtoDeliveryService_serviceWithPercentPrice() {
        DeliveryService service = new DeliveryService();
        service.setCode("CASH_SERVICE");
        service.setEnabled(true);
        service.setPriceSchemaType(DeliveryServicePriceSchemaType.PERCENT_CASH);
        service.setPriceSchemaValue(00.02);
        service.setMinPrice(0.00);
        service.setMaxPrice(1200.00);

        DeliveryCalcProtos.DeliveryService actualResult = FeedParserWorkflowUtils.getProtoDeliveryService(service);

        assertNotNull(actualResult);
        assertEquals(service.getCode(), actualResult.getCode());
        assertEquals(service.getEnabled(), actualResult.getEnabledByDefault());
        assertEquals(DeliveryServicePriceCalculationRule.PERCENT_CASH, actualResult.getPriceCalculationRule());
        assertEquals(00.02, actualResult.getPriceCalculationParameter());
        assertEquals(0L, actualResult.getMinValue());
        assertEquals(1200L, actualResult.getMaxValue());
    }

}

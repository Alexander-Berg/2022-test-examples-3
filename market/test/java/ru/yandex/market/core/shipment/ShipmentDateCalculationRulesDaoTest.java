package ru.yandex.market.core.shipment;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.shipment.dao.ShipmentDateCalculationRulesDao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static ru.yandex.market.core.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.DELIVERY_DATE;
import static ru.yandex.market.core.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.ORDER_CREATION_DATE;

@DbUnitDataSet(before = "shipmentDateCalculationRulesDao.before.csv")
public class ShipmentDateCalculationRulesDaoTest extends FunctionalTest {

    @Autowired
    private ShipmentDateCalculationRulesDao tested;

    @Test
    void testGetForPartner() {
        Optional<ShipmentDateCalculationRule> result = tested.getForPartner(1L);
        assertTrue(result.isPresent());
        assertReflectionEquals(ShipmentDateCalculationRule.builder().
                withPartnerId(1L)
                .withHourBefore(14)
                .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(ORDER_CREATION_DATE)
                        .withDaysToAdd(0)
                        .build())
                .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(DELIVERY_DATE)
                        .withDaysToAdd(0)
                        .build())
                .build(), result.get());
    }

    @Test
    void testGetForPartner_whenNotExisting() {
        Optional<ShipmentDateCalculationRule> result = tested.getForPartner(2L);
        assertFalse(result.isPresent());
    }

    @Test
    @DbUnitDataSet(after = "shipmentDateCalculationRulesDao.insert.after.csv")
    void testInsert() {
        tested.save(ShipmentDateCalculationRule.builder().
                withPartnerId(2L)
                .withHourBefore(18)
                .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(ORDER_CREATION_DATE)
                        .withDaysToAdd(1)
                        .build())
                .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(DELIVERY_DATE)
                        .withDaysToAdd(-1)
                        .build())
                .build());
    }

    @Test
    @DbUnitDataSet(after = "shipmentDateCalculationRulesDao.update.after.csv")
    void testUpdate() {
        tested.save(ShipmentDateCalculationRule.builder().
                withPartnerId(1L)
                .withHourBefore(12)
                .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(DELIVERY_DATE)
                        .withDaysToAdd(-1)
                        .build())
                .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(ORDER_CREATION_DATE)
                        .withDaysToAdd(2)
                        .build())
                .build());
    }
}

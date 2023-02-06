package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;

import static org.junit.Assert.assertNotNull;

public class CustomPeriodApiTest {

//    @Test
//    public void testToCore() {
//        for (ProductPeriodApi type : ProductPeriodApi.values()) {
//            assertNotNull(type.toCoreEnum());
//        }
//    }

    @Test
    public void testFromCore() {
        for (CustomPeriodUnit v : CustomPeriodUnit.values()) {
            assertNotNull(CustomPeriodApi.fromCoreEnum(v));
        }
    }
}

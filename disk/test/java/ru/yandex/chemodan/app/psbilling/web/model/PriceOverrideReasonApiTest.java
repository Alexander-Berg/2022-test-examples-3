package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.groups.PriceOverrideReason;

import static org.junit.Assert.assertNotNull;

public class PriceOverrideReasonApiTest {

    @Test
    public void testFromCoreEnum() {
        for (PriceOverrideReason v : PriceOverrideReason.values()) {
            assertNotNull(PriceOverrideReasonApi.fromCoreEnum(v));
        }
    }
}

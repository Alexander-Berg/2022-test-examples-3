package ru.yandex.travel.api.services.localization;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.travel.hotels.proto.EPansionType;

public class PansionLocalizationTests {
    private final LocalizationService localizationService = new LocalizationService(new Inflector());

    @Test
    public void testHaveAllTranslations() {
        for (EPansionType pansionType : EPansionType.values()) {
            if (pansionType == EPansionType.PT_UNKNOWN || pansionType == EPansionType.UNRECOGNIZED) {
                continue;
            }
            Assert.assertNotEquals("PANSION_" + pansionType, localizationService.localizePansion(pansionType, "ru"));
        }
    }
}

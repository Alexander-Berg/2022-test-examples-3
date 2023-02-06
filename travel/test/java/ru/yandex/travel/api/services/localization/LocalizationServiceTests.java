package ru.yandex.travel.api.services.localization;

import org.junit.Assert;
import org.junit.Test;

public class LocalizationServiceTests {
    private final LocalizationService localizationService = new LocalizationService(new Inflector());

    @Test
    public void testSimple() {
        String enRegion = localizationService.getLocalizedValue("Region", "en");
        String ruRegion = localizationService.getLocalizedValue("Region", "ru");
        String trRegion = localizationService.getLocalizedValue("Region", "tr");
        String enHotel = localizationService.getLocalizedValue("Hotel", "en");
        String ruHotel = localizationService.getLocalizedValue("Hotel", "ru");
        String trHotel = localizationService.getLocalizedValue("Hotel", "tr");
        Assert.assertEquals("Region", enRegion);
        Assert.assertEquals("Регион", ruRegion);
        Assert.assertEquals("Bölge", trRegion);
        Assert.assertEquals("Hotel", enHotel);
        Assert.assertEquals("Гостиница", ruHotel);
        Assert.assertEquals("Otel", trHotel);

        String unknownTranslation = localizationService.getLocalizedValue("Hotal", "ja");
        Assert.assertEquals("Hotal", unknownTranslation);
    }
}

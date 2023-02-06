package ru.yandex.travel.api.endpoints.hotels_portal;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.travel.api.models.hotels.RoomAmenity;
import ru.yandex.travel.api.services.localization.Inflector;
import ru.yandex.travel.api.services.localization.LocalizationService;


public class HotelsPortalUtilsTest {
    @Test
    public void testSameMonthDates() {
        LocalizationService localizationService = new LocalizationService(new Inflector());
        LocalDate from = LocalDate.of(2018, 10, 30);
        LocalDate to =  LocalDate.of(2018, 10, 31);
        List<String> res = HotelsPortalUtils.formatDateRange(from, to, "ru", localizationService);
        Assert.assertEquals("30", res.get(0));
        Assert.assertEquals("31 октября 2018 года", res.get(1));
    }

    @Test
    public void testDiffMonthSameYearDates() {
        LocalizationService localizationService = new LocalizationService(new Inflector());
        LocalDate from =  LocalDate.of(2018, 10, 30);
        LocalDate to =  LocalDate.of(2018, 11, 30);
        List<String> res = HotelsPortalUtils.formatDateRange(from, to, "ru", localizationService);
        Assert.assertEquals("30 октября", res.get(0));
        Assert.assertEquals("30 ноября 2018 года", res.get(1));
    }

    @Test
    public void testDiffYearDates() {
        LocalizationService localizationService = new LocalizationService(new Inflector());
        LocalDate from =  LocalDate.of(2018, 10, 30);
        LocalDate to =  LocalDate.of(2019, 10, 30);
        List<String> res = HotelsPortalUtils.formatDateRange(from, to, "ru", localizationService);
        Assert.assertEquals("30 октября 2018 года", res.get(0));
        Assert.assertEquals("30 октября 2019 года", res.get(1));
    }

    @Test
    public void testExtractGrouppedRoomAmenities() {

        List<HotelsPortalUtils.AmenityInfo> amenityInfos = List.of(
                new HotelsPortalUtils.AmenityInfo(true, 103, "bathroom", "Ванная", new RoomAmenity("bathroom", "bathroom", "bathroom")),
                new HotelsPortalUtils.AmenityInfo(false, 0, "bathroom", "Ванная", new RoomAmenity("bathroom1", "bathroom1", "bathroom1")),
                new HotelsPortalUtils.AmenityInfo(true, 110, "bathroom", "Ванная", new RoomAmenity("bathroom1", "bathroom1", "bathroom1")),
                new HotelsPortalUtils.AmenityInfo(true, 250, "refrigerator", "Холодос", new RoomAmenity("refrigerator", "refrigerator", "refrigerator"))
        );

        var result = HotelsPortalUtils.extractRoomAmenitiesGroupped(HotelsPortalUtils.extractGrouppedAmenityInfo(amenityInfos));
        var bathroom = result.stream().filter(x -> (Objects.equals(x.getId(), "bathroom"))).collect(Collectors.toList());
        var refrigerator = result.stream().filter(x -> (Objects.equals(x.getId(), "refrigerator"))).collect(Collectors.toList());
        Assert.assertEquals(bathroom.size(), 1);
        Assert.assertEquals(bathroom.get(0).getAmenities().size(), 3);
        Assert.assertEquals(refrigerator.size(), 1);


    }
    @Test
    public void testMainAmenities() {

        List<HotelsPortalUtils.AmenityInfo> amenityInfos = List.of(
                new HotelsPortalUtils.AmenityInfo(true, 103, "bathroom", "Ванная", new RoomAmenity("bathroom", "bathroom", "bathroom")),
                new HotelsPortalUtils.AmenityInfo(false, 0, "bathroom", "Ванная", new RoomAmenity("bathroom1", "bathroom1", "bathroom1")),
                new HotelsPortalUtils.AmenityInfo(true, 410, "bathroom", "Ванная", new RoomAmenity("bathroom2", "bathroom2", "bathroom2")),
                new HotelsPortalUtils.AmenityInfo(true, 250, "refrigerator", "Холодос", new RoomAmenity("refrigerator", "refrigerator", "refrigerator"))
        );
        var result = HotelsPortalUtils.exractMainAmenities(HotelsPortalUtils.extractGrouppedAmenityInfo(amenityInfos), 1);
        var bathroom = result.get(0);
        Assert.assertEquals(bathroom.getId(), "bathroom2");

    }

}

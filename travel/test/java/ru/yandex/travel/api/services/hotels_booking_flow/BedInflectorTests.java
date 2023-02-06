package ru.yandex.travel.api.services.hotels_booking_flow;

import org.junit.Test;

import ru.yandex.travel.api.services.localization.Inflector;
import ru.yandex.travel.api.services.localization.LocalizationService;

import static org.assertj.core.api.Assertions.assertThat;

public class BedInflectorTests {
    Inflector inflector = new Inflector();
    BedInflector bedInflector = new BedInflector(
            new LocalizationService(inflector), inflector);

    @Test
    public void testMainBeds() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 1, "ru")).isEqualTo("одно основное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 2, "ru")).isEqualTo("два основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 3, "ru")).isEqualTo("три основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 4, "ru")).isEqualTo("четыре основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 5, "ru")).isEqualTo("пять основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 6, "ru")).isEqualTo("шесть основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 7, "ru")).isEqualTo("семь основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 8, "ru")).isEqualTo("восемь основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 9, "ru")).isEqualTo("девять основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 10, "ru")).isEqualTo("десять основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 11, "ru")).isEqualTo("11 основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 21, "ru")).isEqualTo("21 основное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 22, "ru")).isEqualTo("22 основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 23, "ru")).isEqualTo("23 основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 24, "ru")).isEqualTo("24 основных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 25, "ru")).isEqualTo("25 основных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.MAIN_BED, 99, "ru")).isEqualTo("99 основных мест");
    }

    @Test
    public void testExtraBeds() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 1, "ru")).isEqualTo("одно дополнительное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 2, "ru")).isEqualTo("два дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 3, "ru")).isEqualTo("три дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 4, "ru")).isEqualTo("четыре дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 5, "ru")).isEqualTo("пять дополнительных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 6, "ru")).isEqualTo("шесть дополнительных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 10, "ru")).isEqualTo("десять дополнительных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 11, "ru")).isEqualTo("11 дополнительных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 21, "ru")).isEqualTo("21 дополнительное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 31, "ru")).isEqualTo("31 дополнительное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 91, "ru")).isEqualTo("91 дополнительное место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 22, "ru")).isEqualTo("22 дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 23, "ru")).isEqualTo("23 дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 24, "ru")).isEqualTo("24 дополнительных места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 25, "ru")).isEqualTo("25 дополнительных мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_BED, 99, "ru")).isEqualTo("99 дополнительных мест");
    }

    @Test
    public void testChildBeds() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 1, "ru")).isEqualTo("одно детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 2, "ru")).isEqualTo("два детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 3, "ru")).isEqualTo("три детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 4, "ru")).isEqualTo("четыре детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 5, "ru")).isEqualTo("пять детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 6, "ru")).isEqualTo("шесть детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 10, "ru")).isEqualTo("десять детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 11, "ru")).isEqualTo("11 детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 21, "ru")).isEqualTo("21 детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 31, "ru")).isEqualTo("31 детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 91, "ru")).isEqualTo("91 детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 22, "ru")).isEqualTo("22 детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 23, "ru")).isEqualTo("23 детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 24, "ru")).isEqualTo("24 детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 25, "ru")).isEqualTo("25 детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_BED, 99, "ru")).isEqualTo("99 детских мест");
    }

    @Test
    public void testChildExtraBeds() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 1, "ru")).isEqualTo("одно дополнительное детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 2, "ru")).isEqualTo("два дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 3, "ru")).isEqualTo("три дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 4, "ru")).isEqualTo("четыре дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 5, "ru")).isEqualTo("пять дополнительных детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 6, "ru")).isEqualTo("шесть дополнительных детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 10, "ru")).isEqualTo("десять дополнительных детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 11, "ru")).isEqualTo("11 дополнительных детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 21, "ru")).isEqualTo("21 дополнительное детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 31, "ru")).isEqualTo("31 дополнительное детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 91, "ru")).isEqualTo("91 дополнительное детское место");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 22, "ru")).isEqualTo("22 дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 23, "ru")).isEqualTo("23 дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 24, "ru")).isEqualTo("24 дополнительных детских места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 25, "ru")).isEqualTo("25 дополнительных детских мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.EXTRA_CHILD_BED, 99, "ru")).isEqualTo("99 дополнительных детских мест");
    }

    @Test
    public void testChildrenNoBed() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 1, "ru")).isEqualTo("один ребенок без места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 2, "ru")).isEqualTo("двое детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 3, "ru")).isEqualTo("трое детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 4, "ru")).isEqualTo("четверо детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 5, "ru")).isEqualTo("пятеро детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 6, "ru")).isEqualTo("шестеро детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 10, "ru")).isEqualTo("десятеро детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 11, "ru")).isEqualTo("11 детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 21, "ru")).isEqualTo("21 ребенок без места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 31, "ru")).isEqualTo("31 ребенок без места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 91, "ru")).isEqualTo("91 ребенок без места");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 22, "ru")).isEqualTo("22 детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 23, "ru")).isEqualTo("23 детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 24, "ru")).isEqualTo("24 детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 25, "ru")).isEqualTo("25 детей без мест");
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.CHILD_NO_BED, 99, "ru")).isEqualTo("99 детей без мест");
    }

    @Test
    public void testAdultPlacesOfUnknownKind() {
        assertThat(bedInflector.inflectBedPlace(BedInflector.BedType.ADULT_BED, 2, "ru")).isEqualTo("два взрослых места");
    }
}

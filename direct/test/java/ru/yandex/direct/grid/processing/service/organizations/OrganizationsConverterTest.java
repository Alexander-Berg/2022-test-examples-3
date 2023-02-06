package ru.yandex.direct.grid.processing.service.organizations;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.grid.model.GdTime;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdAddress;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdPointOnMap;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdWorkTime;
import ru.yandex.direct.organizations.swagger.model.Address;
import ru.yandex.direct.organizations.swagger.model.AddressComponent;
import ru.yandex.direct.organizations.swagger.model.Coordinates;
import ru.yandex.direct.organizations.swagger.model.LocalizedString;
import ru.yandex.direct.organizations.swagger.model.Point2;
import ru.yandex.direct.organizations.swagger.model.WorkInterval;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.grid.processing.service.organizations.OrganizationsConverter.toGdAddress;
import static ru.yandex.direct.grid.processing.service.organizations.OrganizationsConverter.toGdPointOnMap;
import static ru.yandex.direct.grid.processing.service.organizations.OrganizationsConverter.toGdTime;
import static ru.yandex.direct.grid.processing.service.organizations.OrganizationsConverter.toGdWorkTimes;
import static ru.yandex.direct.organizations.swagger.model.AddressComponent.KindEnum.COUNTRY;
import static ru.yandex.direct.organizations.swagger.model.AddressComponent.KindEnum.LOCALITY;

@ParametersAreNonnullByDefault
public class OrganizationsConverterTest {
    @Test
    public void toGdPointOnMap_nullTest() {
        assertThat(toGdPointOnMap(null), nullValue());
    }

    @Test
    public void toGdPointOnMap_emptyCoordinatesTest() {
        assertThat(toGdPointOnMap(new Point2().coordinates(new Coordinates())), nullValue());
    }

    @Test
    public void toGdPointOnMap_nullCoordinatesTest() {
        Coordinates coordinates = new Coordinates();
        coordinates.add(null);
        coordinates.add(null);
        assertThat(toGdPointOnMap(new Point2().coordinates(coordinates)), nullValue());
    }

    @Test
    public void toGdPointOnMap_onlyOneCoordinateTest() {
        Coordinates coordinates = new Coordinates();
        coordinates.add(1.0);
        assertThat(toGdPointOnMap(new Point2().coordinates(coordinates)), nullValue());
    }

    @Test
    public void toGdPointOnMap_validCoordinateTest() {
        Coordinates coordinates = new Coordinates();
        coordinates.add(1.0);
        coordinates.add(10.0);
        GdPointOnMap expected = new GdPointOnMap().withY(BigDecimal.valueOf(10.0)).withX(BigDecimal.valueOf(1.0));
        assertThat(toGdPointOnMap(new Point2().coordinates(coordinates)), is(expected));
    }

    @Test
    public void toGdAddress_nullTest() {
        assertThat(toGdAddress(null), nullValue());
    }

    @Test
    public void toGdAddress_countryOnlyTest() {
        Address address = new Address().addComponentsItem(
                new AddressComponent().kind(COUNTRY).name(
                        new LocalizedString().locale("en").value("test")));
        assertThat(toGdAddress(address), is(new GdAddress().withCountry("test").withCity("")));
    }

    @Test
    public void toGdAddress_twoLocalitiesTest() {
        AddressComponent first = new AddressComponent().kind(LOCALITY)
                .name(new LocalizedString().locale("en").value("first"));
        AddressComponent second = new AddressComponent().kind(LOCALITY)
                .name(new LocalizedString().locale("en").value("second"));
        Address address = new Address().addComponentsItem(first).addComponentsItem(second);
        assertThat(toGdAddress(address), is(new GdAddress().withCity("first, second").withCountry("")));
    }

    @Test
    public void toGdTime_nullTest() {
        assertThat(toGdTime(null), nullValue());
    }

    @Test
    public void toGdTime_midnight() {
        assertThat(toGdTime(0L), is(new GdTime().withHour(0).withMinute(0)));
    }

    @Test
    public void toGdTime_oneAfterMidnight() {
        assertThat(toGdTime(1L), is(new GdTime().withHour(0).withMinute(1)));
    }

    @Test
    public void toGdTime_oneToMidnight() {
        assertThat(toGdTime(1439L), is(new GdTime().withHour(23).withMinute(59)));
    }

    @Test
    public void toGdWorkTimes_nullTest() {
        assertThat(toGdWorkTimes(null), empty());
    }

    @Test
    public void toGdWorkTimes_emptyTest() {
        assertThat(toGdWorkTimes(emptyList()), empty());
    }

    @Test
    public void toGdWorkTimes_oneWorkTime() {
        WorkInterval workInterval = new WorkInterval()
                .day(WorkInterval.DayEnum.MONDAY)
                .timeMinutesBegin(10L)
                .timeMinutesEnd(20L);
        GdWorkTime expected = new GdWorkTime()
                .withDaysOfWeek(List.of(0))
                .withStartTime(new GdTime().withMinute(10).withHour(0))
                .withEndTime(new GdTime().withMinute(20).withHour(0));
        assertThat(toGdWorkTimes(singleton(workInterval)), contains(expected));
    }

    @Test
    public void toGdWorkTimes_twoWorkTimes() {
        WorkInterval first = new WorkInterval()
                .day(WorkInterval.DayEnum.MONDAY)
                .timeMinutesBegin(10L)
                .timeMinutesEnd(20L);
        WorkInterval second = new WorkInterval()
                .day(WorkInterval.DayEnum.TUESDAY)
                .timeMinutesBegin(20L)
                .timeMinutesEnd(30L);
        GdWorkTime expectedFirst = new GdWorkTime()
                .withDaysOfWeek(List.of(0))
                .withStartTime(new GdTime().withMinute(10).withHour(0))
                .withEndTime(new GdTime().withMinute(20).withHour(0));
        GdWorkTime expectedSecond = new GdWorkTime()
                .withDaysOfWeek(List.of(1))
                .withStartTime(new GdTime().withMinute(20).withHour(0))
                .withEndTime(new GdTime().withMinute(30).withHour(0));
        assertThat(toGdWorkTimes(List.of(first, second)),
                contains(expectedFirst, expectedSecond));
    }
}

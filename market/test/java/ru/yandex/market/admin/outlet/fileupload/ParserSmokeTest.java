package ru.yandex.market.admin.outlet.fileupload;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
public class ParserSmokeTest {
    private static final DateTimeFormatter SCHEDULE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Nonnull
    private static List<? extends String> sampleData() {
        List<String> fields = new ArrayList<>();
        fields.add("50972371619777197");
        fields.add("123.RU Электрозаводская");
        fields.add("MIXED");
        fields.add("Москва");
        fields.add("Барабанный пер");
        fields.add("4");
        fields.add("1");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("10:00");
        fields.add("22:00");
        fields.add("1");
        fields.add("7(495)225-9123");
        fields.add("");
        fields.add("тел.");
        fields.add("info@123.ru");
        fields.add("1");
        fields.add("0");
        fields.add("");
        fields.add("0");
        fields.add("2");
        fields.add("24");
        fields.add("");
        fields.add("1");
        fields.add("10");
        return Collections.unmodifiableList(fields);
    }

    @Test
    public void testSampleData() throws RowParsingException {
        int shopID = 123;

        List<? extends String> fields = sampleData();

        OutletInfo outlet = Parser.parseRow(shopID, fields);
        assertEquals(shopID, outlet.getDatasourceId());
        assertEquals(fields.get(Column.ID.columnIndex()), outlet.getShopOutletId());
        assertEquals(fields.get(Column.TITLE.columnIndex()), outlet.getName());
        assertEquals(OutletType.MIXED, outlet.getType());

        Address address = outlet.getAddress();
        assertEquals(fields.get(Column.ADDRESS_REGION.columnIndex()), address.getCity());
        assertEquals(fields.get(Column.ADDRESS_STREET.columnIndex()), address.street().get());
        assertEquals(fields.get(Column.ADDRESS_NUMBER.columnIndex()), address.number().get());
        assertEquals(fields.get(Column.ADDRESS_BUILDING.columnIndex()), address.building().get());
        assertFalse(address.block().isPresent());
        assertFalse(address.laneKM().isPresent());
        assertFalse(address.other().isPresent());

        assertFalse(outlet.hasGeoInfo());

        Map<ScheduleLine.DayOfWeek, ScheduleLine> scheduleLines = outlet.getSchedule()
                .getLines()
                .stream()
                .collect(Collectors.toMap(ScheduleLine::getStartDay, Function.identity()));
        assertEquals(fields.get(Column.SCHEDULE_MON_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.MONDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_MON_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.MONDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_TUE_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.TUESDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_TUE_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.TUESDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_WED_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.WEDNESDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_WED_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.WEDNESDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_THU_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.THURSDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_THU_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.THURSDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_FRI_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.FRIDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_FRI_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.FRIDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_SAT_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.SATURDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_SAT_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.SATURDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_SUN_FROM.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.SUNDAY).localStartTime().format(SCHEDULE_TIME_FORMAT));
        assertEquals(fields.get(Column.SCHEDULE_SUN_TO.columnIndex()),
                scheduleLines.get(ScheduleLine.DayOfWeek.SUNDAY).localEndTime().format(SCHEDULE_TIME_FORMAT));
        for (ScheduleLine.DayOfWeek dayOfWeek : ScheduleLine.DayOfWeek.valuesExceptNeversday()) {
            ScheduleLine scheduleLine = scheduleLines.get(dayOfWeek);
            for (LocalTime time : new LocalTime[]{scheduleLine.localStartTime(), scheduleLine.localEndTime()}) {
                assertEquals(0, time.getMinute());
                assertEquals(0, time.getSecond());
                assertEquals(0, time.getNano());
            }
        }

        assertEquals(1, outlet.getPhones().size());
        PhoneNumber phone = outlet.getPhones().get(0);
        assertEquals("7", phone.getCountry());
        assertEquals("495", phone.getCity());
        assertEquals("2259123", phone.getNumber());
        assertEquals(PhoneType.PHONE, phone.getPhoneType());
        assertFalse(phone.comment().isPresent());
        assertFalse(phone.extension().isPresent());

        assertEquals(1, outlet.getEmails().size());
        assertEquals(fields.get(Column.EMAIL.columnIndex()), outlet.getEmails().get(0));

        assertEquals(fields.get(Column.IS_MAIN.columnIndex()).equals("1"), outlet.isMain());
        assertEquals(fields.get(Column.IS_ENABLED.columnIndex()).equals("1") ? OutletVisibility.VISIBLE : OutletVisibility.HIDDEN,
                outlet.getHidden());

        assertEquals(1, outlet.getDeliveryRules().size());
        DeliveryRule rule = outlet.getDeliveryRules().get(0);
        assertEquals(fields.get(Column.DOES_NOT_WORK_DURING_NATIONAL_HOLIDAYS.columnIndex()).equals("1"),
                !rule.getWorkInHoliday());
        assertEquals(fields.get(Column.DELIVERY_REQUEST_RECEIVE_TIME_UP_TO_IN_HOURS.columnIndex()),
                rule.getDateSwitchHour().toString());
        assertEquals(fields.get(Column.IS_DELIVERY_DELAY_UNSPECIFIED.columnIndex()).equals("1"),
                rule.getUnspecifiedDeliveryInterval());
        assertEquals(fields.get(Column.DELIVERY_COST.columnIndex()), rule.getCost().toString());
        assertEquals(fields.get(Column.DELIVERY_DELAY_IN_DAYS_MIN.columnIndex()),
                rule.getMinDeliveryDays().toString());
        assertEquals(fields.get(Column.DELIVERY_DELAY_IN_DAYS_MAX.columnIndex()),
                rule.getMaxDeliveryDays().toString());
        assertEquals(fields.get(Column.STORAGE_PERIOD.columnIndex()),
                outlet.getStoragePeriod().toString());
    }

    @Test
    public void testAddressSingleNonEmptyFieldWithLatitudeAndLongitude() throws RowParsingException {
        int shopID = 123;
        String city = "Москва";
        String addressBuilding = "1";

        String latitude = "30.0";
        String longitude = "30.0";

        List<? extends String> sample = sampleData();
        List<String> fields = new ArrayList<>();
        fields.addAll(sample.subList(0, Column.ADDRESS_REGION.columnIndex()));
        fields.add(city);
        fields.add("");
        fields.add("");
        fields.add(addressBuilding);
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add(latitude);
        fields.add(longitude);
        fields.addAll(sample.subList(Column.LONGITUDE.columnIndex() + 1, sample.size()));

        OutletInfo outlet;
        try {
            outlet = Parser.parseRow(shopID, fields);
        } catch (RowParsingException ex) {
            for (Map.Entry<List<? extends Column>, ? extends ErrorType> error : ex.errors().entrySet()) {
                System.out.println(error.getKey() + " has error " + error.getValue());
            }
            throw ex;
        }

        assertTrue(outlet.hasAddress());
        Address address = outlet.getAddress();
        assertEquals(city, address.getCity());
        assertFalse("", address.street().isPresent());
        assertFalse(address.number().isPresent());
        assertEquals(addressBuilding, address.building().get());
        assertFalse(address.block().isPresent());
        assertFalse(address.laneKM().isPresent());
        assertFalse(address.other().isPresent());

        assertTrue(outlet.hasGeoInfo());
        final Coordinates gpsCoordinates = outlet.getGeoInfo().getGpsCoordinates();
        assertEquals(Double.parseDouble(latitude), gpsCoordinates.getLat(), 1e-6);
        assertEquals(Double.parseDouble(longitude), gpsCoordinates.getLon(), 1e-6);
    }

    @Test(expected = RowParsingException.class)
    public void testAddressSingleNonEmptyFieldWithoutLatitudeAndLongitude() throws RowParsingException {
        int shopID = 123;
        String city = "Москва";
        String addressBuilding = "1";

        List<? extends String> sample = sampleData();
        List<String> fields = new ArrayList<>();
        fields.addAll(sample.subList(0, Column.ADDRESS_REGION.columnIndex()));
        fields.add(city);
        fields.add("");
        fields.add("");
        fields.add(addressBuilding);
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.addAll(sample.subList(Column.LONGITUDE.columnIndex() + 1, sample.size()));

        try {
            Parser.parseRow(shopID, fields);
        } catch (RowParsingException ex) {
            assertNotNull(ex.errors().get(Column.LATITUDE.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.LATITUDE.group()));
            assertNotNull(ex.errors().get(Column.LONGITUDE.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.LONGITUDE.group()));

            throw ex;
        }
    }

    @Test(expected = RowParsingException.class)
    public void testAddressAllFieldsBlank() throws RowParsingException {
        int shopID = 123;
        String city = "Москва";

        String latitude = "30.0";
        String longitude = "30.0";

        List<? extends String> sample = sampleData();
        List<String> fields = new ArrayList<>();
        fields.addAll(sample.subList(0, Column.ADDRESS_REGION.columnIndex()));
        fields.add(city);
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add("");
        fields.add(latitude);
        fields.add(longitude);
        fields.addAll(sample.subList(Column.LONGITUDE.columnIndex() + 1, sample.size()));

        try {
            Parser.parseRow(shopID, fields);
        } catch (RowParsingException ex) {
            assertNotNull(ex.errors().get(Column.ADDRESS_NUMBER.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_NUMBER.group()));
            assertNotNull(ex.errors().get(Column.ADDRESS_BUILDING.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_BUILDING.group()));
            assertNotNull(ex.errors().get(Column.ADDRESS_BLOCK.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_BLOCK.group()));
            assertNotNull(ex.errors().get(Column.ADDRESS_ESTATE.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_ESTATE.group()));
            assertNotNull(ex.errors().get(Column.ADDRESS_LANE_KM.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_LANE_KM.group()));
            assertNotNull(ex.errors().get(Column.ADDRESS_OTHER.group()));
            assertEquals(ErrorType.IS_MISSING, ex.errors().get(Column.ADDRESS_OTHER.group()));

            throw ex;
        }
    }
}

package ru.yandex.market.checkout.checkouter.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeInterval;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeSlotViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeslotsRequest;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemServiceControllerSerializationTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    /**
     * попался кейс с некорректной сериализацией LocalDateTime, пусть будет тестик, чтоб следующие разработчики могли
     * быстро поправить формат в соответствии с контрактами
     */
    @Test
    public void testCorrectDateTimeSerialization() {
        var timeSlotViewModel = new ItemServiceTimeSlotViewModel();
        var testDate = LocalDateTime.now();
        timeSlotViewModel.setDate(testDate);

        String result = testSerializationService.serializeCheckouterObject(timeSlotViewModel);
        var dateField = new JSONObject(result).get("date");
        var expectedResult = testDate.format(DateTimeFormatter.ofPattern(CheckouterDateFormats.DEFAULT));
        assertEquals(expectedResult, dateField);
    }

    /**
     * Exception before:
     * Text '01-12-2021' could not be parsed at index 0
     */
    @Test
    public void testCorrectDateSerialization() {
        String rawInfo = "{ \"date\": \"01-12-2021\"}";
        var model = testSerializationService.deserializeCheckouterObject(rawInfo,
                ItemServiceTimeslotsRequest.ItemServiceInfo.class);
        assertEquals(LocalDate.of(2021, Month.DECEMBER, 1), model.getDate());
    }

    @Test
    public void testCorrectShortTimeSerialization() {
        var timeInterval = new ItemServiceTimeInterval();
        timeInterval.setFromTime(LocalTime.of(9, 30));
        timeInterval.setToTime(LocalTime.of(15, 30));
        var rawTimeInterval = testSerializationService.serializeCheckouterObject(timeInterval);
        var json = new JSONObject(rawTimeInterval);
        assertEquals("09:30", json.getString("fromTime"));
        assertEquals("15:30", json.getString("toTime"));
    }

}

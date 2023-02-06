package ru.yandex.market.b2b.clients.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.mj.generated.client.calendar.api.CalendarApiClient;
import ru.yandex.mj.generated.client.calendar.model.HolidayDescription;
import ru.yandex.mj.generated.client.calendar.model.HolidayDescriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


public class HolidayServiceImplTest extends AbstractFunctionalTest {

    @MockBean
    private CalendarApiClient calendarApiClient;

    private final HolidayServiceImpl holidayService;

    private final HolidayDaoImpl dao;

    private final JdbcTemplate template;

    @Autowired
    public HolidayServiceImplTest(HolidayServiceImpl holidayService, HolidayDaoImpl dao, JdbcTemplate template) {
        this.holidayService = holidayService;
        this.dao = dao;
        this.template = template;
    }

    @BeforeEach
    public void clearHolidaysTable() {
        template.update("DELETE FROM holiday WHERE \"id\" != 1");
        template.update("UPDATE holiday SET \"nonWorkDate\" = '1970-01-01' WHERE \"id\" = 1");
    }

    @Test
    public void testUpdateAndCalc() throws ExecutionException, InterruptedException {
        // пустой календарь
        assertEquals(LocalDate.of(2022, 5, 6), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 1), 225));
        assertEquals(LocalDate.of(2022, 5, 8), holidayService.calcReservationDate(LocalDate.of(2022, 5, 1), 225));

        HolidayDescriptions may2022 = new HolidayDescriptions().holidays(HolidayDaoImplTest.may2022Holidays()
                .stream().map(date -> new HolidayDescription().date(date)).collect(Collectors.toList()));

        CompletableFuture<HolidayDescriptions> completableFutureMock = Mockito.mock(CompletableFuture.class);
        Mockito.when(completableFutureMock.get()).thenReturn(may2022).thenThrow(new InterruptedException());

        ExecuteCall<HolidayDescriptions, RetryStrategy> executeCallMock = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCallMock.schedule()).thenReturn(completableFutureMock);

        Mockito.when(calendarApiClient.getHolidays(any(), any(), eq("225"), any(), eq("holidays")))
                .thenReturn(executeCallMock);

        holidayService.updateHolidays();

        // календарь загружен
        assertEquals(LocalDate.of(2022, 5, 12), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 1), 225));
        assertEquals(LocalDate.of(2022, 5, 16), holidayService.calcReservationDate(LocalDate.of(2022, 5, 1), 225));

        assertEquals(LocalDate.of(2022, 5, 27), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 20), 225));
        assertEquals(LocalDate.of(2022, 5, 31), holidayService.calcReservationDate(LocalDate.of(2022, 5, 20), 225));

        // чистим календарь с 16 мая
        dao.updateHolidays(LocalDate.of(2022, 5, 16), LocalDate.of(2022, 6, 15), 225, new HashSet<>());

        // обновление "сегодня" уже было или ловим исключение и тоже не обновляемся
        holidayService.updateHolidays();

        assertEquals(LocalDate.of(2022, 5, 25), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 20), 225));
        assertEquals(LocalDate.of(2022, 5, 27), holidayService.calcReservationDate(LocalDate.of(2022, 5, 20), 225));

        dao.updateHolidays(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 6, 15), 225, new HashSet<>());
    }

    @Test
    public void testUpdateWithException() throws ExecutionException, InterruptedException {
        assertTrue(dao.getHolidays(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 7, 31), 225).isEmpty());

        // пустой календарь
        assertEquals(LocalDate.of(2022, 5, 6), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 1), 225));
        assertEquals(LocalDate.of(2022, 5, 8), holidayService.calcReservationDate(LocalDate.of(2022, 5, 1), 225));

        CompletableFuture<HolidayDescriptions> completableFutureMock = Mockito.mock(CompletableFuture.class);
        Mockito.when(completableFutureMock.get()).thenThrow(new InterruptedException());

        ExecuteCall<HolidayDescriptions, RetryStrategy> executeCallMock = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCallMock.schedule()).thenReturn(completableFutureMock);

        Mockito.when(calendarApiClient.getHolidays(any(), any(), eq("225"), any(), eq("holidays")))
                .thenReturn(executeCallMock);

        holidayService.updateHolidays();

        // календарь не загружен случилось исключение
        assertEquals(LocalDate.of(2022, 5, 6), holidayService.calcPaymentDate(LocalDate.of(2022, 5, 1), 225));
        assertEquals(LocalDate.of(2022, 5, 8), holidayService.calcReservationDate(LocalDate.of(2022, 5, 1), 225));
    }
}

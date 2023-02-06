package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDeliveryServiceCalendarRequest;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDropshipBySellerGraphRequest;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerHolidaysDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.SetPartnerHolidaysPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Установка выходных партнёра")
class SetPartnerHolidaysConsumerTest extends AbstractContextualTest {
    private static final long PARTNER_ID = 1000L;
    private static final LocalDate DATE_FROM = LocalDate.parse("2020-08-12");
    private static final LocalDate DATE_TO = LocalDate.parse("2020-08-18");
    private static final List<LocalDate> HOLIDAY_DATES = List.of(
        LocalDate.parse("2020-08-13"),
        LocalDate.parse("2020-08-14")
    );

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SetPartnerHolidaysConsumer setPartnerHolidaysConsumer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешная установка выходных")
    @DatabaseSetup("/jobs/consumer/set_partner_holidays/before/dropship_partner_setting.xml")
    void successSet() {
        setPartnerHolidaysConsumer.execute(createTask(HOLIDAY_DATES));
        verify(lmsClient).setPartnerHolidays(PARTNER_ID, DATE_FROM, DATE_TO, partnerHolidaysDtoOf(HOLIDAY_DATES));
    }

    @Test
    @DisplayName("Успешная установка пустых выходных")
    @DatabaseSetup("/jobs/consumer/set_partner_holidays/before/dropship_partner_setting.xml")
    void successSetEmpty() {
        setPartnerHolidaysConsumer.execute(createTask(List.of()));
        verify(lmsClient).setPartnerHolidays(PARTNER_ID, DATE_FROM, DATE_TO, partnerHolidaysDtoOf(List.of()));
    }

    @Test
    @DisplayName("Произошла ошибка, её обработали")
    @DatabaseSetup("/jobs/consumer/set_partner_holidays/before/dropship_partner_setting.xml")
    void exception() {
        doThrow(new RuntimeException("error"))
            .when(lmsClient).setPartnerHolidays(PARTNER_ID, DATE_FROM, DATE_TO, partnerHolidaysDtoOf(HOLIDAY_DATES));

        TaskExecutionResult result = setPartnerHolidaysConsumer.execute(createTask(HOLIDAY_DATES));

        softly.assertThat(result)
            .extracting(TaskExecutionResult::getActionType)
            .isEqualTo(TaskExecutionResult.Type.FAIL);
        verify(lmsClient).setPartnerHolidays(PARTNER_ID, DATE_FROM, DATE_TO, partnerHolidaysDtoOf(HOLIDAY_DATES));
    }

    @Test
    @DisplayName("Успешное обновление логистического графа DBS-партнёра")
    @DatabaseSetup("/jobs/consumer/set_partner_holidays/before/dbs_partner_setting.xml")
    void successUpdateDbsGraph() {
        setPartnerHolidaysConsumer.execute(createTask(HOLIDAY_DATES));
        verify(lmsClient).updateDropshipBySellerGraph(PARTNER_ID, updateDbsGraphRequest());
    }

    @Test
    @DisplayName("Ничего не обновилось в LMS, т.к. нет shop_partner_settings")
    void noLmsCall() {
        softly.assertThatCode(() -> setPartnerHolidaysConsumer.execute(createTask(HOLIDAY_DATES)))
            .doesNotThrowAnyException();
    }

    @Nonnull
    private Task<SetPartnerHolidaysPayload> createTask(List<LocalDate> dates) {
        return new Task<>(
            new QueueShardId("1"),
            new SetPartnerHolidaysPayload(REQUEST_ID, PARTNER_ID, DATE_FROM, DATE_TO, dates, 1L),
            1,
            clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
            null,
            null
        );
    }

    @Nonnull
    private PartnerHolidaysDto partnerHolidaysDtoOf(List<LocalDate> dates) {
        return PartnerHolidaysDto.newBuilder()
            .days(dates)
            .build();
    }

    @Nonnull
    private UpdateDropshipBySellerGraphRequest updateDbsGraphRequest() {
        return UpdateDropshipBySellerGraphRequest.newBuilder()
            .updateDeliveryServiceCalendar(
                UpdateDeliveryServiceCalendarRequest.newBuilder()
                    .dateFrom(DATE_FROM)
                    .dateTo(DATE_TO)
                    .holidayDates(Set.copyOf(HOLIDAY_DATES))
                    .build()
            )
            .build();
    }
}

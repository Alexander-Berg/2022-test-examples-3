package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import ru.yandex.market.logistics.nesu.jobs.model.SetPartnerHolidaysBatchPayload;
import ru.yandex.market.logistics.nesu.jobs.model.SetPartnerHolidaysData;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Установка выходных партнёров")
@DatabaseSetup("/jobs/consumer/set_partner_holidays/before/batch_settings.xml")
class SetPartnerHolidaysBatchConsumerTest extends AbstractContextualTest {
    private static final long PARTNER_ID_1 = 1000L;
    private static final long PARTNER_ID_2 = 1001L;
    private static final long PARTNER_ID_3 = 1002L;
    private static final long PARTNER_ID_4 = 1003L;
    private static final long PARTNER_ID_5 = 1004L;
    private static final long PARTNER_ID_6 = 1005L;
    private static final LocalDate DATE_FROM = LocalDate.parse("2020-08-12");
    private static final LocalDate DATE_TO = LocalDate.parse("2020-08-18");
    private static final List<LocalDate> HOLIDAY_DATES = List.of(
        LocalDate.parse("2020-08-13"),
        LocalDate.parse("2020-08-14")
    );

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SetPartnerHolidaysBatchConsumer setPartnerHolidaysBatchConsumer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешная установка выходных и обновление логистического графа DBS-партнёра")
    void successSet() {
        setPartnerHolidaysBatchConsumer.execute(createBatchTask());
        verify(lmsClient).setPartnersHolidays(
            DATE_FROM,
            DATE_TO,
            Map.of(
                PARTNER_ID_2,
                partnerHolidaysDtoOf(),
                PARTNER_ID_3,
                partnerHolidaysDtoOf()
            )
        );
        verify(lmsClient).updateDropshipBySellerGraphs(List.of(
            updateDbsGraphRequest(PARTNER_ID_1),
            updateDbsGraphRequest(PARTNER_ID_4)
        ));
        verify(lmsClient).updateDropshipBySellerGraphs(List.of(updateDbsGraphRequest(PARTNER_ID_5)));
    }

    @Nonnull
    private Task<SetPartnerHolidaysBatchPayload> createBatchTask() {
        return new Task<>(
            new QueueShardId("1"),
            new SetPartnerHolidaysBatchPayload(
                REQUEST_ID,
                List.of(
                    //dbs shop
                    setPartnerHolidaysData(PARTNER_ID_1, 1L),
                    setPartnerHolidaysData(PARTNER_ID_2, 2L),
                    // non-existent partner
                    setPartnerHolidaysData(1L, 3L),
                    setPartnerHolidaysData(PARTNER_ID_2, 4L),
                    setPartnerHolidaysData(PARTNER_ID_3, 5L),
                    setPartnerHolidaysData(PARTNER_ID_3, 6L),
                    //dbs shop
                    setPartnerHolidaysData(PARTNER_ID_4, 7L),
                    //dbs shop
                    setPartnerHolidaysData(PARTNER_ID_5, 8L),
                    //not active dbs shop
                    setPartnerHolidaysData(PARTNER_ID_6, 9L)
                )
            ),
            1,
            clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
            null,
            null
        );
    }

    @Nonnull
    private PartnerHolidaysDto partnerHolidaysDtoOf() {
        return PartnerHolidaysDto.newBuilder()
            .days(HOLIDAY_DATES)
            .build();
    }

    @Nonnull
    private UpdateDropshipBySellerGraphRequest updateDbsGraphRequest(Long partnerId) {
        return UpdateDropshipBySellerGraphRequest.newBuilder()
            .partnerId(partnerId)
            .updateDeliveryServiceCalendar(
                UpdateDeliveryServiceCalendarRequest.newBuilder()
                    .dateFrom(DATE_FROM)
                    .dateTo(DATE_TO)
                    .holidayDates(Set.copyOf(HOLIDAY_DATES))
                    .build()
            )
            .build();
    }

    @Nonnull
    private SetPartnerHolidaysData setPartnerHolidaysData(long partnerId, long shopId) {
        return new SetPartnerHolidaysData(partnerId, DATE_FROM, DATE_TO, HOLIDAY_DATES, shopId);
    }
}

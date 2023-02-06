package ru.yandex.market.delivery.mdbapp.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.delivery.mdbapp.integration.service.DeliveryScoringService;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.date.DateDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.date.EarlyDateDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.date.LongDateIntervalDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.date.PastDateDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.date.ShortDateDeliveryIntervalOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.time.EarlyTimeDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.time.LongTimeIntervalDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.time.PastTimeDeliveryOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.time.ShortTimeDeliveryIntervalOptionHandler;
import ru.yandex.market.delivery.mdbapp.util.delivery.scoring.time.TimeDeliveryOptionHandler;

public class DeliveryScoringServiceTest {

    private final List<DateDeliveryOptionHandler> dateScoringList = Arrays.asList(
        new EarlyDateDeliveryOptionHandler(),
        new LongDateIntervalDeliveryOptionHandler(),
        new PastDateDeliveryOptionHandler(),
        new ShortDateDeliveryIntervalOptionHandler()
    );

    private final List<TimeDeliveryOptionHandler> timeScoringList = Arrays.asList(
        new EarlyTimeDeliveryOptionHandler(),
        new LongTimeIntervalDeliveryOptionHandler(),
        new PastTimeDeliveryOptionHandler(),
        new ShortTimeDeliveryIntervalOptionHandler()
    );

    private final DeliveryScoringService deliveryScoringService = new DeliveryScoringService(
        dateScoringList,
        timeScoringList
    );

    @Test
    public void findBestDeliveryDateTest() {
        DeliveryDates deliveryDates = createDeliveryDate(
            LocalDate.of(2018, 1, 10),
            LocalDate.of(2018, 1, 13)
        );

        Set<DeliveryOption> result =
            deliveryScoringService.getBestDeliveryOption(createDeliveryOptions(), deliveryDates);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result).extracting(DeliveryOption::getFromDate)
                .containsOnly(
                    LocalDate.of(2018, 1, 11),
                    LocalDate.of(2018, 1, 10)
                );
            softAssertions.assertThat(result).extracting(DeliveryOption::getToDate)
                .containsOnly(
                    LocalDate.of(2018, 1, 12),
                    LocalDate.of(2018, 1, 13)
                );
        });
    }

    @Test
    public void findBestTimeIntervalTest() {
        DeliveryDates deliveryDates = createDeliveryDate(
            LocalDate.of(2018, 1, 10),
            LocalDate.of(2018, 1, 13),
            LocalTime.of(10, 0),
            LocalTime.of(13, 0)
        );

        TimeInterval result = deliveryScoringService.getBestTimeInterval(createDeliveryOptionWithTime(), deliveryDates);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result).extracting(TimeInterval::getFromTime)
                .isEqualTo(LocalTime.of(11, 0));
            softAssertions.assertThat(result).extracting(TimeInterval::getToTime)
                .isEqualTo(LocalTime.of(13, 0));
        });
    }

    private DeliveryDates createDeliveryDate(
        LocalDate dateFrom, LocalDate dateTo,
        LocalTime timeFrom, LocalTime timeTo
    ) {
        DeliveryDates deliveryDates = createDeliveryDate(dateFrom, dateTo);
        deliveryDates.setFromTime(timeFrom);
        deliveryDates.setToTime(timeTo);
        return deliveryDates;
    }

    private DeliveryDates createDeliveryDate(LocalDate dateFrom, LocalDate dateTo) {
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(convertToDate(dateFrom));
        deliveryDates.setToDate(convertToDate(dateTo));
        return deliveryDates;
    }

    @Test
    public void findBestTimeIntervalEmptyValuesTest() {
        DeliveryDates deliveryDates = createDeliveryDate(
            LocalDate.of(2018, 1, 10),
            LocalDate.of(2018, 1, 13),
            LocalTime.of(10, 0),
            LocalTime.of(13, 0)
        );

        TimeInterval result = deliveryScoringService.getBestTimeInterval(ImmutableSet.of(), deliveryDates);
        SoftAssertions.assertSoftly(softAssertions -> softAssertions.assertThat(result).isNull());
    }

    private Set<TimeInterval> createDeliveryOptionWithTime() {
        return ImmutableSet.of(
            createTimeInterval(6, 9),
            createTimeInterval(8, 10),
            createTimeInterval(3, 12),
            createTimeInterval(11, 12),
            createTimeInterval(12, 15),
            createTimeInterval(13, 16),
            createTimeInterval(14, 17),
            createTimeInterval(9, 14),
            createTimeInterval(10, 12),
            createTimeInterval(11, 13),
            createTimeInterval(10, 14),
            createTimeInterval(9, 13)
        );
    }

    private Set<DeliveryOption> createDeliveryOptions() {
        return ImmutableSet.of(
            createDeliveryOption(6, 9),
            createDeliveryOption(8, 10),
            createDeliveryOption(3, 12),
            createDeliveryOption(11, 12),
            createDeliveryOption(12, 15),
            createDeliveryOption(13, 16),
            createDeliveryOption(14, 17),
            createDeliveryOption(9, 14),
            createDeliveryOption(10, 12),
            createDeliveryOption(11, 13),
            createDeliveryOption(10, 14),
            createDeliveryOption(9, 13)
        );
    }

    private TimeInterval createTimeInterval(int fromHour, int toHour) {
        TimeInterval timeInterval = new TimeInterval();
        timeInterval.setFromTime(LocalTime.of(fromHour, 0));
        timeInterval.setToTime(LocalTime.of(toHour, 0));
        return timeInterval;
    }

    private DeliveryOption createDeliveryOption(int dayFrom, int dayTo) {
        DeliveryOption deliveryOption = new DeliveryOption();
        deliveryOption.setFromDate(LocalDate.of(2018, 1, dayFrom));
        deliveryOption.setToDate(LocalDate.of(2018, 1, dayTo));
        return deliveryOption;
    }

    private Date convertToDate(LocalDate dateToConvert) {
        return java.util.Date.from(dateToConvert.atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant());
    }

}

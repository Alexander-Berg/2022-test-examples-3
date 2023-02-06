package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.qameta.allure.Epic;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.ScheduleDayCompareUtils;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class GetInboundScheduleAbstractTest extends AbstractLmsLomTest {

    private static final List<ScheduleDayResponse> EXPECTED_SCHEDULE_DAYS = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/inbound_schedule/filled_inbound_schedule.json"),
        ScheduleDays.class
    )
        .getScheduleDays();

    protected static final LogisticSegmentInboundScheduleFilter FILTER_FOR_FILLED_INBOUND_SCHEDULE =
        new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(INBOUND_SCHEDULE_PARTNER_FROM)
            .setToPartnerId(INBOUND_SCHEDULE_PARTNER_TO)
            .setDeliveryType(FILLED_INBOUND_SCHEDULE_DELIVERY_TYPE);

    protected static final LogisticSegmentInboundScheduleFilter FILTER_FOR_SERVICE_WITHOUT_SCHEDULE =
        new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(INBOUND_SCHEDULE_PARTNER_FROM)
            .setToPartnerId(INBOUND_SCHEDULE_PARTNER_TO)
            .setDeliveryType(EMPTY_INBOUND_SCHEDULE_DELIVERY_TYPE);

    protected static final LogisticSegmentInboundScheduleFilter FILTER_FOR_DS_WITH_NO_SERVICE =
        new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(INBOUND_SCHEDULE_PARTNER_FROM)
            .setToPartnerId(INBOUND_SCHEDULE_PARTNER_TO);

    protected static final LogisticSegmentInboundScheduleFilter SEGMENTS_HAVE_NO_EDGES_FILTER =
        new LogisticSegmentInboundScheduleFilter()
            .setFromPartnerId(INBOUND_SCHEDULE_PARTNER_TO)
            .setToPartnerId(INBOUND_SCHEDULE_PARTNER_FROM)
            .setDeliveryType(FILLED_INBOUND_SCHEDULE_DELIVERY_TYPE);

    @Test
    @DisplayName("Проверка ожидаемых от лмс данных: расписание заборов заполнено")
    public void checkPreConditionForFilledInboundSchedule() {
        List<ScheduleDayResponse> lmsSchedules = LMS_STEPS.getInboundSchedule(FILTER_FOR_FILLED_INBOUND_SCHEDULE)
            .getEntities();

        ScheduleDayCompareUtils.compareSchedules(softly, EXPECTED_SCHEDULE_DAYS, lmsSchedules);
    }

    @Test
    @DisplayName("Проверка ожидаемых от лмс данных: у сервиса СД-сегмента пустое расписание")
    public void checkPreConditionForServiceHasNoSchedule() {
        List<ScheduleDayResponse> lmsSchedules = LMS_STEPS.getInboundSchedule(FILTER_FOR_SERVICE_WITHOUT_SCHEDULE)
            .getEntities();

        ScheduleDayCompareUtils.compareSchedules(softly, List.of(), lmsSchedules);
    }

    @Test
    @DisplayName("Проверка ожидаемых от лмс данных: у СД-сегмента нет сервиса с нужным типом")
    public void checkPreConditionForDsSegmentHasNoService() {
        List<ScheduleDayResponse> lmsSchedules = LMS_STEPS.getInboundSchedule(FILTER_FOR_DS_WITH_NO_SERVICE)
            .getEntities();

        ScheduleDayCompareUtils.compareSchedules(softly, List.of(), lmsSchedules);
    }

    @Test
    @DisplayName("Проверка ожидаемых от лмс данных:  нет ребер между сегментами")
    public void checkPreConditionForSegmentsHaveNoEdge() {
        List<ScheduleDayResponse> lmsSchedules = LMS_STEPS.getInboundSchedule(SEGMENTS_HAVE_NO_EDGES_FILTER)
            .getEntities();

        ScheduleDayCompareUtils.compareSchedules(softly, List.of(), lmsSchedules);
    }

    @Test
    @DisplayName("Поиск расписания по фильтру")
    public void getFilledInboundScheduleByFilter() {
        sendRequestsAndCompareResponses(FILTER_FOR_FILLED_INBOUND_SCHEDULE);
    }

    @Test
    @DisplayName("Поиск расписания по фильтру: у сервиса СД-сегмента пустое расписание")
    public void getInboundScheduleByFilterServiceHasNoSchedule() {
        sendRequestsAndCompareResponses(FILTER_FOR_SERVICE_WITHOUT_SCHEDULE);
    }

    @Test
    @DisplayName("Поиск расписания по фильтру: у СД-сегмента нет сервиса с нужным типом")
    public void getInboundScheduleByFilterDsSegmentHasNoService() {
        sendRequestsAndCompareResponses(FILTER_FOR_DS_WITH_NO_SERVICE);
    }

    @Test
    @DisplayName("Поиск расписания по фильтру: нет ребер между сегментами")
    public void getInboundScheduleByFilterSegmentsHaveNoEdge() {
        sendRequestsAndCompareResponses(SEGMENTS_HAVE_NO_EDGES_FILTER);
    }

    public abstract void sendRequestsAndCompareResponses(LogisticSegmentInboundScheduleFilter filter);

    @Data
    @NoArgsConstructor
    public static class ScheduleDays {
        @JsonProperty("schedule_days")
        List<ScheduleDayResponse> scheduleDays;
    }
}

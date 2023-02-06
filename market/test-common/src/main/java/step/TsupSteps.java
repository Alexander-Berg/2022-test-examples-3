package step;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import client.TsupClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.tsup.controller.dto.NewPipelineDto;
import ru.yandex.market.tsup.domain.entity.rating.StatusColorCode;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleDto;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunResponse;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunsWithAddresses;
import ru.yandex.market.tsup.service.data_provider.entity.run.enums.RunStatus;

@Slf4j
public class TsupSteps {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final TsupClient TSUP = new TsupClient();

    private static final long EXPECTED_RUN_START_POINT = 10001001607L;
    private static final long EXPECTED_RUN_END_POINT = 10000840229L;

    @Step("Ищем trip между двумя партнёрами на выбранный день, возвращаем первый найденный")
    public RunResponse getFirstTrip(
        long fromLogisticPointId,
        long toLogisticPointId,
        LocalDate date,
        TransportationType transportationType
    ) {
        log.debug("Get first trip for the selected day");
        return Retrier.retry(() -> {
            RunsWithAddresses tripsForDay = TSUP.getRuns(
                fromLogisticPointId,
                toLogisticPointId,
                date,
                date,
                transportationType
            );
            Assertions.assertNotNull(tripsForDay.getData(), "Пустой список рейсов");
            Assertions.assertFalse(tripsForDay.getData().isEmpty(), "Пустой список рейсов");
            Assertions.assertNotNull(tripsForDay.getData().get(0), "Пустой объект первого рейса");
            return tripsForDay.getData().get(0);
        });
    }

    @Step("Ожидаем корректный рейс на выбранную дату")
    public void verifyRun(long runId, long fromLogisticPointId, long toLogisticPointId, LocalDate date) {
        log.debug("Verifying trip");
        Retrier.retry(() -> Assertions.assertTrue(
            TSUP.getRuns(fromLogisticPointId, toLogisticPointId, date, date, null).getData()
                .stream()
                .anyMatch(runResponse -> runResponse.getCarrierMovementExternalId().equals("TMT" + runId)),
            "Не найден рейс с id TMT" + runId));
    }

    @Step("Получаем маршрут между двумя точками")
    public Long getRouteBetweenPartners(Long startPartnerId, Long endPartnerId) {
        log.debug("Getting route between two partners");
        return Retrier.retry(() -> {
            Assertions.assertNotNull(
                TSUP.getRoutes(startPartnerId, endPartnerId).getData().get(0).getRouteId(),
                "Не найден маршрут между партнёрами"
            );
            return TSUP.getRoutes(startPartnerId, endPartnerId).getData().get(0).getRouteId();
        });
    }

    @Step("Создаём пайплайн для маршрута между двумя точками")
    public Long createRoutePipeline(Long startPartnerId, Long endPartnerId) {
        log.debug("Creating pipeline for route between two partners");
        NewPipelineDto dto = new NewPipelineDto();
        dto.setPipelineName("ROUTE_CREATOR");
        ObjectMapper mapper = new ObjectMapper();
        var initialPayload = mapper.convertValue((Map.of(
            "routeName", "Маршрут для удаления автотестами, не создавайте расписания!",
            "routePoints", List.of(
                Map.of(
                    "partnerId", startPartnerId,
                    "index", 0),
                Map.of(
                    "partnerId", endPartnerId,
                    "index", 1)
            ),
            "pointPairs", List.of(
                Map.of(
                    "outboundIndex", 0,
                    "inboundIndex", 1
                )
            )
        )), JsonNode.class);
        dto.setInitialPayload(initialPayload);
        return Retrier.retry(() -> TSUP.createPipeline(dto));
    }

    @Step("Получаем расписание по маршруту")
    public List<RouteScheduleDto> getRouteMayBeEmptySchedules(long routeId) {
        log.debug("Getting schedule by route");
        return TSUP.getSchedulesByRoute(routeId).getRouteSchedules();
    }

    @Step("Получаем расписание по маршруту")
    public List<RouteScheduleDto> getRouteSchedules(long routeId) {
        log.debug("Getting schedules by route");
        return Retrier.retry(() -> {
            Assertions.assertTrue(
                TSUP.getSchedulesByRoute(routeId).getRouteSchedules().size() != 0,
                "Не найдено расписаний по данному маршруту"
            );
            return TSUP.getSchedulesByRoute(routeId).getRouteSchedules();
        });
    }

    @Step("Создаём пайплайн для расписания между двумя точками")
    public Long createSchedulePipeline(long routeId, LocalDate startDate, LocalDate endDate) {
        log.debug("Creating pipeline for schedule between two partners");
        NewPipelineDto dto = new NewPipelineDto();
        dto.setPipelineName("ROUTE_SCHEDULE_CREATOR");
        ObjectMapper mapper = new ObjectMapper();

        var initialPayload = mapper.convertValue((Map.of(
            "type", "LINEHAUL",
            "status", "ACTIVE",
            "routeId", routeId,
            "pointParams", List.of(
                Map.of(
                    "index", 0,
                    "arrivalStartTime", "01:00:00",
                    "arrivalEndTime", "02:00:00",
                    "transitionTime", 0
                ),
                Map.of(
                    "index", 1,
                    "arrivalStartTime", "02:00:00",
                    "arrivalEndTime", "03:00:00",
                    "transitionTime", 0
                )
            ),
            "scheduleInfo", Map.of(
                "days", List.of(1, 2, 3, 4, 5, 6, 7),
                "startDate", startDate.format(DateTimeFormatter.ISO_DATE),
                "endDate", endDate.format(DateTimeFormatter.ISO_DATE),
                "holidays", List.of()
            ),
            "transportInfo", Map.of(
                "price", 100500,
                "movingPartnerId", 223463,
                "numberOfPallets", 33
            )
        )), JsonNode.class);
        dto.setInitialPayload(initialPayload);

        return Retrier.retry(() -> TSUP.createPipeline(dto));
    }

    @Step("Выключение расписания с даты")
    public void turnOffSchedule(long scheduleId, String fromDate) {
        log.debug("Turning off schedule");
        TSUP.turnOffSchedule(scheduleId, fromDate);
    }

    @Step("Проверяем отсутствие расписаний у маршрута")
    public void verifyRouteHasNoSchedules(long routeId) {
        Retrier.retry(() -> Assertions.assertEquals(
            0, TSUP.getSchedulesByRoute(routeId).getRouteSchedules().size(), "У маршрута есть расписания"
        ));
    }

    @Step("Удаляем маршрут между двумя точками")
    public void deleteRoute(long id) {
        log.debug("Deleting route");
        Retrier.retry(() -> TSUP.deleteRoute(id));
    }

    public boolean isAnyRootBetweenTwoPartners(Long startPartnerId, Long endPartnerId) {
        log.debug("Checking if route between two partners exists");
        return !TSUP.getRoutes(startPartnerId, endPartnerId).getData().isEmpty();
    }

    @Step("Проверяем отсутствие маршрута между двумя партнёрами")
    public void verifyRouteBetweenPartnersNotExists(Long startPartnerId, Long endPartnerId) {
        Retrier.retry(() -> Assertions.assertFalse(isAnyRootBetweenTwoPartners(startPartnerId, endPartnerId)));
    }

    @Step("Проверяем наличие рейса в списке")
    public void verifyRunInList(long runId) {
        Retrier.retry(() -> {
            RunsWithAddresses runsForToday = TSUP.getRuns(
                EXPECTED_RUN_START_POINT,
                EXPECTED_RUN_END_POINT,
                LocalDate.now(),
                LocalDate.now(),
                null
            );
            Assertions.assertNotNull(runsForToday.getData(), "Нет рейсов между партнёрами на сегодня");
            Assertions.assertTrue(runsForToday.getData()
                .stream()
                .anyMatch(runResponse -> runResponse.getId() == runId), "Рейс отсутствует в списке");
        });
    }

    @Step("Проверяем цвет светофора рейса")
    public void verifyRunStatusColorCode(long runId, StatusColorCode expectedStatusColorCode) {
        Retrier.retry(() -> Assertions.assertEquals(
            expectedStatusColorCode,
            TSUP.getRunById(runId).getRun().getStatusColorCode(),
            "Неправильный цвет светофора, ожидаем " + expectedStatusColorCode.toString()
        ));
    }

    @Step("Проверяем статус мувмента рейса")
    public void verifyRunMovementState(long runId, RunStatus expectedMovementState) {
        Retrier.retry(() -> Assertions.assertEquals(
            expectedMovementState,
            TSUP.getRunById(runId).getRun().getMovementState(),
            "Неправильный статус мувмента, ожидаем " + expectedMovementState.toString()));
    }

    @Step("Проверяем параметры MovingCourier")
    public void verifyRunMovingCourier(long runId, Long expectedId, String expectedSurname, String expectedCarNumber) {
        Retrier.retry(() -> {
            Assertions.assertEquals(
                expectedId,
                TSUP.getRunById(runId).getRun().getMovingCourier().getId(),
                "Неправильный id у MovingCourier, ожидаем " + expectedId);
            Assertions.assertEquals(
                expectedSurname,
                TSUP.getRunById(runId).getRun().getMovingCourier().getSurname(),
                "Неправильная фамилия у MovingCourier, ожидаем " + expectedSurname);
            Assertions.assertEquals(
                expectedCarNumber,
                TSUP.getRunById(runId).getRun().getMovingCourier().getCarNumber(),
                "Неправильный номер авто у MovingCourier, ожидаем " + expectedCarNumber);
        });
    }

}

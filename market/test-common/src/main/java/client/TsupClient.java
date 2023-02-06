package client;

import java.time.LocalDate;

import api.TsupApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.tsup.controller.dto.NewPipelineDto;
import ru.yandex.market.tsup.service.data_provider.entity.route.dto.RouteShortcutsResponse;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleListDto;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunWithAddress;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunsWithAddresses;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/tsup.properties")
public class TsupClient {

    private final TsupApi tsupApi;

    @Property("tsup.host")
    private String host;

    public TsupClient() {
        PropertyLoader.newInstance().populate(this);
        tsupApi = RETROFIT.getRetrofit(host).create(TsupApi.class);
    }

    @SneakyThrows
    public RunsWithAddresses getRuns(
        Long fromLogisticPointId,
        Long toLogisticPointId,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        TransportationType transportationType
    ) {
        log.debug("Getting runs applying params...");
        Response<RunsWithAddresses> execute = tsupApi.getRuns(
            fromLogisticPointId,
            toLogisticPointId,
            startDateFrom,
            startDateTo,
            transportationType,
            500
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить рейсы");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении рейсов");
        return execute.body();
    }

    @SneakyThrows
    public Long createPipeline(NewPipelineDto dto) {
        log.debug("Creating tsup pipeline...");
        Response<Long> execute = tsupApi.createPipeline(dto).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать пайплайн");
        return execute.body();
    }

    @SneakyThrows
    public RouteShortcutsResponse getRoutes(Long startPartnerId, Long endPartnerId) {
        log.debug("Getting routes ...");
        Response<RouteShortcutsResponse> execute = tsupApi.getRoutes(startPartnerId, endPartnerId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить маршруты");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении маршрутов");
        return execute.body();
    }

    @SneakyThrows
    public void deleteRoute(long id) {
        log.debug("Deleting route...");
        Response<ResponseBody> execute = tsupApi.deleteRoute(id).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос удаления маршрута");
    }

    @SneakyThrows
    public RouteScheduleListDto getSchedulesByRoute(long routeId) {
        log.debug("Getting schedules ...");
        Response<RouteScheduleListDto> execute = tsupApi.searchSchedulesByRouteId(routeId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить расписания маршрута");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении расписаний маршрута");
        return execute.body();
    }

    @SneakyThrows
    public void turnOffSchedule(long scheduleId, String fromDate) {
        log.debug("Turning off schedules ...");
        Response<ResponseBody> execute = tsupApi.turnOffScheduled(scheduleId, fromDate).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выключить расписание");
    }

    @SneakyThrows
    public RunWithAddress getRunById(long runId) {
        log.debug("Getting run by id...");
        Response<RunWithAddress> execute = tsupApi.getRunById(runId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить информацию о рейсе");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении информации о рейсе");
        return execute.body();
    }

}

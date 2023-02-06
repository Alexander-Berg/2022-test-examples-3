package client;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.Date;
import java.util.List;

import api.TrackerApi;
import dto.responses.tracker.TracksResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/deliverytracker.properties")
public class TrackerClient {

    private final TrackerApi trackerApi;

    @Property("deliverytracker.host")
    private String host;

    public TrackerClient() {
        PropertyLoader.newInstance().populate(this);
        trackerApi = RETROFIT.getRetrofit(host).create(TrackerApi.class);
    }

    @SneakyThrows
    public void addCheckpoint(
        long trackerId,
        int deliveryCheckpointStatus,
        LocalDate localDate,
        EntityType entityType
    ) {
        log.debug("Adding checkpoint to Tracker...");

        DeliveryTrackCheckpoint deliveryTrackCheckpoint = new DeliveryTrackCheckpoint(
            trackerId,
            Date.from(localDate.atTime(OffsetTime.now()).toInstant()),
            DeliveryCheckpointStatus.findByIdAndEntityType(deliveryCheckpointStatus, entityType),
            SurveyType.MANUAL
        );
        deliveryTrackCheckpoint.setEntityType(entityType);
        Response<ResponseBody> execute = trackerApi.addCheckpoint(
            TVM.INSTANCE.getServiceTicket(TVM.TRACKER),
            trackerId,
            deliveryTrackCheckpoint
        ).execute();
        Assertions.assertEquals(201, execute.code(), "Не добавился чекпоинт по trackId " + trackerId);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SneakyThrows
    public DeliveryTrackMeta registerTrack(
        String trackCode,
        long deliveryServiceId,
        long consumerId,
        String entityId,
        LocalDate estimatedArrivalDateFrom,
        LocalDate estimatedArrivalDateTo,
        DeliveryType deliveryType,
        boolean isGlobalOrder,
        EntityType entityType
    ) {
        DeliveryTrackRequest deliveryTrackRequest = DeliveryTrackRequest.builder()
            .trackCode(trackCode)
            .deliveryServiceId(deliveryServiceId)
            .consumerId(consumerId)
            .entityId(entityId)
            .estimatedArrivalDateFrom(estimatedArrivalDateFrom)
            .estimatedArrivalDateTo(estimatedArrivalDateTo)
            .deliveryType(deliveryType)
            .isGlobalOrder(isGlobalOrder)
            .entityType(entityType)
            .build();
        Response<DeliveryTrackMeta> execute = trackerApi.registerTrack(
            TVM.INSTANCE.getServiceTicket(TVM.TRACKER),
            deliveryTrackRequest
        )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос регистрации трека");
        Assertions.assertNotNull(execute.body(), "Пустой ответ после регистрации трека");
        return execute.body();
    }

    /**
     * Получаем id в трекере по entityId. Можно сделать аналогично по trackCode
     **/
    @SneakyThrows
    public TracksResponse getTracksByEntityId(String entityId, Integer entityType) {
        log.debug("Getting tracks information...");
        Response<TracksResponse> execute = trackerApi.getTracksByEntityId(
            TVM.INSTANCE.getServiceTicket(TVM.TRACKER),
            entityId,
            entityType
        )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос получения треков с entityId = " + entityId);
        Assertions.assertNotNull(execute.body(), "Не удалось получить треки с entityId = " + entityId);
        return execute.body();
    }

    @SneakyThrows
    public TracksResponse getTracksByEntityId(String entityId) {
        return getTracksByEntityId(entityId, null);
    }

    @SneakyThrows
    public DeliveryTrack getTrackById(long trackId) {
        log.debug("Getting track information...");
        Response<DeliveryTrack> execute = trackerApi.getTrackById(
            TVM.INSTANCE.getServiceTicket(TVM.TRACKER),
            trackId
        )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос получения трека с id = " + trackId);
        Assertions.assertNotNull(execute.body(), "Не удалось получить трек с id = " + trackId);
        return execute.body();
    }

    @SneakyThrows
    public void instantRequest(List<String> orders) {
        log.debug("Set request time as now()...");
        Response<ResponseBody> execute = trackerApi.instantRequest(
            TVM.INSTANCE.getServiceTicket(TVM.TRACKER),
            orders
        )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос смены времени обработки трека по заказу");
        Assertions.assertNotNull(execute.body(), "Не сменить время обработки трека по заказу ");

    }
}

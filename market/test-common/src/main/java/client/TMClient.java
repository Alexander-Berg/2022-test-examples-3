package client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import api.TmApi;
import dto.requests.tm.GetTransportationsByTagRequest;
import dto.requests.tm.RefreshTransportationRequest;
import dto.responses.tm.admin.movement.TmAdminMovementResponse;
import dto.responses.tm.admin.register_unit.RegisterUnitResponse;
import dto.responses.tm.admin.search.ItemsItem;
import dto.responses.tm.admin.search.TmAdminSearchResponse;
import dto.responses.tm.admin.status_history.TmAdminStatusHistoryResponse;
import dto.responses.tm.admin.task.TmAdminTaskResponse;
import dto.responses.tm.admin.transportation.TmTransportationResponse;
import dto.responses.tm.register.TmRegisterSearchResponse;
import dto.responses.tm.transportation_unit.TransportationUnitResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.domain.Pageable;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.dto.trip.TripShortcutDto;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/tm.properties")
public class TMClient {

    private final TmApi tmApi;

    @Property("tm.host")
    private String host;

    public TMClient() {
        PropertyLoader.newInstance().populate(this);
        tmApi = RETROFIT.getRetrofit(host).create(TmApi.class);
    }

    @SneakyThrows
    public TmAdminSearchResponse getTransportationForDay(
        long outboundPartnerId,
        long inboundPartnerId,
        LocalDate planned,
        TransportationStatus status
    ) {
        log.debug("Getting transportationId for selected day...");
        Response<TmAdminSearchResponse> execute = tmApi.getTransportationForDay(
            outboundPartnerId,
            inboundPartnerId,
            planned,
            status
        ).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось получить перемещение между двумя партнёрами на выбранный день"
        );
        Assertions.assertNotNull(execute.body(), "Пустой ответ на получение перемещения");
        return execute.body();
    }

    @SneakyThrows
    public TmAdminSearchResponse getTransportationById(long transportationId) {
        log.debug("Getting if transportation is active...");
        Response<TmAdminSearchResponse> execute = tmApi.getTransportationById(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить перемещение по id " + transportationId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ на получение перемещения c id " + transportationId);
        return execute.body();
    }

    @SneakyThrows
    public void refreshTransportation() {
        log.debug("Refresh transportation by config...");
        Response<ResponseBody> execute = tmApi.refreshTransportation(
            new RefreshTransportationRequest("refreshTransportationsByConfig"))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось обновить перемещения по конфигу в YT");
    }

    /**
     * Создаём задачу на перемещение между двумя логточками с заданным ssku, поставщиком и количеством единиц
     * Ограничение: можно создавать задачу только с одной товарной номенклатурой.
     **/
    @SneakyThrows
    public Long createTransportationTask(
        long logisticPointFrom,
        long logisticPointTo,
        String ssku,
        String supplierId,
        String realSupplierId,
        int count
    ) {
        log.debug("Creating transportation task...");
        Response<Long> execute = tmApi.createTransportationTask(Map.of(
            "logisticPointFrom", logisticPointFrom,
            "logisticPointTo", logisticPointTo,
            "register", List.of(
                Map.of(
                    "ssku", ssku,
                    "supplierId", supplierId,
                    "realSupplierId", realSupplierId,
                    "count", count
                )
            ),
            "externalId", UUID.randomUUID().getLeastSignificantBits()
            )
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать задачу на перемещение");
        return execute.body();
    }

    @SneakyThrows
    public TmTransportationResponse getTransportation(long transportationId) {
        log.debug("Getting transportation information...");
        Response<TmTransportationResponse> execute = tmApi.getTransportation(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить перемещение с id = " + transportationId);
        Assertions.assertNotNull(execute.body(), "Пустое перемещение с id = " + transportationId);
        return execute.body();
    }

    @SneakyThrows
    public TmAdminTaskResponse getTransportationTask(long transportationTaskId) {
        log.debug("Getting transportation task information...");
        Response<TmAdminTaskResponse> execute = tmApi.getTransportationTask(transportationTaskId).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось получить задачу на перемещение с id = " + transportationTaskId
        );
        Assertions.assertNotNull(execute.body(), "Пустая задача на перемещение с id = " + transportationTaskId);
        return execute.body();
    }

    @SneakyThrows
    public TmAdminSearchResponse getTransportationForTask(long transportationTaskId) {
        log.debug("Getting transportation for transportation task...");
        Response<TmAdminSearchResponse> execute = tmApi.getTransportationForTask(transportationTaskId).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось получить перемещение для задачи " + transportationTaskId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустой ответ на получение перемещения для задачи " + transportationTaskId
        );
        return execute.body();
    }

    @SneakyThrows
    public TmAdminSearchResponse getTransportationRegister(long transportationId) {
        log.debug("Getting plan register for transportation...");
        Response<TmAdminSearchResponse> execute = tmApi.getTransportationRegister(transportationId).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось получить плановый реестр перемещения по id " + transportationId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустой ответ на получение плановый реестр перемещения c id " + transportationId
        );
        return execute.body();
    }

    /**
     * Этим методом удобно получать количество товаров и паллет
     **/
    @SneakyThrows
    public TmRegisterSearchResponse getRegister(long registerId) {
        log.info("Getting information from register...");
        Response<TmRegisterSearchResponse> execute = tmApi.getRegister(Map.of(
            "registerId", registerId
        )).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос получения реестра по id = " + registerId);
        Assertions.assertNotNull(execute.body(), "Пустое тело получения реестра по id = " + registerId);
        return execute.body();
    }

    @SneakyThrows
    public void startTransportation(Long transportationId) {
        log.debug("Start particular transportation...");
        Response<ResponseBody> execute = tmApi.startTransportation(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось стартовать перемещение");
    }

    @SneakyThrows
    public TmAdminMovementResponse getTransportationOutbound(Long transportationId) {
        log.debug("Getting outbound id for transportation...");
        Response<TmAdminMovementResponse> execute = tmApi.getTransportationOutbound(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить отгрузки перемещения " + transportationId);
        Assertions.assertNotNull(execute.body(), "Пустое тело отгрузки перемещения " + transportationId);
        return execute.body();
    }

    @SneakyThrows
    public TmAdminMovementResponse getTransportationInbound(Long transportationId) {
        log.debug("Getting inbound id for transportation...");
        Response<TmAdminMovementResponse> execute = tmApi.getTransportationInbound(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить приёмки перемещения " + transportationId);
        Assertions.assertNotNull(execute.body(), "Пустое тело приёмки перемещения " + transportationId);
        return execute.body();
    }

    @SneakyThrows
    public TmAdminMovementResponse getTransportationMovement(Long transportationId) {
        log.debug("Getting movement id for transportation...");
        Response<TmAdminMovementResponse> execute = tmApi.getTransportationMovement(transportationId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить мувменты перемещения " + transportationId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ у мувментов для перемещения " + transportationId);
        return execute.body();
    }

    @SneakyThrows
    public TransportationUnitResponse getTransportationUnit(Long transportationUnitId) {
        log.debug("Getting transportation unit (outbound or inbound)...");
        Response<TransportationUnitResponse> execute = tmApi.getTransportationUnit(transportationUnitId).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось получить transportation unit " + transportationUnitId
        );
        Assertions.assertNotNull(execute.body(), "Пустой ответ у мувментов для перемещения " + transportationUnitId);
        return execute.body();
    }

    /**
     * Метод получения истории статусов в ТМ (задачи на перемещение, перемещения, отгрузки, приёмки, movement'a
     * Для задачи на перемещения нужен её id; для всего остального id соответствующего перемещения
     **/
    @SneakyThrows
    public TmAdminStatusHistoryResponse getStatusHistory(String entityType, long id) {
        log.info("Getting {} history with id {}...", entityType, id);
        Response<TmAdminStatusHistoryResponse> execute = tmApi.getStatusHistory(entityType, id).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить историю статусов id = " + id);
        Assertions.assertNotNull(execute.body(), "Пустой ответ у истории статусов id = " + id);
        return execute.body();
    }

    /**
     * Этим методом удобно получать заказы в реестре, зовёт метод контроллера админки
     **/
    @SneakyThrows
    public RegisterUnitResponse getRegisterUnits(long registerId) {
        log.debug("Getting units for plan register...");
        Response<RegisterUnitResponse> execute = tmApi.getRegisterUnits(registerId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить заказы в реестре с id = " + registerId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ у заказы в реестре с id = " + registerId);
        return execute.body();
    }

    @SneakyThrows
    public List<ItemsItem> getTransportationsByTag(String tagCode, String value) {
        log.debug("Searching transportations by tag...");
        Response<List<ItemsItem>> execute = tmApi.getTransportationsByTag(
            new GetTransportationsByTagRequest(tagCode, value)).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить перемещения по тегу = " + value);
        Assertions.assertNotNull(execute.body(), "Пустой ответ в запросе перемещений с тегом " + value);
        return execute.body();
    }

    @SneakyThrows
    public MovementDto getMovement(long movementId) {
        log.debug("Getting movement...");
        Response<MovementDto> execute = tmApi.getMovement(movementId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить dto movement'a " + movementId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ в запросе movement'a " + movementId);
        return execute.body();
    }

    @SneakyThrows
    public List<TripShortcutDto> getTripSearch(Long transportationIds, Long tripId) {
        log.debug("Searching trips & transportations...");
        Response<List<TripShortcutDto>> execute = tmApi.getTripSearch(transportationIds, tripId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить перемещения и рейсы");
        Assertions.assertNotNull(execute.body(), "Пустой ответ в запросе перемещений и рейсов");
        return execute.body();
    }

    @SneakyThrows
    public PageResult<TransportationSearchDto> searchTransportations(
        TransportationSearchFilter filter,
        Pageable pageable
    ) {
        log.debug("Searching transportations by filter {}", filter);
        Response<PageResult<TransportationSearchDto>> execute = tmApi.searchTransportations(filter)
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить перемещения");
        Assertions.assertNotNull(execute.body(), "Пустой ответ в запросе перемещений");
        return execute.body();
    }
}

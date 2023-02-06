package client;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import api.ScIntApi;
import dto.responses.scapi.orders.ApiOrderDto;
import dto.responses.scapi.orders.AvailableCellsItem;
import dto.responses.scint.manualcells.ScCell;
import dto.responses.scint.manualcells.ScRoute;
import dto.responses.scintmanualcells.GetCellsResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.common.util.collections.CollectionUtils;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/scint.properties")
public class ScIntClient {

    private static final String TOKEN = "OAuth " + System.getenv("PARTNER_2000_TOKEN");
    private final ScIntApi scIntApi;
    @Property("scint.host")
    private String host;

    public ScIntClient() {
        PropertyLoader.newInstance().populate(this);
        scIntApi = RETROFIT.getRetrofit(host).create(ScIntApi.class);
    }

    @SneakyThrows
    public GetCellsResponse getCellsForZone(Long scId, Long zoneId) {
        log.debug("Getting cells for zone in sorting center...");
        Response<GetCellsResponse> execute = scIntApi.getCellsForZone(scId, zoneId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось найти ячейки в зоне " + zoneId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ при запросе ячеек в зоне " + zoneId);
        Assertions.assertNotNull(execute.body().getCells().get(0).getId(), "Нет ячеек в зоне " + zoneId);
        return execute.body();
    }

    @SneakyThrows
    public List<Long> getCellIds(Long scId) {
        log.debug("Getting cells for sorting center...");
        Response<List<ScRoute>> execute = scIntApi.getRoutes(scId, "OUTGOING_WAREHOUSE", TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось найти маршруты в СЦ " + scId);
        return execute.body().stream()
            .flatMap(route -> getCells(scId, route.getId()).stream())
            .filter(Objects::nonNull)
            .map(ScCell::getId)
            .collect(Collectors.toList());
    }

    @SneakyThrows
    private List<ScCell> getCells(Long scId, Long routeId) {
        Response<ScRoute> execute = scIntApi.getRouteInfo(routeId, scId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось найти маршрут в СЦ " + scId);
        return CollectionUtils.emptyIfNull(execute.body().getCells());
    }

    @SneakyThrows
    public List<Long> getAvailableCellsForOrder(Long scId, String externalId) {
        Response<ApiOrderDto> execute = scIntApi.getOrder(scId, externalId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось найти заказ в СЦ " + scId);
        return execute.body().getAvailableCells().stream().map(AvailableCellsItem::getId).toList();
    }

    @SneakyThrows
    public void clearCell(Long cellId) {
        log.debug("Clearing cell in sorting center...");
        Response<ResponseBody> execute = scIntApi.clearCell(cellId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось очистить ячейку " + cellId);
    }

    @SneakyThrows
    public void acceptAndSortOrder(String orderId, String placeId, Long sortingCenterId, List<Long> cellIds) {
        log.debug("Accept and sort order in sorting center...");
        Assertions.assertTrue(CollectionUtils.isNonEmpty(cellIds), "Пустой список ячеек на СЦ " + sortingCenterId);
        Response<ResponseBody> execute = null;
        for (Long cellId : cellIds) {
            execute = scIntApi.acceptAndSortOrder(
                    orderId,
                    placeId,
                    sortingCenterId,
                    cellId,
                    TOKEN
                )
                .execute();
            if (execute.isSuccessful()) {
                return;
            }
        }
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось принять и отсортировать заказ на СЦ " + sortingCenterId
        );
    }

    @SneakyThrows
    public void sortOrder(String orderId, String placeId, Long sortingCenterId, List<Long> cellIds) {
        log.debug("Sort order in sorting center");
        Assertions.assertTrue(CollectionUtils.isNonEmpty(cellIds), "Пустой список ячеек на СЦ " + sortingCenterId);
        Response<ResponseBody> execute = null;
        for (Long cellId : cellIds) {
            execute = scIntApi.sortOrder(
                    orderId,
                    placeId,
                    sortingCenterId,
                    cellId,
                    TOKEN
                )
                .execute();
            if (execute.isSuccessful()) {
                return;
            }
        }
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось отсортировать заказ на СЦ " + sortingCenterId
        );
    }

    @SneakyThrows
    public void shipOrder(String orderId, String placeId, Long sortingCenterId) {
        log.debug("Ship order in sorting center...");
        Response<ResponseBody> execute = scIntApi.shipOrder(
                orderId,
                placeId,
                sortingCenterId,
                TOKEN
            )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось отгрузить заказ на СЦ " + sortingCenterId);
    }

    @SneakyThrows
    public void sendSegmentFfStatusHistoryToSqs() {
        log.debug("Send SC Segment Status History To Sqs...");
        Response<ResponseBody> execute = scIntApi.sendSegmentFfStatusHistoryToSqs(TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось отправить статусы СЦ в LES");
    }

    @SneakyThrows
    public void performInboundAction(Long scId, String inboundId, String action) {
        log.debug("Performing action to inbound...");
        Response<ResponseBody> execute = scIntApi.performInboundAction(scId, inboundId, action, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить действие " + action + " с поставкой");
    }

    @SneakyThrows
    public void carArrived(Long scId, String inboundId) {
        log.debug("Adding car and driver data to inbound...");
        Response<ResponseBody> execute = scIntApi.carArrived(
            scId,
            inboundId,
            Map.of(
                "fullName", "ХардкодноеИмя ВодителяИзАвтотестов",
                "phoneNumber", "+70000000000",
                "carNumber", "М111ММ76",
                "carBrand", "Тесла",
                "trailerNumber", "",
                "comment", "какой-то комментарий из автотестов"
            ),
            TOKEN
        ).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось добавить данные авто и водителя поставке " + inboundId
        );
    }

    @SneakyThrows
    public void acceptOrder(String externalOrderId, String externalPlaceId, Long scId) {
        log.debug("Accept order...");
        Response<ResponseBody> execute = scIntApi.acceptOrder(externalOrderId, externalPlaceId, scId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось принять заказ на СЦ");
    }

    @SneakyThrows
    public void fixInbound(String inboundExternalId) {
        log.debug("Closing inbound in sorting center...");
        Response<ResponseBody> execute = scIntApi.fixInbound(inboundExternalId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось завершить поставку с id " + inboundExternalId);
    }
}

package client;

import java.util.Map;
import java.util.UUID;

import api.ScApi;
import dto.requests.scapi.ApiSortableSortRequest;
import dto.requests.scapi.InboundsLinkRequest;
import dto.responses.inbounds.ScInboundsAcceptResponse;
import dto.responses.scapi.orders.ApiOrderDto;
import dto.responses.scapi.sortable.SortableSort;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/scapi.properties")
public class ScApiClient {

    private static final String TOKEN = "OAuth " + System.getenv("PARTNER_2000_TOKEN");
    private final ScApi scApi;
    @Property("scapi.host")
    private String host;

    public ScApiClient() {
        PropertyLoader.newInstance().populate(this);
        scApi = RETROFIT.getRetrofit(host).create(ScApi.class);
    }

    @SneakyThrows
    public ScInboundsAcceptResponse inboundsAccept(String inboundYandexId) {
        log.debug("Accepting inbound in sorting center...");
        Response<ScInboundsAcceptResponse> execute = scApi.inboundsAccept(inboundYandexId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось принять в сц поставку с id " + inboundYandexId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ при принятии в сц поставки с id " + inboundYandexId);
        return execute.body();
    }

    @SneakyThrows
    public String xDocInboundsLink(String inboundId) {
        log.debug("Linking inbound to pallet...");
        String sortableId = "XDOC-" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
        InboundsLinkRequest inboundsLinkRequest = new InboundsLinkRequest(sortableId, "XDOC_PALLET");
        Response<ResponseBody> execute = scApi.inboundsLink(inboundId, inboundsLinkRequest, TOKEN).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось связать в сц поставку с id " + inboundId + " с паллетой"
        );
        return sortableId;
    }

    @SneakyThrows
    public ApiOrderDto ordersAccept(String sortableId) {
        log.debug("Accepting sorting center order...");
        Response<ApiOrderDto> execute = scApi.ordersAccept(Map.of("externalId", sortableId), TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось принять в сц заказ с sortableId " + sortableId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ при принятии в сц заказа с sortableId " + sortableId);
        Assertions.assertNotNull(
            execute.body().getId(),
            "Пустой id в ответе при принятии в сц заказа с sortableId " + sortableId
        );
        Assertions.assertNotNull(execute.body().getAvailableCells(), "Нет зон заказа с sortableId " + sortableId);
        Assertions.assertNotNull(
            execute.body().getAvailableCells().get(0).getId(),
            "Нет зон для заказа с sortableId " + sortableId
        );
        return execute.body();
    }

    @SneakyThrows
    public ApiOrderDto sortOrder(Long orderId, Long cellId) {
        log.debug("Sorting order in sorting center...");
        Response<ApiOrderDto> execute = scApi.sortOrder(orderId, Map.of("cellId", cellId), TOKEN).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Не удалось отсортировать заказ " + orderId + " в ячейку " + cellId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустой ответ при сортировке заказа " + orderId + " в ячейку " + cellId
        );
        Assertions.assertEquals(execute.body().getStatus(), "KEEP", "Статус заказа не соответствует ожидаемому");
        return execute.body();
    }

    @SneakyThrows
    public void fixInbound(String inboundId) {
        log.debug("Closing inbound in sorting center...");
        Response<ResponseBody> execute = scApi.fixInbound(inboundId, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось завершить поставку с id " + inboundId);
    }

    @SneakyThrows
    public SortableSort sortableSort(ApiSortableSortRequest sortableSortRequest) {
        log.debug("Sorting order in sorting center using /api/sortable/beta/sort");
        Response<SortableSort> execute = scApi.sortableSort(sortableSortRequest, TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось отсортировать в ячейку");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при сортировке в ячейку");
        return execute.body();
    }

}

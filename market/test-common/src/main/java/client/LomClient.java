package client;

import java.util.Set;

import api.LomApi;
import dto.responses.lom.admin.business_process.BusinessProcessesResponse;
import dto.responses.lom.admin.order.AdminLomOrderResponse;
import dto.responses.lom.admin.order.OrdersResponse;
import dto.responses.lom.admin.order.route.RouteResponse;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistics.lom.model.dto.IdDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/lom.properties")
public class LomClient {

    private final LomApi lomApi;
    @Property("lom.host")
    private String host;

    public LomClient() {
        PropertyLoader.newInstance().populate(this);
        lomApi = RETROFIT.getRetrofit(host).create(LomApi.class);
    }

    @SneakyThrows
    public OrderDto createOrder(WaybillOrderRequestDto orderDto) {
        Response<OrderDto> bodyResponse = lomApi.createOrder(orderDto).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос создания заказа в ломе неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа создания заказа в ломе");
        return bodyResponse.body();
    }

    @SneakyThrows
    public OrdersResponse orderSearch(String externalId) {
        return orderSearch(OrderSearchFilter.builder().externalIds(Set.of(externalId)).build());
    }

    @SneakyThrows
    public OrdersResponse orderSearch(OrderSearchFilter filter) {
        Response<OrdersResponse> bodyResponse = lomApi.orderSearch(filter).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения заказа из лома неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения заказа из лома");
        return bodyResponse.body();
    }

    @SneakyThrows
    public OrderDto getOrder(Long orderId) {
        return getOrder(orderId, null);
    }

    @SneakyThrows
    public OrderDto getOrder(Long orderId, Set<OptionalOrderPart> optionalParts) {
        Response<OrderDto> bodyResponse = lomApi.getOrder(orderId, optionalParts).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения заказа из лома неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения заказа из лома");
        return bodyResponse.body();
    }

    @SneakyThrows
    public AdminLomOrderResponse getAdminLomOrder(Long orderId) {
        Response<AdminLomOrderResponse> bodyResponse = lomApi.getAdminLomOrder(orderId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения заказа из админки лома неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения заказа из админки лома");
        return bodyResponse.body();
    }

    @SneakyThrows
    public RouteResponse getAdminLomRoute(Long routeId) {
        Response<RouteResponse> bodyResponse = lomApi.getAdminLomRoute(routeId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения роутов из админки лома неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения роутов админки из лома");
        return bodyResponse.body();
    }

    @SneakyThrows
    public BusinessProcessesResponse getAdminBusinessProcesses(Long orderId) {
        Response<BusinessProcessesResponse> bodyResponse = lomApi.getAdminBusinessProcesses(orderId).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения фоновых бизнесс процессов из админки лома неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа получения фоновых бизнесс процессов из админки из лома"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public void retryBusinessProcess(Long processId) {
        IdDto idDto = new IdDto();
        idDto.setId(processId);
        Response<ResponseBody> bodyResponse = lomApi.retryBusinessProcess(idDto).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос перевыставления процесса " + processId + " из админки лома неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа перевыставления процесса " + processId + " из  админки из лома"
        );
    }
}

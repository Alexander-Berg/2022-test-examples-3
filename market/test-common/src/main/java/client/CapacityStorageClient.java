package client;

import java.util.List;

import api.CapacityStorageApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistics.cs.domain.dto.InternalEventDto;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/capacityStorage.properties")
public class CapacityStorageClient {

    private final CapacityStorageApi capacityStorageApi;

    @Property("cs.host")
    private String host;

    public CapacityStorageClient() {
        PropertyLoader.newInstance().populate(this);
        capacityStorageApi = RETROFIT.getRetrofit(host).create(CapacityStorageApi.class);
    }

    @SneakyThrows
    public void snapshot() {
        Response<ResponseBody> execute = capacityStorageApi.snapshot().execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос вызова snapshot у cs");
    }

    @SneakyThrows
    public List<InternalEventDto> getOrderEvents(Long orderId) {
        log.debug("Calling CS get order events...");
        Response<List<InternalEventDto>> execute = capacityStorageApi.getOrderEvents(
            TVM.INSTANCE.getServiceTicket(TVM.CS),
            orderId
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос к order events");
        Assertions.assertNotNull(execute.body(), "Пустой ответ от order events");
        return execute.body();
    }
}

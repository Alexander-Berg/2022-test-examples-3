package client;

import java.time.LocalDate;

import api.WmsApi;
import dto.requests.wms.ChangeShipmentDateRequest;
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
@Resource.Classpath({"delivery/wms.properties"})
public class WmsClient {

    private final WmsApi wmsApi;
    @Property("wms.host")
    private String host;

    public WmsClient() {
        PropertyLoader.newInstance().populate(this);
        wmsApi = RETROFIT.getRetrofit(host).create(WmsApi.class);
    }

    @SneakyThrows
    public void changeShipmentDate(String orderKey, LocalDate shipDate) {
        Response<ResponseBody> execute = wmsApi.changeShipmentDate(new ChangeShipmentDateRequest(orderKey, shipDate))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос смены даты отгрузки, orderKey " + orderKey);
        Assertions.assertNotNull(execute.body(), "Не удалось сменить дату отгрузки, orderKey " + orderKey);
    }

    @SneakyThrows
    public void calculateAndUpdateOrdersStatus() {
        Response<ResponseBody> execute = wmsApi.calculateAndUpdateOrdersStatus().execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос вызова шедулера на складе");
    }
}

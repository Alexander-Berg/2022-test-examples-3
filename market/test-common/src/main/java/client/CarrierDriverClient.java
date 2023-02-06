package client;

import java.util.List;

import api.CarrierDriverApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftDto;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/carrierdriver.properties")
public class CarrierDriverClient {
    private static final String TOKEN = "OAuth " + System.getenv("AUTOTEST_DRIVER_TOKEN");

    private final CarrierDriverApi carrierDriverApi;
    @Property("carrierdriver.host")
    private String host;

    public CarrierDriverClient() {
        PropertyLoader.newInstance().populate(this);
        carrierDriverApi = RETROFIT.getRetrofit(host).create(CarrierDriverApi.class);
    }

    @SneakyThrows
    public List<UserShiftDto> getShifts() {
        log.debug("Getting shifts for driver...");
        Response<List<UserShiftDto>> execute = carrierDriverApi.getShifts(TOKEN).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить смены для автотестового водителя ");
        return execute.body();
    }
}

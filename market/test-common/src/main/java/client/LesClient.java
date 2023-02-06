package client;

import api.LesApi;
import dto.requests.les.AddEventDto;
import dto.requests.les.EventDto;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/les.properties")
public class LesClient {

    private final LesApi lesApi;
    @Property("les.host")
    private String host;

    public LesClient() {
        PropertyLoader.newInstance().populate(this);
        lesApi = RETROFIT.getRetrofit(host).create(LesApi.class);
    }

    @SneakyThrows
    public void addEvent(EventDto event, String queue) {
        Response<ResponseBody> execute = lesApi.addEvent(new AddEventDto(event, queue)).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Ошибка отправки события в LES");
    }
}

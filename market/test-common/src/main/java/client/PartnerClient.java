package client;

import java.util.List;

import api.PartnerApi;
import dto.requests.partner.PackOrderRequest;
import dto.requests.partner.PartnerBox;
import dto.requests.partner.PartnerItem;
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
@Resource.Classpath("delivery/partnerapi.properties")
public class PartnerClient {

    private final PartnerApi partnerApi;
    private final long campaignId;
    private final long yandexUid;
    @Property("partnerapi.host")
    private String host;

    public PartnerClient(long yandexUid, long campaignId) {
        PropertyLoader.newInstance().populate(this);
        partnerApi = RETROFIT.getRetrofit(host).create(PartnerApi.class);
        this.campaignId = campaignId;
        this.yandexUid = yandexUid;
    }

    @SneakyThrows
    public void packOrder(long orderId, long parcelId, long itemId) {
        log.debug("Packing order {}...", orderId);

        Response<ResponseBody> execute = partnerApi.packOrder(
            String.format("yandexuid=%s", yandexUid),
            "Mock",
            campaignId,
            orderId,
            parcelId,
            new PackOrderRequest(List.of(new PartnerBox(
                orderId,
                5,
                5,
                5,
                5,
                List.of(new PartnerItem(itemId, 1))
            )))
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось упаковать заказ " + orderId);
    }

}

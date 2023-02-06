package client;

import java.util.List;

import api.TplPvzApi;
import dto.requests.tpl.pvz.ReceiveCreateDto;
import dto.requests.tpl.pvz.ReceiveCreateItemDto;
import dto.requests.tpl.pvz.VerifyCodeDto;
import dto.responses.tpl.pvz.OrderPageDto;
import dto.responses.tpl.pvz.PickupPointRequestData;
import dto.responses.tpl.pvz.PvzOrderDto;
import dto.responses.tpl.pvz.PvzReturnDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT_LMS;

@Resource.Classpath("delivery/pvz.properties")
public class TplPvzClient {
    private final TplPvzApi tplPvzApi;
    @Property("tpl.pvz.host")
    private String host;

    public TplPvzClient() {
        PropertyLoader.newInstance().populate(this);
        tplPvzApi = RETROFIT_LMS.getRetrofit(host).create(TplPvzApi.class);
    }

    @SneakyThrows
    public PvzOrderDto verifyCodeForPvzOrder(String pvzId, String id, String code) {
        Response<PvzOrderDto> bodyResponse = tplPvzApi.verifyCodeForPvzOrder(
                pvzId,
                id,
                new VerifyCodeDto(code)
            )
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос верификации кода заказа ПВЗ неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа верификации кода заказа ПВЗ");
        return bodyResponse.body();
    }

    @SneakyThrows
    public PvzReturnDto receiveReturn(Long pvzId, Long returnId) {
        Response<PvzReturnDto> bodyResponse = tplPvzApi.receiveReturn(pvzId, returnId)
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос приёмки возврата в ПВЗ неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа приёмки возврата в ПВЗ");
        return bodyResponse.body();
    }

    @SneakyThrows
    public PickupPointRequestData receiveOrder(Long pvzId, String itemId) {
        Response<PickupPointRequestData> bodyResponse = tplPvzApi.receiveOrder(
                pvzId,
                "1",
                new ReceiveCreateDto(List.of(new ReceiveCreateItemDto(itemId)))
            )
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос приёмки заказа в ПВЗ неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа приёмки заказа в ПВЗ");
        return bodyResponse.body();
    }

    @SneakyThrows
    public OrderPageDto getOrder(Long pvzId, String externalId) {
        Response<OrderPageDto> bodyResponse = tplPvzApi.getOrder(pvzId, externalId)
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения заказа в ПВЗ неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения заказа в ПВЗ");
        return bodyResponse.body();
    }
}

package client;

import java.util.List;

import api.NesuInternalApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.logistics.nesu.client.model.SenderDto;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.logistics.nesu.client.model.page.Page;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentSearchDto;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("nesu/nesu.properties")
@Slf4j
public class NesuInternalClient {

    private final NesuInternalApi nesuInternalApi;

    @Property("nesu.host")
    private String host;

    public NesuInternalClient() {
        PropertyLoader.newInstance().populate(this);
        nesuInternalApi = RETROFIT.getRetrofit(host).create(NesuInternalApi.class);
    }

    @SneakyThrows
    public void registerShop(RegisterShopDto registerShopDto) {
        Response<ResponseBody> execute = nesuInternalApi.registerShop(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            registerShopDto
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос регистрации магазина = " + registerShopDto);
        Assertions.assertNotNull(execute.body(), "Пустое тело при регистрации магазина " + registerShopDto);
    }

    @SneakyThrows
    public void configureShop(Long shopId, ConfigureShopDto configureShopDto) {
        Response<ResponseBody> execute = nesuInternalApi.configureShop(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            shopId,
            configureShopDto
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос конфигурации магазина = " + configureShopDto);
        Assertions.assertNotNull(execute.body(), "Пустое тело при конфигурации магазина " + configureShopDto);
    }

    @SneakyThrows
    public List<ShopWithSendersDto> searchShopWithSenders(ShopWithSendersFilter filter) {
        Response<List<ShopWithSendersDto>> execute = nesuInternalApi.searchShopWithSenders(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            filter
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос поиска магазинов  " + filter);
        Assertions.assertNotNull(execute.body(), "Пустое тело при поиске магазинов " + filter);
        return execute.body();
    }

    @SneakyThrows
    public List<SenderDto> getSenders() {
        Response<List<SenderDto>> execute = nesuInternalApi.getSenders(
            TVM.INSTANCE.getServiceTicket(TVM.NESU)
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос поиска отправителей");
        Assertions.assertNotNull(execute.body(), "Пустое тело при поиске отправителей");
        return execute.body();
    }

    @SneakyThrows
    public SenderDto getSender(Long senderId) {
        Response<SenderDto> execute = nesuInternalApi.getSender(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            senderId
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос поиска отправителя с id = " + senderId);
        Assertions.assertNotNull(execute.body(), "Пустое тело при поиске отправителя с id = " + senderId);
        return execute.body();
    }

    @SneakyThrows
    public Page<PartnerShipmentSearchDto> searchPartnerShipments(
        Long userId,
        Long shopId,
        PartnerShipmentFilter filter
    ) {
        Response<Page<PartnerShipmentSearchDto>> execute = nesuInternalApi.searchPartnerShipments(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            userId,
            shopId,
            filter
        ).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Неуспешный запрос поиска отправлений с userId = " + userId + " shopId = " + shopId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустое тело при поиске отправлений  с userId = " + userId + " shopId = " + shopId
        );
        return execute.body();
    }

    @SneakyThrows
    public PartnerShipmentDto getShipment(Long shipmentId, Long userId, Long shopId) {
        Response<PartnerShipmentDto> execute = nesuInternalApi.getShipment(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            shipmentId,
            userId,
            shopId
        ).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Неуспешный запрос поиска отправления с userId = " + userId + " shopId = " + shopId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустое тело при поиске отправления  с userId = " + userId + " shopId = " + shopId
        );
        return execute.body();
    }

    @SneakyThrows
    public void confirmShipment(Long shipmentId, Long userId, Long shopId, PartnerShipmentConfirmRequest request) {
        Response<ResponseBody> execute = nesuInternalApi.confirmShipment(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            shipmentId,
            userId,
            shopId,
            request
        ).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Неуспешный запрос подтверждения отправления с shipmentId = " + shipmentId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Пустое тело при подтверждения отправления с shipmentId = " + shipmentId
        );
    }
}

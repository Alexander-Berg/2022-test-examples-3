package step;

import java.util.List;

import client.NesuInternalClient;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

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

public class NesuInternalSteps {

    private static final NesuInternalClient NESU_CLIENT = new NesuInternalClient();

    @Step("Регистрируем магазин")
    public void registerShop(RegisterShopDto registerShopDto) {
        NESU_CLIENT.registerShop(registerShopDto);
    }

    @Step("Конфигурируем магазин")
    public void configureShop(Long shopId, ConfigureShopDto configureShopDto) {
        NESU_CLIENT.configureShop(shopId, configureShopDto);
    }

    @Step("Поиск магазина с отправителями")
    public List<ShopWithSendersDto> searchShopWithSenders(ShopWithSendersFilter filter) {
        return NESU_CLIENT.searchShopWithSenders(filter);
    }

    @Step("Поиск отправителей ")
    public List<SenderDto> getSenders() {
        return NESU_CLIENT.getSenders();
    }

    @Step("Поиск отправителя id= {senderId}")
    public SenderDto getSender(Long senderId) {
        return NESU_CLIENT.getSender(senderId);
    }

    @Step("Поиск отправлений userId = {userId} shopId = {shopId}")
    public Page<PartnerShipmentSearchDto> searchPartnerShipments(
        Long userId,
        Long shopId,
        PartnerShipmentFilter filter
    ) {
        return NESU_CLIENT.searchPartnerShipments(userId, shopId, filter);
    }

    @Step("Поиск отправления shipmentId = {shipmentId}  userId = {userId} shopId = {shopId}")
    public PartnerShipmentDto getShipment(Long shipmentId, Long userId, Long shopId) {
        return NESU_CLIENT.getShipment(shipmentId, userId, shopId);
    }

    @Step("Проверить доступность скачивания акта расхождений")
    public void verifyDiscrepancyActDownloadAvailability(Long shipmentId, Long userId, Long shopId) {
        Retrier.retry(() -> Assertions.assertTrue(getShipment(shipmentId, userId, shopId).getAvailableActions()
            .isDownloadDiscrepancyAct()));
    }

    @Step("Поиск отправления shipmentId = {shipmentId}  userId = {userId} shopId = {shopId}")
    public void confirmShipment(Long shipmentId, Long userId, Long shopId, PartnerShipmentConfirmRequest request) {
        NESU_CLIENT.confirmShipment(shipmentId, userId, shopId, request);
    }
}

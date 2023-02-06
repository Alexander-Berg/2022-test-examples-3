package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.WrapInfor;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OutboundStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

public class OutboundSteps {

    private static final ServiceBus serviceBus = new ServiceBus();

    protected OutboundSteps() {}

    @Step("Создаем изъятие на завтра, годный сток")
    public Outbound createOutbound(Item item) {
        return serviceBus.createOutbound(
                UniqueId.get(),
                List.of(item),
                DateUtil.tomorrowDateTime()
        );
    }

    @Step("Создаем изъятие на завтра, {stock} сток")
    public Outbound createOutbound(Item item, StockType stock) {
        return serviceBus.createOutbound(
                UniqueId.get(),
                List.of(item),
                DateUtil.tomorrowDateTime(),
                stock
        );
    }

    @Step("Создаем изъятие на завтра, годный сток")
    public Outbound createOutbound(List<Item> itemsList) {
        return serviceBus.createOutbound(
                UniqueId.get(),
                itemsList,
                DateUtil.tomorrowDateTime()
        );
    }

    @Step("Проверяем, что статус изъятия: {status}")
    public void verifyOutboundStatus(Outbound outbound, OutboundStatus status) {
        Retrier.retry(() -> serviceBus.getOutboundsStatus(outbound)
                .body("root.response.outboundsStatus.outboundStatus.status.statusCode.toInteger()",
                is(status.getId())),
                Retrier.RETRIES_TINY,
                Retrier.TIMEOUT_TINY,
                TimeUnit.SECONDS
        );
    }

    @Step("Создаем изъятие BBXD")
    public Outbound putOutboundBbxd(long yandexId,
                                    String interval,
                                    long receiptYandexId,
                                    String carrierCode) {
        return serviceBus.putOutboundBbxd(yandexId, interval, receiptYandexId, carrierCode);
    }

    public void putOutboundRegistry(long yandexId,
                                    String fulfillmentId,
                                    String article,
                                    int vendorId) {
        serviceBus.putOutboundRegistry(yandexId, fulfillmentId, DateUtil.currentDateTime(),
                article, vendorId);
    }
}

package ru.yandex.market.logistics.lrm.api.returns;

import javax.annotation.Nonnull;

import ru.yandex.market.logistics.lrm.client.model.CancellationReturnBox;
import ru.yandex.market.logistics.lrm.client.model.CancellationReturnItem;
import ru.yandex.market.logistics.lrm.client.model.Dimensions;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ReturnBoxRequest;
import ru.yandex.market.logistics.lrm.client.model.ReturnItem;

final class ReturnFactory {

    private ReturnFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static ReturnBoxRequest defaultBox() {
        return new ReturnBoxRequest()
            .externalId("box-external-id");
    }

    @Nonnull
    static ReturnItem defaultItem() {
        return new ReturnItem()
            .supplierId(765L)
            .vendorCode("item-vendor-code");
    }

    @Nonnull
    static OrderItemInfo defaultOrderItemInfo() {
        return new OrderItemInfo()
            .vendorCode("item-vendor-code");
    }

    @Nonnull
    static CancellationReturnItem defaultCancellationItem() {
        return new CancellationReturnItem()
            .supplierId(200L)
            .vendorCode("item-vendor-code");
    }

    @Nonnull
    static CancellationReturnBox defaultCancellationBox() {
        return new CancellationReturnBox()
            .externalId("box-external-id")
            .dimensions(defaultDimensions());
    }

    @Nonnull
    static Dimensions defaultDimensions() {
        return new Dimensions()
            .weight(100)
            .length(200)
            .width(300)
            .height(400);
    }
}

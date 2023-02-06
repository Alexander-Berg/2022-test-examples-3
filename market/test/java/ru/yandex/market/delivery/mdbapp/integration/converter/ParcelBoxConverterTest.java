package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ItemPlace;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Korobyte;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Place;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.UnitId;

public class ParcelBoxConverterTest {

    private ParcelBoxConverter parcelBoxConverter = new ParcelBoxConverter();

    @Test
    public void successfulConvert() {
        ParcelBox parcelBox = parcelBoxConverter.convert(null, getPlace());

        Assert.assertEquals("partnerId must be equal to EXTPARTNERID",
            getPlace().getPlaceId().getPartnerId(),
            parcelBox.getFulfilmentId());

        Assert.assertEquals("height must be equal to 1",
            getKorobyte().getHeight().longValue(),
            parcelBox.getHeight().longValue());

        Assert.assertEquals("length must be equal to 2",
            getKorobyte().getLength().longValue(),
            parcelBox.getDepth().longValue());

        Assert.assertEquals("width must be equal to 3",
            getKorobyte().getWidth().longValue(),
            parcelBox.getWidth().longValue());

        Assert.assertEquals("weigth must be equal to 4000 g",
            4000,
            parcelBox.getWeight().longValue());

    }

    @Test
    public void successfulConvertWithItemPlaces() {
        Order order = new Order();

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setShopSku("123");
        item.setSupplierId(1L);
        item.setCount(1);
        order.setItems(Collections.singletonList(item));

        ParcelBox parcelBox = parcelBoxConverter.convert(order, getPlaceWithItems());

        Assert.assertEquals("partnerId must be equal to EXTPARTNERID",
            getPlace().getPlaceId().getPartnerId(),
            parcelBox.getFulfilmentId());

        Assert.assertEquals("items list size",
            1,
            parcelBox.getItems().size());

        Assert.assertEquals("item count",
            1,
            parcelBox.getItems().get(0).getCount());
    }

    @Test
    public void successfulConvertWithZeroValues() {
        ParcelBox parcelBox = parcelBoxConverter.convert(null, getPlaceWithZeroKorobyte());

        Assert.assertEquals("partnerId must be equal to EXTPARTNERID",
            getPlace().getPlaceId().getPartnerId(),
            parcelBox.getFulfilmentId());

        Assert.assertNull("height must be equal to null",
            parcelBox.getHeight());

        Assert.assertNull("length must be equal to null",
            parcelBox.getDepth());

        Assert.assertNull("width must be equal to null",
            parcelBox.getWidth());

        Assert.assertNull("weigth must be equal to null",
            parcelBox.getWeight());

    }

    private Place getPlace() {
        return new Place(new ResourceId("123456", "EXTPARTNERID"), getKorobyte(), null);
    }

    private Place getPlaceWithItems() {
        ItemPlace itemPlace = new ItemPlace(new UnitId(null, 1L, "123"), 1);

        return new Place(
            new ResourceId("123456", "EXTPARTNERID"),
            getKorobyte(),
            Collections.singletonList(itemPlace)
        );
    }

    private Place getPlaceWithZeroKorobyte() {
        return new Place(new ResourceId("123456", "EXTPARTNERID"), getZeroKorobyte(), null);
    }

    private Korobyte getZeroKorobyte() {
        return new Korobyte(0, 0, 0, BigDecimal.valueOf(0), null, null);
    }

    private Korobyte getKorobyte() {
        return new Korobyte(3, 1, 2, BigDecimal.valueOf(4), null, null);
    }
}

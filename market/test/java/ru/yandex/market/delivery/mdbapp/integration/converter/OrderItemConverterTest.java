package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import steps.orderSteps.itemSteps.ItemsSteps;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.CustomsTranslation;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Tax;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaxType;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.VatValue;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class OrderItemConverterTest {

    private SimplifiedOrderItemConverter orderItemConverter;

    @Before
    public void before() {
        orderItemConverter = new RedOrderItemConverter(new TaxConverter(), new KorobyteConverter());
    }

    @Test
    public void convertItemsSingleItem() {
        List<OrderItem> orderItems = getOrderItems();

        List<Item> expectedItems = new ArrayList<Item>() {{
            Item item = new Item.ItemBuilder(
                "offer name",
                3,
                BigDecimal.valueOf(123.0)
            )
                .setArticle("123123")
                .setNameEnglish("english offer name")
                .setCargoType(CargoType.ART)
                .setCargoTypes(Collections.singletonList(CargoType.ART))
                .setCategoryName("Category full name")
                .setHsCode("123456")
                .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
                .setKorobyte(new Korobyte(null, null, null, null, null, null))
                .setUnitId(new UnitId(null, 123L, "123321"))
                .build();

            add(item);
        }};

        Map<Long, List<CustomsTranslation>> expectedTranslations = new HashMap<>();

        assertThat(orderItemConverter.convertItems(orderItems, getParcel(), expectedTranslations, Map.of()))
            .as("Order items converted").isEqualTo(expectedItems);
    }

    @Test
    public void convertItemsSingleItemWithoutUnitId() {
        List<OrderItem> orderItems = getOrderItems();

        orderItems.get(1).setShopSku(null);
        orderItems.get(1).setSupplierId(null);

        List<Item> expectedItems = new ArrayList<Item>() {{
            Item item = new Item.ItemBuilder(
                "offer name",
                3,
                BigDecimal.valueOf(123.0)
            )
                .setArticle("123123")
                .setNameEnglish("english offer name")
                .setCargoType(CargoType.ART)
                .setCargoTypes(Collections.singletonList(CargoType.ART))
                .setCategoryName("Category full name")
                .setHsCode("123456")
                .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.TWENTY)))
                .setKorobyte(new Korobyte(null, null, null, null, null, null))
                .setUnitId(null)
                .build();

            add(item);
        }};

        Map<Long, List<CustomsTranslation>> expectedTranslations = new HashMap<>();

        assertThat(orderItemConverter.convertItems(orderItems, getParcel(), expectedTranslations, Map.of()))
            .as("Order items converted").isEqualTo(expectedItems);
    }

    @Test
    public void convertWithUnknownCargoType() {
        List<OrderItem> orderItems = getOrderItems();

        orderItems.get(0).setCargoTypes(ImmutableSet.of(0));
        orderItems.get(1).setCargoTypes(ImmutableSet.of(-1));

        Map<Long, List<CustomsTranslation>> expectedTranslations = new HashMap<>();

        assertThat(orderItemConverter.convertItems(orderItems, getParcel(), expectedTranslations, Map.of()))
            .as("Order items converted")
            .flatExtracting(Item::getCargoTypes)
            .containsOnly(CargoType.UNKNOWN);
    }

    @Test
    public void convertWithSupplierInn() {
        List<OrderItem> orderItems = getOrderItems();

        Map<Long, List<CustomsTranslation>> expectedTranslations = new HashMap<>();

        Map<Long, LegalInfo> supplierLegalInfo = Map.of(123L, LegalInfo.newBuilder().setInn("1231231234").build());
        assertThat(orderItemConverter.convertItems(orderItems, getParcel(), expectedTranslations, supplierLegalInfo))
            .as("Order items converted")
            .extracting(Item::getSupplier)
            .extracting(Supplier::getInn)
            .containsOnly("1231231234");
    }

    @Test
    public void convertLomItemsTest() {
        List<OrderItem> orderItems = getOrderItems();
        orderItems.get(1).setMsku(123L);

        assertThat(orderItemConverter.convertLomItems(
            orderItems,
            getParcel(),
            "test",
            Set.of(
                OrderItemRemovalPermission.initAllowed(orderItems.get(1).getId())
            )
        ))
            .as("Order items converted")
            .matches(l -> l.size() == 1)
            .first()
            .extracting(
                ItemDto::getMsku,
                ItemDto::getRemovableIfAbsent
            )
            .containsExactly(123L, true);
    }

    private Parcel getParcel() {
        Parcel parcel = new Parcel();

        parcel.setParcelItems(Collections.singletonList(new ParcelItem(2L, 3)));

        return parcel;
    }

    private OrderItem getOrderItem(Long itemId, int count) {
        OrderItem item = ItemsSteps.getOrderItem(itemId);

        item.setCount(count);
        item.setEnglishName("english offer name");
        item.setCargoTypes(ImmutableSet.of(50));
        item.setCategoryFullName("Category full name");
        item.setHsCode(123456L);
        item.setVat(VatType.VAT_20);
        item.setSupplierId(123L);
        item.setShopSku("123321");

        return item;
    }

    private List<OrderItem> getOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();

        orderItems.add(getOrderItem(1L, 2));
        orderItems.add(getOrderItem(2L, 4));

        return orderItems;
    }
}

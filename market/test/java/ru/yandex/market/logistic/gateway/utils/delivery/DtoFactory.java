package ru.yandex.market.logistic.gateway.utils.delivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Transfer;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.TransferItem;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Car;
import ru.yandex.market.logistic.gateway.common.model.delivery.Courier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusEvent;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;

public class DtoFactory {

    private DtoFactory() {
    }

    public static ClientTask createClientTask(long id, RequestFlow flow, String message) {
        ClientTask clientTask = new ClientTask();

        clientTask.setId(id);
        clientTask.setRootId(0L);
        clientTask.setParentId(0L);

        clientTask.setCountRetry(0);
        clientTask.setDelaySeconds(0);
        clientTask.setFlow(flow);
        clientTask.setMessage(message);
        clientTask.setStatus(TaskStatus.IN_PROGRESS);

        return clientTask;
    }

    public static Location createLocation() {
        return new Location.LocationBuilder("Россия", "Екатеринбург", "Свердловская область")
            .setFederalDistrict("Уральский федеральный округ")
            .setSubRegion("Муниципальное образование Екатеринбург")
            .setStreet("ул. Техническая")
            .setHouse("16")
            .setLocationId(54)
            .build();
    }

    public static ResourceId createResourceId() {
        return ResourceId.builder().setYandexId("111").build();
    }

    public static Warehouse createWarehouse() {
        return new Warehouse.WarehouseBuilder(ResourceId.builder().setYandexId("9955214").build(),
            createLocation(),
            Collections.singletonList(
                new WorkTime(1,
                    Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00")))))
            .setPhones(Collections.singletonList(new Phone("++74951234567", null)))
            .build();
    }

    public static ItemStocks createItemStocks() {
        return new ItemStocks(new UnitId("111", 222L, "333"),
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder().setYandexId("9955214").build(),
            Collections.singletonList(createStock()));
    }

    public static Stock createStock() {
        return new Stock.StockBuilder(StockType.QUARANTINE, new DateTime("2016-03-21T12:34:56+03:00"))
            .setCount(3)
            .build();
    }

    public static Transfer createTransfer() {
        String id = "1";
        long vendorId = 2L;
        String article = "3";
        ru.yandex.market.logistic.api.model.fulfillment.UnitId unitId =
            new ru.yandex.market.logistic.api.model.fulfillment.UnitId(id, vendorId, article);
        String yandexId = "yabdexid";
        String partnerId = "partnerid";
        Integer count = 227;

        ru.yandex.market.logistic.api.model.fulfillment.ResourceId resourceId = new ru.yandex.market.logistic.api.model.fulfillment.ResourceId(
            yandexId, partnerId);
        ru.yandex.market.logistic.api.model.fulfillment.ResourceId inboundId = new ru.yandex.market.logistic.api.model.fulfillment.ResourceId(
            yandexId, partnerId);

        ru.yandex.market.logistic.api.model.fulfillment.StockType from = ru.yandex.market.logistic.api.model.fulfillment.StockType.FIT;
        ru.yandex.market.logistic.api.model.fulfillment.StockType to = ru.yandex.market.logistic.api.model.fulfillment.StockType.SURPLUS;


        List<TransferItem> transferItems = new ArrayList<>();
        TransferItem transferItem = new TransferItem(unitId, count);
        transferItem.setInstances(
            ImmutableList.of(
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
            )
        );
        transferItems.add(transferItem);
        return new Transfer(resourceId,
            inboundId,
            from,
            to,
            transferItems);
    }

    public static TransferStatus createTransferStatus() {
        return new TransferStatus(
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId("111")
                .setPartnerId("222")
                .build(),
            new TransferStatusEvent(TransferStatusType.ACCEPTED, new DateTime("2018-12-21T11:59:59+03:00")));
    }

    public static TransferStatusHistory createTransferStatusHistory() {
        return new TransferStatusHistory(Collections.singletonList(new TransferStatusEvent(TransferStatusType.ACCEPTED,
            new DateTime("2018-12-21T11:59:59+03:00"))),
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId("111")
                .setPartnerId("222")
                .build()
        );
    }

    public static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2019-02-14T00:00:00+03:00/2019-02-21T00:00:00+03:00");
    }

    public static SelfExport createSelfExport() {
        return new SelfExport.SelfExportBuilder()
            .setCourier(createCourier())
            .setSelfExportId(createResourceId())
            .setTime(createDateTimeInterval())
            .setVolume(1.1f)
            .setWarehouse(createWarehouse())
            .setWeight(5.5f)
            .setOrdersId(Arrays.asList(
                ResourceId.builder().setYandexId("2").setPartnerId("ext2").build(),
                ResourceId.builder().setYandexId("3").setPartnerId("ext3").build()))
            .build();
    }

    private static Car createCar() {
        return new Car.CarBuilder("А019МР199").setDescription("Моя красная тачка").build();
    }

    private static Courier createCourier() {
        return new Courier.CourierBuilder(Collections.singletonList(
            new Person.PersonBuilder("Иван", "Доставляев")
                .setPatronymic("Васильевич")
                .build()))
            .setCar(createCar())
            .build();
    }

}

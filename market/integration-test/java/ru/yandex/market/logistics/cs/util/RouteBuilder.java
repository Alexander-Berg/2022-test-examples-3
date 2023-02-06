package ru.yandex.market.logistics.cs.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

import ru.yandex.market.logistics.cs.logbroker.checkouter.SimpleCombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.Date;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryService;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.Point;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.PointIds;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.ProcessedItem;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.Timestamp;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.RoutePointServiceType;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;

public final class RouteBuilder {

    private RouteBuilder() {
        throw new UnsupportedOperationException();
    }

    public static DeliveryRoute route(Point... points) {
        var route = new DeliveryRoute();
        route.setPoints(new LinkedList<>());
        for (var point : points) {
            route.getPoints().add(point);
        }
        return route;
    }

    public static SimpleCombinatorRoute combinatorRoute(Point... points) {
        var route = route(points);
        return new SimpleCombinatorRoute(route, List.of());
    }

    public static PointBuilder segment(long id) {
        return new PointBuilder(id);
    }

    public static DeliveryServiceBuilder service(long id) {
        return new DeliveryServiceBuilder(id);
    }

    public static ProcessedItemBuilder item(int index) {
        return new ProcessedItemBuilder(index);
    }

    public static class PointBuilder {

        private final Point point;

        private PointBuilder(long id) {
            point = new Point();
            point.setSegmentId(id);
        }

        public PointBuilder type(PointType type) {
            point.setSegmentType(type);
            return this;
        }

        public PointBuilder partnerName(String name) {
            point.setPartnerName(name);
            return this;
        }

        public PointBuilder partnerType(PartnerType type) {
            point.setPartnerType(type);
            return this;
        }

        public PointBuilder partnerId(long partnerId) {
            if (point.getIds() == null) {
                point.setIds(new PointIds());
            }
            point.getIds().setPartnerId(partnerId);
            return this;
        }

        public Point services(DeliveryService... services) {
            point.setServices(new LinkedList<>());
            for (var service: services) {
                point.getServices().add(service);
            }
            return point;
        }
    }

    public static class ProcessedItemBuilder {
        private final ProcessedItem item;

        private ProcessedItemBuilder(int index) {
            item = new ProcessedItem();
            item.setItemIndex(index);
        }

        public ProcessedItem quantity(int quantity) {
            item.setQuantity(quantity);
            return item;
        }
    }

    public static class DeliveryServiceBuilder {
        private final DeliveryService service;

        private DeliveryServiceBuilder(long id) {
            service = new DeliveryService();
            service.setId(id);
        }

        public DeliveryServiceBuilder code(ServiceCodeName code) {
            service.setCode(code);
            return this;
        }

        public DeliveryServiceBuilder type(RoutePointServiceType type) {
            service.setType(type);
            return this;
        }

        public DeliveryServiceBuilder start(LocalDateTime startTime) {
            var instant = startTime.toInstant(ZoneOffset.UTC);
            var timestamp = new Timestamp();
            timestamp.setSeconds(instant.getEpochSecond());
            timestamp.setNanos(instant.getNano());
            service.setStartTime(timestamp);
            return this;
        }

        public DeliveryServiceBuilder logisticDate(LocalDate date) {
            var logisticDate = new Date();
            logisticDate.setYear(date.getYear());
            logisticDate.setMonth(date.getMonthValue());
            logisticDate.setDay(date.getDayOfMonth());
            service.setLogisticDate(logisticDate);
            return this;
        }

        public DeliveryService items(ProcessedItem... items) {
            service.setItems(new LinkedList<>());
            for (var item: items) {
                service.getItems().add(item);
            }
            return service;
        }
    }

}

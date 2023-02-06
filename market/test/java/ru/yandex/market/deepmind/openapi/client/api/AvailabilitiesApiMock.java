package ru.yandex.market.deepmind.openapi.client.api;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;

import ru.yandex.market.deepmind.openapi.client.ApiException;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilitiesByIntervalResponse;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilitiesResponse;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilityByIntervalInfo;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilityInfo;
import ru.yandex.market.deepmind.openapi.client.model.BlockInfo;
import ru.yandex.market.deepmind.openapi.client.model.GetAvailabilitiesByIntervalRequest;
import ru.yandex.market.deepmind.openapi.client.model.GetAvailabilitiesRequest;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuWKey;

public class AvailabilitiesApiMock extends AvailabilitiesApi {
    private final Map<ShopSkuWKey, Container> map = new HashMap<>();

    public AvailabilitiesApiMock() {
        super(null);
    }

    @Override
    public List<AvailabilitiesResponse> getBySsku(GetAvailabilitiesRequest request) {
        if (request.getWarehouseIds() == null || request.getWarehouseIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "warehouseIds should be set");
        }

        var date = request.getDate();

        var result = new ArrayList<AvailabilitiesResponse>();
        for (ShopSkuKey key : request.getKeys()) {
            var response = new AvailabilitiesResponse();
            response.setKey(key);
            for (Long warehouseId : request.getWarehouseIds()) {
                var container = map.get(new ShopSkuWKey(key, warehouseId));
                var infos = container == null ? List.<BlockInfo>of() : container.get(date);

                var info = new AvailabilityInfo()
                    .warehouseId(warehouseId)
                    .infos(infos)
                    .allowInbound(infos.isEmpty());
                response.addAvailabilitiesItem(info);
            }
            result.add(response);
        }
        return result;
    }

    @Override
    public List<AvailabilitiesByIntervalResponse> getBySskuOnInterval(GetAvailabilitiesByIntervalRequest request) {
        if (request.getWarehouseIds() == null || request.getWarehouseIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "warehouseIds should be set");
        }

        var dateFrom = request.getDateFrom();
        var dateTo = request.getDateTo();

        var result = new ArrayList<AvailabilitiesByIntervalResponse>();
        for (ShopSkuKey key : request.getKeys()) {
            var response = new AvailabilitiesByIntervalResponse();
            response.setKey(key);
            for (Long warehouseId : request.getWarehouseIds()) {
                var container = map.get(new ShopSkuWKey(key, warehouseId));

                LocalDate fromDate = null;
                LocalDate toDate = null;
                List<BlockInfo> prevInfos = null;

                for (LocalDate now = dateFrom; !now.isAfter(dateTo); now = now.plusDays(1)) {
                    var infos = container == null ? List.<BlockInfo>of() : container.get(now);

                    if (prevInfos != null && prevInfos.equals(infos)) {
                        toDate = now;
                    } else {
                        if (prevInfos != null) {
                            var info = new AvailabilityByIntervalInfo()
                                .warehouseId(warehouseId)
                                .dateFrom(fromDate)
                                .dateTo(toDate)
                                .infos(prevInfos)
                                .allowInbound(prevInfos.isEmpty());
                            response.addAvailabilitiesItem(info);
                        }
                        fromDate = now;
                        toDate = now;
                        prevInfos = infos;
                    }
                }

                if (prevInfos != null) {
                    var info = new AvailabilityByIntervalInfo()
                        .warehouseId(warehouseId)
                        .dateFrom(fromDate)
                        .dateTo(toDate)
                        .infos(prevInfos)
                        .allowInbound(prevInfos.isEmpty());
                    response.addAvailabilitiesItem(info);
                }
            }
            result.add(response);
        }
        return result;
    }

    public AvailabilitiesApiMock putAvailability(ShopSkuKey key, long warehouseId, BlockInfo... blockInfo) {
        map.putIfAbsent(new ShopSkuWKey(key, warehouseId), new Container());
        var container = map.get(new ShopSkuWKey(key, warehouseId));
        container.add(List.of(blockInfo));
        return this;
    }

    public AvailabilitiesApiMock putAvailability(ShopSkuKey key, long warehouseId, LocalDate from, LocalDate to,
                                                 BlockInfo... blockInfo) {
        map.putIfAbsent(new ShopSkuWKey(key, warehouseId), new Container());
        var container = map.get(new ShopSkuWKey(key, warehouseId));
        container.add(from, to, List.of(blockInfo));
        return this;
    }

    private static class Container {
        private final List<BlockInfo> blockInfos = new ArrayList<>();
        private final Map<LocalDate, List<BlockInfo>> dates = new HashMap<>();

        public void add(Collection<BlockInfo> blockInfos) {
            this.blockInfos.addAll(blockInfos);
        }

        public void add(LocalDate from, LocalDate to, Collection<BlockInfo> blockInfos) {
            if (from == LocalDate.MIN && to == LocalDate.MAX) {
                add(blockInfos);
                return;
            }
            if (from == LocalDate.MIN) {
                from = to.minusYears(2);
            }
            if (to == LocalDate.MAX) {
                to = from.plusYears(2);
            }

            var between = Duration.between(from.atStartOfDay(), to.atStartOfDay());
            if (between.compareTo(Duration.ofDays(356 * 2 + 100)) > 0) {
                throw new RuntimeException("Sorry, this mock not supported blocks longer than 2 years");
            }

            for (LocalDate now = from; !now.isAfter(to); now = now.plusDays(1)) {
                if (!dates.containsKey(now)) {
                    dates.put(now, new ArrayList<>());
                }
                var list = dates.get(now);
                list.addAll(blockInfos);
            }
        }

        public List<BlockInfo> get(LocalDate date) {
            return Stream.concat(this.blockInfos.stream(), dates.getOrDefault(date, List.of()).stream())
                .collect(Collectors.toList());
        }
    }
}

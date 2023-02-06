package ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.model.dto.AOSZoneSettingsDto;
import ru.yandex.market.wms.autostart.settings.service.AutostartZoneSettingsService;
import ru.yandex.market.wms.common.dao.SkuDao;
import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrdersInventoryDetailDao;

import static java.math.BigDecimal.ONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config.TestData.toOrderDetails;


@TestConfiguration
public class TestConfigurations {
    @Mock
    private OrderDetailDao orderDetailDao;
    @Mock
    private OrdersInventoryDetailDao ordersInventoryDetailDao;
    @Mock
    private SkuDao skuDao;
    @Mock
    private AutostartZoneSettingsService autostartZoneSettingsService;

    @Bean
    @Primary
    public OrderDetailDao orderDetailDao(Properties properties) {
        MockitoAnnotations.openMocks(this);
        when(orderDetailDao.getOrderDetailsByOrders(anyList())).thenReturn(toOrderDetails(properties.orderWithDetails));
        return orderDetailDao;
    }

    @Bean
    @Primary
    public OrdersInventoryDetailDao ordersInventoryDetailDao(Properties properties) {
        MockitoAnnotations.openMocks(this);
        when(ordersInventoryDetailDao.findInventoryByOrderKeys(anyList(), any(Boolean.class), any(Boolean.class)))
                .thenReturn(properties.orderInventoryDetails);
        return ordersInventoryDetailDao;
    }

    @Bean
    @Primary
    public SkuDao skuDao(Properties properties) {
        MockitoAnnotations.openMocks(this);
        Map<SkuId, Dimensions> dimensions = properties.orderWithDetails.stream()
                .flatMap(owd -> owd.getOrderDetails().stream())
                .map(OrderDetail::skuId)
                .collect(Collectors.toMap(
                        Function.identity(),
                        (k) -> new Dimensions.DimensionsBuilder().weight(ONE).height(ONE).length(ONE).build()));
        when(skuDao.getDimensions(anyList())).thenReturn(dimensions);
        return skuDao;
    }

    @Bean
    @Primary
    public AutostartZoneSettingsService autostartZoneSettingsService() {
        MockitoAnnotations.openMocks(this);
        when(autostartZoneSettingsService.getZoneSettings(any(String.class))).thenReturn(
                AOSZoneSettingsDto.builder()
                        .itemsIntoPickingOrder(10)
                        .maxWeightPerPickingOrder(100.0)
                        .maxVolumePerPickingOrder(100.0)
                        .build()
        );
        return autostartZoneSettingsService;
    }

    @Data
    @Builder
    public static class Properties {
        @Builder.Default
        List<OrderWithDetails> orderWithDetails = new ArrayList<>();
        @Builder.Default
        List<OrderInventoryDetail> orderInventoryDetails = new ArrayList<>();
        LocalDateTime currentTime;
    }
}

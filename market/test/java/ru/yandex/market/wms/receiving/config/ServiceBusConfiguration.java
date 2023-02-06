package ru.yandex.market.wms.receiving.config;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.KorobyteDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.MappedSkuDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuIdDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.SkuTrustworthyInfoDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.GetTrustworthyInfoRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.request.MapSkuRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.request.ServicebusRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.GetTrustworthyInfoResponse;
import ru.yandex.market.wms.common.spring.servicebus.model.response.MapSkuResponse;
import ru.yandex.market.wms.common.spring.servicebus.model.response.ServicebusResponse;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@TestConfiguration
public class ServiceBusConfiguration {

    @Primary
    @Bean
    public ServicebusClient servicebusClient() {
        return new ServiceBusStub();
    }

    static class ServiceBusStub implements ServicebusClient {

        @Override
        public MapSkuResponse mapSku(MapSkuRequest request) {
            List<MappedSkuDto> mappedSkuDtos = request
                    .getManufacturerSkus()
                    .stream()
                    .map(it -> new MappedSkuDto(
                            it,
                            new SkuIdDto(
                                    it.getStorerKey(),
                                    generateWarehouseSku(it.getStorerKey(), it.getManufacturerSku())
                            )
                    ))
                    .collect(collectingAndThen(toList(), Collections::unmodifiableList));
            return new MapSkuResponse(mappedSkuDtos);
        }

        @Override
        public GetTrustworthyInfoResponse getTrustworthyInfo(GetTrustworthyInfoRequest request) {
            List<SkuTrustworthyInfoDto> skuDtos = request
                    .getManufacturerSkus()
                    .stream()
                    .map(it -> SkuTrustworthyInfoDto.builder()
                                    .storerKey(it.getStorerKey().toString())
                                    .manufacturerSku(it.getManufacturerSku())
                                    .name("Test Item")
                                    .korobyte(KorobyteDto.builder()
                                            .length(BigDecimal.valueOf(100))
                                            .width(BigDecimal.valueOf(100))
                                            .height(BigDecimal.valueOf(100))
                                            .weightNet(BigDecimal.valueOf(20))
                                            .weightTare(BigDecimal.valueOf(5))
                                            .weightGross(BigDecimal.valueOf(25))
                                            .build())
                                    .build()
                    )
                    .collect(collectingAndThen(toList(), Collections::unmodifiableList));
            return new GetTrustworthyInfoResponse(skuDtos);
        }

        @Override
        public <T extends ServicebusRequest, R extends ServicebusResponse> R processRequest(String path, T request,
                                                                                            Class<R> responseClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends ServicebusRequest, R extends ServicebusResponse> R processRequest(String path, T request,
                                                                                            Class<R> responseClass,
                                                                                            long timeoutMs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> void processQueueRequest(String queue, T payLoad) {
            throw new UnsupportedOperationException();
        }

        private String generateWarehouseSku(long storer, String msku) {
            return "ROV" + storer + msku;
        }
    }
}

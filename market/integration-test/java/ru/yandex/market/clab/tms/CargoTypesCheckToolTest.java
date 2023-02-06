package ru.yandex.market.clab.tms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.clab.common.config.component.ServicesConfig;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodFilter;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepository;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementFilter;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementRepository;
import ru.yandex.market.clab.common.service.warehouse.WarehouseRepository;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedMovementState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Warehouse;
import ru.yandex.market.clab.test.BaseIntegrationTest;
import ru.yandex.market.mboc.http.DeliveryParamsStub;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootTest(
    classes = {
        ServicesConfig.class
    })
@Ignore
public class CargoTypesCheckToolTest extends BaseIntegrationTest {

    private final Logger logger = LogManager.getLogger();

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private RequestedMovementRepository requestedMovementRepository;

    @Autowired
    private RequestedGoodRepository requestedGoodRepository;

    @Test
    public void checkCargoTypes() {

        DeliveryParamsStub deliveryParams = new DeliveryParamsStub();
        deliveryParams.setHost("http://cm-api.vs.market.yandex.net/proto/deliveryParams/");

        List<RequestedMovement> movements =
            requestedMovementRepository.find(new RequestedMovementFilter().addState(RequestedMovementState.IN_PROCESS))
                .stream()
                .sorted(Comparator.comparing(RequestedMovement::getId))
                .collect(Collectors.toList());

        movements.forEach(requestedMovement -> {
            List<RequestedGood> goods = requestedGoodRepository.findGoods(new RequestedGoodFilter()
                .setMovementId(requestedMovement.getId()));
            Warehouse warehouseFrom = warehouseRepository.getById(requestedMovement.getWarehouseFromId());
            Warehouse warehouseTo = warehouseRepository.getById(requestedMovement.getWarehouseToId());

            MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest request =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest.newBuilder()
                    .addWarehouseId(requestedMovement.getWarehouseToId())
                    .setReturnMasterData(true)
                    .addAllKeys(goods.stream().map(g ->
                        MboMappingsForDelivery.FulfillmentShopSkuKey.newBuilder()
                            .setSupplierId(g.getSupplierId().intValue())
                            .setShopSku(g.getSupplierSkuId())
                            .build()).collect(Collectors.toList()))
                    .build();

            MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse response =
                deliveryParams.searchFulfilmentSskuParams(request);

            AtomicInteger cargoTypesNotAllowed = new AtomicInteger();
            AtomicInteger inboundNotAllowed = new AtomicInteger();
            response.getFulfilmentInfoList().forEach(info -> {
                if (info.hasAllowCargoType() && !info.getAllowCargoType()) {
                    cargoTypesNotAllowed.incrementAndGet();
                }
                if (info.hasAllowInbound() && !info.getAllowInbound()) {
                    inboundNotAllowed.incrementAndGet();
                }
            });

            if (cargoTypesNotAllowed.get() == 0 && inboundNotAllowed.get() == 0) {
                return;
            }
            logger.info("Found errors for movement {} ({}) from {} to {} with {} goods:" +
                " {} goods not allowed cargo types and {} goods not allowed inbound",
                requestedMovement.getId(), requestedMovement.getErpRequestIds(),
                warehouseFrom.getName(), warehouseTo.getName(), goods.size(),
                cargoTypesNotAllowed.get(), inboundNotAllowed.get());
            response.getFulfilmentInfoList().forEach(info -> {
                if (info.hasAllowCargoType() && !info.getAllowCargoType()) {
                    logger.info("Cargo type not allowed for good {} ({}): {}; missing: {}",
                        info.getShopSku(), info.getMarketSkuId(), info.getAllowCargoTypeComment().getMessageCode(),
                        info.getMissingCargoTypesList());
                }
                if (info.hasAllowInbound() && !info.getAllowInbound()) {
                    logger.info("Inbound not allowed for good {} ({}): {}; {}",
                        info.getShopSku(), info.getMarketSkuId(), info.getAllowInboundComment().getMessageCode(),
                        info.getAllowInboundComment().getJsonDataForMustacheTemplate());
                }
            });
        });
    }
}

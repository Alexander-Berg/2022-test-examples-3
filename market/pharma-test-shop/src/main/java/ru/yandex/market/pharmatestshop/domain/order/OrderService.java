package ru.yandex.market.pharmatestshop.domain.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pharmatestshop.domain.client.MarketClient;
import ru.yandex.market.pharmatestshop.domain.json.JsonMapper;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderSubstatus;
import ru.yandex.market.pharmatestshop.domain.orderjson.OrderJson;
import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyRepository;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository repository;
    private final MarketClient marketClient;

    final
    PharmacyRepository pharmacyRepository;

    @Autowired
    public OrderService(OrderRepository repository, MarketClient marketClient,
                        PharmacyRepository pharmacyRepository) {
        this.repository = repository;
        this.marketClient = marketClient;
        this.pharmacyRepository = pharmacyRepository;
    }

    public Order getResponse(OrderDto request) {
        return OrderMapper.map(request);
    }

    @Transactional
    public int insertOrUpdateOrderJson(String orderJsonString, long shopId) {
        //Json->orderEntityStatus [OrderDto -> Order]

        JsonMapper mapper = new JsonMapper();
        long id = mapper.getIdFromJsonData(orderJsonString);
        OrderStatus orderStatus = mapper.getStatusFromJsonData(orderJsonString);
        OrderSubstatus orderSubstatus = mapper.getSubStatusFromJsonData(orderJsonString);
        String deliveryType = mapper.getDeliveryTypeFromJsonData(orderJsonString);


        OrderJson orderJson = OrderJson.builder()
                .shopId(shopId)
                .id(id)
                .status(orderStatus)
                .substatus(orderSubstatus)
                .deliveryType(deliveryType)
                .jsonData(orderJsonString)
                .campaignId(pharmacyRepository.findByShopId(shopId).getCampaignId())
                .build();
        repository.save(orderJson);

        return 1;
    }

    @Transactional
    public void upgradeStatus() {
        for (OrderJson order : repository.findAll()) {
            switch (order.getStatus()) {
                case PROCESSING:
                    switch (order.getDeliveryType()) {
                        case "DELIVERY":
                            order.setStatus(OrderStatus.DELIVERY);
                            order.setSubstatus(OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

                            JsonMapper mapper = new JsonMapper();
                            order.setJsonData(mapper.setStatusAndSubstatusToJsonData(order.getJsonData(),
                                    order.getStatus(), order.getSubstatus()));
                            sendOrderToMarket(order);
                            break;
                        case "PICKUP":
                            order.setStatus(OrderStatus.PICKUP);
                            order.setSubstatus(OrderSubstatus.PICKUP_SERVICE_RECEIVED);
                            sendOrderToMarket(order);
                            break;
                    }
                    break;
                case PICKUP:
                case DELIVERY:
                    order.setStatus(OrderStatus.DELIVERED);
                    order.setSubstatus(OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
                    sendOrderToMarket(order);
                    break;
            }
        }

    }

    private void sendOrderToMarket(OrderJson order) {
        log.info("Updated: {}", order.getJsonData());
        marketClient.putStatus(order.getJsonData(), order.getId(), order.getShopId());
    }
}

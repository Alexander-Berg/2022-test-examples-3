package ru.yandex.market.tpl.core.service.delivery.ds;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.delivery.request.UpdateItemsInstancesRequest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.USE_BASE64_ENCODED_ORDER_ITEM_INSTANCES_ENABLED;

@RequiredArgsConstructor

@ContextConfiguration(classes = {
        LogisticApiRequestProcessingConfiguration.class
})
public class UpdateItemsInstancesLogisticApiProcessorTest extends TplAbstractTest {
    private final UpdateOrderItemsInstancesDsApiProcessor processor;
    private final DsRequestReader dsRequestReader;
    private final OrderGenerateService orderGenerateService;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationProviderAdapter config;

    @AfterEach
    void clean() {
        Mockito.reset(config);
    }

    @SneakyThrows
    @Test
    @Transactional
    void updateCisValues() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsItemCount(2).build())
                        .build()
        );
        OrderItem orderItem = order.getItems().get(0);

        UpdateItemsInstancesRequest request = dsRequestReader.readRequest(
                "/ds/update_order_items_instances.xml",
                UpdateItemsInstancesRequest.class,
                order.getExternalOrderId(),
                orderItem.getVendorArticle().getVendorId(),
                orderItem.getVendorArticle().getArticle());

        processor.apiCall(request, partnerRepository.findByIdOrThrow(order.getDeliveryServiceId()));

        order = orderRepository.findByIdOrThrow(order.getId());
        order.getItems().stream()
                .filter(item -> item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisValues()).hasSize(2));
        order.getItems().stream()
                .filter(item -> !item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisValues()).hasSize(item.getCount()));
    }

    @SneakyThrows
    @Test
    @Transactional
    void updateCisFullValues() {
        doReturn(true).when(config).isBooleanEnabled(USE_BASE64_ENCODED_ORDER_ITEM_INSTANCES_ENABLED);
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsItemCount(2).build())
                        .build()
        );
        OrderItem orderItem = order.getItems().get(0);

        UpdateItemsInstancesRequest request = dsRequestReader.readRequest(
                "/ds/update_order_items_instances.xml",
                UpdateItemsInstancesRequest.class,
                order.getExternalOrderId(),
                orderItem.getVendorArticle().getVendorId(),
                orderItem.getVendorArticle().getArticle());

        processor.apiCall(request, partnerRepository.findByIdOrThrow(order.getDeliveryServiceId()));

        order = orderRepository.findByIdOrThrow(order.getId());
        order.getItems().stream()
                .filter(item -> item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisValues()).hasSize(2));
        order.getItems().stream()
                .filter(item -> !item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisValues()).hasSize(item.getCount()));
        order.getItems().stream()
                .filter(item -> item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisFullValues()).hasSize(2));
        order.getItems().stream()
                .filter(item -> !item.getId().equals(orderItem.getId()))
                .forEach(item -> assertThat(item.getAllCisFullValues()).hasSize(item.getCount()));
    }

    @SneakyThrows
    @Test
    void updateItemInstances() {
        OrderItem orderItem = transactionTemplate.execute(ts -> {
            var order1 = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsItemCount(2).build())
                            .build()
            );
           return order1.getItems().get(0);
        });
        Order order = orderItem.getOrder();


        UpdateItemsInstancesRequest request = dsRequestReader.readRequest(
                "/ds/update_order_items_instances.xml",
                UpdateItemsInstancesRequest.class,
                order.getExternalOrderId(),
                orderItem.getVendorArticle().getVendorId(),
                orderItem.getVendorArticle().getArticle());

        processor.apiCall(request, partnerRepository.findByIdOrThrow(order.getDeliveryServiceId()));

        transactionTemplate.execute(ts -> {
            var updatedOrder = orderRepository.findByIdOrThrow(order.getId());
            updatedOrder.getItems().stream()
                    .filter(item -> item.getId().equals(orderItem.getId()))
                    .forEach(item -> assertThat(item.getInstances()).hasSize(2));
            return null;
        });
    }

    @SneakyThrows
    @Test
    void updateItemInstancesWithDoesNotThrowValidateCountInstancesIsEqualToItemsCount() {
        OrderItem orderItem = transactionTemplate.execute(ts -> {
            var order1 = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryPrice(BigDecimal.ONE)
                            .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                    .itemsCount(2)
                                    .itemsItemCount(2)
                                    .isFashion(true)
                                    .build())
                            .build()
            );
            return order1.getItems().get(0);
        });
        Order order = orderItem.getOrder();


        UpdateItemsInstancesRequest request = dsRequestReader.readRequest(
                "/ds/update_order_items_instances.xml",
                UpdateItemsInstancesRequest.class,
                order.getExternalOrderId(),
                orderItem.getVendorArticle().getVendorId(),
                orderItem.getVendorArticle().getArticle());

        assertDoesNotThrow(
                () -> processor.apiCall(request, partnerRepository.findByIdOrThrow(order.getDeliveryServiceId()))
        );
    }

    @SneakyThrows
    @Test
    @Transactional
    void updateItemInstancesWhenItemCountNotEqualsInputInstances() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsItemCount(2).build())
                        .build()
        );
        OrderItem orderItem = order.getItems().get(0);

        UpdateItemsInstancesRequest request = dsRequestReader.readRequest(
                "/ds/update_order_items_instances_without_instances.xml",
                UpdateItemsInstancesRequest.class,
                order.getExternalOrderId(),
                orderItem.getVendorArticle().getVendorId(),
                orderItem.getVendorArticle().getArticle());

        processor.apiCall(request, partnerRepository.findByIdOrThrow(order.getDeliveryServiceId()));

        order = orderRepository.findByIdOrThrow(order.getId());
        order.getItems().stream()
                .filter(i -> !i.isService())
                .forEach(i -> assertThat(i.getInstances().size()).isEqualTo(i.getCount()));
    }

}

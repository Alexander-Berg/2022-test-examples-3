package ru.yandex.market.checkout.checkouter.controller;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.cashback.details.CashbackInfoService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.OrderController;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolver;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureService;
import ru.yandex.market.checkout.checkouter.order.CheckpointRequest;
import ru.yandex.market.checkout.checkouter.order.DigitalOrderService;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.archive.ActualOrdersFetcher;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveOrdersFetcher;
import ru.yandex.market.checkout.checkouter.order.archive.DistributedArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.OrdersFetcher;
import ru.yandex.market.checkout.checkouter.order.eda.EdaOrderService;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundlesFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.checkouter.views.CancellationRulesShuffler;
import ru.yandex.market.checkout.checkouter.views.services.BiMapper;
import ru.yandex.market.checkout.checkouter.views.services.Mapper;
import ru.yandex.market.checkout.checkouter.views.services.OrderChangesViewModelService;
import ru.yandex.market.checkout.checkouter.views.services.OrderViewModelService;
import ru.yandex.market.checkout.checkouter.views.services.mappers.AddressToAddressViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.DeliveryToDeliveryVerificationPartMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.DeliveryToOrderDeliveryViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.ItemServiceToItemServiceViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.OrderItemToOrderItemViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.OrderToOrderViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.PaymentToPaymentViewModelMapper;
import ru.yandex.market.checkout.checkouter.warranty.WarrantyService;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.json.Names.Buyer.ASSESSOR;

@ContextConfiguration(classes = OrderControllerTest.Context.class)
public class OrderControllerTest extends AbstractControllerTestBase {

    @Autowired
    private OrdersFetcher actualOrdersFetcher;
    @Autowired
    private CancellationRulesShuffler cancellationRulesShuffler;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[][]{
                        {
                                "by-uid",
                                get("/orders/by-uid/123123").param(ASSESSOR, "true"),
                                ClientRole.USER
                        },
                        {
                                "orders",
                                get("/orders").param(ASSESSOR, "true").param(CLIENT_ROLE, ClientRole.SYSTEM.name()),
                                ClientRole.SYSTEM
                        },
                        buildPostGetOrder()
                }
        ).stream().map(Arguments::of);
    }

    @Nonnull
    private static Object[] buildPostGetOrder() {
        return new Object[]{
                "get-order",
                post("/get-orders")
                        .content("{\"assessor\":true}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name()),
                ClientRole.SYSTEM
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void shouldPassAssessorParam(String caseName, MockHttpServletRequestBuilder request,
                                        ClientRole expectedRole) throws Exception {
        ArgumentCaptor<OrderSearchRequest> orderSearchRequest = ArgumentCaptor.forClass(OrderSearchRequest.class);
        ArgumentCaptor<ClientInfo> clientInfo = ArgumentCaptor.forClass(ClientInfo.class);

        PagedOrders value = new PagedOrders();
        value.setPager(new Pager());
        value.setItems(Collections.emptyList());

        Mockito.when(actualOrdersFetcher.getOrders(
                orderSearchRequest.capture(),
                clientInfo.capture(),
                any(CheckpointRequest.class))
        ).thenReturn(value);

        mockMvc.perform(request)
                .andExpect(status().isOk());

        Assertions.assertEquals(Boolean.TRUE, orderSearchRequest.getValue().assessor);
        Assertions.assertEquals(expectedRole, clientInfo.getValue().getRole());
    }

    @Test
    public void shouldShuffleCancellationRulesForUser() throws Exception {
        MockHttpServletRequestBuilder request = get("/orders/cancellation-substatuses")
                .param(CLIENT_ROLE, ClientRole.USER.name());
        mockMvc.perform(request)
                .andExpect(status().isOk());

        verify(cancellationRulesShuffler, times(1)).shuffleCancellationRules(any());
    }

    @Test
    public void shouldNotShuffleCancellationRulesForCallCenterOperator() throws Exception {
        MockHttpServletRequestBuilder request = get("/orders/cancellation-substatuses")
                .param(CLIENT_ROLE, ClientRole.CALL_CENTER_OPERATOR.name());
        mockMvc.perform(request)
                .andExpect(status().isOk());

        verify(cancellationRulesShuffler, never()).shuffleCancellationRules(any());
    }

    @ImportResource("classpath:int-test-views.xml")
    @Configuration
    public static class Context extends AbstractControllerContext {

        @Bean
        public OrderService orderService() {
            return Mockito.mock(OrderService.class);
        }

        @Bean
        public LoyaltyService loyaltyService() {
            return Mockito.mock(LoyaltyService.class);
        }

        @Bean
        public OrderUpdateService orderUpdateService() {
            return Mockito.mock(OrderUpdateService.class);
        }

        @Bean
        public OrderArchiveService orderArchiveService() {
            return Mockito.mock(OrderArchiveService.class);
        }

        @Bean
        public OrderEditService orderEditService() {
            return Mockito.mock(OrderEditService.class);
        }

        @Bean
        public WarrantyService warrantyService() {
            return Mockito.mock(WarrantyService.class);
        }

        @Bean
        public DigitalOrderService digitalOrderService() {
            return Mockito.mock(DigitalOrderService.class);
        }

        @Bean
        public BundlesFeatureSupportHelper bundlesFeatureSupportHelper() {
            return Mockito.mock(BundlesFeatureSupportHelper.class);
        }

        @Bean
        public OrderViewModelService orderViewModelService() {
            return new OrderViewModelService(
                    new OrderToOrderViewModelMapper(
                            new OrderItemToOrderItemViewModelMapper(new ItemServiceToItemServiceViewModelMapper()),
                            new DeliveryToOrderDeliveryViewModelMapper(
                                    new AddressToAddressViewModelMapper(),
                                    new DeliveryToDeliveryVerificationPartMapper()
                            ),
                            new PaymentToPaymentViewModelMapper()
                    ),
                    Mockito.mock(BiMapper.class),
                    Mockito.mock(Mapper.class));
        }

        @Bean
        public OrderChangesViewModelService orderChangesViewModelService() {
            return new OrderChangesViewModelService(Mockito.mock(Mapper.class));
        }

        @Bean
        public ActualOrdersFetcher actualOrdersFetcher() {
            return Mockito.mock(ActualOrdersFetcher.class);
        }

        @Bean
        public ArchiveOrdersFetcher archiveOrdersFetcher() {
            return Mockito.mock(ArchiveOrdersFetcher.class);
        }

        @Bean
        public DistributedArchiveService distributedArchiveService() {
            return Mockito.mock(DistributedArchiveService.class);
        }

        @Bean
        public EdaOrderService edaOrderService() {
            return Mockito.mock(EdaOrderService.class);
        }

        @Bean
        public CancellationRulesShuffler cancellationRulesShuffler() {
            return Mockito.mock(CancellationRulesShuffler.class);
        }

        @Bean
        public CheckouterFeatureReader checkouterFeatureReader() {
            return new CheckouterFeatureResolver(new ObjectMapper(), Mockito.mock(CheckouterFeatureService.class));
        }

        @Bean
        public CashbackInfoService cashbackInfoService() {
            return Mockito.mock(CashbackInfoService.class);
        }

        @Bean
        public OrderController orderController(
                OrderViewModelService orderViewModelService,
                CheckouterFeatureReader checkouterFeatureReader
        ) {
            CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl();
            return new OrderController(
                    orderViewModelService,
                    orderService(),
                    orderUpdateService(),
                    Clock.systemDefaultZone(),
                    checkouterProperties,
                    checkouterFeatureReader,
                    orderEditService(),
                    orderChangesViewModelService(),
                    cashbackInfoService()
            );
        }
    }
}

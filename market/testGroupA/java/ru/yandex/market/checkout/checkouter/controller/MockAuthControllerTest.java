package ru.yandex.market.checkout.checkouter.controller;

import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.util.collections.Either;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.auth.NoAuthForbiddenException;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.config.web.ErrorsConfig;
import ru.yandex.market.checkout.checkouter.config.web.ViewsConfig;
import ru.yandex.market.checkout.checkouter.controllers.oms.AuthController;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MoveOrdersService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCreateService;
import ru.yandex.market.checkout.checkouter.order.limit.HitRateGroupRateLimitsChecker;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundlesFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.service.bind.BindFailReason;
import ru.yandex.market.checkout.checkouter.service.bind.OrderBindService;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.checkouter.storage.util.MultiLockHelper;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.views.services.BiMapper;
import ru.yandex.market.checkout.checkouter.views.services.Mapper;
import ru.yandex.market.checkout.checkouter.views.services.OrderViewModelService;
import ru.yandex.market.checkout.checkouter.views.services.mappers.AddressToAddressViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.DeliveryToDeliveryVerificationPartMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.DeliveryToOrderDeliveryViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.ItemServiceToItemServiceViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.OrderItemToOrderItemViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.OrderToOrderViewModelMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.PaymentToPaymentViewModelMapper;
import ru.yandex.market.checkout.common.ratelimit.HitRateLimitException;
import ru.yandex.market.checkout.storage.StorageCallback;
import ru.yandex.market.checkout.storage.impl.LockCallback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = MockAuthControllerTest.SimpleConfiguration.class)
public class MockAuthControllerTest extends AbstractControllerTestBase {

    private static final String BIND_KEY = "123.asdasd";
    private static final String MUID_COOKIE = "SOME_RUINED_COOKIE";
    private static final String URL = "/orders/by-bind-key/{bindKey}";
    private static final long UID = 123L;

    @Autowired
    private AuthService authService;
    @Autowired
    private OrderBindService orderBindService;
    @Autowired
    private HitRateGroupRateLimitsChecker hitRateGroupRateLimitsChecker;

    @AfterEach
    public void tearDown() {
        Mockito.reset(authService, orderBindService, hitRateGroupRateLimitsChecker);
    }

    @Test
    public void shouldReturnOkOnNullCookie() throws Exception {
        Mockito.when(orderBindService.findOrderByBindKey(
                Mockito.eq(BIND_KEY), Mockito.isNull(Long.class), Mockito.anyCollectionOf(String.class),
                Mockito.anyLong(), Mockito.isNull(Boolean.class))
        ).thenReturn(Either.left(new Order()));

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isOk());

        Mockito.verify(authService, Mockito.never()).checkMuid(Mockito.anyString());
    }

    @Test
    public void shouldReturnOkOnNullUid() throws Exception {
        Mockito.when(orderBindService.findOrderByBindKey(
                Mockito.eq(BIND_KEY), Mockito.anyLong(), Mockito.anyCollectionOf(String.class),
                Mockito.isNull(Long.class), Mockito.eq(true))
        ).thenReturn(Either.left(new Order()));
        Mockito.when(authService.checkMuid(Mockito.anyString())).thenReturn(UID);

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(true))
                        .cookie(new Cookie("muid", MUID_COOKIE))
        )
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnForbiddenOnIncorrectCookie() throws Exception {

        Mockito.when(authService.checkMuid(MUID_COOKIE))
                .thenThrow(new NoAuthForbiddenException("invalid_muid_cookie", "Muid decryption failed"));

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_muid_cookie"));
    }

    @Test
    public void shouldReturnOrderNotFoundIfServiceReturnedNotFound() throws Exception {
        mockFail(BindFailReason.ORDER_NOT_FOUND);

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

    }

    @Test
    public void shouldReturnOrderBindExpiredIfServiceReturnedBindExpired() throws Exception {
        mockFail(BindFailReason.ORDER_BIND_EXPIRED);

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_BIND_EXPIRED"));

    }

    @Test
    public void shouldReturnOrderNotFoundIfServiceReturnedNotEnoughCredentials() throws Exception {
        mockFail(BindFailReason.NOT_ENOUGH_CREDENTIALS);

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_CREDENTIALS"));

    }

    @Test
    public void shouldReturnOrderAlreadyBoundIfServiceReturnedAlreadyBound() throws Exception {
        mockFail(BindFailReason.ORDER_ALREADY_BOUND);

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_BOUND"));
    }

    @Test
    public void shouldReturnOrderRateLimitException() throws Exception {
        Mockito.doThrow(new HitRateLimitException("asdasd"))
                .when(hitRateGroupRateLimitsChecker).checkAndIncrement(UID, HitRateGroup.LIMIT, 1,
                Collections.emptySet());

        mockMvc.perform(
                get(URL, BIND_KEY)
                        .cookie(new Cookie("muid", MUID_COOKIE))
                        .param(CheckouterClientParams.UID, String.valueOf(UID))
        )
                .andExpect(status().isMethodFailure())
                .andExpect(jsonPath("$.code").value("HIT_RATE_LIMIT"));
    }

    private void mockFail(BindFailReason reason) {
        long muid = 1L;
        Mockito.when(authService.checkMuid(MUID_COOKIE))
                .thenReturn(muid);
        Mockito.when(orderBindService.findOrderByBindKey(Mockito.eq(BIND_KEY), Mockito.eq(muid),
                Mockito.anyCollectionOf(String.class), Mockito.eq(UID), Mockito.isNull(Boolean.class)))
                .thenReturn(Either.right(reason));
    }


    @Configuration
    @Import({ViewsConfig.class, ErrorsConfig.class})
    @ImportResource({"classpath:int-test-views.xml"})
    public static class SimpleConfiguration {

        @Bean
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }

        @Bean
        public OrderBindService orderBindService() {
            return Mockito.mock(OrderBindService.class);
        }

        @Bean
        public TransactionTemplate defaultTransactionTemplate() {
            TransactionTemplate mock = Mockito.mock(TransactionTemplate.class);
            TransactionStatus transactionStatus = Mockito.mock(TransactionStatus.class);
            Mockito.when(transactionStatus.isRollbackOnly()).thenReturn(false);
            Mockito.when(transactionStatus.isCompleted()).thenReturn(false);
            Mockito.when(transactionStatus.isNewTransaction()).thenReturn(false);

            Mockito.when(mock.execute(Mockito.any())).thenAnswer(
                    inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(transactionStatus)
            );
            return mock;
        }

        @Bean
        public LoyaltyService loyaltyService() {
            return Mockito.mock(LoyaltyService.class);
        }

        @Bean
        public MultiLockHelper multiLockHelper() {
            return new MultiLockHelper() {

                @Override
                public <T> T updateWithOrderLocks(Collection<Long> orderIds, StorageCallback<T> callback) {
                    return callback.doQuery();
                }

                @Override
                public <T> T doLockedWithoutTransaction(Collection<Long> orderIds, LockCallback<T> callback) {
                    return callback.doLocked(null);
                }
            };
        }

        @Bean
        public MoveOrdersService moveOrdersService() {
            return Mockito.mock(MoveOrdersService.class);
        }

        @Bean
        public OrderCreateService orderCreateService() {
            return Mockito.mock(OrderCreateService.class);
        }

        @Bean
        public CheckouterProperties checkouterProperties() {
            return Mockito.mock(CheckouterProperties.class);
        }

        @Bean
        public CheckouterFeatureReader checkouterFeatureReader() {
            return Mockito.mock(CheckouterFeatureReader.class);
        }

        @Bean
        public HitRateGroupRateLimitsChecker orderByBindKeyRateLimitsChecker() {
            return Mockito.mock(HitRateGroupRateLimitsChecker.class);
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
        public MemCachedAgent memCachedAgent() {
            return Mockito.mock(MemCachedAgent.class);
        }

        @Bean
        public AuthController orderBindController(
                AuthService authService,
                MoveOrdersService moveOrdersService,
                OrderBindService orderBindService,
                HitRateGroupRateLimitsChecker hitRateGroupRateLimitsChecker,
                OrderViewModelService orderViewModelService
        ) {
            return new AuthController(
                    authService,
                    orderViewModelService,
                    moveOrdersService,
                    orderBindService,
                    hitRateGroupRateLimitsChecker
            );
        }
    }
}

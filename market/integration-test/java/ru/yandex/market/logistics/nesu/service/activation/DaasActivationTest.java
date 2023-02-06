package ru.yandex.market.logistics.nesu.service.activation;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Collections2;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Активация Daas-магазина")
public class DaasActivationTest extends ShopActivationTest {
    @Test
    @DisplayName("Магазин не активируется, так как находится в неподходящем статусе")
    @DatabaseSetup("/service/shop/activation/prepare_inappropriate_status.xml")
    void inappropriateShopStatus() {
        createSender();
        createWarehouse();
        createProduct();
        assertShopStatus(ShopStatus.OFF);
        verify(lmsClient).createLogisticsPoint(logisticsPointCreateRequestBuilder().businessId(41L).build());
    }

    @DisplayName("Активировать DaaS магазин")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("activateDaasShopSource")
    @DatabaseSetup("/service/shop/activation/prepare_daas.xml")
    void activateDaasShop(
        @SuppressWarnings("unused") String displayName,
        List<Consumer<ShopActivationTest>> methodCalls,
        int lmsClientGetLogisticsPointsInvocationCount
    ) {
        methodCalls.forEach(consumer -> {
            assertShopStatus(ShopStatus.NEED_SETTINGS);
            consumer.accept(this);
        });
        assertShopStatus(ShopStatus.ACTIVE);
        verify(lmsClient, times(lmsClientGetLogisticsPointsInvocationCount))
            .getLogisticsPoints(refEq(logisticsPointFilter(41L)));
        verify(sendNotificationToShopProducer).produceTask(MBI_NOTIFICATION_SHOP_ACTIVATE_ID, 1L, null, null);

        MessageRecipients messageRecipients = new MessageRecipients();
        messageRecipients.setToAddressList(List.of(YADO_SALES_EMAIL));
        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationProducer).produceTask(
            eq(MBI_SALES_NOTIFICATION_SHOP_ACTIVATE_ID),
            eq(messageRecipients),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue()).isXmlEqualTo(extractFileContent("service/shop/activation/shop.xml"));
        verify(lmsClient).createLogisticsPoint(logisticsPointCreateRequestBuilder().businessId(41L).build());
    }

    @Nonnull
    private static Stream<Arguments> activateDaasShopSource() {
        List<Pair<String, Consumer<DaasActivationTest>>> namedMethodCallPairs = List.of(
            Pair.of("createWarehouse", DaasActivationTest::createWarehouse),
            Pair.of("createProduct", DaasActivationTest::createProduct)
        );
        return Collections2.permutations(namedMethodCallPairs).stream().map(DaasActivationTest::createArguments);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Неудачная активация")
    void activationFail(@SuppressWarnings("unused") String name, Shop shop, boolean verifyLms) {
        LogisticsPointFilter filter = logisticsPointFilter(shop.getBusinessId());
        when(lmsClient.getLogisticsPoints(filter)).thenReturn(List.of());

        shopStatusService.activateFromStatus(shop, ShopStatus.NEED_SETTINGS);
        softly.assertThat(shop.getStatus()).isEqualTo(ShopStatus.NEED_SETTINGS);
        if (verifyLms) {
            verify(lmsClient).getLogisticsPoints(filter);
        }
    }

    @Nonnull
    private static Stream<Arguments> activationFail() {
        return Stream.of(
            Arguments.of("Без marketId", validDaasShop().setMarketId(null), false),
            Arguments.of("Без balanceClientId", validDaasShop().setBalanceClientId(null), false),
            Arguments.of("Без taxSystem", validDaasShop().setTaxSystem(null), false),
            Arguments.of("Без balanceContractId", validDaasShop().setBalanceContractId(null), false),
            Arguments.of("Без balancePersonId", validDaasShop().setBalancePersonId(null), false),
            Arguments.of("Без balanceProductId", validDaasShop().setBalanceProductId(null), false),
            Arguments.of("Без сендеров", validDaasShop().setSenders(List.of()), false),
            Arguments.of("Без склада", validDaasShop(), true)
        );
    }

    @Nonnull
    private static Arguments createArguments(List<Pair<String, Consumer<DaasActivationTest>>> namedMethodCallPairs) {
        List<String> methodNames = namedMethodCallPairs.stream().map(Pair::getLeft).collect(Collectors.toList());
        List<Consumer<DaasActivationTest>> methodCalls = namedMethodCallPairs.stream()
            .map(Pair::getRight)
            .collect(Collectors.toList());

        String displayName = namedMethodCallPairs.stream().map(Pair::getLeft).collect(Collectors.joining(", "));

        // если последним из всех методов вызывается метод создания склада,
        // то проверка количества складов магазина в методе ShopService#checkAndActivate делается два раза,
        // иначе - один раз
        int lastMethodIndex = methodNames.size() - 1;
        int invocationsCount = "createWarehouse".equals(methodNames.get(lastMethodIndex)) ? 2 : 1;
        return Arguments.of(displayName, methodCalls, invocationsCount);
    }

    @SneakyThrows
    private void createProduct() {
        mockMvc.perform(post("/internal/shops/1/create-product"));

    }

    @SneakyThrows
    private void createSender() {
        mockMvc.perform(
            post("/back-office/senders")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/sender/create_sender_request.json"))
        );
    }

    @SneakyThrows
    private void createWarehouse() {
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(41L))).thenReturn(List.of(
            LmsFactory.createLogisticsPointResponse(1L, null, null, "name", PointType.WAREHOUSE)
        ));
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(100L)))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(1L,  null, null, "name", PointType.WAREHOUSE)
            ));
        when(lmsClient.getLogisticsPoints(refEq(createLogisticsPointFilter())))
            .thenReturn(List.of(LOGISTICS_POINT_RESPONSE));
        mockMvc.perform(
            request(HttpMethod.POST, "/back-office/warehouses", warehouseCreateRequest())
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Override
    protected Shop getShop() {
        return validDaasShop();
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter(Long businessId) {
        return LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .active(true)
            .hasPartner(false)
            .businessIds(Set.of(businessId))
            .build();
    }
}

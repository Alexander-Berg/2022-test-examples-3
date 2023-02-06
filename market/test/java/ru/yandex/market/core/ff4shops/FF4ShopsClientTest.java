package ru.yandex.market.core.ff4shops;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.stocks.FF4ShopsClientImpl;
import ru.yandex.market.ff4shops.api.model.CourierDto;
import ru.yandex.market.ff4shops.api.model.CourierWithDeadlineDTO;
import ru.yandex.market.ff4shops.api.model.CouriersWithDeadlinesResponse;
import ru.yandex.market.ff4shops.api.model.auth.ClientRole;
import ru.yandex.market.ff4shops.api.model.order.DisabledRemovingFromOrderReason;
import ru.yandex.market.ff4shops.api.model.order.DisabledRemovingItemReason;
import ru.yandex.market.ff4shops.api.model.order.OrderItemRemovalPermissionDto;
import ru.yandex.market.ff4shops.api.model.order.OrderRemovalPermissionsDto;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class FF4ShopsClientTest {
    protected String uri = "localhost";
    protected FF4ShopsClient client;
    protected MockRestServiceServer mockServer;

    private ObjectMapper mapper = new ObjectMapper();

    {
        mapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void setUp() {
        RestTemplate ff4ShopsRestTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(ff4ShopsRestTemplate);
        client = new FF4ShopsClientImpl(uri, ff4ShopsRestTemplate, new ObjectMapper());
    }

    @AfterEach
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    @DisplayName("Получение разрешений на удаление товаров из заказа")
    void getRemovalPermissions() {
        long partnerId = 123;
        long orderId = 456;

        String body = StringTestUtil.getString(this.getClass(),"stocks/get_removal_permissions.json");

        mockServer.expect(requestTo(startsWith(uri + "/orders/" + orderId + "/removalPermissions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("clientId", String.valueOf(partnerId)))
                .andExpect(queryParam("shopId", String.valueOf(partnerId)))
                .andExpect(queryParam("clientRole", ClientRole.SHOP.name()))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        OrderRemovalPermissionsDto removalPermissions = client.getRemovalPermissions(partnerId, orderId);
        assertEquals(orderId, removalPermissions.getOrderId());
        assertTrue(removalPermissions.isRemovalAllowed());
        assertEquals(100, removalPermissions.getMaxTotalPercentRemovable().intValue());
        Set<OrderItemRemovalPermissionDto> permissions = removalPermissions.getItemRemovalPermissions();
        assertTrue(CollectionUtils.isNonEmpty(permissions));
        assertEquals(1, permissions.size());

        assertTrue(
                permissions.contains(
                        OrderItemRemovalPermissionDto.builder().setItemId(1L).setRemovalAllowed(true).build()
                )
        );
    }

    @Test
    @DisplayName("Получение запретов на удаление товара из заказа")
    void getRemovalPermissionsDenialItem() {
        long partnerId = 123;
        long orderId = 456;

        String body = StringTestUtil.getString(this.getClass(),"stocks/get_removal_permissions_denial_item.json");

        mockServer.expect(requestTo(startsWith(uri + "/orders/" + orderId + "/removalPermissions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("clientId", String.valueOf(partnerId)))
                .andExpect(queryParam("shopId", String.valueOf(partnerId)))
                .andExpect(queryParam("clientRole", ClientRole.SHOP.name()))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        OrderRemovalPermissionsDto removalPermissions = client.getRemovalPermissions(partnerId, orderId);
        assertEquals(orderId, removalPermissions.getOrderId());
        assertTrue(removalPermissions.isRemovalAllowed());
        assertEquals(50, removalPermissions.getMaxTotalPercentRemovable().intValue());
        Set<OrderItemRemovalPermissionDto> permissions = removalPermissions.getItemRemovalPermissions();
        assertTrue(CollectionUtils.isNonEmpty(permissions));
        assertEquals(2, permissions.size());

        assertTrue(
                permissions.contains(
                        OrderItemRemovalPermissionDto.builder()
                                .setItemId(1L)
                                .setRemovalAllowed(true)
                                .build()
                )
        );
        assertTrue(
                permissions.contains(
                        OrderItemRemovalPermissionDto.builder()
                                .setItemId(2L)
                                .setRemovalAllowed(false)
                                .setDisabledReasons(Collections.singleton(DisabledRemovingItemReason.NOT_ALLOWED_PROMO))
                                .build()
                )
        );
    }

    @Test
    @DisplayName("Получение запретов на удаление товаров из всего заказа")
    void getRemovalPermissionsDenialOrder() {
        long partnerId = 123;
        long orderId = 456;

        String body = StringTestUtil.getString(this.getClass(),"stocks/get_removal_permissions_denial_order.json");

        mockServer.expect(requestTo(startsWith(uri + "/orders/" + orderId + "/removalPermissions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("clientId", String.valueOf(partnerId)))
                .andExpect(queryParam("shopId", String.valueOf(partnerId)))
                .andExpect(queryParam("clientRole", ClientRole.SHOP.name()))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        OrderRemovalPermissionsDto removalPermissions = client.getRemovalPermissions(partnerId, orderId);
        assertEquals(orderId, removalPermissions.getOrderId());
        assertFalse(removalPermissions.isRemovalAllowed());
        Set<DisabledRemovingFromOrderReason> disabledReasons = removalPermissions.getDisabledReasons();
        assertTrue(CollectionUtils.isNonEmpty(disabledReasons));
        assertEquals(1, disabledReasons.size());
        assertTrue(disabledReasons.contains(DisabledRemovingFromOrderReason.NOT_ALLOWED_PAYMENT_TYPE));

        assertEquals(0, removalPermissions.getMaxTotalPercentRemovable().intValue());
        Set<OrderItemRemovalPermissionDto> permissions = removalPermissions.getItemRemovalPermissions();
        assertTrue(CollectionUtils.isNonEmpty(permissions));
        assertEquals(2, permissions.size());

        assertTrue(
                permissions.contains(
                        OrderItemRemovalPermissionDto.builder()
                                .setItemId(1L)
                                .setRemovalAllowed(false)
                                .setDisabledReasons(
                                        Collections.singleton(DisabledRemovingItemReason.NOT_ALLOWED_BY_ORDER)
                                )
                                .build()
                )
        );
        assertTrue(
                permissions.contains(
                        OrderItemRemovalPermissionDto.builder()
                                .setItemId(2L)
                                .setRemovalAllowed(false)
                                .setDisabledReasons(
                                        Collections.singleton(DisabledRemovingItemReason.NOT_ALLOWED_BY_ORDER)
                                )
                                .build()
                )
        );
    }

    @Test
    @DisplayName("Получение дедлайнов по заказам")
    void getOrdersDeadlines() {
        List<Long> orderIds = List.of(33016282L,33016402L);
        String body = StringTestUtil.getString(this.getClass(),"orders/orders_deadlines.json");

        mockServer.expect(requestTo(startsWith(uri + "/orders/deadlines/get-by-ids")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("orderIds",
                                      String.valueOf(orderIds.get(0)), String.valueOf(orderIds.get(1))))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        Map<Long, Instant> deadlines = client.getDeadlinesByOrderIds(orderIds);
        assertEquals(deadlines.get(33016282L), Instant.ofEpochSecond(1644580402));
        assertEquals(deadlines.get(33016402L), Instant.ofEpochSecond(1644585180));
    }

    @Test
    @DisplayName("Получение информации по заказам")
    void getOrdersInfo() throws IOException {
        List<Long> orderIds = List.of(94616251L, 94650671L, 94656852L);
        String body = StringTestUtil.getString(this.getClass(),"orders/orders_info_ff4shops_response.json");

        mockServer.expect(requestTo(startsWith(uri + "/orders/extend-info/get-by-ids")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("orderIds",
                        String.valueOf(orderIds.get(0)), String.valueOf(orderIds.get(1)),
                                String.valueOf(orderIds.get(2))))
                .andRespond(
                        withStatus(OK)
                                .body(body)
                                .contentType(APPLICATION_JSON)
                );

        Map<Long, CourierWithDeadlineDTO> mapResponse = client.getCouriersWithDeadlines(orderIds);
        Map<Long, CourierWithDeadlineDTO> mapExpect = mapper.readValue(
                StringTestUtil.getString(this.getClass(),"orders/orders_info_ff4shops_expected_map.json"),
                CouriersWithDeadlinesResponse.class).getCouriersWithDeadlines();
        mapExpect.values().forEach(orderInfo -> {
            CourierWithDeadlineDTO courierResponse = mapResponse.get(orderInfo.getOrderId());
            assertNotNull(courierResponse);
            assertEquals(orderInfo.getDeadline(), courierResponse.getDeadline());
            assertCourier(orderInfo.getCourier(), courierResponse.getCourier());
        });
    }

    private void assertCourier(CourierDto expectedCourier, CourierDto responseCourier) {
        if (expectedCourier == null) {
            assertNull(responseCourier);
        } else {
            assertEquals(expectedCourier.getFirstName(), responseCourier.getFirstName());
            assertEquals(expectedCourier.getLastName(), responseCourier.getLastName());
            assertEquals(expectedCourier.getMiddleName(), responseCourier.getMiddleName());
            assertEquals(expectedCourier.getVehicleNumber(), responseCourier.getVehicleNumber());
            assertEquals(expectedCourier.getVehicleDescription(), responseCourier.getVehicleDescription());
            assertEquals(expectedCourier.getPhoneNumber(), responseCourier.getPhoneNumber());
            assertEquals(expectedCourier.getPhoneExtension(), responseCourier.getPhoneExtension());
            assertEquals(expectedCourier.getCourierType(), responseCourier.getCourierType());
            assertEquals(expectedCourier.getUrl(), responseCourier.getUrl());
            assertEquals(expectedCourier.getElectronicAcceptanceCertificateCode(),
                    responseCourier.getElectronicAcceptanceCertificateCode());
            assertEquals(expectedCourier.getElectronicAcceptCodeRequired(),
                    responseCourier.getElectronicAcceptCodeRequired());
            assertEquals(expectedCourier.getElectronicAcceptCodeStatus(),
                    responseCourier.getElectronicAcceptCodeStatus());
        }
    }
}

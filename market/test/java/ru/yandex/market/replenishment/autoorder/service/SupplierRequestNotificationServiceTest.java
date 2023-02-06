package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.SupplierRequestNotificationInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
public class SupplierRequestNotificationServiceTest extends FunctionalTest {

    private static final int MESSAGES_NUMBER = 20;

    @Autowired
    private SupplierRequestNotificationService notificationService;

    private MockWebServer mockWebServer;

    @Before
    public void setUp() {
        this.mockWebServer = new MockWebServer();
        ReflectionTestUtils
                .setField(notificationService, "templateId", RandomStringUtils.random(3));
        ReflectionTestUtils
                .setField(notificationService, "sendMessageToShopUrl", mockWebServer.url("/").toString());
    }

    private void fillResponses(MockResponse response) {
        IntStream.range(0, MESSAGES_NUMBER)
                .mapToObj(i -> response.clone())
                .forEach(mockWebServer::enqueue);
    }

    @Test
    public void testNotificationIsOk() {
        fillResponses(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("OK")
                .setResponseCode(200));
        final Long supplierId = 123L;
        executeNotification(supplierId)
                .forEach(result -> {
                    assertNotNull(result);
                    assertNull(result.getError());
                    assertNotNull(result.getInfo());
                    assertEquals(supplierId, result.getInfo().getSupplierId());
                });
    }

    @Test
    public void testNotificationIsOkWithoutBody() {
        fillResponses(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setResponseCode(200));
        final Long supplierId = 123L;
        executeNotification(supplierId)
                .forEach(result -> {
                    assertNotNull(result);
                    assertNull(result.getError());
                    assertNotNull(result.getInfo());
                    assertEquals(supplierId, result.getInfo().getSupplierId());
                });
    }

    @Test
    public void testNotificationFailWithBadRequest() {
        fillResponses(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("Error")
                .setResponseCode(404));

        final Long supplierId = 123L;
        executeNotification(supplierId)
                .forEach(result -> {
                    assertNotNull(result);
                    assertNotNull(result.getInfo());
                    assertEquals("Error sending message with status code 404", result.getError());
                    assertEquals(supplierId, result.getInfo().getSupplierId());
                });
    }

    @Test
    public void testNotificationFailWithBadRequestWithoutBody() {
        fillResponses(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setResponseCode(404));

        final Long supplierId = 123L;
        executeNotification(supplierId)
                .forEach(result -> {
                    assertNotNull(result);
                    assertNotNull(result.getInfo());
                    assertEquals("Error sending message with status code 404", result.getError());
                    assertEquals(supplierId, result.getInfo().getSupplierId());
                });
    }

    @Test
    public void testNotificationFailWithTimeoutException() {
        ReflectionTestUtils
                .setField(notificationService, "sendMessageToShopUrl", "test/test");
        final Long supplierId = 123L;
        executeNotification(supplierId)
                .forEach(result -> {
                    assertNotNull(result);
                    assertNotNull(result.getInfo());
                    assertEquals("Unexpected error null", result.getError());
                    assertEquals(supplierId, result.getInfo().getSupplierId());
                });
    }

    private SupplierRequestNotificationService.NotificationInfoAndError getCompletedNotification(
            CompletableFuture<SupplierRequestNotificationService.NotificationInfoAndError> futureToComplete) {
        if (futureToComplete != null) {
            try {
                return futureToComplete.join();
            } catch (Exception e) {
                throw new AssertionError("Fail to complete task");
            }
        }
        return null;
    }

    private Stream<SupplierRequestNotificationService.NotificationInfoAndError> executeNotification(Long supplierId) {
        final LocalDate now = LocalDate.now();
        return IntStream.range(0, MESSAGES_NUMBER)
                .mapToObj(i -> makeInfo(supplierId, 10L + i))
                .map(info -> notificationService.notifySupplierAboutRequest(info, now, now))
                .collect(Collectors.toList())
                .stream()
                .map(this::getCompletedNotification);
    }

    private SupplierRequestNotificationInfo makeInfo(long supplierId, long supplierRequestId) {
        final SupplierRequestNotificationInfo info = new SupplierRequestNotificationInfo();
        info.setSupplierId(supplierId);
        info.setRequestId(supplierRequestId);
        info.setRequestIdString("RPL-" + supplierRequestId);
        info.setSupplierName(RandomStringUtils.random(3));
        info.setProductNumber(1L);
        info.setWarehouseName(RandomStringUtils.random(3));
        info.setProductQuantity(1L);
        info.setCampaignId(1L);
        return info;
    }

}

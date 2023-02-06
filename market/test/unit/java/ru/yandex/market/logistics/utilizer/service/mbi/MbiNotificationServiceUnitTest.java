package ru.yandex.market.logistics.utilizer.service.mbi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.service.mbi.entity.MbiDataFields;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.eq;

public class MbiNotificationServiceUnitTest extends SoftAssertionSupport {

    private MbiNotificationService mbiNotificationService;
    private MbiApiClient mbiApiClient;

    @BeforeEach
    public void init() {
        mbiApiClient = Mockito.mock(MbiApiClient.class);
        mbiNotificationService = new MbiNotificationService(mbiApiClient);
    }

    @Test
    public void sendNotification() {
        MbiDataFields mbiDataFields = MbiDataFields.builder()
                .supplierName("Ромашка")
                .itemsCount("12")
                .itemsTotalCost("28 000")
                .deadline("20.12.2020")
                .build();
        mbiNotificationService.sendNotification(1, 2, mbiDataFields);

        String expectedData =
                "<data>" +
                "<supplier-name>Ромашка</supplier-name>" +
                "<items-count>12</items-count>" +
                "<items-total-cost>28 000</items-total-cost>" +
                "<deadline>20.12.2020</deadline>" +
                "</data>";
        Mockito.verify(mbiApiClient).sendMessageToSupplier(eq(2L), eq(1), eq(expectedData));
    }
}

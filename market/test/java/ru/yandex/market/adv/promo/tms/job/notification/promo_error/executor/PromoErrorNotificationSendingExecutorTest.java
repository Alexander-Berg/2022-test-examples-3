package ru.yandex.market.adv.promo.tms.job.notification.promo_error.executor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.tms.job.notification.promo_error.dao.PromoErrorYTDao;
import ru.yandex.market.adv.promo.tms.job.notification.promo_error.model.PromoDescriptionError;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class PromoErrorNotificationSendingExecutorTest extends FunctionalTest {
    private static final int PROMO_ERROR_NOTIFICATION_TYPE_ID = 1606180000;

    @Autowired
    private PromoErrorNotificationSendingExecutor executor;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private PromoErrorYTDao promoErrorYTDao;

    @Test
    void testEmptyErrors() {
        when(promoErrorYTDao.selectUnfinishedPromoErrors()).thenReturn(Collections.emptyList());
        executor.doJob(null);
        Mockito.verify(mbiApiClient, never()).
                sendNotificationToEmail(anyString(), eq(PROMO_ERROR_NOTIFICATION_TYPE_ID), anyString());
    }

    @Test
    void testSomeErrors() {
        when(promoErrorYTDao.selectUnfinishedPromoErrors()).thenReturn(
                List.of(
                        new PromoDescriptionError("id1", "promo1", "error1\n", "responsible1"),
                        new PromoDescriptionError("id2", "promo2", "error2", "responsible2"),
                        new PromoDescriptionError("id3", "promo3", "error3", "responsible1")
                )
        );
        when(mbiApiClient.sendNotificationToEmail(anyString(), eq(PROMO_ERROR_NOTIFICATION_TYPE_ID), anyString())).
                thenReturn(new SendNotificationResponse(1L));
        executor.doJob(null);

        ArgumentCaptor<String> recipients = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramsXML = ArgumentCaptor.forClass(String.class);

        Mockito.verify(mbiApiClient, times(6)).
                sendNotificationToEmail(recipients.capture(), eq(PROMO_ERROR_NOTIFICATION_TYPE_ID), paramsXML.capture());
        Set<String> actual = Set.of(
                recipients.getAllValues().get(0) + " -> " + paramsXML.getAllValues().get(0),
                recipients.getAllValues().get(1) + " -> " + paramsXML.getAllValues().get(1),
                recipients.getAllValues().get(2) + " -> " + paramsXML.getAllValues().get(2),
                recipients.getAllValues().get(3) + " -> " + paramsXML.getAllValues().get(3),
                recipients.getAllValues().get(4) + " -> " + paramsXML.getAllValues().get(4),
                recipients.getAllValues().get(5) + " -> " + paramsXML.getAllValues().get(5)
        );
        Set<String> expected = Set.of(
                "responsible1@yandex-team.ru -> <errors>id1 - promo1 : error1\nid3 - promo3 : error3\n</errors>",
                "cat@yandex-team.ru -> <errors>id1 - promo1 : error1\nid3 - promo3 : error3\n</errors>",
                "dog@yandex-team.ru -> <errors>id1 - promo1 : error1\nid3 - promo3 : error3\n</errors>",
                "responsible2@yandex-team.ru -> <errors>id2 - promo2 : error2\n</errors>",
                "cat@yandex-team.ru -> <errors>id2 - promo2 : error2\n</errors>",
                "dog@yandex-team.ru -> <errors>id2 - promo2 : error2\n</errors>"
        );
        assertEquals(expected, actual);
    }
}

package ru.yandex.market.mbi.api.controller.outlet;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.cutoff.CutoffNotificationStatus;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.outlets2.BulkOutletModerationRequestDTO;
import ru.yandex.market.mbi.api.client.entity.outlets2.ClosedOutletBulkDTO;
import ru.yandex.market.mbi.api.client.entity.outlets2.ClosedOutletDTO;
import ru.yandex.market.mbi.api.client.entity.outlets2.OutletModerationRequestDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.ModerationInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.ModerationLevelDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "OutletFunctionalTest.before.csv")
class OutletModerationFunctionalTest extends FunctionalTest {
    @Autowired
    private RestMbiApiClient mbiApiClient;
    @Autowired
    private ObjectMapper mbiApiObjectMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Test
    void xmlSerializationTest() throws IOException {
        OutletModerationRequestDTO outletModerationRequest1 =
                getOutletModeration(1, OutletStatus.FAILED, "test", 15000000000L);

        OutletModerationRequestDTO outletModerationRequest2 =
                getOutletModeration(1, OutletStatus.MODERATED, "test",
                        System.currentTimeMillis() + 100000000L);

        BulkOutletModerationRequestDTO bulkOutletModerationRequest = new BulkOutletModerationRequestDTO(
                ImmutableList.of(outletModerationRequest1
                        , outletModerationRequest2
                ));

        String requestXML = mbiApiObjectMapper.writeValueAsString(bulkOutletModerationRequest);

        String url = baseUrl() + "/outlets/moderate/v2";
        String actual = FunctionalTestHelper.postForXml(url, requestXML);
        String expected =
                // language=xml
                "<closed-outlets>" +
                        "<closed-outlet outlet-id=\"1\" status=\"IGNORED\" notification-status=\"NOT_SENT\"/>" +
                        "<closed-outlet outlet-id=\"1\" status=\"OK\" notification-status=\"NOT_SENT\"/>" +
                        "</closed-outlets>";
        MbiAsserts.assertXmlEquals(expected, actual);
    }

    /**
     * Тест модерации пустого списка аутлетов.
     */
    @Test
    void emptyModerationRequest() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> mbiApiClient.moderateOutlets(Collections.emptyList())
        );
    }

    @Test
    void checkReasonChangeStatusFromFailedToAtModerationTest() {
        moderateOutlet(OutletStatus.FAILED, "Failed moderation",
                new ModerationInfoDTO("Failed moderation", ModerationLevelDTO.MODERATION_LEVEL_2));

        ModerationInfoDTO expected = new ModerationInfoDTO("", ModerationLevelDTO.MODERATION_LEVEL_2);
        moderateOutlet(OutletStatus.AT_MODERATION, "at moderation", expected);
        moderateOutlet(OutletStatus.MODERATED, "moderated", expected);
    }

    @Test
    void checkReasonChangeStatusFromFailedToModeratedTest() {
        moderateOutlet(OutletStatus.AT_MODERATION, "at moderation", null);
        moderateOutlet(OutletStatus.MODERATED, "moderated", null);
    }

    @Test
    void dontSendNotificationTest() {
        moderateOutlets(1L, OutletStatus.MODERATED, CutoffNotificationStatus.NOT_SENT, false);
    }

    @Test
    void check() {
        OutletModerationRequestDTO outletModerationRequest1 =
                getOutletModeration(1, OutletStatus.FAILED, "test1", Instant.now().toEpochMilli());
        OutletModerationRequestDTO outletModerationRequest2 =
                getOutletModeration(8, OutletStatus.FAILED, "test2", Instant.now().toEpochMilli());

        when(balanceService.getClient(anyLong())).thenReturn(new ClientInfo());

        ClosedOutletBulkDTO closedOutletBulkDTO =
                mbiApiClient.moderateOutlets(ImmutableList.of(outletModerationRequest1, outletModerationRequest2));
        List<Long> outletsId = closedOutletBulkDTO.getItems()
                .stream()
                .map(ClosedOutletDTO::getOutletId)
                .collect(Collectors.toList());

        Assertions.assertEquals(2, outletsId.size());
        Assertions.assertTrue(outletsId.contains(1L));
        Assertions.assertTrue(outletsId.contains(8L));

        verifyNotification(1L, true);
        verifyNotification(4L, true);
    }

    @Test
    @DisplayName("Должна отправляться нотификация, только когда в результате модерации есть проблемы")
    void dontSendNotification2Test() {
        moderateOutlets(2L, OutletStatus.FAILED, CutoffNotificationStatus.SENT, true);
        moderateOutlets(1L, OutletStatus.MODERATED, CutoffNotificationStatus.NOT_SENT, true);
    }

    @Test
    @DisplayName("Для несуществующего аутлета, статус (CutoffActionStatus) в ответе должен быть IGNORED")
    void checkIgnoredStatusFromResponse() {
        OutletModerationRequestDTO outletModerationRequest =
                getOutletModeration(888L, OutletStatus.FAILED, "test1", Instant.now().toEpochMilli());

        ClosedOutletBulkDTO response =
                mbiApiClient.moderateOutlets(ImmutableList.of(outletModerationRequest));

        assertThat(response.getItems(), hasSize(1));
        assertThat(response.getItems().get(0).getStatus(), is(CutoffActionStatus.IGNORED));
    }

    @Test
    @DisplayName("Проверка статуса аутлета, если было отправлено 2 одинаковых запроса с результатом модерации")
    void checkOutletStatusAfterDoubleModeration() {
        moderateOutlet(OutletStatus.FAILED, "Failed moderation",
                new ModerationInfoDTO("Failed moderation", ModerationLevelDTO.MODERATION_LEVEL_2));
        ModerationInfoDTO expected = new ModerationInfoDTO("", ModerationLevelDTO.MODERATION_LEVEL_2);
        moderateOutlet(OutletStatus.AT_MODERATION, "at moderation", expected);

        ClosedOutletDTO result1 = moderateOutlet(OutletStatus.MODERATED, "moderated", expected);
        assertThat("Для первого запроса ожидался статус OK", result1.getStatus(), is(CutoffActionStatus.OK));
        // повторный запрос не должен изменить статус аутлета
        ClosedOutletDTO result2 = moderateOutlet(OutletStatus.MODERATED, "moderated", expected);
        assertThat("Для второго запроса ожидался статус IGNORED", result2.getStatus(), is(CutoffActionStatus.IGNORED));
    }

    private ClosedOutletDTO moderateOutlet(OutletStatus outletStatus, String reason,
                                           @Nullable ModerationInfoDTO expected) {
        OutletModerationRequestDTO outletModerationRequest =
                getOutletModeration(1L, outletStatus, reason, Instant.now().toEpochMilli());

        ClosedOutletBulkDTO result = mbiApiClient.moderateOutlets(ImmutableList.of(outletModerationRequest));

        PagedOutletsDTO outletsV2 = mbiApiClient.getOutletsV2(1L, 1L, null, null, null, null, null, null, null);

        OutletInfoDTO actual = outletsV2.getOutlets().get(0);

        Assertions.assertEquals(expected, actual.getModerationInfo());
        Assertions.assertEquals(outletStatus, actual.getOutletStatus());
        assertThat(result.getItems(), hasSize(1));
        return result.getItems().get(0);
    }

    private void moderateOutlets(long outletId, OutletStatus status,
                                 CutoffNotificationStatus notificationStatus,
                                 boolean sendNotification) {
        OutletModerationRequestDTO outletModerationRequest =
                getOutletModeration(outletId, status, "test", Instant.now().toEpochMilli());

        ClosedOutletBulkDTO closedOutletBulkDTO =
                mbiApiClient.moderateOutlets(ImmutableList.of(outletModerationRequest));

        ClosedOutletDTO closedOutletExpected =
                new ClosedOutletDTO(outletId, CutoffActionStatus.OK, notificationStatus, null);

        Assertions.assertEquals(closedOutletExpected, closedOutletBulkDTO.getItems().get(0));
        verifyNotification(1L, sendNotification);
    }

    @Nonnull
    private OutletModerationRequestDTO getOutletModeration(
            long outletId, OutletStatus status, String reason, long time
    ) {
        return new OutletModerationRequestDTO(outletId, 100, status, reason, time, time);
    }

    private void verifyNotification(long partnerId, boolean sendNotification) {
        Mockito.verify(notificationService, times(sendNotification ? 1 : 0))
                .send(Mockito.eq(93), Mockito.eq(partnerId), anyList());
    }
}

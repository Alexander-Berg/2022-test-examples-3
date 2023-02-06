package ru.yandex.market.partner.message;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.message.db.DbMessageService;
import ru.yandex.market.core.security.model.DualUidable;
import ru.yandex.market.partner.notification.client.model.GetMessageHeadersResponse;
import ru.yandex.market.partner.notification.client.model.MessageDTO;
import ru.yandex.market.partner.notification.client.model.MessageHeaderDTO;
import ru.yandex.market.partner.notification.client.model.PagerDTO;
import ru.yandex.market.partner.notification.client.model.PriorityDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Базовый тест для тестирования сервантлетов, которые извлекают клиента из баланса.
 *
 * @author avetokhin 16/11/16.
 */
public abstract class AbstractClientBasedTest extends FunctionalTest {
    protected static final long USER_ID = 1L;
    protected static final long USER_ID_NO_CLIENT = 2L;
    protected static final long USER_ID_NO_BALANCE = 3L;
    protected static final long AGENCY_USER_ID = 4L;

    protected static final long CLIENT_ID = 1L;
    protected static final long AGENCY_CLIENT_ID = 2L;
    protected static final long CLIENT_ID_NO_BALANCE = 3L;
    protected static final long INVALID_CLIENT_ID = -1L;
    protected static final long AGENCY_ID = 13L;

    protected static final ClientInfo CLIENT_INFO = new ClientInfo(CLIENT_ID, null, false, 0);

    protected static AgencyService getAgencyService() {
        AgencyService agencyService = mock(AgencyService.class);
        when(agencyService.isAgency(eq(CLIENT_ID))).thenReturn(false);
        when(agencyService.isAgency(eq(AGENCY_CLIENT_ID))).thenReturn(true);
        return agencyService;
    }

    protected static ContactService getContactService() {
        ContactService contactService = mock(ContactService.class);
        when(contactService.getClientIdByUid(eq(USER_ID))).thenReturn(CLIENT_ID);
        when(contactService.getClientIdByUid(eq(USER_ID_NO_CLIENT))).thenReturn(INVALID_CLIENT_ID);
        when(contactService.getClientIdByUid(eq(USER_ID_NO_BALANCE))).thenReturn(CLIENT_ID_NO_BALANCE);
        when(contactService.getClientIdByUid(eq(AGENCY_USER_ID))).thenReturn(AGENCY_CLIENT_ID);
        return contactService;
    }

    protected static PartnerNotificationClient partnerNotificationClient() {
        PartnerNotificationClient partnerNotificationClient = mock(PartnerNotificationClient.class);
        GetMessageHeadersResponse messageHeadersResponse = new GetMessageHeadersResponse().headers(
        List.of(new MessageHeaderDTO()
                .messageId(1L)
                .sentTime(OffsetDateTime.of(2022, 2, 22, 22, 22, 22, 0, ZoneOffset.UTC))
                .subject("Test subject")))
                .pager(new PagerDTO().currentPage(0).pageSize(10).itemCount(5));
        when(partnerNotificationClient.getMessageHeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(messageHeadersResponse);

        when(partnerNotificationClient.getMessage(any(), any(), any()))
                .thenReturn(new MessageDTO()
                        .id(101L)
                        .sentTime(OffsetDateTime.of(2022, 2, 22, 22, 22, 22, 0, ZoneOffset.UTC))
                        .priority(PriorityDTO.LOW)
                        .body("message body")
                        .subject("message subject")
                        .shopId(1001L)
                        .priority(PriorityDTO.LOW)
                );


        return partnerNotificationClient;
    }

    @FunctionalInterface
    interface MakeTest {
        void test(
                AbstractMessageServantlet servantlet,
                DbMessageService messageService,
                MockServResponse response
        );
    }

    interface Request extends ServRequest, DualUidable {
    }
}

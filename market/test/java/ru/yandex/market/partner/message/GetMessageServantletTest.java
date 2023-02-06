package ru.yandex.market.partner.message;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.message.PartnerNotificationMessageService;
import ru.yandex.market.core.message.db.DbMessageService;
import ru.yandex.market.core.message.model.MessageClass;
import ru.yandex.market.core.message.model.NotificationMessage;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link GetMessageServantletTest}.
 *
 * @author avetokhin 28/10/16.
 */
public class GetMessageServantletTest extends AbstractClientBasedTest {

    private static final String MESSAGE_ID = "101";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final String CAMPAIGN_ID = "777";
    private static final String SHOP_ID = "666";

    /**
     * Проверить вызовы сервиса с проксированием в partner-notification.
     */
    @Test
    public void testServicePnProxyCall() {
        test((servantlet, messageService, response) -> {

            Request request = mock(Request.class);
            when(request.getEffectiveUid()).thenReturn(USER_ID);
            when(request.getParam(eq("message_id"), anyBoolean())).thenReturn(MESSAGE_ID);
            when(request.getParam(eq("campaign_id"), anyBoolean())).thenReturn(CAMPAIGN_ID);
            when(request.getParam(eq("client_id"), anyBoolean())).thenReturn(String.valueOf(CLIENT_ID));

            servantlet.process(request, response);

            assertThat(response.getErrors()).isEmpty();

            verify(partnerNotificationClient).getMessage(
                    eq(Long.valueOf(MESSAGE_ID)),
                    eq(USER_ID),
                    eq(USER_ID)
            );

            NotificationMessage expected = new NotificationMessage();
            expected.setId(101L);
            expected.setSubject("message subject");
            expected.setBody("message body");
            expected.setTemplateId(0);
            expected.setSentDate(new Date(1645568542000L));
            expected.setImportance(NotificationPriority.LOW);
            expected.setMessageClass(MessageClass.NOTIFICATION);
            expected.setShopId(1001L);
            assertThat(response.getData().get(0)).isEqualTo(expected);
        });
    }

    private void test(MakeTest test) {
        var messageService = mock(DbMessageService.class);
        var campaignInfo = new CampaignInfo(Long.parseLong(CAMPAIGN_ID), Long.parseLong(SHOP_ID), 0, 0);
        var campaignService = mock(CampaignService.class);
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);
        partnerNotificationClient = partnerNotificationClient();
        var servantlet = new GetMessageServantlet(
                new PartnerNotificationMessageService(
                        partnerNotificationClient,
                        messageService
                ));
        servantlet.setDateFormat(DateUtil.newThreadLocalOfSimpleDateFormat(DATE_FORMAT));
        var response = new MockServResponse();
        test.test(servantlet, messageService, response);
    }
}

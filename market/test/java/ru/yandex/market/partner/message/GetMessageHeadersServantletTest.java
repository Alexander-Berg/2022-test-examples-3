package ru.yandex.market.partner.message;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.framework.pager.Pager;
import ru.yandex.market.core.message.PartnerNotificationMessageService;
import ru.yandex.market.core.message.db.DbMessageService;
import ru.yandex.market.core.message.model.Header;
import ru.yandex.market.mbi.common.Mbi;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.partner.notification.client.model.PriorityDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link GetMessageHeadersServantlet}.
 *
 * @author avetokhin 28/10/16.
 */
public class GetMessageHeadersServantletTest extends AbstractClientBasedTest {

    private static final int PAGE = 1;
    private static final int PAGE_SIZE = 10;
    private static final String FROM_DATE_TIME = "2015-07-29 07:30:52,000";
    private static final String TO_DATE_TIME = "2015-08-29 07:30:52,000";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final String IMPORTANCES = "1,2";
    private static final String CAMPAIGN_ID = "777";
    private static final String SHOP_ID = "666";
    private static final String THEME_ID = "1";

    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    /**
     * Проверить вызовы сервиса с проксированием в partner-notification.
     */
    @Test
    @DbUnitDataSet(before = {
            "GetMessageHeadersServantletTest.before.csv"
    })
    public void testServicePnProxyCall() {
        test((servantlet, messageService, response) -> {

            Request request = mock(Request.class);
            when(request.getEffectiveUid()).thenReturn(USER_ID);
            when(request.getParamAsInt(eq("p"), anyInt())).thenReturn(PAGE);
            when(request.getParamAsInt(eq("page_size"), anyInt())).thenReturn(PAGE_SIZE);
            when(request.getParam(eq("fdt"), anyBoolean())).thenReturn(FROM_DATE_TIME);
            when(request.getParam(eq("tdt"), anyBoolean())).thenReturn(TO_DATE_TIME);
            when(request.getParam(eq("importance"), anyBoolean())).thenReturn(IMPORTANCES);
            when(request.getParam(eq("campaign_id"), anyBoolean())).thenReturn(CAMPAIGN_ID);
            when(request.getParam(eq("client_id"), anyBoolean())).thenReturn(String.valueOf(CLIENT_ID));
            when(request.getParam(eq("theme_id"), anyBoolean())).thenReturn(THEME_ID);

            servantlet.process(request, response);

            assertThat(response.getErrors()).isEmpty();
            verify(partnerNotificationClient).getMessageHeaders(
                    eq(USER_ID),
                    eq(USER_ID),
                    eq(parseDate(FROM_DATE_TIME).toInstant().atZone(Mbi.DEFAULT_TIME_ZONE).toOffsetDateTime()),
                    eq(parseDate(TO_DATE_TIME).toInstant().atZone(Mbi.DEFAULT_TIME_ZONE).toOffsetDateTime()),
                    eq(Long.parseLong(SHOP_ID)),
                    eq(Long.parseLong(THEME_ID)),
                    argThat(arg -> arg.size() == 2 &&
                            arg.containsAll(Arrays.asList(PriorityDTO.NORMAL, PriorityDTO.HIGH))),
                    eq(Collections.emptyList()),
                    eq(Integer.valueOf(PAGE)),
                    eq(PAGE_SIZE)
            );

            Header expected = new Header(1L, new Date(1645568542000L), "Test subject", NotificationPriority.NORMAL);
            Pager expectedPager = new Pager(0, PAGE_SIZE);
            expectedPager.setItemCount(5);
            assertThat(response.getData().get(0)).asList()
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
            assertThat(response.getData().get(1))
                    .usingRecursiveComparison()
                    .isEqualTo(expectedPager);
        });
    }

    private Date parseDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void test(MakeTest test) {
        partnerNotificationClient = partnerNotificationClient();
        var messageService = mock(DbMessageService.class);
        var campaignInfo = new CampaignInfo(Long.parseLong(CAMPAIGN_ID), Long.parseLong(SHOP_ID), 0, 0);
        var campaignService = mock(CampaignService.class);
        var removeClientEnvironmentService = mock(RemoveClientEnvironmentService.class);
        when(campaignService.getMarketCampaign(anyLong())).thenReturn(campaignInfo);
        when(removeClientEnvironmentService.isNewAbstractClientServantletProcessing()).thenReturn(true);
        var servantlet = new GetMessageHeadersServantlet(
                new PartnerNotificationMessageService(
                        partnerNotificationClient,
                        messageService
                ),
                campaignService,
                getAgencyService(),
                getContactService(),
                removeClientEnvironmentService
        );
        servantlet.setDateFormat(DateUtil.newThreadLocalOfSimpleDateFormat(DATE_FORMAT));
        var response = new MockServResponse();
        test.test(servantlet, messageService, response);
    }
}

package ru.yandex.market.crm.platform.mappers;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.communication.proxy.client.model.RedirectInfoResponse;
import ru.yandex.market.communication.proxy.client.model.RedirectType;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.platform.models.CallProxy;
import ru.yandex.market.crm.util.CrmStrings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author zilzilok
 */
class CallProxyMapperTest {

    @InjectMocks
    private CallProxyMapper callProxyMapper;
    @Mock
    private CommunicationProxyClient communicationProxyClient;
    @Mock
    private PersonalService personalService;

    public static final String CALLER_NUM = "+79999999999";
    public static final String FAKE_CALLEE_NUM = "+78888888888";
    public static final String UID = "77777777777";
    public static final String REAL_CALLEE_NUM = "+" + UID;
    public static final String REAL_CALLEE_NUM_ID = "11111111111111111111111111111111";
    public static final String RECORD_ID = "11111111-1111-1111-1111-111111111111";
    public static final String STARTED = "2001-01-01T00:00:00Z";
    public static final String ENDED = "2001-01-01T00:01:00Z";
    public static final String RESOLUTION = "CallResolutionConnected";
    public static final Long ORDER_ID = 1L;
    private static final String CDR = "" +
            "{" +
            "  \"caller_num\":\"" + CALLER_NUM + "\"," +
            "  \"callee_num\":\"" + FAKE_CALLEE_NUM + "\"," +
            "  \"recording_id\":\"" + RECORD_ID + "\"," +
            "  \"call_started\": \"" + STARTED + "\"," +
            "  \"call_completed\": \"" + ENDED + "\"," +
            "  \"resolution\": \"" + RESOLUTION + "\"" +
            "}";

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMapping() {
        when(communicationProxyClient.getRedirect(FAKE_CALLEE_NUM)).thenReturn(createRedirectInfoResponse());
        when(personalService.retrievePhone(REAL_CALLEE_NUM_ID)).thenReturn(REAL_CALLEE_NUM);

        CallProxy callProxy = callProxyMapper.apply(CrmStrings.getBytes(CDR)).iterator().next();

        assertEquals(CALLER_NUM, callProxy.getCallerNum());
        assertEquals(REAL_CALLEE_NUM, callProxy.getRealCalleeNum());
        assertEquals(UID, callProxy.getUid().getStringValue());
        assertEquals(FAKE_CALLEE_NUM, callProxy.getFakeCalleeNum());
        assertEquals(RECORD_ID, callProxy.getRecordId());
        assertEquals(ORDER_ID, callProxy.getOrderId());
        assertEquals(getTimestamp(STARTED), callProxy.getStarted());
        assertEquals(getTimestamp(ENDED), callProxy.getEnded());
    }

    private static long getTimestamp(String text) {
        return ZonedDateTime.parse(text)
                .toInstant()
                .toEpochMilli();
    }

    private static RedirectInfoResponse createRedirectInfoResponse() {
        return new RedirectInfoResponse()
                .sourceNumber(FAKE_CALLEE_NUM)
                .targetNumberId(REAL_CALLEE_NUM_ID)
                .redirectType(RedirectType.DBS_ORDER)
                .orderId(ORDER_ID);
    }
}

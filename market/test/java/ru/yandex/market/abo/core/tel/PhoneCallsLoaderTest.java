package ru.yandex.market.abo.core.tel;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author kukabara
 */
public class PhoneCallsLoaderTest {

    @InjectMocks
    @Autowired
    PhoneCallsLoader loader;
    @Mock
    PhoneCallService phoneCallService;
    @Mock
    SipService sipService;

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void parseLine() {
        SipService sipService = new SipService();

        TelCall phoneCall = sipService.parseLine(
                "<a href=\"qas-925440203404611584.ogg\">qas-925440203404611584.ogg</a>                         " +
                        "28-Dec-2016 14:30              232406");
        assertNull(phoneCall);

        phoneCall = sipService.parseLine(
                "<a href=\"ViktoriaVasylieva-74993906011-20161228-154948.ogg\">" +
                        "ViktoriaVasylieva-74993906011-20161228-154948.ogg</a>  28-Dec-2016 12:52              436955");
        assertNotNull(phoneCall);
        assertEquals("ViktoriaVasylieva", phoneCall.getName());
        assertEquals("74993906011", phoneCall.getPhoneNumber());
        assertEquals(
                "http://cipt-helper3.yndx.net:8088/market/2016/12/28/ViktoriaVasylieva-74993906011-20161228-154948.ogg",
                phoneCall.getUrl()
        );

        phoneCall = sipService.parseLine(
                "<a href=\"EkaterinaZinovyeva-78123130323-20181001-130530.ogg\">EkaterinaZinovyeva-78123130323-20181001-130530.ogg</a>");
        assertNotNull(phoneCall);
    }

    @Test
    public void severalDays() {
        int days = 4;
        doReturn(DateUtils.addDays(new Date(), -days)).when(phoneCallService).getLastCallDate();

        loader.load();

        verify(sipService, times(days + 1)).loadCalls(any(Date.class));
    }
}

package ru.yandex.market.pers.notify.push;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author vtarasoff
 * @since 03.12.2020
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PusherServiceNameByPropertiesService.class)
@TestPropertySource(properties = {
    "xiva.service.name=default",
    "pokupki.xiva.service.name=pokupki",
    "lotalot.xiva.service.name=lotalot",
    "market.appId=1",
    "pokupki.appId=2"
})
public class PusherServiceNameByPropertiesServiceTest {
    @Autowired
    private PusherServiceNameService service;

    @Test
    public void shouldReturnNullAccountForNullService() {
        assertNull(service.accountBy(null));
    }

    @Test
    public void shouldReturnNullAccountForEmptyService() {
        assertNull(service.accountBy(""));
    }

    @Test
    public void shouldReturnNullAccountForUnknownService() {
        assertNull(service.accountBy("unknown"));
    }

    @Test
    public void shouldReturnMarketAccountForDefaultService() {
        assertEquals("market", service.accountBy("default"));
    }

    @Test
    public void shouldReturnBeruAccountForPokupkiService() {
        assertEquals("beru", service.accountBy("pokupki"));
    }

    @Test
    public void shouldReturnBringlyAccountForLotalotService() {
        assertEquals("bringly", service.accountBy("lotalot"));
    }

    @Test
    public void shouldReturnNullAppIdForNullService() {
        assertNull(service.appIdBy(null));
    }

    @Test
    public void shouldReturnNullAppIdForEmptyService() {
        assertNull(service.appIdBy(""));
    }

    @Test
    public void shouldReturnNullAppIdForUnknownService() {
        assertNull(service.appIdBy("unknown"));
    }

    @Test
    public void shouldReturnMarketAppIdForDefaultService() {
        assertEquals("1", service.appIdBy("default"));
    }

    @Test
    public void shouldReturnBeruAppIdForPokupkiService() {
        assertEquals("2", service.appIdBy("pokupki"));
    }

    @Test
    public void shouldReturnNullAppIdForLotalotService() {
        assertNull(service.appIdBy("lotalot"));
    }
}

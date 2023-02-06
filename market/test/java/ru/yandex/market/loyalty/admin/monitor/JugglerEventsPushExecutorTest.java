package ru.yandex.market.loyalty.admin.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.loyalty.admin.monitoring.AdminMonitorType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.core.config.Juggler;
import ru.yandex.market.loyalty.core.utils.JugglerEventView;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.monitoring.beans.JugglerEventsPushExecutor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static ru.yandex.market.loyalty.core.utils.JugglerTestUtils.JUGGLER_CLIENT_REQUEST_TYPE;

public class JugglerEventsPushExecutorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private JugglerEventsPushExecutor jugglerEventsPushExecutor;
    @Autowired
    @Default
    private PushMonitor pushMonitor;
    @Autowired
    @Juggler
    private HttpClient jugglerHttpClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Ignore
    @Test
    public void shouldSendTmsEventsToJugglerPushServer() throws IOException {
        pushMonitor.addTemporaryWarning(AdminMonitorType.NOTIFY_ABOUT_CREATED_COINS, "", 1, TimeUnit.HOURS);

        jugglerEventsPushExecutor.jugglerPushEvents();

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        then(jugglerHttpClient).should(atLeast(1)).execute(captor.capture());

        final List<HttpUriRequest> allValues = captor.getAllValues();
        assertThat(allValues, hasSize(1));
        final HttpPost httpRequest = (HttpPost) allValues.get(0);
        final List<JugglerEventView> jugglerEvents = objectMapper.readValue(
                httpRequest.getEntity().getContent(), JUGGLER_CLIENT_REQUEST_TYPE);

        assertThat(jugglerEvents, hasItem(
                allOf(
                        hasProperty("service",
                                equalTo(AdminMonitorType.NOTIFY_ABOUT_CREATED_COINS.getJugglerService())),
                        hasProperty("status", equalTo(JugglerEvent.Status.CRIT.name()))
                )
        ));
    }

}

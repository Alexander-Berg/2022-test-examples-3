package ru.yandex.market.helpers;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils;
import ru.yandex.market.util.TestSerializationService;
import ru.yandex.market.util.feeddispatcher.FeedDispatcherConfigurer;
import ru.yandex.market.util.report.ReportConfigurer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebTestHelper
public class CartHelper {

    private final TestableClock clock;
    private final ReportConfigurer reportConfigurer;
    private final MockMvc mockMvc;
    private final TestSerializationService testSerializationService;
    private final FeedDispatcherConfigurer feedDispatcherConfigurer;

    public CartHelper(TestableClock clock,
                      ReportConfigurer reportConfigurer,
                      MockMvc mockMvc,
                      TestSerializationService testSerializationService,
                      FeedDispatcherConfigurer feedDispatcherConfigurer) {
        this.clock = clock;
        this.reportConfigurer = reportConfigurer;
        this.mockMvc = mockMvc;
        this.testSerializationService = testSerializationService;
        this.feedDispatcherConfigurer = feedDispatcherConfigurer;
    }

    public ResultActions cart(CartParameters cartParameters) throws Exception {
        customizeClock(cartParameters.getFakeNow());
        feedDispatcherConfigurer.configureFeedDispatcher(cartParameters.getFeedDispatcherOffers());
        reportConfigurer.mockReport(cartParameters.getReportParameters());
        reportConfigurer.mockGeo(cartParameters.getReportGeoParameters());

        return mockMvc.perform(post("/{shopId}/cart", cartParameters.getShopId())
                .content(testSerializationService.serializeXml(cartParameters.getCartRequest()))
                .contentType(MediaType.APPLICATION_XML)
                .characterEncoding(StandardCharsets.UTF_8.name())
        );
    }

    private void customizeClock(LocalDateTime fakeNow) {
        if (fakeNow == null) {
            return;
        }
        Clock fakeClock = StubPushApiTestUtils.getClock(fakeNow.toString());
        clock.setFixed(Instant.now(fakeClock), ZoneId.systemDefault());
    }

    public List<ServeEvent> getReportEvents() {
        return reportConfigurer.getReportEvents();
    }
}

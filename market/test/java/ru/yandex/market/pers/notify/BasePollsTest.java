package ru.yandex.market.pers.notify;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.pers.notify.test.TestUtil;
import ru.yandex.market.pers.notify.ugc.UgcPollService;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public abstract class BasePollsTest extends MarketMailerMockedDbTest {

    protected static final long UID = 12345L;
    protected static final Instant EVENT_TIME = Instant.parse("2017-01-18T10:15:30Z");
    protected static final long ORDER_ID = 7890L;
    protected static final long SHOP_ID = 34567L;
    protected static final int CATEGORY_ID = 91491;
    protected static final int MODEL_ID = 1234;
    protected static final String PICTURE_URL = "http://offer.jpg";

    @Autowired
    @Qualifier("ugcRestTemplate")
    protected RestTemplate ugcRestTemplate;

    @Value("${ugc.mediator.http.url}")
    private String ugcServerUrl;

    @Autowired
    protected UgcPollService pollService;

    protected MockRestServiceServer ugcServerMock;

    public void setUpUgcServerMock() {
        ugcServerMock = MockRestServiceServer.createServer(ugcRestTemplate);
    }

    protected void expectCallToUgc(String contentFileName) throws IOException {
        ugcServerMock.expect(requestTo(ugcServerUrl + "poll"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(TestUtil.contentJsonFromFile(contentFileName))
                .andRespond(withSuccess("{\"status\": \"ok\"}", MediaType.APPLICATION_JSON));
    }

}

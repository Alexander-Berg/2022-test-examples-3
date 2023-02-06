package ru.yandex.direct.jobs.shoprating;

import java.util.Collections;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.domain.repository.MarketRatingRepository;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.direct.test.utils.MockedHttpWebServerExtention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JobsTest
@ExtendWith(SpringExtension.class)
class JobUpdateShopRatingTest {

    private static final String PING_PATH = "/ping";
    private static final String PING_ANSWER = "ok";
    private static final int NO_LIMIT = 0;

    @Value("${minimum_shop_ratings_count}")
    private int configMinRatingsCount;

    private JobUpdateShopRating job;

    @Autowired
    private DomainService domainService;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    @Autowired
    private HostingsHandler hostingsHandler;

    @Mock
    private SolomonPushClient solomonPushClient;

    @Autowired
    private MarketRatingRepository marketRatingRepository;

    @RegisterExtension
    static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.TEXT_PLAIN);

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = getJob("", NO_LIMIT);
    }

    private JobUpdateShopRating getJob(String url, int minimumRatingsCount) {
        return new JobUpdateShopRating(domainService, asyncHttpClient, hostingsHandler, solomonPushClient,
                server.getServerURL() + url, minimumRatingsCount);
    }

    @Test
    void downloadRatingInfo() {
        server.addResponse(PING_PATH, PING_ANSWER);
        assertEquals(PING_ANSWER, getJob(PING_PATH, NO_LIMIT).downloadRatingInfo(server.getServerURL() + PING_PATH));
    }

    @Test
    void parseRatingInfo_ProperLine() {
        assertEquals(job.parseRatingInfo("[{\"rating\":1,\"domain\":\"c.ru\"}]"), Collections.singletonMap("c.ru", 1L));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{\"rating\":1,\"domain\":\"c\"}]",
            "[{\"rating\":1,\"domain\":\"c.\"}]",
            "[{\"rating\":1,\"domain\":\"c.ru d\"}]",
            "[{\"rating\":4,\"domain\":\"Колледж бодибилдинга и фитнеса имени Бена Вейдера\"}]"
    })
    void parseRatingInfo_WrongDomainsLine(String ratingInfo) {
        assertEquals(job.parseRatingInfo(ratingInfo), Collections.EMPTY_MAP);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{}]",
            "\"[{\"rating\":1}]\"",
            "[{\"domain\":\"c.ru\"}]",
            "\"[{\"domain\":null},{\"rating\":null}]\"",
            "[{\"field1\":1,\"field2\":\"c.ru\"}]"
    })
    void parseRatingInfo_WrongJsonFormat(String ratingInfo) {
        Assertions.assertThatThrownBy(() -> job.parseRatingInfo(ratingInfo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("can not deserialize object from json");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a.b.c.d",
            "а.б.в.г",
            "rukodelie.shop",
            "домен.ру",
            "www.yandex",
            "ya.ru"
    })
    void validateDomain_GoodDomains(String domain) {
        assertTrue(job.validateDomain(domain));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-a.b.c.d",
            "abcd",
            "Интер-Сервис",
            "Магазин \"Фотик\" в Смоленске"
    })
    void validateDomain_BadDomains(String domain) {
        assertFalse(job.validateDomain(domain));
    }

    @Test
    void updateDomainsRatings_WithLimit() {
        String requestPath = "/text.txt";
        String answer = "[{\"rating\":1,\"domain\":\"ya.ru\"}"
                + ",{\"rating\":2,\"domain\":\"yandex.ru\"}]";
        server.addResponse(requestPath, answer);
        Assertions.assertThatThrownBy(() -> getJob(requestPath, configMinRatingsCount).execute())
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Something wrong with shops ratings. Expected more than");
    }

    @Test
    void updateDomainsRatings_WithoutLimit() throws Exception {
        String requestPath1 = "/text1.txt";
        String answer1 = "[{\"rating\":1,\"domain\":\"www.holodilnik.ru\"}"
                + ",{\"rating\":2,\"domain\":\"kaluga.holodilnik.ru\"}"
                + ",{\"rating\":3,\"domain\":\"spb.003.ru\"}]";
        server.addResponse(requestPath1, answer1);
        getJob(requestPath1, NO_LIMIT).execute();

        String requestPath2 = "/text2.txt";
        String answer2 = "[{\"rating\":4,\"domain\":\"www.holodilnik.ru\"}"
                + ",{\"rating\":2,\"domain\":\"kaluga.holodilnik.ru\"}"
                + ",{\"rating\":5,\"domain\":\"novomoskovsk.positronica.ru\"}]";
        server.addResponse(requestPath2, answer2);
        getJob(requestPath2, NO_LIMIT).execute();

        Map<String, Long> map = marketRatingRepository.getAllByName();
        assertEquals((long) map.get("holodilnik.ru"), 4L);
        assertEquals((long) map.get("kaluga.holodilnik.ru"), 2L);
        assertEquals((long) map.get("spb.003.ru"), -1L);
        assertEquals((long) map.get("novomoskovsk.positronica.ru"), 5L);
    }
}

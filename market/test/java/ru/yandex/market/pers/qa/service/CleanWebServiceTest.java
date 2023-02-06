package ru.yandex.market.pers.qa.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.CleanWebContent;
import ru.yandex.market.cleanweb.CleanWebRequestDto;
import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.model.AutoFilterData;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.mockito.Mockito.mock;

/**
 * Тест проверяет работы CleanWebService в зависимости от того, что получили от чв
 */
public class CleanWebServiceTest extends PersQATest {

    public static final UserInfo UID = UserInfo.uid(32);
    public static final long ID = 1234;
    public static final AutoFilterData ITEM = new AutoFilterData(ID, UID, UUID.randomUUID().toString(), QaEntityType.COMMENT);

    private HttpClient httpClient;
    private CleanWebService cleanWebService;
    private ComplexMonitoring complexMonitoring;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;
    @Autowired
    private UserBanService userBanService;

    @BeforeEach
    void init() {
        PersQaServiceMockFactory.resetMocks();
        httpClient = mock(HttpClient.class);
        CleanWebClient client = new CleanWebClient(
            "http://target:90",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)),
            "service_name");

        cleanWebService = new CleanWebService(client, qaJdbcTemplate, userBanService);
    }

    @Test
    public void testRejectedResult() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_ban.json");

        Assertions.assertEquals(ModState.AUTO_FILTER_REJECTED, cleanWebService.getCleanWebOnlineResult(ITEM));
    }

    // тест на всякий случай(если фильтр на мат не сработал - список вердиктов будет пуст)
    @Test
    public void testPassedResultByPassedFilter() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_not_ban.json");

        Assertions.assertEquals(ModState.AUTO_FILTER_PASSED, cleanWebService.getCleanWebOnlineResult(ITEM));
    }

    @Test
    public void testPassedResultByEmptyVerdict() {
        // empty verdict - every filters is ok
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_empty.json");
        Assertions.assertEquals(ModState.AUTO_FILTER_PASSED, cleanWebService.getCleanWebOnlineResult(ITEM));
    }


    @Test
    public void testUnknownResultByError() {
        // error
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_error.json");
        Assertions.assertEquals(ModState.AUTO_FILTER_UNKNOWN, cleanWebService.getCleanWebOnlineResult(ITEM));
    }

    @Test
    public void testFraudContentAnswer() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_fraud.json");
        Assertions.assertEquals(ModState.ANTIFRAUD_REJECTED, cleanWebService.getCleanWebOnlineResult(ITEM));

        //check that user do not appear in qa.user_content_ban_queue
        Long userBanQueueCount = qaJdbcTemplate.queryForObject(
            "select count(*) from qa.user_content_ban_queue", Long.class);
        Assertions.assertEquals(0L, userBanQueueCount);
    }

    @Test
    public void testFraudUserAnswer() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/data/clean_web_user_fraud.json");
        Assertions.assertEquals(ModState.ANTIFRAUD_REJECTED, cleanWebService.getCleanWebOnlineResult(ITEM));

        //check that with user ban he has appeared in qa.user_content_ban_queue
        Long userBanQueueCount = qaJdbcTemplate.queryForObject(
            "select count(*) \n" +
                "from qa.user_content_ban_queue \n" +
                "where user_id = ?",
            Long.class, "<USER_ID>");
        Assertions.assertEquals(1L, userBanQueueCount);
    }

    @Test
    public void testSerializationCleanWebRequestDao() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", "1234567");
        params.put("ip", "127.0.0.1");
        params.put("user_agent", "user_agent");
        params.put("is_shop", true);
        params.put("is_brand", false);

        CleanWebContent content =
            CleanWebContent.builder()
                .id("id")
                .key("key")
                .text("text")
                .type("type")
                .parameters(params)
                .build();
        CleanWebRequestDto dto = new CleanWebRequestDto("test", true, content);

        System.out.println(new ObjectMapper().writeValueAsString(dto));
        JSONAssert.assertEquals(
            IOUtils.readInputStream(getClass().getResourceAsStream("/data/clean_web_request_dto.json")),
            new ObjectMapper().writeValueAsString(dto),
            JSONCompareMode.LENIENT
        );
    }

}

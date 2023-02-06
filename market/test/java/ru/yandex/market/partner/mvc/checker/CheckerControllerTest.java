package ru.yandex.market.partner.mvc.checker;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.security.AuthorityChecker;
import ru.yandex.market.security.model.Authority;

import static org.apache.http.HttpStatus.SC_OK;

class CheckerControllerTest extends FunctionalTest {

    private String url;
    private String urlBatch;

    @BeforeEach
    void setUp() {
        url = baseUrl + "/checker/result?authorityName=authorityName" +
                "&checkerName={checkerName}&param=param&domain=domain&_user_id=user_id";
        urlBatch = baseUrl + "checker/result?_user_id=user_id";
    }

    // this checker always ends with an exception
    public static class ExceptionChecker implements AuthorityChecker {
        public boolean check(Object data, Authority authority) {
            throw new RuntimeException();
        }
    }

    @Bean
    public AuthorityChecker exceptionChecker() {
        return new ExceptionChecker();
    }

    @Test
    void it_must_return_false_for_unknown_checker() {
        String response = FunctionalTestHelper.get(url, "UNKNOWN_CHECKER").getBody();
        Assertions.assertEquals("false", response);
    }

    @Test
    void it_must_return_true_for_trueAuthorityChecker() {
        String response = FunctionalTestHelper.get(url, "trueAuthorityChecker").getBody();
        Assertions.assertEquals("true", response);
    }

    @Test
    void it_must_return_false_for_falseAuthorityChecker() {
        String response = FunctionalTestHelper.get(url, "falseAuthorityChecker").getBody();
        Assertions.assertEquals("false", response);
    }

    @Test
    void shouldMarkCheckAsFalseWhenException() {
        String response = FunctionalTestHelper.get(url, "exceptionChecker").getBody();
        Assertions.assertEquals("false", response);
    }

    @Test
    void batchAuthoritiesShouldMarkCheckAsFalseWhenException() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                urlBatch,
                JsonTestUtil.getJsonHttpEntity(
                        //language=json
                        "{" +
                        "    \"authorities\": [" +
                        "        {" +
                        "            \"checkResult\": null," +
                        "            \"checker\": \"exceptionChecker\"," +
                        "            \"domain\": \"MBI-PARTNER\"," +
                        "            \"id\": 123456," +
                        "            \"name\": \"checkName\"," +
                        "            \"params\": \"\"" +
                        "        }," +
                        "{" +
                        "            \"checkResult\": null," +
                        "            \"checker\": \"trueAuthorityChecker\"," +
                        "            \"domain\": \"MBI-PARTNER\"," +
                        "            \"id\": 123456," +
                        "            \"name\": \"checkName\"," +
                        "            \"params\": \"\"" +
                        "        }" +
                        "    ]" +
                        "}"));
        Assertions.assertEquals(SC_OK, response.getStatusCode().value());
        JsonTestUtil.assertEquals(response.getBody(),
                //language=json
                "{\n" +
                "  \"authorities\": [\n" +
                "    {\n" +
                "      \"id\": 123456,\n" +
                "      \"name\": \"checkName\",\n" +
                "      \"checker\": \"exceptionChecker\",\n" +
                "      \"params\": \"\",\n" +
                "      \"domain\": \"MBI-PARTNER\",\n" +
                "      \"checkResult\": false\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 123456,\n" +
                "      \"name\": \"checkName\",\n" +
                "      \"checker\": \"trueAuthorityChecker\",\n" +
                "      \"params\": \"\",\n" +
                "      \"domain\": \"MBI-PARTNER\",\n" +
                "      \"checkResult\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}");
    }


    @Test
    void it_must_return_true_for_batch_authorities() throws Exception {
        List<AuthorityDto> authorities = ImmutableList.of(
                new AuthorityDto(0L, "authorityName", "trueAuthorityChecker", "param",
                        "domain"),
                new AuthorityDto(0L, "authorityName", "falseAuthorityChecker", "param",
                        "domain"),
                new AuthorityDto(0L, "authorityName", "UNKNOWN_CHECKER", "param",
                        "domain")
        );
        AuthoritiesBatch toSend = new AuthoritiesBatch(authorities);

        AuthoritiesBatch result = FunctionalTestHelper.exchange(urlBatch, toSend, HttpMethod.POST,
                AuthoritiesBatch.class).getBody();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.getAuthorities().size());
        equalsAuthorityDto(authorities.get(0), true, result.getAuthorities().get(0));
        equalsAuthorityDto(authorities.get(1), false, result.getAuthorities().get(1));
        equalsAuthorityDto(authorities.get(2), false, result.getAuthorities().get(2));
    }

    private void equalsAuthorityDto(AuthorityDto expected, Boolean expectCheckerResult, AuthorityDto actual) {
        Assertions.assertEquals(expected.getChecker(), actual.getChecker());
        Assertions.assertEquals(expected.getDomain(), actual.getDomain());
        Assertions.assertEquals(expected.getId(), actual.getId());
        Assertions.assertEquals(expected.getName(), actual.getName());
        Assertions.assertEquals(expected.getParams(), actual.getParams());
        Assertions.assertEquals(expectCheckerResult, actual.getCheckResult());
    }

}

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.lrm.client.ApiClient;
import ru.yandex.market.tpl.common.lrm.client.LrmClientException;
import ru.yandex.market.tpl.common.lrm.client.RestTemplateResponseErrorHandler;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ContextConfiguration(classes = {ReturnsApi.class, ApiClient.class, RestTemplate.class})
@ExtendWith(SpringExtension.class)
@RestClientTest
public class RestTemplateResponseErrorHandlerTest {
    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private RestTemplateBuilder builder;

    @BeforeEach
    public void init() {
        Assertions.assertThat(this.server).isNotNull();
        Assertions.assertThat(this.builder).isNotNull();
    }

    @Test
    public void testExceptionHandler() {
        RestTemplate restTemplate = this.builder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();


        String errorBody = "Very very very very very bad request";
        this.server
                .expect(ExpectedCount.once(), requestTo("/returns"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(errorBody));

        LrmClientException exception = assertThrows(
                LrmClientException.class,
                () -> restTemplate
                        .postForObject("/returns", null, String.class)
        );

        Assertions.assertThat(exception.getMessage()).isEqualTo(HttpStatus.BAD_REQUEST + " " + errorBody);
    }
}

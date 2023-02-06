package ru.yandex.direct.geosearch;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.geosearch.model.Address;
import ru.yandex.direct.test.utils.MockedHttpWebServerRule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestingConfiguration.class)
public class GeosearchClientFaildTest extends GeosearchClientTestBase {
    @Rule
    public final MockedHttpWebServerRule server = new MockedHttpWebServerRule(ContentType.DEFAULT_BINARY);

    private String validResponsePath = "/valid_response_after_retry";
    private Address address = new Address().withCountry("Россия").withCity("Москва")
            .withStreet("Красная площадь").withBuilding("1");

    private String requestrequest;

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String msg = new RuntimeException("Failed").getMessage();
                return new MockResponse().setResponseCode(HttpStatus.SC_BAD_REQUEST)
                        .setBody(msg);
            }
        };
    }

    @Test
    public void getGeoData_error_whenAllRequestsFailed() throws Exception {
        assertThatThrownBy(() -> geosearchClient.searchAddress(address)).isInstanceOf(GeosearchClientException.class);
    }
}

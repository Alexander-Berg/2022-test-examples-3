package ru.yandex.direct.staff.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.staff.client.model.json.StaffRawInfo;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class StaffClientTest extends StaffClientTestBase {

    private static final String[] LOGINS = new String[]{"login1", "login2"};
    private static final String ANSWER_EXAMPLE_PATH = "staff/server_answer_example.json";
    private static final String PATH = "/v3/persons";

    @Test
    public void getStaffUserInfos_success() {
        Map<String, PersonInfo> staffUserInfos = staffClient.getStaffUserInfos(asList(LOGINS));
        assertThat(staffUserInfos.keySet()).hasSize(2);
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                    String comaSeparatedLogins = String.join(",", LOGINS);
                    String decodedLogins = encode(comaSeparatedLogins, StandardCharsets.UTF_8.name());
                    String comaSeparatedFields = String.join(",", StaffRawInfo.DEFAULT_FIELDS);
                    String decodedFields = encode(comaSeparatedFields, StandardCharsets.UTF_8.name());
                    String expectedUrl = String.format("%s?login=%s&_fields=%s", PATH, decodedLogins, decodedFields);
                    softAssertions.assertThat(request.getPath()).isEqualTo(expectedUrl);
                    return new MockResponse().setBody(getAnswerExample());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private String getAnswerExample() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = classloader.getResourceAsStream(ANSWER_EXAMPLE_PATH)) {
            assumeThat(stream, notNullValue());
            //noinspection ConstantConditions
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

}

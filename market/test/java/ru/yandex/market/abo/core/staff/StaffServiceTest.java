package ru.yandex.market.abo.core.staff;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.assessor.AssessorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zilzilok
 * @date 10.06.2021
 */
class StaffServiceTest {
    private static final HttpResponse DUMMY_HTTP_RESPONSE =
            DefaultHttpResponseFactory.INSTANCE.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                    HttpStatus.SC_OK, null), null);
    private static final String STAFF_API = "http://foo";
    private static final String TOKEN = "XXX";
    private static final Long UID = 123L;
    private static final String LOGIN = "pupok";

    @Mock
    private HttpClient staffHttpClient;
    @Mock
    private AssessorService assessorService;
    private StaffService staffService;

    @BeforeEach
    void init() throws IOException {
        MockitoAnnotations.openMocks(this);
        StaffClient staffClient = new StaffClient(staffHttpClient, STAFF_API, TOKEN);
        staffService = new StaffService(staffClient, assessorService);
        when(staffHttpClient.execute(any())).thenReturn(DUMMY_HTTP_RESPONSE);
    }

    @ParameterizedTest
    @MethodSource("requestPhoneSource")
    void requestPhone(String json, String expectedPhone) {
        DUMMY_HTTP_RESPONSE.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        when(assessorService.loadStaffLogin(UID)).thenReturn(Optional.of(LOGIN));
        assertEquals(expectedPhone, staffService.getPhoneNumber(UID));
    }

    // https://staff-api.yandex-team.ru/v3/persons?_one=1&login=zilzilok&_fields=work_phone
    private static Stream<Arguments> requestPhoneSource() {
        return Stream.of(
                Arguments.of("{\"work_phone\": 2377}", "2377"),
                Arguments.of("{\"work_phone\": null}", ""),
                Arguments.of("" +
                        "{\n" +
                        "  \"error_message\": \"not found\", \n" +
                        "  \"details\": {\n" +
                        "    \"request\": {\n" +
                        "      \"_pretty\": \"1\", \n" +
                        "      \"_one\": \"1\", \n" +
                        "      \"login\": \"-3\", \n" +
                        "      \"_fields\": \"work_phone\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}", "")
        );
    }

    @ParameterizedTest
    @MethodSource("requestDepartmentsSource")
    void requestDepartments(String json, Map<Long, String> expectedDepartments) {
        DUMMY_HTTP_RESPONSE.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        assertEquals(expectedDepartments, staffService.getDepartments(Collections.singletonList(UID)));
    }

    // https://staff-api.yandex-team.ru/v3/persons?_pretty=1&uid=1120000000330881&_fields=uid,department_group.name
    private static Stream<Arguments> requestDepartmentsSource() {
        return Stream.of(
                Arguments.of("" +
                        "{\n" +
                        "  \"links\": {}, \n" +
                        "  \"page\": 1, \n" +
                        "  \"limit\": 50, \n" +
                        "  \"result\": [\n" +
                        "    {\n" +
                        "      \"uid\": \"-1\", \n" +
                        "      \"department_group\": {\n" +
                        "        \"name\": \"gde-to\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ], \n" +
                        "  \"total\": 1, \n" +
                        "  \"pages\": 1\n" +
                        "}", Collections.singletonMap(-1L, "gde-to")),
                Arguments.of("" +
                        "{\n" +
                        "  \"links\": {}, \n" +
                        "  \"page\": 1, \n" +
                        "  \"limit\": 50, \n" +
                        "  \"result\": [], \n" +
                        "  \"total\": 0, \n" +
                        "  \"pages\": 0\n" +
                        "}", Collections.emptyMap())
        );
    }
}

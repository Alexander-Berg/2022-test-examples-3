package ru.yandex.market.psku.postprocessor.service.staff;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.StaffUser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StaffClientTest extends BaseDBTest {

    //https://staff-api.yandex-team.ru/v3/persons?login=boristsyganov,volozh
    String response = "{\n" +
            "  \"links\": {},\n" +
            "  \"page\": 1,\n" +
            "  \"limit\": 50,\n" +
            "  \"result\": [\n" +
            "    {\n" +
            "      \"is_deleted\": false,\n" +
            "      \"uid\": \"1120000000000529\",\n" +
            "      \"official\": {\n" +
            "        \"affiliation\": \"yandex\",\n" +
            "        \"is_dismissed\": false,\n" +
            "        \"is_homeworker\": false,\n" +
            "        \"is_robot\": false\n" +
            "      },\n" +
            "      \"login\": \"user1\",\n" +
            "      \"id\": 1,\n" +
            "      \"name\": {\n" +
            "        \"last\": {\n" +
            "          \"ru\": \"Фамилия\",\n" +
            "          \"en\": \"Surname\"\n" +
            "        },\n" +
            "        \"first\": {\n" +
            "          \"ru\": \"Имя\",\n" +
            "          \"en\": \"Name\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"is_deleted\": false,\n" +
            "      \"uid\": \"1120000000373042\",\n" +
            "      \"official\": {\n" +
            "        \"affiliation\": \"yandex\",\n" +
            "        \"is_dismissed\": false,\n" +
            "        \"is_homeworker\": false,\n" +
            "        \"is_robot\": false\n" +
            "      },\n" +
            "      \"login\": \"user2\",\n" +
            "      \"id\": 2,\n" +
            "      \"name\": {\n" +
            "        \"last\": {\n" +
            "          \"ru\": \"Фамилия\",\n" +
            "          \"en\": \"Surname\"\n" +
            "        },\n" +
            "        \"first\": {\n" +
            "          \"ru\": \"Имя\",\n" +
            "          \"en\": \"Name\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"total\": 2,\n" +
            "  \"pages\": 1\n" +
            "}";
    private StaffClient staffClient;

    @Before
    public void setUp() throws IOException {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);
        staffClient = new StaffClient(httpClient, () -> "test_ticket", "test_host");
    }

    @Test
    public void test() throws IOException {
        List<StaffUser> users = staffClient.getUsers(Arrays.asList("user1", "user2"));
        Assertions.assertThat(users.size()).isEqualTo(2);
        Assertions.assertThat(users.get(0).getId()).isEqualTo(1);
        Assertions.assertThat(users.get(0).getLogin()).isEqualTo("user1");
        Assertions.assertThat(users.get(0).getUid()).isEqualTo("1120000000000529");

        Assertions.assertThat(users.get(1).getId()).isEqualTo(2);
        Assertions.assertThat(users.get(1).getLogin()).isEqualTo("user2");
        Assertions.assertThat(users.get(1).getUid()).isEqualTo("1120000000373042");

    }
}

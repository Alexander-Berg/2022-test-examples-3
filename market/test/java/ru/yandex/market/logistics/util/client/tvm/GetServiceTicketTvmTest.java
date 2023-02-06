package ru.yandex.market.logistics.util.client.tvm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.util.client.tvm.client.DetailedTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.ServiceInfo;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class GetServiceTicketTvmTest extends AbstractClientTest {
    @Autowired
    private DetailedTvmClient detailedTvmClient;

    @Test
    @DisplayName("Получить информацию о сервисе")
    void getServiceInfo() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo("https://tvm.yandex-team.ru/client/1/info"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response.json"))
            );

        ServiceInfo expected = ServiceInfo.builder()
            .id(1L)
            .name("Passport. Blackbox Prod")
            .createTime(1455978612L)
            .creatorUid(1120000000000048L)
            .abcServiceId(14L)
            .vaultLink("https://ya.ru")
            .status("ok")
            .build();

        softly.assertThat(detailedTvmClient.getServiceInfo(1L)).isEqualToComparingFieldByField(expected);
    }

    @Test
    @DisplayName("Получить информацию о несуществующем сервисе")
    void getServiceInfoWithInvalidId() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo("https://tvm.yandex-team.ru/client/15674839201/info"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        softly.assertThatThrownBy(
            () -> detailedTvmClient.getServiceInfo(15674839201L)
        ).hasMessage("400 Bad Request");
    }

    @Test
    @DisplayName("Получить информацию сервисе по null-id")
    void getServiceInfoWithNull() {
        mock.expect(method(HttpMethod.GET))
            .andExpect(requestTo("https://tvm.yandex-team.ru/client/null/info"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        softly.assertThatThrownBy(() -> detailedTvmClient.getServiceInfo(null)).hasMessage("400 Bad Request");
    }
}

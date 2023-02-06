package ru.yandex.market.logistics.datacamp.client;

import java.util.Collections;

import Market.DataCamp.SyncAPI.SyncCommon;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Получение карго-типов")
class GetCargoTypesTest extends AbstractDataCampClientTest {

    @Test
    @DisplayName("Получение непустого списка")
    void getCargoTypesTest() {
        mock.expect(requestTo(startsWith(url + "/v1/partners/1/offers/services/2/cargotypes")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .body(SyncCommon.GetInt32ListValueResponse.newBuilder()
                    .addAllValues(ImmutableList.of(300, 301))
                    .build()
                    .toByteArray()
                )
            );

        softly.assertThat(dataCampClient.getCargoTypes(1, 2)).isEqualTo(ImmutableList.of(300, 301));
    }

    @Test
    @DisplayName("Получение пустого списка")
    void getCargoTypesEmptyTest() {
        mock.expect(requestTo(startsWith(url + "/v1/partners/1/offers/services/2/cargotypes")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .body(new byte[0])
            );

        softly.assertThat(dataCampClient.getCargoTypes(1, 2)).isEqualTo(Collections.emptyList());
    }
}

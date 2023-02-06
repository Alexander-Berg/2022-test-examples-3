package ru.yandex.market.logistics.management.client;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.client.util.TestUtil;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerRouteRequest;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerRouteResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientPartnerRouteTest extends AbstractClientTest {

    @Test
    void testCreatePartnerRouteSuccessfully() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/3000/route"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(TestUtil.jsonContent("data/controller/partnerRoute/create_route_request.json", false))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partnerRoute/create_route_response.json")));

        PartnerRouteResponse response = client.createPartnerRoute(3000,
            new PartnerRouteRequest(
                100,
                500,
                Set.of(1, 2, 5)
            )
        );

        softly.assertThat(response.getId()).isEqualTo(1);
        softly.assertThat(response.getPartnerId()).isEqualTo(3000);
        softly.assertThat(response.getLocationFrom()).isEqualTo(100);
        softly.assertThat(response.getLocationTo()).isEqualTo(500);
        softly.assertThat(response.getWeekDays()).containsExactlyInAnyOrder(1, 2, 5);
    }
}

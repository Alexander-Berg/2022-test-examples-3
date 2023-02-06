package ru.yandex.market.logistics.management.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerCustomerInfoResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;
import static ru.yandex.market.logistics.management.entity.type.TrackCodeSource.ORDER_NO;

class LmsClientPartnerCustomerInfoTest extends AbstractClientTest {

    @Test
    void setCustomerInfoToPartner() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/partners/150/customerInfo/10")
                .toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResource("data/controller/partnerCustomerInfo/response.json")));

        PartnerResponse response = client.setCustomerInfoToPartner(150, 10);

        softly.assertThat(response.getId()).isEqualTo(150);
        PartnerCustomerInfoResponse customerInfo = response.getCustomerInfo();
        softly.assertThat(customerInfo.getId()).isEqualTo(10);
        softly.assertThat(customerInfo.getName()).isEqualTo("Customer info name");
        softly.assertThat(customerInfo.getPhones()).isEqualTo(
            List.of("79991234567", "79993215476")
        );
        softly.assertThat(customerInfo.getTrackOrderSite()).isEqualTo("https://some.site");
        softly.assertThat(customerInfo.getTrackCodeSource()).isEqualTo(ORDER_NO);
    }
}

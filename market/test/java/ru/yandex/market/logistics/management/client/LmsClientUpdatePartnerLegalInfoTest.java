package ru.yandex.market.logistics.management.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.client.util.DtoFactory;
import ru.yandex.market.logistics.management.entity.request.partner.UpdateLegalInfoDto;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientUpdatePartnerLegalInfoTest extends AbstractClientTest {
    @Test
    void updateSuccess() {
        mockServer.expect(requestTo(uri + "/externalApi/partner/1/legalInfo"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/legalInfo/request.json")))
            .andRespond(withStatus(OK));

        client.updatePartnerLegalInfo(
            1L,
            UpdateLegalInfoDto.newBuilder()
                .taxationSystem(TaxationSystem.COMMON)
                .account("account")
                .legalAddress(DtoFactory.getAddressDto())
                .postAddress(DtoFactory.getAddressDto())
                .bik("bik")
                .kpp("kpp")
                .build()
        );
    }
}

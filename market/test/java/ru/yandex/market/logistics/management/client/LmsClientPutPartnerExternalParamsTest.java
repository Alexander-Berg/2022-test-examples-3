package ru.yandex.market.logistics.management.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientPutPartnerExternalParamsTest extends AbstractClientTest {

    @Test
    void testAddExternalParams() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/1/externalParams").toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/partnerExternalParams/partner_external_params_request.json"
            )))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/partnerExternalParams/partner_external_params_response.json"))
            );

        PartnerExternalParamGroup result = client.addOrUpdatePartnerExternalParams(
            1,
            List.of(
                new PartnerExternalParamRequest(PartnerExternalParamType.IS_GLOBAL, "1"),
                new PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, "1")
            )
        );

        softly.assertThat(result)
            .isEqualTo(
                new PartnerExternalParamGroup(
                    1L,
                    List.of(
                        new PartnerExternalParam("IS_GLOBAL", "global", "1"),
                        new PartnerExternalParam("IS_COMMON", "common", "1")
                    )
                )
            );
    }

    @Test
    void testAddExternalParamsEmpty() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/1/externalParams").toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/partnerExternalParams/partner_external_params_empty_request.json"
            )))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource(
                        "data/controller/partnerExternalParams/partner_external_params_empty_response.json"
                    ))
            );

        PartnerExternalParamGroup result = client.addOrUpdatePartnerExternalParams(1, List.of());

        softly.assertThat(result)
            .isEqualTo(new PartnerExternalParamGroup(1L, List.of()));
    }
}

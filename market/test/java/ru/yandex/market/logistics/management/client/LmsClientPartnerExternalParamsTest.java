package ru.yandex.market.logistics.management.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamTypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientPartnerExternalParamsTest extends AbstractClientTest {

    @Test
    void getExternalParams() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner/externalParam")
                    .queryParam("paramTypes", "LOGO")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_external_params.json")));

        List<PartnerExternalParamGroup> result = client
            .getPartnerExternalParams(Collections.singleton(PartnerExternalParamType.LOGO));

        softly.assertThat(result)
            .contains(new PartnerExternalParamGroup(
                1L,
                Collections.singletonList(new PartnerExternalParam("LOGO", "some", "1"))
            ));
    }

    @Test
    void getPartnerEmails() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/externalParams/SERVICE_EMAILS")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_emails.json")));

        PartnerExternalParamResponse result = client
            .getPartnerExternalParam(1L, PartnerExternalParamType.SERVICE_EMAILS);

        softly.assertThat(result).isEqualTo(PartnerExternalParamResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .key(PartnerExternalParamType.SERVICE_EMAILS)
            .description("Электронные адреса службы")
            .value("test@yandex.ru")
            .build());
    }

    @Test
    void getPartnerExternalParam_missingPartner() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1000/externalParams/SERVICE_EMAILS")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(NOT_FOUND));

        assertThrows(
            HttpTemplateException.class,
            () -> client.getPartnerExternalParam(1000L, PartnerExternalParamType.SERVICE_EMAILS)
        );
    }

    @Test
    void getPartnerExternalParam_partnerMissingParameter() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/externalParams/ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(NO_CONTENT));

        assertThrows(
            HttpTemplateException.class,
            () -> client.getPartnerExternalParam(
                1L,
                PartnerExternalParamType.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED
            )
        );
    }

    @Test
    void findPartnerExternalParam_missingPartner() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1000/externalParams/SERVICE_EMAILS")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(NOT_FOUND));

        assertThrows(
            HttpTemplateException.class,
            () -> client.findPartnerExternalParam(1000L, PartnerExternalParamType.SERVICE_EMAILS)
        );
    }

    @Test
    void findPartnerExternalParam_partnerMissingParameter() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/externalParams/ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(NO_CONTENT));

        Optional<PartnerExternalParamResponse> response = client.findPartnerExternalParam(
            1L,
            PartnerExternalParamType.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED
        );

        assertThat(response).isEmpty();
    }

    @Test
    void getAllEmails() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partner/externalParam")
                    .queryParam("paramTypes", "SERVICE_EMAILS")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partners_emails.json")));

        List<PartnerExternalParamGroup> result = client
            .getPartnerExternalParams(Collections.singleton(PartnerExternalParamType.SERVICE_EMAILS));

        softly.assertThat(result)
            .contains(new PartnerExternalParamGroup(
                1L,
                Collections.singletonList(
                    new PartnerExternalParam("SERVICE_EMAILS", "Электронные адреса службы", "test@yandex.ru")
                )
            ))
            .contains(new PartnerExternalParamGroup(
                2L,
                Collections.singletonList(
                    new PartnerExternalParam("SERVICE_EMAILS", "Электронные адреса службы", "test2@yandex.ru")
                )
            ));
    }

    @Test
    void getDaysForReturnOrder() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/externalParams/DAYS_FOR_RETURN_ORDER")
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_days_for_return_order.json")));

        PartnerExternalParamResponse result = client
            .getPartnerExternalParam(1L, PartnerExternalParamType.DAYS_FOR_RETURN_ORDER);

        softly.assertThat(result).isEqualTo(PartnerExternalParamResponse.newBuilder()
            .id(1L)
            .partnerId(1L)
            .key(PartnerExternalParamType.DAYS_FOR_RETURN_ORDER)
            .description("Максимальный срок возврата заказов в днях")
            .value("80")
            .build());
    }

    @Test
    void testAddExternalParam() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/1/externalParam").toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/partnerExternalParams/partner_external_param_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partnerExternalParams/partner_external_param_response.json")));

        PartnerExternalParamResponse response = client.addOrUpdatePartnerExternalParam(
            1L,
            new PartnerExternalParamRequest(PartnerExternalParamType.LOGO, "1")
        );

        softly.assertThat(response)
            .isEqualTo(
                PartnerExternalParamResponse.newBuilder()
                    .id(100500L)
                    .partnerId(1L)
                    .key(PartnerExternalParamType.LOGO)
                    .description("some")
                    .value("1")
                    .build()
            );
    }

    @Test
    void testGetPartnerExternalParamTypeOptions() {
        mockServer.expect(requestTo(getBuilder(uri, "/externalApi/partners/externalParamsTypeOptions").toUriString()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(
                    "data/controller/partnerExternalParams/partner_external_params_type_options.json"
                ))
            );

        List<PartnerExternalParamTypeResponse> externalParamTypes = client.getPartnerExternalParamTypeOptions();

        softly.assertThat(externalParamTypes).containsExactlyInAnyOrder(
            PartnerExternalParamTypeResponse.builder().id(1L).key("TYPE1").description("Param 1").build(),
            PartnerExternalParamTypeResponse.builder().id(2L).key("TYPE2").description("Param 2").build()
        );
    }

    @Test
    void deleteParamForPartnerByTypes() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/externalParams/delete").toUriString()
            ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(
                "data/controller/partnerExternalParams/partner_external_params_delete_request.json"
            )))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deleteParamsForPartnerByTypes(
                1,
                List.of(PartnerExternalParamType.IS_COMMON, PartnerExternalParamType.IS_GLOBAL)
            ))
            .doesNotThrowAnyException();
    }
}

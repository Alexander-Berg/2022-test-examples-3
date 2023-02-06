package ru.yandex.market.logistics.management.client;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.possibleOrderChanges.PossibleOrderChangeRequest;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeDto;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeGroup;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;
import static ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.PARTNER_API;
import static ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.ORDER_ITEMS;

class LmsClientPossibleOrderChangeTest extends AbstractClientTest {

    @Test
    void getPartnerPossibleOrderChanges() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/possible-order-changes?types=ORDER_ITEMS"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_possible_order_changes.json"))
            );

        List<PossibleOrderChangeGroup> apiResponse = client.getPartnerPossibleOrderChanges(Collections.singleton(
            ORDER_ITEMS
        ));

        softly.assertThat(apiResponse)
            .containsExactlyInAnyOrder(
                new PossibleOrderChangeGroup(1L, Collections.singletonList(
                    PossibleOrderChangeDto.builder()
                        .id(1L)
                        .partnerId(1L)
                        .type(ORDER_ITEMS)
                        .method(PARTNER_API)
                        .checkpointStatusFrom(113)
                        .checkpointStatusTo(114)
                        .enabled(true)
                        .build()
                ))
            );
    }

    @Test
    void getPartnerPossibleOrderChangesPage() {
        mockServer.expect(requestTo(startsWith(uri + "/externalApi/partners/possible-order-changes/page")))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "1"))
            .andExpect(queryParam("types", "ORDER_ITEMS"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_possible_order_changes_page.json"))
            );

        PageResult<PossibleOrderChangeDto> apiResponse = client.getPartnerPossibleOrderChangesPage(
            new PageRequest(0, 1), Set.of(ORDER_ITEMS)
        );

        softly.assertThat(apiResponse)
            .isEqualTo(
                new PageResult<PossibleOrderChangeDto>()
                    .setData(List.of(
                        PossibleOrderChangeDto.builder()
                            .id(1L)
                            .partnerId(1L)
                            .type(ORDER_ITEMS)
                            .method(PARTNER_API)
                            .checkpointStatusFrom(113)
                            .checkpointStatusTo(114)
                            .enabled(true)
                            .build()
                    ))
                    .setPage(0)
                    .setTotalPages(1)
                    .setTotalElements(1)
                    .setSize(1)
            );
    }

    @Test
    void createPossibleOrderChange() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/possible-order-changes"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource("data/controller/partner_possible_order_change_request.json")))
            .andRespond(withStatus(CREATED)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_possible_order_change_response.json"))
            );

        PossibleOrderChangeDto apiResponse = client.createPossibleOrderChange(
            PossibleOrderChangeRequest.builder()
                .partnerId(1L)
                .type(PossibleOrderChangeType.RECIPIENT)
                .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                .checkpointStatusFrom(113)
                .checkpointStatusTo(114)
                .enabled(true)
                .build()
        );

        softly.assertThat(apiResponse)
            .isEqualTo(
                PossibleOrderChangeDto.builder()
                    .id(1L)
                    .partnerId(1L)
                    .type(PossibleOrderChangeType.RECIPIENT)
                    .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                    .checkpointStatusFrom(113)
                    .checkpointStatusTo(114)
                    .enabled(true)
                    .enabledAt(Instant.parse("2020-02-20T20:20:20Z"))
                    .build()
            );
    }

    @Test
    void createMultiplePossibleOrderChange() {
        mockServer.expect(requestTo(uri + "/externalApi/partners/possible-order-changes/multiple"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(jsonResource(
                "data/controller/partner_possible_order_change_multiple_request.json"
            )))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_possible_order_change_multiple_response.json"))
            );

        List<PossibleOrderChangeDto> apiResponse = client.createMultiplePossibleOrderChanges(List.of(
            PossibleOrderChangeRequest.builder()
                .partnerId(1L)
                .type(PossibleOrderChangeType.ORDER_ITEMS)
                .method(PossibleOrderChangeMethod.PARTNER_API)
                .checkpointStatusFrom(113)
                .checkpointStatusTo(114)
                .enabled(true)
                .build(),
            PossibleOrderChangeRequest.builder()
                .partnerId(1L)
                .type(PossibleOrderChangeType.SHOW_RUNNING_COURIER)
                .method(PossibleOrderChangeMethod.PARTNER_API)
                .checkpointStatusFrom(115)
                .checkpointStatusTo(116)
                .enabled(true)
                .build(),
            PossibleOrderChangeRequest.builder()
                .partnerId(1L)
                .type(PossibleOrderChangeType.ORDER_ITEMS)
                .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                .checkpointStatusFrom(117)
                .checkpointStatusTo(118)
                .enabled(true)
                .build()
        ));

        softly.assertThat(apiResponse).isEqualTo(List.of(
            PossibleOrderChangeDto.builder()
                .id(1L)
                .partnerId(1L)
                .type(PossibleOrderChangeType.ORDER_ITEMS)
                .method(PossibleOrderChangeMethod.PARTNER_API)
                .checkpointStatusFrom(113)
                .checkpointStatusTo(114)
                .enabled(true)
                .enabledAt(Instant.parse("2020-02-20T20:20:20Z"))
                .build(),
            PossibleOrderChangeDto.builder()
                .id(2L)
                .partnerId(1L)
                .type(PossibleOrderChangeType.SHOW_RUNNING_COURIER)
                .method(PossibleOrderChangeMethod.PARTNER_API)
                .checkpointStatusFrom(115)
                .checkpointStatusTo(116)
                .enabled(true)
                .enabledAt(Instant.parse("2020-02-20T20:20:20Z"))
                .build(),
            PossibleOrderChangeDto.builder()
                .id(3L)
                .partnerId(1L)
                .type(PossibleOrderChangeType.ORDER_ITEMS)
                .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                .checkpointStatusFrom(117)
                .checkpointStatusTo(118)
                .enabled(true)
                .enabledAt(Instant.parse("2020-02-20T20:20:20Z"))
                .build()
        ));
    }
}

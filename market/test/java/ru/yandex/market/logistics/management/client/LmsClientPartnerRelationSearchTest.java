package ru.yandex.market.logistics.management.client;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.ProductRatingDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientPartnerRelationSearchTest extends AbstractClientTest {
    @Test
    void searchPartnerRelations() {
        sendSearchRequest(
            "data/controller/partnerRelation/search_by_partner_ids_filter.json",
            "data/controller/partnerRelation/partner_relation_1_response.json"
        );

        softly.assertThat(client.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(1L)
                .toPartnerId(2L)
                .build()
        )).usingRecursiveFieldByFieldElementComparator().isEqualTo(
            List.of(buildExpectedPartnerRelationEntityDto())
        );
    }

    @Test
    void searchPartnerRelationsByIdsSet() {
        sendSearchRequest(
            "data/controller/partnerRelation/search_by_partners_ids_set_filter.json",
            "data/controller/partnerRelation/partner_relation_1_3_response.json"
        );

        List<PartnerRelationEntityDto> partnerRelations = client.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(ImmutableSet.of(1L, 3L))
                .toPartnersIds(ImmutableSet.of(2L))
                .build()
        );

        softly.assertThat(partnerRelations).usingRecursiveFieldByFieldElementComparator().isEqualTo(
            List.of(
                buildExpectedPartnerRelationEntityDto(),
                PartnerRelationEntityDto.newBuilder()
                    .id(3L)
                    .fromPartnerId(3L)
                    .fromPartnerType(PartnerType.FULFILLMENT)
                    .toPartnerId(2L)
                    .toPartnerType(PartnerType.DELIVERY)
                    .handlingTime(1)
                    .returnPartnerId(1L)
                    .importSchedule(Collections.emptySet())
                    .shipmentType(ShipmentType.WITHDRAW)
                    .enabled(true)
                    .intakeSchedule(Collections.emptySet())
                    .registerSchedule(Collections.emptySet())
                    .productRatings(Collections.emptySet())
                    .cutoffs(Collections.emptySet())
                    .build()
            )
        );
    }

    private PartnerRelationEntityDto buildExpectedPartnerRelationEntityDto() {
        return PartnerRelationEntityDto.newBuilder()
            .id(1L)
            .fromPartnerId(1L)
            .fromPartnerType(PartnerType.FULFILLMENT)
            .toPartnerId(2L)
            .toPartnerType(PartnerType.DELIVERY)
            .toPartnerLogisticsPointId(1L)
            .handlingTime(10)
            .returnPartnerId(1L)
            .shipmentType(ShipmentType.WITHDRAW)
            .enabled(false)
            .productRatings(Set.of(
                ProductRatingDto.newBuilder()
                    .id(1L)
                    .partnerRelationId(1L)
                    .locationId(225)
                    .rating(20)
                    .build(),
                ProductRatingDto.newBuilder()
                    .id(2L)
                    .partnerRelationId(1L)
                    .locationId(213)
                    .rating(10)
                    .build(),
                ProductRatingDto.newBuilder()
                    .id(3L)
                    .partnerRelationId(1L)
                    .locationId(1)
                    .rating(15)
                    .build()
            ))
            .cutoffs(Set.of(CutoffResponse.newBuilder()
                .id(1L)
                .locationId(1)
                .cutoffTime(LocalTime.of(14, 9))
                .packagingDuration(Duration.parse("PT12H30M"))
                .build()
            ))
            .importSchedule(Set.of(
                new ScheduleDayResponse(5L, 5, LocalTime.of(14, 0), LocalTime.of(17, 0), false),
                new ScheduleDayResponse(6L, 6, LocalTime.of(15, 0), LocalTime.of(18, 0), false)
            ))
            .intakeSchedule(Set.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(12, 0), LocalTime.of(13, 0), false),
                new ScheduleDayResponse(2L, 2, LocalTime.of(13, 0), LocalTime.of(14, 0), false)
            ))
            .registerSchedule(Set.of(
                new ScheduleDayResponse(4L, 4, LocalTime.of(13, 0), LocalTime.of(16, 0), false),
                new ScheduleDayResponse(3L, 3, LocalTime.of(12, 0), LocalTime.of(15, 0), false)
            ))
            .build();
    }

    private void sendSearchRequest(String requestPath, String responsePath) {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/partner-relation/search").toUriString())
        )
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource(requestPath)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(responsePath))
            );
    }
}

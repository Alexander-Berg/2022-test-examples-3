package ru.yandex.market.logistics.management.client;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationActivateDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationCreateDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationUpdateDto;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientPartnerRelationTest extends AbstractClientTest {

    private static final String DATA_PACKAGE = "data/controller/partnerRelation/";

    @Test
    void createPartnerRelation() {
        sendRequest(
            "/externalApi/partner-relation",
            HttpMethod.POST,
            "new_partner_relation_with_multiple_schedules_for_one_day.json",
            "new_partner_relation_with_multiple_schedules_for_one_day_response.json"
        );

        PartnerRelationEntityDto partnerRelation = client.createPartnerRelation(getCreatePartnerRelationRequest());

        validateResponse(partnerRelation);
    }

    @Test
    void updatePartnerRelation() {
        sendRequest(
            "/externalApi/partner-relation/1",
            HttpMethod.PUT,
            "update_existing_partner_relation_with_multiple_schedules_for_one_day.json",
            "new_partner_relation_with_multiple_schedules_for_one_day_response.json"
        );

        PartnerRelationEntityDto partnerRelation = client.updatePartnerRelation(1L, getUpdatePartnerRelationRequest());

        validateResponse(partnerRelation);
    }

    @Test
    void activatePartnerRelationAndDependencies() {
        sendRequest(
            "/externalApi/partner-relation/4/activate",
            HttpMethod.PUT,
            "activate_partner_relation_both.json",
            "new_partner_relation_with_multiple_schedules_for_one_day_response.json"
        );

        PartnerRelationEntityDto partnerRelation = client.activatePartnerRelationAndDependencies(
            4L,
            getActivatePartnerRelationRequest()
        );

        validateResponse(partnerRelation);
    }

    private void validateResponse(PartnerRelationEntityDto partnerRelation) {
        PartnerRelationEntityDto expected = getExpected();

        softly.assertThat(partnerRelation).as("Proper partner relation should be returned")
            .isEqualToIgnoringGivenFields(expected, "importSchedule", "intakeSchedule", "registerSchedule");
        softly.assertThat(partnerRelation.getImportSchedule())
            .containsExactlyInAnyOrderElementsOf(expected.getImportSchedule());
        softly.assertThat(partnerRelation.getIntakeSchedule())
            .containsExactlyInAnyOrderElementsOf(expected.getIntakeSchedule());
        softly.assertThat(partnerRelation.getRegisterSchedule())
            .containsExactlyInAnyOrderElementsOf(expected.getRegisterSchedule());
    }

    private void sendRequest(String url, HttpMethod method, String requestPath, String responsePath) {
        mockServer.expect(requestTo(
            getBuilder(uri, url).toUriString()))
            .andExpect(method(method))
            .andExpect(content().json(jsonResource(DATA_PACKAGE + requestPath)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(DATA_PACKAGE + responsePath)));
    }

    private PartnerRelationEntityDto getExpected() {
        return PartnerRelationEntityDto.newBuilder()
            .id(4L)
            .fromPartnerId(1L)
            .toPartnerId(4L)
            .returnPartnerId(3L)
            .handlingTime(1)
            .shipmentType(ShipmentType.IMPORT)
            .enabled(true)
            .importSchedule(getImportSchedule())
            .intakeSchedule(getIntakeSchedule())
            .registerSchedule(getRegisterSchedule())
            .transferTime(Duration.ofHours(2))
            .inboundTime(Duration.ofHours(3))
            .intakeDeadline(Duration.ofHours(4))
            .build();
    }

    private PartnerRelationCreateDto getCreatePartnerRelationRequest() {
        return PartnerRelationCreateDto.newBuilder()
            .fromPartnerId(1L)
            .toPartnerId(4L)
            .returnPartnerId(3L)
            .handlingTime(1)
            .shipmentType(ShipmentType.IMPORT)
            .enabled(true)
            .registerSchedule(getRegisterSchedule())
            .intakeSchedule(getIntakeSchedule())
            .importSchedule(getImportSchedule())
            .transferTime(Duration.ofHours(2))
            .inboundTime(Duration.ofHours(3))
            .intakeDeadline(Duration.ofHours(4))
            .build();
    }

    private PartnerRelationUpdateDto getUpdatePartnerRelationRequest() {
        return PartnerRelationUpdateDto.newBuilder()
            .returnPartnerId(3L)
            .handlingTime(2)
            .enabled(true)
            .shipmentType(ShipmentType.WITHDRAW)
            .registerSchedule(getRegisterSchedule())
            .intakeSchedule(getIntakeSchedule())
            .importSchedule(getImportSchedule())
            .build();
    }

    private PartnerRelationActivateDto getActivatePartnerRelationRequest() {
        return PartnerRelationActivateDto.newBuilder()
            .activationType(PartnerRelationActivateDto.ActivationType.BOTH)
            .build();
    }

    private Set<ScheduleDayResponse> getRegisterSchedule() {
        LocalTime time = LocalTime.of(12, 0, 0);
        Set<ScheduleDayResponse> schedule = getSchedule(time);
        schedule.add(getScheduleDay(time.plusHours(1), 3));
        schedule.add(getScheduleDay(time.plusHours(2), 3));
        return schedule;
    }

    private Set<ScheduleDayResponse> getIntakeSchedule() {
        return getSchedule(LocalTime.of(13, 0, 0));
    }

    private Set<ScheduleDayResponse> getImportSchedule() {
        return getSchedule(LocalTime.of(14, 0, 0));
    }

    private Set<ScheduleDayResponse> getSchedule(LocalTime startTime) {
        Set<ScheduleDayResponse> schedule = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            schedule.add(getScheduleDay(startTime, i));
        }
        return schedule;
    }

    private ScheduleDayResponse getScheduleDay(LocalTime startTime, int dayNumber) {
        return new ScheduleDayResponse(null, dayNumber, startTime, startTime.plusHours(1));
    }
}

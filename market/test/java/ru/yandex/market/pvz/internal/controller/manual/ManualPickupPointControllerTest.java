package ru.yandex.market.pvz.internal.controller.manual;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveType;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.PickupPointChangeActiveLogRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualPickupPointControllerTest extends BaseShallowTest {

    private final Clock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointChangeActiveLogRepository pickupPointChangeActiveLogRepository;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointRepository pickupPointRepository;
    private final DeactivationReasonRepository deactivationReasonRepository;

    @Test
    void changeActiveTest() throws Exception {
        var pickupPoint = getPickupPointForTest(false);
        var pickupPoint2 = getPickupPointForTest(false);

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_deactivate.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_active.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful());

        var logs = pickupPointChangeActiveLogRepository.findAll();
        assertThat(logs.get(0).isOutdated()).isTrue();
        assertThat(logs.get(1).isOutdated()).isTrue();
        assertThat(logs.get(0).getAppliedAt()).isNotNull();
        assertThat(logs.get(1).getAppliedAt()).isNotNull();

        var actualPickupPoint = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(pickupPoint).isEqualTo(actualPickupPoint);

        var actualPickupPoint2 = pickupPointRepository.findByIdOrThrow(pickupPoint2.getId());
        assertThat(pickupPoint2).isEqualTo(actualPickupPoint2);
    }

    @Test
    void changeActiveDelayedTest() throws Exception {
        var pickupPoint = getPickupPointForTest(true);
        var pickupPoint2 = getPickupPointForTest(true);

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_active_delayed.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                LocalDate.now(clock).plusDays(10))))
                .andExpect(status().is2xxSuccessful());

        var logs = pickupPointChangeActiveLogRepository.findAll();
        assertThat(logs.get(0).isOutdated()).isFalse();
        assertThat(logs.get(1).isOutdated()).isFalse();
        assertThat(logs.get(0).getAppliedAt()).isNull();
        assertThat(logs.get(1).getAppliedAt()).isNull();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint.getId()).getActive()).isTrue();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint2.getId()).getActive()).isTrue();
    }

    @Test
    void changeActiveThenCancelTest() throws Exception {
        var pickupPoint = getPickupPointForTest(true);
        var pickupPoint2 = getPickupPointForTest(true);

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_active_delayed.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                LocalDate.now(clock).plusDays(10))))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_active_cancel.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful());

        var logs = pickupPointChangeActiveLogRepository.findAll();
        assertThat(logs.get(0).isOutdated()).isTrue();
        assertThat(logs.get(1).isOutdated()).isTrue();
        assertThat(logs.get(0).getAppliedAt()).isNull();
        assertThat(logs.get(1).getAppliedAt()).isNull();
        assertThat(logs.get(0).getType()).isEqualTo(ChangeActiveType.CANCEL);
        assertThat(logs.get(1).getType()).isEqualTo(ChangeActiveType.CANCEL);
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint.getId()).getActive()).isTrue();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint2.getId()).getActive()).isTrue();
    }

    @Test
    void changeActiveActivatedPickupPointTest() throws Exception {
        var pickupPoint = getPickupPointForTest(true);
        var pickupPoint2 = getPickupPointForTest(true);

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_change_active.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                LocalDate.now(clock).plusDays(10))))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void changeActiveNoIdsTest() throws Exception {

        mockMvc.perform(
                post("/manual/pickup-points/change-active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("manual/request_change_active_no_ids.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deactivate() throws Exception {
        var pickupPoint = getPickupPointForTest(true);
        var pickupPoint2 = getPickupPointForTest(true);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Причина",
                "Описание причины", true, true, null);

        mockMvc.perform(
                post("/manual/pickup-points/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_deactivation_with_reasons.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                deactivationReason.getId())))
                .andExpect(status().is2xxSuccessful());

        var actualPickupPoint = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(pickupPoint).isEqualTo(actualPickupPoint);

        var actualPickupPoint2 = pickupPointRepository.findByIdOrThrow(pickupPoint2.getId());
        assertThat(pickupPoint2).isEqualTo(actualPickupPoint2);
    }

    @Test
    void cancelDeactivation() throws Exception {
        var pickupPoint = getPickupPointForTest(true);
        var pickupPoint2 = getPickupPointForTest(true);

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Причина",
                "Описание причины", true, true, "LOGISTIC");

        mockMvc.perform(
                post("/manual/pickup-points/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_deactivation_with_reasons.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                deactivationReason.getId())))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                patch("/manual/pickup-points/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("manual/request_deactivation_with_reasons.json"),
                                pickupPoint.getPvzMarketId(),
                                pickupPoint2.getPvzMarketId(),
                                deactivationReason.getId())))
                .andExpect(status().is2xxSuccessful());

        var actualPickupPoint = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(pickupPoint).isEqualTo(actualPickupPoint);

        var actualPickupPoint2 = pickupPointRepository.findByIdOrThrow(pickupPoint2.getId());
        assertThat(pickupPoint2).isEqualTo(actualPickupPoint2);
    }

    @Test
    void addDeactivationReason() throws Exception {

        mockMvc.perform(
                post("/manual/pickup-points/deactivation-reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("manual/request_deactivation_reason.json")))
                .andExpect(status().is2xxSuccessful());

        var deactivationReasons = deactivationReasonRepository.findAll();
        assertThat(deactivationReasons.size()).isEqualTo(1);
        assertThat(deactivationReasons.get(0).getReason()).isEqualTo("Причина");
        assertThat(deactivationReasons.get(0).getDetails()).isEqualTo("Описание причины");
        assertThat(deactivationReasons.get(0).isFullDeactivation()).isTrue();
        assertThat(deactivationReasons.get(0).isCanBeCancelled()).isTrue();
    }

    private PickupPoint getPickupPointForTest(boolean isActive) {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(true)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(isActive)
                        .build());

        return pickupPoint;
    }
}

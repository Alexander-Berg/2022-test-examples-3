package ru.yandex.market.pvz.internal.domain.lms.pickup_point;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.domain.lms.pickup_point.dto.LmsPickupPointBatchToggleDto;
import ru.yandex.market.pvz.internal.domain.lms.pickup_point.model.PickupPointCancelDeactivationModel;
import ru.yandex.market.pvz.internal.domain.lms.pickup_point.model.PickupPointDeactivationModel;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Import(LmsBatchPvzToggleService.class)
class LmsBatchPvzToggleServiceTest {

    private final Clock clock;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;

    private final LmsBatchPvzToggleService lmsBatchPvzToggleService;

    private final TestPickupPointFactory pickupPointFactory;

    @Test
    void testSuccessFlow() {
        var pickupPoint = createPickupPoint();
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("1", "1", true, true, null);

        ResponseEntity<?> responseEntity = lmsBatchPvzToggleService.batchPvzToggle(new LmsPickupPointBatchToggleDto(
                buildDeactivationCsv(List.of(
                        PickupPointDeactivationModel.builder()
                                .lmsId(pickupPoint.getLmsId())
                                .reasonId(deactivationReason.getId())
                                .date(LocalDate.now(clock))
                                .build()
                )),
                null
        ));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deactivation = pickupPointDeactivationLogRepository
                .findFirstByPickupPointIdAndDeactivationReasonIdAndActivationDateIsNull(pickupPoint.getId(),
                        deactivationReason.getId()).get();

        assertThat(deactivation.getDeactivationDate()).isEqualTo(LocalDate.now(clock));
        assertThat(deactivation.getDeactivationAppliedAt()).isNotNull();
        assertThat(deactivation.isCancelled()).isFalse();

        responseEntity = lmsBatchPvzToggleService.batchPvzToggle(new LmsPickupPointBatchToggleDto(
                null,
                buildActivationCsv(List.of(
                        PickupPointCancelDeactivationModel.builder()
                                .lmsId(pickupPoint.getLmsId())
                                .reasonId(deactivationReason.getId())
                                .build()
                ))
        ));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        deactivation = pickupPointDeactivationLogRepository.findByIdOrThrow(deactivation.getId());
        assertThat(deactivation.isCancelled()).isTrue();
    }

    @Test
    void testErrorInDeactivation() {
        var pickupPoint = createPickupPoint();

        ResponseEntity<?> responseEntity = lmsBatchPvzToggleService.batchPvzToggle(new LmsPickupPointBatchToggleDto(
                buildDeactivationCsv(List.of(
                        PickupPointDeactivationModel.builder()
                                .lmsId(pickupPoint.getLmsId())
                                .reasonId(9999)
                                .date(LocalDate.now(clock))
                                .build()
                )),
                null
        ));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testErrorInActivation() {
        var pickupPoint = createPickupPoint();

        ResponseEntity<?> responseEntity = lmsBatchPvzToggleService.batchPvzToggle(new LmsPickupPointBatchToggleDto(
                null,
                buildActivationCsv(List.of(
                        PickupPointCancelDeactivationModel.builder()
                                .lmsId(pickupPoint.getLmsId())
                                .reasonId(1)
                                .build()
                ))
        ));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private byte[] buildDeactivationCsv(List<PickupPointDeactivationModel> deactivations) {
        StringBuilder csvBuilder = new StringBuilder("lmsId,reasonId,date\n");
        for (PickupPointDeactivationModel deactivation : deactivations) {
            csvBuilder.append(sf(
                    "{},{},{}\n",
                    deactivation.getLmsId(),
                    deactivation.getReasonId(),
                    deactivation.getDate().format(DateTimeFormatter.ISO_DATE)
            ));
        }
        return csvBuilder.toString().getBytes();
    }

    private byte[] buildActivationCsv(List<PickupPointCancelDeactivationModel> activations) {
        StringBuilder csvBuilder = new StringBuilder("lmsId,reasonId\n");
        for (PickupPointCancelDeactivationModel activation : activations) {
            csvBuilder.append(sf(
                    "{},{}\n",
                    activation.getLmsId(),
                    activation.getReasonId()
            ));
        }
        return csvBuilder.toString().getBytes();
    }

    private PickupPoint createPickupPoint() {
        return pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .active(true)
                        .build());
    }

}

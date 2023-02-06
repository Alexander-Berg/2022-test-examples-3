package ru.yandex.market.tpl.carrier.planner.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.LegalEntity;
import ru.yandex.market.logistic.api.model.common.LegalForm;
import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Movement;
import ru.yandex.market.logistic.api.model.common.MovementType;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.TripInfo;
import ru.yandex.market.logistic.api.model.common.TripType;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil;
import ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementHelper;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DsOrderManagerTest extends BasePlannerWebTest {
    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final PutMovementHelper putMovementHelper;
    private final EntityManager entityManager;
    private final MovementRepository movementRepository;
    private final RunRepository runRepository;
    private final RunHelper runHelper;

    private Company company;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(tx -> {
            company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
            deliveryService = dsRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
            deliveryService.setToken("ds_token");
            entityManager.flush();
            return null;
        });
    }

    private Movement buildMovement(Long runId, String tripId,
                                   String movementId, String movementExternalId, MovementType movementType,
                                   Integer fromIndex, Integer toIndex, Integer totalCount, TripType tripType,
                                   String partyFromId, String partyFromYandexId,
                                   String partyToId, String partyToYandexId) {
        String routeName = "Route";
        Long price = 3000L;
        TripInfo trip = new TripInfo(
                ResourceId.builder().setPartnerId(String.valueOf(runId)).setYandexId(tripId).build(),
                routeName,
                fromIndex,
                toIndex,
                totalCount,
                price,
                tripType);
        String comment = "Hello!";
        LegalEntity legalEntity = LegalEntity.builder()
                .setLegalName("name")
                .setLegalForm(LegalForm.OOO)
                .setName("name")
                .build();
        DateTimeInterval interval = new DateTimeInterval(OffsetDateTime.MIN, OffsetDateTime.MAX);
        Location location = Location.builder("Ru", "Ru", "Ru").setLat(BigDecimal.ONE).setLng(BigDecimal.ONE).build();
        Party partyFrom = Party.builder(
                LogisticPoint.builder(
                        ResourceId.builder().setPartnerId(partyFromId).setYandexId(partyFromYandexId).build()
                        )
                .setLocation(location)
                .setName("from")
                .build()
        )
        .setLegalEntity(legalEntity)
        .build();
        Party partyTo = Party.builder(
                LogisticPoint.builder(
                        ResourceId.builder().setPartnerId(partyToId).setYandexId(partyToYandexId).build()
                )
                .setLocation(location)
                .setName("from")
                .build()
        )
        .setLegalEntity(legalEntity)
        .build();

        return Movement.builder(
                ResourceId.builder().setPartnerId(movementId).setYandexId(movementExternalId).build(),
                interval,
                new BigDecimal(33))
            .setTrip(trip)
            .setMaxPalletCapacity(33)
            .setShipper(partyFrom)
            .setReceiver(partyTo)
            .setComment(comment)
            .setType(movementType)
            .setWeight(new BigDecimal(10))
            .setInboundInterval(interval)
            .setOutboundInterval(interval)
        .build();
    }

    @Test
    void shouldAcceptTwoScheduledMovements() {
        var scheduledPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("2", "2"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 4, 4,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 3, 4,
                                3000L, TripType.MAIN)
                ));

        {
            putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest1);
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(runRepository.findByExternalId("TMT1001").isEmpty());
        }

        {
            putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest2);
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
        }
    }


    @Test
    void shouldAcceptTwoScheduledAndTwoAdditionalMovements() {
        var scheduledPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 4, 4,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 3, 4,
                                3000L, TripType.MAIN)
                ));

        var scheduledPutMovementUpdateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 8, 8,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementUpdateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                4, 5, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM3", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 7, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM4", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                3, 6, 8,
                                3000L, TripType.MAIN)
                ));

        //создаем рейс по расписанию из 2 мувментов
        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(runRepository.findByExternalId("TMT1001").isEmpty());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });


        //в рейс догружается два дополнительных мувмента, происходит update старых и create догруженных мувментов
        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());
            Assertions.assertFalse(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertTrue(additionalMovement1.isApplied());
            Assertions.assertTrue(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(4, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

    }

    @Test
    void shouldAcceptTwoScheduledAndTwoAdditionalMovementsInConfirmedRun() {
        var scheduledPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 4, 4,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 3, 4,
                                3000L, TripType.MAIN)
                ));

        var scheduledPutMovementUpdateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 8, 8,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementUpdateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                4, 5, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM3", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 7, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM4", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                3, 6, 8,
                                3000L, TripType.MAIN)
                ));

        //создаем рейс по расписанию из 2 мувментов
        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());

            Assertions.assertTrue(runRepository.findByExternalId("TMT1001").isEmpty());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());


            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

        //confirm-им рейс
        runHelper.confirm(runRepository.findByExternalId("TMT1001").orElseThrow());

        //в рейс догружается два дополнительных мувмента, происходит update старых и create догруженных мувментов
        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CONFIRMED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CONFIRMED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());
            Assertions.assertFalse(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CONFIRMED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertTrue(additionalMovement1.isApplied());
            Assertions.assertTrue(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(4, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.CONFIRMED, run.getStatus());
            return null;
        });
    }

    @Test
    void shouldAcceptTwoScheduledAndTwoAdditionalMovementsInAssignedRun() {
        var scheduledPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 4, 4,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 3, 4,
                                3000L, TripType.MAIN)
                ));

        var scheduledPutMovementUpdateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                1, 8, 8,
                                3000L, TripType.MAIN)
                ));
        var scheduledPutMovementUpdateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null), //movement
                        new ResourceId("2", "2"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                4, 5, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest1 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM3", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                2, 7, 8,
                                3000L, TripType.MAIN)
                ));
        var additionalPutMovementCreateRequest2 = PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM4", null), //movement
                        new ResourceId("1", "1"), //from
                        new ResourceId("3", "3"),//to
                        new TripInfo(new ResourceId("TMT1001", null), "Route",
                                3, 6, 8,
                                3000L, TripType.MAIN)
                ));

        //создаем рейс по расписанию из 2 мувментов
        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());

            Assertions.assertTrue(runRepository.findByExternalId("TMT1001").isEmpty());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(RunStatus.CREATED, run.getStatus());
            return null;
        });

        //assign-им рейс
        runHelper.assignUserAndTransport(runRepository.findByExternalId("TMT1001").orElseThrow(),
                testUserHelper.findOrCreateUser(1L, company.getName()),
                testUserHelper.findOrCreateTransport("бэтмобиль", company.getName()));

        //в рейс догружается два дополнительных мувмента, происходит update старых и create догруженных мувментов
        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.ASSIGNED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest1);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.ASSIGNED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(additionalPutMovementCreateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertFalse(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertFalse(additionalMovement1.isApplied());
            Assertions.assertFalse(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(2, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.ASSIGNED, run.getStatus());
            return null;
        });

        putMovementHelper.performPutMovement(scheduledPutMovementUpdateRequest2);
        transactionTemplate.execute(tx -> {
            var scheduledMovement1 = movementRepository.findByExternalId("TMM1").orElseThrow();
            var scheduledMovement2 = movementRepository.findByExternalId("TMM2").orElseThrow();
            var additionalMovement1 = movementRepository.findByExternalId("TMM3").orElseThrow();
            var additionalMovement2 = movementRepository.findByExternalId("TMM4").orElseThrow();
            Assertions.assertTrue(scheduledMovement1.isApplied());
            Assertions.assertTrue(scheduledMovement2.isApplied());
            Assertions.assertTrue(additionalMovement1.isApplied());
            Assertions.assertTrue(additionalMovement2.isApplied());

            var run = runRepository.findByExternalId("TMT1001").orElseThrow();
            Assertions.assertEquals(4, run.streamRunItems().count());
            Assertions.assertEquals(RunStatus.ASSIGNED, run.getStatus());
            return null;
        });
    }

}

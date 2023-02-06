package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.TripInfo;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.PutMovementRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItemAbstract;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDoc;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDocRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties.BUFFERED_PUT_MOVEMENT_DISABLED;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.prepareMovement;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PutMovementControllerTripTest extends BasePlannerWebTest {

    private static final String TRIP_YANDEX_ID = "TMM123";

    private final PutMovementHelper putMovementHelper;
    private final TestUserHelper testUserHelper;
    private final DutyGenerator dutyGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;

    private final RunRepository runRepository;
    private final DutyRepository dutyRepository;
    private final UserShiftRepository userShiftRepository;
    private final CarRequestDocRepository carRequestDocRepository;

    private final RunCommandService runCommandService;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @SneakyThrows

    @Test
    void shouldSaveTrip() {
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                prepareMovement(
                        new ResourceId("TMM2", null),
                        PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                        BigDecimal.ONE,
                        new ResourceId("2", "1234"),
                        new ResourceId("3", "1234"),
                        INBOUND_DEFAULT_INTERVAL,
                        OUTBOUND_DEFAULT_INTERVAL,
                        mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                .setRouteName("RouteName")
                                .setFromIndex(0)
                                .setToIndex(1)
                                .setTotalCount(2)
                                .build())
                )
        ))
        .andExpect(status().isOk())
        .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            Assertions.assertThat(run.getName()).isEqualTo("RouteName");

            Assertions.assertThat(run.getStatus())
                    .isEqualTo(RunStatus.CREATED);
            List<Movement> movements = run.streamMovements().toList();

            List<CarRequestDoc> docs = carRequestDocRepository.findCarRequestDocByRun(run);
            Assertions.assertThat(docs).hasSize(1);

            Assertions.assertThat(movements).hasSize(1);
            Assertions.assertThat(movements.get(0).getExternalId()).isEqualTo("TMM2");
            return null;
        });
    }

    @Test
    @SneakyThrows
    void testWithCourier() {
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        User courier = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                        .setRouteName("RouteName")
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(4)
                                        .build()),
                                new PutMovementRestrictedData(courier.getId(), transport.getId())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM3", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("4", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                        .setRouteName("RouteName")
                                        .setFromIndex(2)
                                        .setToIndex(3)
                                        .setTotalCount(4)
                                        .build()),
                                new PutMovementRestrictedData(courier.getId(), transport.getId())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            UserShift userShift =
                    userShiftRepository.findByUserAndStatus(courier, UserShiftStatus.SHIFT_CREATED).get(0);

            Assertions.assertThat(userShift.getRunId()).isEqualTo(run.getId());
            Assertions.assertThat(userShift.getTransport()).isEqualTo(transport);
            Assertions.assertThat(userShift.getUser()).isEqualTo(courier);

            return null;
        });
    }

    @Test
    @SneakyThrows
    void shouldFindTripByPartnerId() {

        Company company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        OrderWarehouse orderWarehouse = orderWarehouseGenerator.generateWarehouse();

        Duty duty = dutyGenerator.generate(db -> db.deliveryServiceId(100500L)
                .dutyWarehouseId(orderWarehouse.getYandexId())
        );
        Run run = duty.getRunDuty().stream().findAny().map(RunItemAbstract::getRun).orElseThrow();

        runCommandService.confirm(new RunCommand.Confirm(run.getId()));

        var transport = testUserHelper.findOrCreateTransport();

        var user = testUserHelper.findOrCreateUser(998L);

        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.arriveAtRoutePoint(userShift.getFirstRoutePoint());

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, String.valueOf(run.getId())))
                                        .setRouteName("RouteName")
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(2)
                                        .setPrice(10_000_000L)
                                        .build())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run foundRun = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            Duty foundDuty = dutyRepository.findByIdOrThrow(duty.getId());
            Assertions.assertThat(foundRun.getName()).isEqualTo("RouteName");
            Assertions.assertThat(foundRun.getTotalCount()).isEqualTo(3);
            Assertions.assertThat(foundRun.getStatus()).isEqualTo(RunStatus.STARTED);
            Assertions.assertThat(foundRun.getPriceCents()).isEqualTo(10_000_000L);

            Assertions.assertThat(foundDuty.getStatus()).isEqualTo(DutyStatus.TRIP_CREATED);

            List<Movement> movements = foundRun.streamMovements().toList();

            Assertions.assertThat(movements).hasSize(1);
            Assertions.assertThat(movements.get(0).getExternalId()).isEqualTo("TMM2");
            return null;
        });
    }

    @Test
    @SneakyThrows
    void shouldNotCreateRunForDutyNotOnPoint() {

        Company company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        OrderWarehouse orderWarehouse = orderWarehouseGenerator.generateWarehouse();

        Duty duty = dutyGenerator.generate(db -> db.deliveryServiceId(100500L)
                .dutyWarehouseId(orderWarehouse.getYandexId())
                .name("test_duty")
                .priceCents(10_000L)
        );
        Run run = duty.getRunDuty().stream().findAny().map(RunItemAbstract::getRun).orElseThrow();

        runCommandService.confirm(new RunCommand.Confirm(run.getId()));

        var transport = testUserHelper.findOrCreateTransport();

        var user = testUserHelper.findOrCreateUser(998L);

        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        putMovementHelper.performPutMovementWithoutErrorCheck(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, String.valueOf(run.getId())))
                                        .setRouteName("RouteName")
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(2)
                                        .setPrice(10_000_000L)
                                        .build())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(true));

        transactionTemplate.execute(tc -> {
            Run foundRun = runRepository.findById(run.getId()).orElseThrow();
            Duty foundDuty = dutyRepository.findByIdOrThrow(duty.getId());
            Assertions.assertThat(foundRun.getName()).isEqualTo("test_duty");
            Assertions.assertThat(foundRun.getTotalCount()).isEqualTo(1);
            Assertions.assertThat(foundRun.getStatus()).isEqualTo(RunStatus.ASSIGNED);
            Assertions.assertThat(foundRun.getPriceCents()).isEqualTo(10_000L);

            Assertions.assertThat(foundDuty.getStatus()).isEqualTo(DutyStatus.ASSIGNED);

            return null;
        });
    }


    @SneakyThrows
    @Test
    @Deprecated
    void shouldAddMovementsToTrip() {
        configurationServiceAdapter.mergeValue(BUFFERED_PUT_MOVEMENT_DISABLED, true);

        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                prepareMovement(
                        new ResourceId("TMM2", null),
                        PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                        BigDecimal.ONE,
                        new ResourceId("2", "1234"),
                        new ResourceId("3", "1234"),
                        INBOUND_DEFAULT_INTERVAL,
                        OUTBOUND_DEFAULT_INTERVAL,
                        mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                .setFromIndex(0)
                                .setToIndex(1)
                                .setTotalCount(4)
                                .build())
                )
        ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            Assertions.assertThat(run.getStatus())
                    .isEqualTo(RunStatus.DRAFT);

            return null;
        });

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                prepareMovement(
                        new ResourceId("TMM3", null),
                        PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                        BigDecimal.ONE,
                        new ResourceId("4", "1234"),
                        new ResourceId("5", "1234"),
                        INBOUND_DEFAULT_INTERVAL,
                        OUTBOUND_DEFAULT_INTERVAL,
                        mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                .setFromIndex(2)
                                .setToIndex(3)
                                .setTotalCount(4)
                                .build())
                )
        ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            Assertions.assertThat(run.getStatus())
                    .isEqualTo(RunStatus.CREATED);

            List<RunItem> runItems = run.streamRunItems().toList();

            Assertions.assertThat(runItems).hasSize(2);
            Assertions.assertThat(runItems.get(0).getMovement().getExternalId()).isEqualTo("TMM2");
            Assertions.assertThat(runItems.get(0).getFromIndex()).isEqualTo(0);
            Assertions.assertThat(runItems.get(0).getToIndex()).isEqualTo(1);
            Assertions.assertThat(runItems.get(1).getMovement().getExternalId()).isEqualTo("TMM3");
            Assertions.assertThat(runItems.get(1).getFromIndex()).isEqualTo(2);
            Assertions.assertThat(runItems.get(1).getToIndex()).isEqualTo(3);
            return null;
        });
    }

    @SneakyThrows
    @Test
    @Deprecated
    void shouldUpdateRunDate() {
        configurationServiceAdapter.mergeValue(BUFFERED_PUT_MOVEMENT_DISABLED, true);

        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(Company.DEFAULT_CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(100500L))
                .build());

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(4)
                                        .build())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();
            Assertions.assertThat(run.getStatus())
                    .isEqualTo(RunStatus.DRAFT);

            return null;
        });

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM3", null),
                                DateTimeInterval.fromFormattedValue(
                                        "2021-03-02T18:00:00+03:00/2021-03-03T19:00:00+03:00"
                                ),
                                BigDecimal.ONE,
                                new ResourceId("4", "1234"),
                                new ResourceId("5", "1234"),
                                DateTimeInterval.fromFormattedValue(
                                        "2021-03-03T18:00:00+03:00/2021-03-03T19:00:00+03:00"
                                ),
                                DateTimeInterval.fromFormattedValue(
                                        "2021-03-02T18:00:00+03:00/2021-03-02T19:00:00+03:00"
                                ),
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId(TRIP_YANDEX_ID, null))
                                        .setFromIndex(2)
                                        .setToIndex(3)
                                        .setTotalCount(4)
                                        .build())
                        )
                ))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));

        transactionTemplate.execute(tc -> {
            Run run = runRepository.findByExternalId(TRIP_YANDEX_ID).orElseThrow();

            Assertions.assertThat(run.getRunDate()).isEqualTo(LocalDate.of(2021, 3, 2));
            return null;
        });
    }
}

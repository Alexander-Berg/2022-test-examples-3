package ru.yandex.market.tpl.carrier.tms.dbqueue.run;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.maps.client.MapsClient;
import ru.yandex.market.tpl.common.maps.client.RouteSummary;

@TmsIntTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UpdateRunLengthServiceTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunRepository runRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final MapsClient mapsClient;
    private final TestableClock clock;

    private final DbQueueTestUtil dbQueueTestUtil;

    private User user;
    private Transport transport;
    private Run run;
    private LocalDate runDate;
    private Instant expectedFirst;
    private Instant expectedSecond;

    @Test
    void shouldUpdateEstimateTime() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 600000); //10 mins

        testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();

        runDate = LocalDate.now(clock); // 1989-12-31T21:00:00Z
        expectedFirst = Instant.now(clock).minusSeconds(4 * 60 * 60); // 1989-12-31T17:00:00Z
        expectedSecond = Instant.now(clock).minusSeconds(2 * 60 * 60); // 1989-12-31T19:00:00Z

        Mockito.when(mapsClient.getRouteSummary(Mockito.any()))
                .thenReturn(Optional.of(RouteSummary.builder()
                        .lengthMeters(300_000)
                        .build()));
        run = runGenerator.generate(r -> r
                .runDate(runDate)
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .outboundArrivalTime(expectedFirst)
                                .inboundArrivalTime(expectedSecond)
                                .build())
                        .orderNumber(1)
                        .build()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_RUN_LENGTH);
        run = runRepository.findById(run.getId()).get();
        Assertions.assertThat(run.getLength()).isEqualTo(300);
    }
}

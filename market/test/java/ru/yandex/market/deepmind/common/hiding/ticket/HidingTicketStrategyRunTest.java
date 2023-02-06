package ru.yandex.market.deepmind.common.hiding.ticket;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;
import ru.yandex.market.deepmind.common.hiding.configuration.HidingConfiguration;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffService;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffServiceMock;

/**
 * Тесты {@link HidingTicketStrategy}, которые проверяют логику запуска обработки.
 */
public class HidingTicketStrategyRunTest extends BaseHidingTicketTest {
    private HidingTicketServiceMock hidingTicketService;
    private HidingDiffService hidingDiffService;
    private HidingTicketSskuStatusService hidingTicketSskuStatusService;

    private HidingTicketStrategy hidingTicketStrategy;

    @Before
    public void setUp() {
        hidingTicketService = new HidingTicketServiceMock();
        hidingDiffService = new HidingDiffServiceMock();
        hidingTicketSskuStatusService = new HidingTicketSskuStatusService(hidingTicketSskuRepository,
            hidingTicketHistoryRepository,
            hidingTicketService);
        hidingTicketStrategy = new HidingTicketStrategy(
            hidingDiffService,
            hidingTicketService,
            hidingTicketProcessingRepository,
            hidingTicketSskuStatusService,
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            hidingRepository,
            deepmindCategoryTeamRepository,
            "http://url.com"
        );
    }

    @Test
    public void testRunWithoutTicketProcessing() {
        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all).isEmpty();
    }

    @Test
    public void testSimpleRun() {
        var nowTime = setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(new HidingTicketProcessing().setReasonKey("REASON").setEnabled(true));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "enabled", "forceRun", "lastRunTs", "LastRunCorefixTs")
            .containsExactly(
                new HidingTicketProcessing()
                    .setReasonKey("REASON")
                    .setEnabled(true)
                    .setForceRun(false)
                    .setLastRunTs(nowTime)
                    .setLastRunCorefixTs(nowTime)
            );
    }

    @Test
    public void testSimpleRunForCorefix() {
        var nowTime = setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(new HidingTicketProcessing().setReasonKey("REASON").setEnabled(true));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTicketsForCorefix(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "enabled", "forceRun", "lastRunTs", "lastRunCorefixTs")
            .containsExactly(
                new HidingTicketProcessing()
                    .setReasonKey("REASON")
                    .setEnabled(true)
                    .setForceRun(false)
                    .setLastRunCorefixTs(nowTime)
            );
    }

    @Test
    public void testRunIfCorefixWasRunOnThisWeek() {
        var nowTime = setRunTime("2020-11-05T10:15:30.00Z");
        hidingTicketProcessingRepository.save(
            ticketProcessing("REASON", "2020-10-03T10:00:00.00Z")
                .setLastRunCorefixTs(Instant.parse("2020-11-03T10:15:30.00Z"))
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "enabled", "forceRun", "lastRunTs", "lastRunCorefixTs")
            .containsExactly(
                new HidingTicketProcessing()
                    .setReasonKey("REASON")
                    .setEnabled(true)
                    .setForceRun(false)
                    .setLastRunTs(nowTime)
                    .setLastRunCorefixTs(nowTime)
            );
    }

    @Test
    public void testRunWithNotEnabledConfiguration() {
        hidingTicketProcessingRepository.save(new HidingTicketProcessing()
            .setReasonKey("REASON").setEnabled(false));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "enabled", "forceRun", "lastRunTs", "lastRunCorefixTs")
            .containsExactly(
                new HidingTicketProcessing()
                    .setReasonKey("REASON")
                    .setEnabled(false)
                    .setForceRun(false)
            );
    }

    @Test
    public void enabledIsFalseAndForceRunIsTrueIsNotAllowed() {
        Assertions.assertThatThrownBy(() -> {
            hidingTicketProcessingRepository.save(new HidingTicketProcessing()
                .setReasonKey("REASON")
                .setEnabled(false)
                .setForceRun(true)
            );
        }).hasMessageContaining("force_run_can_be_true_when_enabled_is_true");
    }

    @Test
    public void dontProcessIfItAlreadyWasOnThisWeekend() {
        setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(ticketProcessing("REASON", "2020-11-02T10:00:00.00Z"));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "lastRunTs")
            .containsExactly(ticketProcessing("REASON", "2020-11-02T10:00:00.00Z"));
    }

    @Test
    public void dontProcessIfItAlreadyWasTodayForCorefix() {
        setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(ticketProcessing("REASON", "2020-11-03T10:10:30.00Z"));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTicketsForCorefix(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "lastRunTs")
            .containsExactly(ticketProcessing("REASON", "2020-11-03T10:10:30.00Z"));
    }

    @Test
    public void processIfItAlreadyWasOnThisWeekendAndForceRunIsTrue() {
        setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(
            ticketProcessing("REASON", "2020-11-02T10:00:00.00Z")
                .setForceRun(true)
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "lastRunTs", "forceRun")
            .containsExactly(
                ticketProcessing("REASON", "2020-11-03T10:15:30.00Z")
                    .setForceRun(false) // force run should changed to false
            );
    }

    @Test
    public void processIfItWasOnPrevWeekend() {
        setRunTime("2020-11-03T10:15:30.00Z");
        hidingTicketProcessingRepository.save(ticketProcessing("REASON", "2020-10-29T11:00:00.00Z"));

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTickets(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "lastRunTs")
            .containsExactly(
                ticketProcessing("REASON", "2020-11-03T10:15:30.00Z")
            );
    }

    @Test
    public void processIfItWasYesterdayForCorefix() {
        setRunTime("2020-11-03T11:00:00.00Z");
        hidingTicketProcessingRepository.save(
            ticketProcessing("REASON", "2020-11-02T10:00:00.00Z")
                .setLastRunCorefixTs(Instant.parse("2020-11-02T10:00:00.00Z"))
        );

        var config = random.nextObject(HidingConfiguration.class)
            .setReasonKey("REASON");
        hidingTicketStrategy.processTicketsForCorefix(config);

        var all = hidingTicketProcessingRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "lastRunCorefixTs")
            .containsExactly(
                new HidingTicketProcessing()
                    .setReasonKey("REASON")
                    .setLastRunCorefixTs(Instant.parse("2020-11-03T11:00:00.00Z"))
            );
    }

    private Instant setRunTime(String time) {
        var instant = Instant.parse(time);
        hidingTicketStrategy.setClock(Clock.fixed(instant, ZoneOffset.UTC));
        return instant;
    }
}

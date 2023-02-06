package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDate;
import java.util.Collections;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepositoryMock;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class UpdateDeadlineFromTrackerExecutorTest extends BaseDbTestClass {

    private TrackerServiceMock trackerServiceMock;
    private OfferRepositoryMock repositoryMock;
    private UpdateDeadlineFromTrackerExecutor executor;
    private ProcessingTicketInfoService processingTicketInfoService;
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataSource slaveDataSource;
    @Autowired
    private DataSource masterDataSource;

    @Before
    public void setup() {
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());
        repositoryMock = Mockito.spy(new OfferRepositoryMock());
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(
                new ProcessingTicketInfoRepositoryMock());

        offerBatchProcessor = new OfferBatchProcessor(slaveDataSource, masterDataSource,
            transactionManager, transactionManager, repositoryMock, repositoryMock, transactionTemplate);

        executor = new UpdateDeadlineFromTrackerExecutor(trackerServiceMock, processingTicketInfoService,
            offerBatchProcessor);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldProcessCorrectTypes() {
        executor.execute();
        Mockito.verify(trackerServiceMock, Mockito.times(4))
                .fetchOpenTickets(Mockito.any());
        Mockito.verify(trackerServiceMock, Mockito.times(1))
                .fetchOpenTickets(Mockito.eq(TicketType.CLASSIFICATION));
        Mockito.verify(trackerServiceMock, Mockito.times(1))
                .fetchOpenTickets(Mockito.eq(TicketType.RECLASSIFICATION));
        Mockito.verify(trackerServiceMock, Mockito.times(1))
                .fetchOpenTickets(Mockito.eq(TicketType.MATCHING));
        Mockito.verify(trackerServiceMock, Mockito.times(1))
                .fetchOpenTickets(Mockito.eq(TicketType.RE_SORT));
    }

    @Test
    public void shouldNotUpdateOfferForClosedTicket() {
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.empty());
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket(issue1.getKey())
                .setTicketDeadline(LocalDate.now())
                .setTicketCritical(false));
        LocalDate deadline = LocalDate.now().plusDays(2);
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket("FAKE_TICKET")
                .setTicketDeadline(deadline)
                .setTicketCritical(false));
        executor.execute();
        //first offer - deadline erased, second - remains the same
        Assertions.assertThat(repositoryMock.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
                .extracting(Offer::getTicketDeadline)
                .containsExactly(null, deadline);
    }

    @Test
    public void shouldUpdateOfferIfDeadlineChanged() {
        LocalDate deadline1 = LocalDate.now().plusDays(2);
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(org.joda.time.LocalDate.now().plusDays(2)));
        LocalDate deadline2 = null;
        IssueMock issue2 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.empty());
        LocalDate deadline3 = LocalDate.now();
        IssueMock issue3 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(org.joda.time.LocalDate.now()));

        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket(issue1.getKey())
                .setTicketDeadline(LocalDate.now())
                .setTicketCritical(false));
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setTrackerTicket(issue2.getKey())
                .setTicketDeadline(LocalDate.now())
                .setTicketCritical(false));
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECLASSIFICATION)
                .setTrackerTicket(issue3.getKey())
                .setTicketDeadline(null)
                .setTicketCritical(false));

        executor.execute();

        Mockito.verify(repositoryMock, Mockito.times(1))
                .updateOffers(Mockito.anyCollection());
        Assertions.assertThat(repositoryMock.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
                .extracting(Offer::getTicketDeadline)
                .containsExactly(deadline1, deadline2, deadline3);
    }

    @Test
    public void shouldNotUpdateOfferIfDeadlineIsSame() {
        LocalDate deadline1 = LocalDate.now();
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(org.joda.time.LocalDate.now()));
        LocalDate deadline2 = null;
        IssueMock issue2 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.empty());

        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket(issue1.getKey())
                .setTicketDeadline(LocalDate.now())
                .setTicketCritical(false));
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket(issue2.getKey())
                .setTicketDeadline(null)
                .setTicketCritical(false));

        executor.execute();

        Mockito.verify(repositoryMock, Mockito.times(0))
                .updateOffers(Mockito.anyCollection());
        Assertions.assertThat(repositoryMock.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
                .extracting(Offer::getTicketDeadline)
                .containsExactly(deadline1, deadline2);
    }


    @Test
    public void shouldFindDistinctTickets() {
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(org.joda.time.LocalDate.now()))
                .setKey("DOPPLEGANGER");
        IssueMock issue2 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(org.joda.time.LocalDate.now()))
                .setKey("DOPPLEGANGER");

        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setTrackerTicket("DOPPLEGANGER")
                .setTicketDeadline(null)
                .setTicketCritical(false));
        repositoryMock.insertOffer(OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .setTrackerTicket("DOPPLEGANGER")
                .setTicketDeadline(null)
                .setTicketCritical(false));

        executor.execute();

        Mockito.verify(repositoryMock, Mockito.times(1))
                .updateOffers(Mockito.anyCollection());
        Assertions.assertThat(repositoryMock.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
                .extracting(Offer::getTicketDeadline)
                .containsExactly(LocalDate.now(), LocalDate.now());
    }

    @Test
    public void shouldUpdatePriority() {
        org.joda.time.LocalDate deadline = org.joda.time.LocalDate.now();
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(deadline))
                .setKey("T1")
                .setPriority("critical");
        IssueMock issue2 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
                .setDeadline(Option.of(deadline))
                .setKey("T2");

        Offer issue1Offer = OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setTrackerTicket(issue1.getKey())
                .setTicketDeadline(LocalDate.now());
        var ticket1 = processingTicketInfoService.createNew(issue1, TicketType.MATCHING,
            Collections.singletonList(issue1Offer));
        repositoryMock.insertOffer(issue1Offer);

        Offer issue2Offer = OfferTestUtils.simpleOffer()
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
                .setTrackerTicket(issue2.getKey())
                .setTicketCritical(true)
                .setTicketDeadline(LocalDate.now());
        var ticket2 = processingTicketInfoService.createNew(issue2, TicketType.MATCHING,
            Collections.singletonList(issue2Offer));
        repositoryMock.insertOffer(issue2Offer);

        executor.execute();

        Mockito.verify(repositoryMock, Mockito.times(1))
                .updateOffers(Mockito.anyCollection());
        Assertions.assertThat(repositoryMock.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
                .extracting(Offer::getTicketCritical)
                .containsExactly(true, false);
        Assertions.assertThat(processingTicketInfoService.getById(ticket1.getId()))
                .extracting(p -> p.getDeadline().toString(), p -> p.getComputedDeadline().toString(),
                        ProcessingTicketInfo::getCritical)
                .containsExactly(deadline.toString(), deadline.toString(), true);
        Assertions.assertThat(processingTicketInfoService.getById(ticket2.getId()))
                .extracting(p -> p.getDeadline().toString(), p -> p.getComputedDeadline().toString(),
                        ProcessingTicketInfo::getCritical)
                .containsExactly(deadline.toString(), deadline.toString(), false);
    }

    @Test
    public void shouldUpdateProcessingTicketInfoIfNotConsistent() {
        org.joda.time.LocalDate jodaDeadline = org.joda.time.LocalDate.now();
        IssueMock issue1 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
            .setDeadline(Option.of(jodaDeadline))
            .setKey("T1")
            .setPriority("critical");
        IssueMock issue2 = ((IssueMock) trackerServiceMock.simpleIssue(TicketType.MATCHING))
            .setDeadline(Option.of(jodaDeadline))
            .setKey("T2");

        LocalDate deadline = LocalDate.parse(jodaDeadline.toString());

        Offer issue1Offer = OfferTestUtils.simpleOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
            .setTrackerTicket(issue1.getKey())
            .setTicketCritical(true)
            .setTicketDeadline(deadline);
        var ticket1 = processingTicketInfoService.createNew(issue1, TicketType.MATCHING,
            Collections.singletonList(issue1Offer));
        LocalDate incorrectDeadline = deadline.minusDays(10);
        processingTicketInfoService.update(ticket1.setDeadline(incorrectDeadline));
        repositoryMock.insertOffer(issue1Offer);

        Offer issue2Offer = OfferTestUtils.simpleOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
            .setTrackerTicket(issue2.getKey())
            .setTicketCritical(false)
            .setTicketDeadline(deadline);
        var ticket2 = processingTicketInfoService.createNew(issue2, TicketType.MATCHING,
            Collections.singletonList(issue2Offer));
        processingTicketInfoService.update(ticket2.setDeadline(incorrectDeadline));
        repositoryMock.insertOffer(issue2Offer);

        executor.execute();

        Mockito.verify(repositoryMock, Mockito.times(0))
            .updateOffers(Mockito.anyCollection());
        Assertions.assertThat(processingTicketInfoService.getById(ticket1.getId()))
            .extracting(p -> p.getDeadline().toString(), p -> p.getComputedDeadline().toString(),
                ProcessingTicketInfo::getCritical)
            .containsExactly(deadline.toString(), deadline.toString(), true);
        Assertions.assertThat(processingTicketInfoService.getById(ticket2.getId()))
            .extracting(p -> p.getDeadline().toString(), p -> p.getComputedDeadline().toString(),
                ProcessingTicketInfo::getCritical)
            .containsExactly(deadline.toString(), deadline.toString(), false);
    }
}

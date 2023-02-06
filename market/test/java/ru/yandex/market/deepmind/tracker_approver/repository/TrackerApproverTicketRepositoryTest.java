package ru.yandex.market.deepmind.tracker_approver.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;

import static ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository.Filter;

public class TrackerApproverTicketRepositoryTest extends BaseTrackerApproverTest {
    private static final String[] COMPARING_FIELDS = new String[]{"ticket", "type", "state"};

    @Test
    public void ticketsAreBeingFilteredByState() {
        //arrange
        ticketRepository.save(ticketStatus("1", "to_pending", TicketState.NEW));
        ticketRepository.save(ticketStatus("2", "to_inactive", TicketState.CLOSED));

        //act
        var filter = new Filter().setStates(TicketState.NEW);

        //assert that only new tickets will return
        Assertions.assertThat(ticketRepository.findByFilter(filter))
            .usingElementComparatorOnFields(COMPARING_FIELDS)
            .containsExactly(ticketStatus("1", "to_pending", TicketState.NEW));
    }

    @Test
    public void ticketsAreBeingFilteredByRetryCount() {
        //arrange
        ticketRepository.save(ticketStatus("1", "to_pending", TicketState.NEW, 0));
        ticketRepository.save(ticketStatus("2", "to_inactive", TicketState.CLOSED, 1));
        ticketRepository.save(ticketStatus("3", "to_pending", TicketState.CLOSED, 5));

        //act
        var filter = new Filter()
            .setRetryCountMoreThanExcluded(0)
            .setRetryCountLessThanExcluded(5);

        //assert that only new tickets will return
        Assertions.assertThat(ticketRepository.findByFilter(filter))
            .usingElementComparatorOnFields(COMPARING_FIELDS)
            .containsExactly(ticketStatus("2", "to_inactive", TicketState.CLOSED));
    }

    @Test
    public void ticketsAreBeingFilteredByTwoFilters() {
        ticketRepository.save(ticketStatus("1", "to_pending", TicketState.NEW, 0));
        ticketRepository.save(ticketStatus("2", "to_inactive", TicketState.CLOSED, 1));
        ticketRepository.save(ticketStatus("3", "to_pending", TicketState.CLOSED, 5));

        var newFilter = new Filter().setStates(List.of(TicketState.NEW));
        var retryCountFilter = new Filter()
            .setRetryCountMoreThanExcluded(0)
            .setRetryCountLessThanExcluded(5);

        //assert
        Assertions.assertThat(ticketRepository.findByFiltersUsingOr(retryCountFilter, newFilter))
            .usingElementComparatorOnFields(COMPARING_FIELDS)
            .containsExactlyInAnyOrder(
                ticketStatus("1", "to_pending", TicketState.NEW),
                ticketStatus("2", "to_inactive", TicketState.CLOSED)
            );
    }

    @Test
    public void filteringByTwoFiltersDoesntLeadsToDuplicates() {
        ticketRepository.save(ticketStatus("1", "to_pending", TicketState.NEW, 0));
        ticketRepository.save(ticketStatus("2", "to_inactive", TicketState.NEW, 1));

        var newFilter = new Filter().setStates(List.of(TicketState.NEW));
        var retryCountFilter = new Filter()
            .setRetryCountMoreThanExcluded(0)
            .setRetryCountLessThanExcluded(5);

        //assert
        Assertions.assertThat(ticketRepository.findByFiltersUsingOr(retryCountFilter, newFilter))
            .usingElementComparatorOnFields(COMPARING_FIELDS)
            .containsExactlyInAnyOrder(
                ticketStatus("1", "to_pending", TicketState.NEW),
                ticketStatus("2", "to_inactive", TicketState.NEW)
            );
    }

    private TrackerApproverTicketRawStatus ticketStatus(String ticket, String type, TicketState state, int retryCount) {
        return ticketStatus(ticket, type, state).setRetryCount(retryCount);
    }

    private TrackerApproverTicketRawStatus ticketStatus(String ticket, String type, TicketState state) {
        return new TrackerApproverTicketRawStatus(ticket, type, state);
    }
}

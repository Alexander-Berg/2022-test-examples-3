package ru.yandex.market.deepmind.common.hiding.ticket;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;

public class HidingTicketSskuStatusServiceTest extends BaseHidingTicketTest {
    private HidingTicketSskuStatusService hidingTicketSskuStatusService;
    private HidingTicketServiceMock hidingTicketService;

    @Before
    public void setUp() {
        hidingTicketService = new HidingTicketServiceMock();
        hidingTicketSskuStatusService = new HidingTicketSskuStatusService(
            hidingTicketSskuRepository,
            hidingTicketHistoryRepository,
            hidingTicketService
        );
    }

    @Test
    public void saveTicket() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("Reason", "TEST-1", 1, "sku1"),
            hidingTicketSsku("REASON", "TEST-2", 1, "sku1").setIsEffectivelyHidden(false),
            hidingTicketSsku("REASON", "TEST-2", 1, "sku2")
        );
        hidingTicketHistoryRepository.deleteAll();

        hidingTicketSskuStatusService.saveTicket("REASON", "TEST-3", List.of(
            new ServiceOfferKey(1, "sku1"),
            new ServiceOfferKey(1, "sku2"),
            new ServiceOfferKey(1, "sku3")
        ));

        Assertions.assertThat(hidingTicketSskuRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("Reason", "TEST-1", 1, "sku1"),
                hidingTicketSsku("REASON", "TEST-3", 1, "sku1"),
                hidingTicketSsku("REASON", "TEST-3", 1, "sku2"),
                hidingTicketSsku("REASON", "TEST-3", 1, "sku3")
            );

        Assertions.assertThat(hidingTicketHistoryRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                ticketHistory("REASON", "TEST-3", 1, "sku1"),
                ticketHistory("REASON", "TEST-3", 1, "sku2"),
                ticketHistory("REASON", "TEST-3", 1, "sku3")
            );
    }

    @Test
    public void filterSskusWithOpenTickets() {
        hidingTicketService.setOpenTickets("OPEN-1");

        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON1", "CLOSED-1", 1, "sku1"),
            hidingTicketSsku("REASON2", "OPEN-1", 1, "sku1"),

            hidingTicketSsku("REASON1", "CLOSED-1", 1, "sku2"),
            hidingTicketSsku("REASON2", "CLOSED-2", 1, "sku2")
        );

        var withoutTickets = hidingTicketSskuStatusService.filterSskusWithOpenTickets("REASON2",
            new ServiceOfferKey(1, "sku1"),
            new ServiceOfferKey(1, "sku2")
        );
        Assertions.assertThat(withoutTickets)
            .containsExactlyInAnyOrder(new ServiceOfferKey(1, "sku1"));
    }

    @Test
    public void filterSskusWithOpenTicketsWithOldReopenedOne() {
        hidingTicketService.setOpenTickets("REOPEN-1");

        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON1", "CLOSED-1", 2, "sku1"),
            hidingTicketSsku("REASON2", "REOPEN-1", 2, "sku1"),
            hidingTicketSsku("REASON3", "CLOSED-2", 2, "sku1")
        );

        var withoutTickets = hidingTicketSskuStatusService.filterSskusWithOpenTickets("REASON3",
            new ServiceOfferKey(2, "sku1")
        );
        Assertions.assertThat(withoutTickets).isEmpty();
    }

    @Test
    public void testMarkAsNotHidden() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("Reason", "TEST-1", 1, "sku1").setIsEffectivelyHidden(true),
            hidingTicketSsku("REASON", "TEST-2", 1, "sku1").setIsEffectivelyHidden(true),
            hidingTicketSsku("REASON", "TEST-2", 2, "sku2").setIsEffectivelyHidden(true)
        );

        hidingTicketSskuStatusService.markSskuEffectivelyNotHidden("REASON", List.of(
            new ServiceOfferKey(1, "sku1")
        ));

        Assertions.assertThat(hidingTicketSskuRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("Reason", "TEST-1", 1, "sku1").setIsEffectivelyHidden(true),
                hidingTicketSsku("REASON", "TEST-2", 1, "sku1").setIsEffectivelyHidden(false),
                hidingTicketSsku("REASON", "TEST-2", 2, "sku2").setIsEffectivelyHidden(true)
            );
    }

    @Test
    public void testMarkAsHidden() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON", "TEST-2", 1, "sku1").setIsEffectivelyHidden(false),
            hidingTicketSsku("REASON", "TEST-2", 2, "sku2").setIsEffectivelyHidden(true)
        );

        hidingTicketSskuStatusService.markSskuEffectivelyHidden("REASON", List.of(
            new ServiceOfferKey(1, "sku1"),
            new ServiceOfferKey(1050, "1050"))
        );

        Assertions.assertThat(hidingTicketSskuRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku", "isEffectivelyHidden")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON", "TEST-2", 1, "sku1").setIsEffectivelyHidden(true),
                hidingTicketSsku("REASON", "TEST-2", 2, "sku2").setIsEffectivelyHidden(true)
            );
    }
}

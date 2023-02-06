package ru.yandex.market.deepmind.common.hiding.ticket;

import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffService;
import ru.yandex.market.deepmind.common.hiding.diff.HidingDiffServiceImpl;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;

/**
 * Тесты {@link HidingTicketStrategy}, которые стараются не использовать моки.
 * И проверяют кейсы, когда тикет переезжает из одной очереди в другую
 */
public class HidingTicketStrategyMovingTicketsTest extends BaseHidingTicketTest {
    private HidingTicketServiceMock hidingTicketService;
    private HidingDiffService hidingDiffService;
    private HidingTicketSskuStatusService hidingTicketSskuStatusService;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;

    private HidingTicketStrategy hidingTicketStrategy;

    @Before
    public void setUp() {
        hidingTicketService = new HidingTicketServiceMock();
        var categoryManagerTeamService = new CategoryManagerTeamService(deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository, categoryCachingServiceMock);
        hidingDiffService = new HidingDiffServiceImpl(jdbcTemplate, categoryManagerTeamService);
        hidingTicketSskuStatusService = new HidingTicketSskuStatusService(
            hidingTicketSskuRepository,
            hidingTicketHistoryRepository,
            hidingTicketService
        );
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

        hidingTicketProcessingRepository.save(new HidingTicketProcessing().setReasonKey("REASON_s").setEnabled(true));
    }

    @Test
    public void findAndUpdateMovedTickets() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "sku11"),
            hidingTicketSsku("REASON_s", "TEST-1", 1, "sku12"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "sku21"),
            hidingTicketSsku("REASON_s", "TEST-3", 3, "sku31")
        );
        hidingTicketService.setMovedTickets(Map.of(
            "TEST-1", "NEWTEST-1",
            "TEST-3", "NEWTEST-3"
        ));
        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3", "TEST-4", "TEST-5");

        hidingTicketStrategy.findAndUpdateMovedTickets();

        var all = hidingTicketSskuRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "NEWTEST-1", 1, "sku11"),
                hidingTicketSsku("REASON_s", "NEWTEST-1", 1, "sku12"),
                hidingTicketSsku("REASON_s", "TEST-2", 2, "sku21"),
                hidingTicketSsku("REASON_s", "NEWTEST-3", 3, "sku31")
            );
    }

    @Test
    public void findAndUpdateMovedTicketsAlsoUpdatesHistory() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "sku11")
        );
        hidingTicketHistoryRepository.save(
            ticketHistory("REASON_s", "TEST-1", 1, "sku11"),
            ticketHistory("REASON_s", "TEST-2", 1, "sku11"),
            ticketHistory("REASON_s", "TEST-3", 1, "sku11")
        );

        hidingTicketService.setMovedTickets(Map.of(
            "TEST-1", "NEWTEST-1",
            "TEST-3", "NEWTEST-3"
        ));
        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3", "TEST-4", "TEST-5");

        hidingTicketStrategy.findAndUpdateMovedTickets();

        Assertions.assertThat(hidingTicketSskuRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                hidingTicketSsku("REASON_s", "NEWTEST-1", 1, "sku11")
            );
        Assertions.assertThat(hidingTicketHistoryRepository.findAll())
            .usingElementComparatorOnFields("reasonKey", "ticket", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                ticketHistory("REASON_s", "NEWTEST-1", 1, "sku11"),
                ticketHistory("REASON_s", "TEST-2", 1, "sku11"),
                // TEST-3 не будет обновлен, так как обновляются только тикеты из hidingTicketSskuRepository
                // Это не страшно, так как трекер все равно средиректит на новый тикет
                ticketHistory("REASON_s", "TEST-3", 1, "sku11")
            );
    }

    @Test
    public void closeTicketsWithoutSskus() {
        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3");

        hidingTicketStrategy.closeTicketsWithoutSskus();

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets).isEmpty();
    }

    @Test
    public void closeTicketsWithoutSskusWithRecordsInDb() {
        hidingTicketService.setOpenTickets("TEST-1", "TEST-2", "TEST-3");
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21").setIsEffectivelyHidden(false)
        );

        hidingTicketStrategy.closeTicketsWithoutSskus();

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets)
            .containsExactlyInAnyOrder("TEST-1", "TEST-2"); // не закрываем тикеты, по которым есть строки в БД
    }

    @Test
    public void closeTicketsWithoutSskusWithMovedTickets() {
        hidingTicketSskuRepository.save(
            hidingTicketSsku("REASON_s", "TEST-1", 1, "ssku11"),
            hidingTicketSsku("REASON_s", "TEST-2", 2, "ssku21").setIsEffectivelyHidden(false)
        );
        hidingTicketService.setMovedTickets(Map.of(
            "TEST-1", "NEWTEST-1",
            "TEST-2", "NEWTEST-2",
            "TEST-3", "NEWTEST-3"
        ));
        hidingTicketService.setOpenTickets("NEWTEST-1", "NEWTEST-2", "NEWTEST-3");

        hidingTicketStrategy.closeTicketsWithoutSskus();

        var openTickets = hidingTicketService.findOpenTicketsByCurrentUser();
        Assertions.assertThat(openTickets)
            .containsExactlyInAnyOrder("NEWTEST-1", "NEWTEST-2"); // не закрываем тикеты, по которым есть строки в БД
    }
}

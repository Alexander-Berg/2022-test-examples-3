package ru.yandex.market.abo.core.premod;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodProblem;
import ru.yandex.market.abo.core.premod.model.PremodProblemTypeId;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.service.PremodRepo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 */
public class PremodProblemServiceTest extends EmptyTest {

    @Autowired
    private PremodProblemService problemService;
    @Autowired
    private PremodRepo.PremodItemRepo premodItemRepo;
    @Autowired
    private PremodRepo.PremodTicketRepo premodTicketRepo;

    @Test
    public void crud() {
        PremodTicket ticket = new PremodTicket(0, 0, PremodCheckType.CPC_PREMODERATION);
        premodTicketRepo.save(ticket);
        PremodItem item = new PremodItem(ticket.getId(), PremodItemStatus.NEWBORN, PremodItemType.SHOP_INFO_COLLECTED);
        premodItemRepo.save(item);

        final long itemId = item.getId();
        final int problemType = PremodProblemTypeId.CANNOT_PLACE_CURSED;
        PremodProblem p = new PremodProblem(itemId, problemType, 0, null);
        problemService.createPremodProblem(p);

        assertFalse(problemService.loadPremodProblemsByItem(itemId).isEmpty());

        assertTrue(problemService.cannotPlaceProblemsExist(itemId));

        problemService.deleteByItemAndTypes(itemId, List.of(problemType));
        assertTrue(problemService.loadPremodProblemsByItem(itemId).isEmpty());

        // пустой itemId без ошибки
        problemService.deleteByItem(itemId);
    }
}

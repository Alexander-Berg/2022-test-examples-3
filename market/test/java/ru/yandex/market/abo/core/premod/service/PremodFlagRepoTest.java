package ru.yandex.market.abo.core.premod.service;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodFlag;
import ru.yandex.market.abo.core.premod.model.PremodFlagName;
import ru.yandex.market.abo.core.premod.model.PremodTicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author agavrikov
 * @date 20.03.18
 */
public class PremodFlagRepoTest extends EmptyTest {

    @Autowired
    private PremodFlagRepo premodFlagRepo;
    @Autowired
    private PremodRepo.PremodTicketRepo premodTicketRepo;

    @Test
    public void testRepo() {
        PremodFlag premodFlag = initPremodFlag();
        premodFlagRepo.findAll();

        PremodFlag dbPremodFlag = premodFlagRepo.findByIdOrNull(premodFlag.getId());
        assertEquals(premodFlag.getFlagName(), dbPremodFlag.getFlagName());
        assertEquals(premodFlag.getTicketId(), dbPremodFlag.getTicketId());
    }

    @Test
    public void testSearch() {
        PremodFlag flag = initPremodFlag();

        assertFalse(premodFlagRepo.findAllByTicketIdInAndFlagName(
                Arrays.asList(flag.getTicketId()), PremodFlagName.OFFERS_CREATED).isEmpty());

        assertTrue(premodFlagRepo.findAllByTicketIdInAndFlagName(
                Arrays.asList(flag.getTicketId() + 1), PremodFlagName.OFFERS_CREATED).isEmpty());
    }

    private PremodFlag initPremodFlag() {
        PremodTicket ticket = new PremodTicket(0, 0, PremodCheckType.CPC_PREMODERATION);
        premodTicketRepo.save(ticket);

        PremodFlag premodFlag = new PremodFlag(PremodFlagName.OFFERS_CREATED, ticket.getId());
        premodFlagRepo.save(premodFlag);

        return premodFlag;
    }
}

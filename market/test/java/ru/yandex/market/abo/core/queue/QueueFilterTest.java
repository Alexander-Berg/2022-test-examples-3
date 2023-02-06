package ru.yandex.market.abo.core.queue;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.queue.entity.QueueGenUser;
import ru.yandex.market.abo.core.queue.entity.QueueRegion;
import ru.yandex.market.abo.core.queue.entity.QueueRestriction;
import ru.yandex.market.abo.core.queue.entity.QueueTicket;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov.
 */
public class QueueFilterTest extends EmptyTest {

    private static final int GEN_ID = 2;
    private static final long USER_ID = -1L;
    private static final long TICKET_ID = 1;
    private static final long SHOP_ID = 774L;
    private static final long REGION_ID = Regions.RUSSIA;

    @Autowired
    QueueFilter queueFilter;

    @Autowired
    Queue.GenUserRepository queueGenUserRepository;

    @Autowired
    Queue.RegionRepository queueRegionRepository;

    @AfterEach
    public void tearDown() {
        queueGenUserRepository.deleteAll();
        queueRegionRepository.deleteAll();

        queueFilter.genCache.refresh(GEN_ID);
        queueFilter.userCache.refresh(USER_ID);
        queueFilter.regionCache.refresh(USER_ID);
    }

    @Test
    public void testGeneratorRestrictions() {
        queueGenUserRepository.save(new QueueGenUser(USER_ID, GEN_ID, QueueRestriction.GEN));
        queueFilter.genCache.refresh(GEN_ID);

        QueueTicket queueTicket = new QueueTicket(TICKET_ID, GEN_ID, SHOP_ID, CheckMethod.DEFAULT, REGION_ID);
        assertTrue(queueFilter.check(queueTicket, USER_ID, Collections.emptyList()));

        // Генератор могут проверять только избранные
        assertFalse(queueFilter.check(queueTicket, USER_ID + 1, Collections.emptyList()));
    }

    @Test
    public void userRestrictions() {
        queueGenUserRepository.save(new QueueGenUser(USER_ID, GEN_ID, QueueRestriction.USER));
        queueFilter.userCache.refresh(USER_ID);

        QueueTicket queueTicket = new QueueTicket(TICKET_ID, GEN_ID, SHOP_ID, CheckMethod.DEFAULT, REGION_ID);
        assertTrue(queueFilter.check(queueTicket, USER_ID, Collections.emptyList()));

        // Данный пользователь может проверять только этот генератор
        queueTicket.setGeneratorId(GEN_ID + 1);
        assertFalse(queueFilter.check(queueTicket, USER_ID, Collections.emptyList()));
    }

    @Test
    public void testRegionRestrictions() {
        addRegion(Regions.RUSSIA); // Russia
        addRegion(QueueRegion.ALL);

        QueueTicket queueTicket = new QueueTicket(TICKET_ID, -1, SHOP_ID, CheckMethod.DEFAULT, 187L); // Ukraine
        assertFalse(queueFilter.check(queueTicket, USER_ID, Collections.emptyList()));

        queueTicket.setCountryRegionId(109371L); // Шэньчжэнь
        assertTrue(queueFilter.check(queueTicket, USER_ID, Collections.emptyList()));
    }

    private void addRegion(long regionId) {
        queueRegionRepository.save(new QueueRegion(USER_ID, regionId));
        queueFilter.regionCache.refresh(USER_ID);
    }

    @Test
    public void regionRestrictions() {
        QueueTicket queueTicket = new QueueTicket(TICKET_ID, -1, SHOP_ID, CheckMethod.DEFAULT, REGION_ID);
        assertFalse(queueFilter.check(queueTicket, USER_ID, Collections.singletonList(SHOP_ID)));
    }
}

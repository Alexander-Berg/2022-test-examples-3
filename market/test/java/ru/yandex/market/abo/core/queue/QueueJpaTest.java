package ru.yandex.market.abo.core.queue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.queue.entity.QueueGenUser;
import ru.yandex.market.abo.core.queue.entity.QueueRestriction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author imelnikov
 */
public class QueueJpaTest extends EmptyTest {

    @Autowired
    Queue.GenUserRepository queueGenUserRepository;

    long USER_ID = -1;
    int genId = 1;

    @Test
    public void testRestrictions() {
        queueGenUserRepository.save(new QueueGenUser(USER_ID, genId, QueueRestriction.GEN));
        queueGenUserRepository.save(new QueueGenUser(USER_ID + 1, genId, QueueRestriction.USER));

        assertEquals(1, queueGenUserRepository.findAllByRestriction(QueueRestriction.USER).size());
        assertEquals(1, queueGenUserRepository.findAllByRestriction(QueueRestriction.GEN).size());
    }
}

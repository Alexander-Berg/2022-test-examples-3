package ru.yandex.market.pers.basket.service;

import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.UserIdType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author ifilippov5
 */
public class BasketServiceTest extends PersBasketTest {

    @Autowired
    private BasketService basketv2Service;

    @Test
    public void testUsers() {
        Random rand = new Random();
        for (UserIdType type : UserIdType.values()) {
            if (type == UserIdType.SESSION || type == UserIdType.OWNER_ID) {
                continue;
            }
            UserIdType userIdType = type;
            String userAnyId = String.valueOf(rand.nextLong());
            BasketOwner owner = BasketOwner.from(userIdType, userAnyId);
            Long ownerId = basketv2Service.getOrAddOwnerId(owner);
            assertTrue(ownerId > 0);
            userIdType = UserIdType.OWNER_ID;
            userAnyId = String.valueOf(ownerId);
            owner = BasketOwner.from(userIdType, userAnyId);
            assertEquals(ownerId, owner.getId());
        }
    }

}

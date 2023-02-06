package ru.yandex.market.core.message.dao;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.message.dao.impl.LastMessageDaoImpl;
import ru.yandex.market.core.message.model.LastShopMessage;
import ru.yandex.market.core.message.model.LastShopUserMessage;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "LastMessageDaoImplFunctionalTest.before.csv")
class LastMessageDaoImplFunctionalTest extends FunctionalTest {
    private static final long NOTIFICATION_TYPE = 1526180601L;

    @Autowired
    LastMessageDaoImpl lastMessageDao;

    @Test
    @DbUnitDataSet(after = "LastMessageDaoImplFunctionalTest.saveLastShopMessage.after.csv")
    void saveLastShopMessage() {
        var messageId = 100500L;
        lastMessageDao.saveLastShopMessage(new LastShopUserMessage(messageId, null, null, NOTIFICATION_TYPE));
        lastMessageDao.saveLastShopMessage(new LastShopUserMessage(messageId, 100L, null, NOTIFICATION_TYPE));
        lastMessageDao.saveLastShopMessage(new LastShopUserMessage(messageId, null, 200L, NOTIFICATION_TYPE));
        lastMessageDao.saveLastShopMessage(new LastShopUserMessage(messageId, 100L, 200L, NOTIFICATION_TYPE));
    }

    @Test
    void getLastMessagesForShops() {
        var lastMessagesForShops = lastMessageDao.getLastMessagesForShops(Set.of(100L, 200L, 300L, 400L, 500L));
        assertThat(lastMessagesForShops).isEqualTo(Map.of(
                100L, Set.of(new LastShopMessage(1L, NOTIFICATION_TYPE)),
                200L, Set.of(new LastShopMessage(2L, NOTIFICATION_TYPE)),
                300L, Set.of(new LastShopMessage(3L, NOTIFICATION_TYPE))
        ));
    }

    @Test
    @DbUnitDataSet(after = "LastMessageDaoImplFunctionalTest.updateUserLastReadMessage.after.csv")
    void updateUserLastReadMessage() {
        lastMessageDao.updateUserLastReadMessage(100L, 333L);
        lastMessageDao.updateUserLastReadMessage(200L, 333L);
    }
}

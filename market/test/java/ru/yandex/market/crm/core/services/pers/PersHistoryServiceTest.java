package ru.yandex.market.crm.core.services.pers;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.mapreduce.domain.user.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
class PersHistoryServiceTest {

    private static UserInfo userWithId(Uid node) {
        return new User("123")
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(node)
                );
    }

    @Test
    void testSelectCorrectYuids() {
        var yuid = Uid.asYuid("111222333");
        var selectedId = PersHistoryService.selectUserId(userWithId(yuid));

        assertTrue(selectedId.isPresent());
        assertEquals(yuid, selectedId.get());
    }

    @Test
    void testDoNotSelectInvalidYuid() {
        var yuid = Uid.asYuid("lvoevodinka@gmail.com");
        var selectedId = PersHistoryService.selectUserId(userWithId(yuid));

        assertTrue(selectedId.isEmpty());
    }
}

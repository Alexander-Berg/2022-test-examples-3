package ru.yandex.direct.core.testing.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.SMS_QUEUE;

public class TestSmsQueueRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Получить текст в очереди на отправку смс по id пользователя
     *
     * @param shard  шард
     * @param userId ID пользователя
     * @return пары вида номер кампании - список текстов смс
     */
    @QueryWithoutIndex("Только для тестов")
    public Map<Long, List<String>> getSmsQueueCampaignIdToText(int shard, long userId) {
        return dslContextProvider.ppc(shard)
                .select(SMS_QUEUE.CID, SMS_QUEUE.SMS_TEXT)
                .from(SMS_QUEUE)
                .where(SMS_QUEUE.UID.eq(userId))
                .fetchGroups(SMS_QUEUE.CID, SMS_QUEUE.SMS_TEXT);
    }
}

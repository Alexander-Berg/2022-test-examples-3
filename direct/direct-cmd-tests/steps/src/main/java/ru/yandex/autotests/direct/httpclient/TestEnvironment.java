package ru.yandex.autotests.direct.httpclient;

import java.util.concurrent.ConcurrentHashMap;

import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;

public class TestEnvironment {

    private static DirectJooqDbSteps newDbSteps;
    private static ConcurrentHashMap<Integer, DirectJooqDbSteps> shardedDbStepsCache = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Integer> shardsByLogin = new ConcurrentHashMap<>();

    public static DirectJooqDbSteps newDbSteps() {
        if (newDbSteps == null) {
            newDbSteps = new DirectJooqDbSteps();
        }
        return newDbSteps;
    }

    /**
     * Возвращает инстанс DirectJooqDbSteps настроенный для работы с объектами клиента ulogin
     * Для этого вычисляется шард, где клиент прописан (с использованием кеша), и берется
     * закешированный инстанс, смотрящий в этот шард.
     * Преимущество над newDbSteps() в том, что не нужно после вызова еще дополнительно указывать шард,
     * и в том, что newDbSteps() имеет только один кешированный инстанс на все вызовы функции. Т.е. если
     * вызвать ее один раз, выставить шард N, а потом вызвать еще раз и выставить шард M, оба объекта будут смотреть
     * на шард M.
     * Данная функция хранит отдельный кешированный инстанс для каждого шарда.
     *
     * @param ulogin
     * @return
     */
    public static DirectJooqDbSteps newDbSteps(String ulogin) {
        Integer shardNo = shardsByLogin.computeIfAbsent(ulogin, TestEnvironment::getShardByLogin);
        return shardedDbStepsCache.computeIfAbsent(shardNo, TestEnvironment::getDbStepsWithShard);
    }

    private static Integer getShardByLogin(String ulogin) {
        return newDbSteps().shardingSteps().getShardByLogin(ulogin);
    }

    private static DirectJooqDbSteps getDbStepsWithShard(int shardNo) {
        return new DirectJooqDbSteps().useShard(shardNo);
    }
}

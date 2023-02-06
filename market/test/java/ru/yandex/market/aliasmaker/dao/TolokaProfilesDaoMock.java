package ru.yandex.market.aliasmaker.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class TolokaProfilesDaoMock implements TolokaProfilesDao {
    private final Map<String, String> data = new HashMap<>();

    @Override
    public Map<String, String> loadWorkerIdsByLogins(Collection<String> logins) {
        return logins.stream()
                .filter(data::containsKey)
                .collect(Collectors.toMap(Function.identity(), data::get));
    }

    public void put(String login, String workerId) {
        data.put(login, workerId);
    }
}

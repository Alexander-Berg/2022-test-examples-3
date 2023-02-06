package ru.yandex.market.api.testing;

import java.util.List;

/**
 * Created by kudrale on 06.11.14.
 */
public interface PullApiErrorsDao {

    void addErrors(String path, List<Integer> errorCodes);

    Integer getAndRemoveError(String path);
}

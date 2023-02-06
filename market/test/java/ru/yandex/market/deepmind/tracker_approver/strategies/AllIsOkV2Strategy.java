package ru.yandex.market.deepmind.tracker_approver.strategies;

import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartResponse;

/**
 * Стратегия, которая всегда выполняет все успешно.
 */
public class AllIsOkV2Strategy extends AllIsOkStrategy {

    @Override
    public StartResponse<MyKey, MyMeta, MyMeta> start(StartRequest<MyKey, MyMeta, MyMeta> request) {
        return StartResponse.of("ALL_IS_OK_V2-1");
    }

    @Override
    public int getStrategyVersion() {
        return 2;
    }
}

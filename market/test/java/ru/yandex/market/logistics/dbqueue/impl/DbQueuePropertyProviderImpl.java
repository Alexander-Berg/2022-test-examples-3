package ru.yandex.market.logistics.dbqueue.impl;

import org.springframework.stereotype.Service;

@Service
public class DbQueuePropertyProviderImpl implements DbQueuePropertyProviderInterface {
    @Override
    public int getMaxNumberOfRowsInQueueLog() {
        return 1;
    }
}

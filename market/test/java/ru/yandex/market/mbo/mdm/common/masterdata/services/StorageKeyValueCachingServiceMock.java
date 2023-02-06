package ru.yandex.market.mbo.mdm.common.masterdata.services;

import javax.annotation.Nonnull;

import com.google.common.base.Ticker;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

/**
 * @author dmserebr
 * @date 13/05/2020
 */
public class StorageKeyValueCachingServiceMock extends StorageKeyValueCachingService {
    public StorageKeyValueCachingServiceMock() {
        super(new StorageKeyValueServiceMock());
    }

    public StorageKeyValueCachingServiceMock(StorageKeyValueService delegateService) {
        super(delegateService);
    }

    public StorageKeyValueCachingServiceMock(StorageKeyValueService delegateService, Ticker ticker) {
        super(delegateService, ticker);
    }

    public <T> void putValue(@Nonnull String key, T value) {
        delegateService.putValue(key, value);
    }
}
